### 9장. 퍼시스턴트 볼륨(PV)과 퍼시스턴트 볼륨 클레임(PVC)

> 정리 범위: PV/PVC 개념, 종류, 주의할 점 / hostPath, emptyDir (로컬 볼륨) / 네트워크 볼륨의 개념과 종류 / 다이나믹 프로비저닝의 개념과 동작 방식
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드 `docker-desktop`), 기본 StorageClass `hostpath` (프로비저너 `docker.io/hostpath`), kubectl v1.35.2
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 실험은 `t9` 네임스페이스와 이름에 `t9`가 들어간 PV/StorageClass만 썼고 끝나고 모두 지웠다 (노드의 임시 디렉터리 포함).
>
> **단일 노드 환경의 한계**: 노드가 하나뿐이라 "다른 노드로 Pod가 옮겨 갔을 때 볼륨이 따라가는가" 같은 멀티 노드 동작은 확인하지 못했다. 해당 부분은 문서 지식으로 표시했다.

```bash
k() { kubectl --context docker-desktop "$@"; }
# 노드(Docker Desktop VM) 안의 파일을 보는 방법
docker run --rm --privileged --pid=host alpine:3.20 nsenter -t 1 -m -- <명령>
```

#### 목차

1. PV와 PVC: 개념, 종류, 주의할 점
2. 로컬 볼륨: emptyDir, hostPath (그리고 local)
3. 네트워크 볼륨
4. 다이나믹 프로비저닝
5. 확인하지 못한 부분

---

#### 1. PV와 PVC: 개념, 종류, 주의할 점

##### 1-1. 왜 필요한가

컨테이너의 파일시스템은 컨테이너가 사라지면 같이 사라진다 (2장). Pod의 `volumes`로 볼륨을 붙일 수 있는데, 볼륨의 **수명이 Pod에 묶이는 종류**(emptyDir)가 있고, 실제 저장소(NFS 서버 주소, 클라우드 디스크 ID 등)를 Pod 정의에 직접 적으면 **개발자가 인프라 세부를 알아야 한다**. PV/PVC는 이 둘을 분리한다.

| 오브젝트 | 비유 | 누가 만드나 | 네임스페이스 |
|---|---|---|---|
| **PersistentVolume (PV)** | 실제 저장 공간 (공급) | 관리자, 또는 프로비저너가 자동 생성 | **없음** (클러스터 범위) |
| **PersistentVolumeClaim (PVC)** | "이만큼 주세요" 요청서 (수요) | 개발자 | **있음** |
| StorageClass | 저장소의 종류/등급과 만드는 방법 | 관리자 | 없음 (클러스터 범위) |

```
Pod ──volumes: persistentVolumeClaim──▶ PVC ──bind(1:1)──▶ PV ──▶ 실제 저장소(NFS, 디스크, 호스트 디렉터리 ...)
 (개발자: 크기/접근 방식만 요청)              (관리자: 실제 저장소 정의)
```

- Pod는 **PVC 이름만** 안다. PV가 NFS인지 디스크인지는 모른다.
- PVC는 네임스페이스에 속하고 PV는 속하지 않는다. `k get pv`는 `-n` 없이 전체가 나온다.

##### 1-2. PV 정의 (정적 프로비저닝)

```yaml
apiVersion: v1
kind: PersistentVolume
metadata: {name: t9-pv1}
spec:
  capacity: {storage: 1Gi}
  accessModes: [ReadWriteOnce]
  persistentVolumeReclaimPolicy: Retain
  storageClassName: manual
  hostPath: {path: /var/tmp/t9-pv1, type: DirectoryOrCreate}   # 실제 저장소 종류별 필드 (nfs, hostPath, local, csi ...)
```

| 필드 | 의미 |
|---|---|
| `capacity.storage` | 용량 |
| `accessModes` | 접근 방식 (아래 표) |
| `persistentVolumeReclaimPolicy` | PVC가 삭제된 뒤 PV를 어떻게 할지 (`Retain`, `Delete`) |
| `storageClassName` | 속한 클래스. PVC의 클래스와 같아야 바인딩 후보가 된다 |
| 종류별 블록 | `hostPath`, `nfs`, `local`, `csi` 등 실제 저장소 정보 |
| `nodeAffinity` | 이 볼륨에 접근할 수 있는 노드 제한 (local 등) |

**접근 방식(accessModes)**

| 모드 | 약어 | 의미 | 이 환경에서 확인 |
|---|---|---|---|
| ReadWriteOnce | RWO | **한 노드**가 읽기/쓰기로 마운트 (같은 노드의 여러 Pod는 가능) | hostPath/동적 PV에서 사용 |
| ReadOnlyMany | ROX | 여러 노드가 읽기 전용 | 확인 안 함 |
| ReadWriteMany | RWX | 여러 노드가 읽기/쓰기 | **NFS에서** Pod 2개가 동시에 쓰기 (3절) |
| ReadWriteOncePod | RWOP | **Pod 하나만** 읽기/쓰기 | 확인 안 함 |

- 접근 방식은 **저장소가 지원하는 능력을 선언**하는 것이고, 마운트 때 강제 검사를 하는 것이 아니다 (단일 노드라 RWO의 "노드 하나" 제한은 시험하지 못했다).
- RWO는 "Pod 하나"가 아니라 **"노드 하나"** 라는 점이 흔한 오해다. 같은 노드에 있는 Pod 여러 개는 RWO 볼륨을 함께 쓸 수 있다 (문서 설명이고 이 환경에서는 확인하지 않았다).

##### 1-3. PVC 정의와 바인딩 조건

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata: {name: pvc-a, namespace: t9}
spec:
  accessModes: [ReadWriteOnce]
  storageClassName: manual
  resources: {requests: {storage: 500Mi}}
```

PVC가 PV에 붙으려면 **크기, 접근 방식, 클래스**가 맞아야 한다. 1Gi / RWO / `manual` PV(`Available`)가 있는 상태에서 조건이 다른 PVC를 만들었다.

| PVC | 요청 | 결과 (실측) |
|---|---|---|
| `pvc-big` | 2Gi (PV는 1Gi) | **Pending** (크기가 모자람) |
| `pvc-rwx` | ReadWriteMany (PV는 RWO) | **Pending** (접근 방식 불일치) |
| `pvc-wrongclass` | 클래스 `other` | **Pending** (클래스 불일치) |
| `pvc-a` | 500Mi / RWO / `manual` | **Bound** → PV `t9-pv1` |

```
$ k -n t9 get pvc pvc-a ; k get pv t9-pv1
pvc-a    Bound   t9-pv1   1Gi   RWO   manual
t9-pv1   1Gi   RWO   Retain   Bound   t9/pvc-a   manual
$ k get pv t9-pv1 -o jsonpath='{.spec.claimRef}'
{"apiVersion":"v1","kind":"PersistentVolumeClaim","name":"pvc-a","namespace":"t9","uid":"9755...","resourceVersion":"..."}
```

- 500Mi를 요청했는데 **1Gi PV 전체가 바인딩되어 `CAPACITY`가 1Gi**로 보인다. PV는 쪼개서 일부만 줄 수 없다. 요청과 가장 가까운 PV를 고르지만 남는 용량은 낭비된다.
- 바인딩은 **PV 하나 ↔ PVC 하나, 1:1**이다. 이미 바인딩된 PV는 다른 PVC가 쓸 수 없고, PV의 `claimRef`가 어느 PVC와 묶였는지 기록한다.
- 이 세 Pending PVC의 이벤트는 `storageclass.storage.k8s.io "manual" not found` 같은 `ProvisioningFailed` 메시지만 남겼다 (이 클러스터에는 `manual`이라는 StorageClass 오브젝트가 없기 때문). 즉 **이벤트 메시지에 "크기가 모자라다/모드가 다르다"가 직접 적히지는 않아서**, 원인을 이벤트만 보고 알기 어려웠다. 위 표는 각 PVC를 하나씩 바꿔서 얻은 결과다.
- `StorageClass`가 없는 이름(`manual`, `other`)을 써도 **정적 바인딩은 클래스 이름이 같으면 된다**. `manual` PV와 `manual` PVC가 StorageClass 오브젝트 없이 바인딩됐다.

##### 1-4. PV의 생명주기

```
Available ──PVC가 바인딩──▶ Bound ──PVC 삭제──▶ Released ──reclaim 정책──▶ (Retain: 그대로 / Delete: PV 삭제)
```

| 상태 | 의미 |
|---|---|
| Available | 아직 어떤 PVC에도 묶이지 않음 |
| Bound | PVC와 묶임 |
| Released | 묶였던 PVC가 삭제됨. **아직 재사용 불가** (이전 데이터가 남아 있을 수 있어서) |
| Failed | 자동 회수 실패 |

**① PVC 보호 (`kubernetes.io/pvc-protection`)**: Pod가 쓰고 있는 PVC를 삭제하면 바로 사라지지 않는다.

```
$ k -n t9 get pvc pvc-a -o jsonpath='{.metadata.finalizers}'
["kubernetes.io/pvc-protection"]
$ k -n t9 delete pvc pvc-a --wait=false      # Pod(pv-user)가 사용 중
$ k -n t9 get pvc pvc-a
pvc-a   Terminating   t9-pv1   1Gi   RWO   manual
$ k get pv t9-pv1          →  Bound            # PV도 아직 Bound
$ k -n t9 delete pod pv-user                   # Pod 삭제 뒤에야
$ k -n t9 get pvc pvc-a    →  NotFound
$ k get pv t9-pv1          →  Retain  Released  t9/pvc-a
```

사용 중인 볼륨이 갑자기 사라지는 사고를 막는다 (finalizer가 Pod 사용이 끝날 때까지 삭제를 붙잡는다).

**② `Retain`: 데이터가 남는다.** PVC를 지운 뒤에도 노드의 데이터가 그대로다.

```
$ docker run ... nsenter -t 1 -m -- cat /var/tmp/t9-pv1/f.txt
data-v1
```

**③ Released PV는 새 PVC가 바로 쓸 수 없다.** 같은 조건의 새 PVC를 만들어도 `Pending`이었고 PV는 `Released`로 남았다. 재사용하려면 관리자가 직접 `claimRef`를 지워야 한다.

```
$ k patch pv t9-pv1 --type=json -p '[{"op":"remove","path":"/spec/claimRef"}]'
$ k get pv t9-pv1          →  Available          # 바로 Available로 돌아감
$ k -n t9 get pvc pvc-c    →  Bound after ~48s   # 대기 중이던 PVC가 약 48초 뒤에 바인딩
```

- `claimRef`를 지우면 PV가 `Available`로 돌아가고 대기 중인 PVC가 붙는다. 이전 데이터(`f.txt`)도 그대로 남아 있으므로 **이전 사용자의 데이터가 다음 사용자에게 노출될 수 있다.** 그래서 자동 재사용을 막는 것이다.
- 약 48초가 걸린 것은 이 클러스터의 kube-controller-manager가 `--pvclaimbinder-sync-period=60s`로 실행 중이기 때문으로 보인다 (6장에서 확인한 실행 옵션). 이벤트가 아니라 주기적 재확인으로 붙었다. 새 PVC를 만들 때는 몇 초 안에 바로 붙었다 (생성 이벤트로 처리).

**④ `Delete` 정책을 정적 hostPath PV에 적용하면**

```
$ k patch pv t9-pv1 -p '{"spec":{"persistentVolumeReclaimPolicy":"Delete"}}'
$ k -n t9 delete pvc pvc-c                  # 8초 뒤
$ k get pv t9-pv1                           →  NotFound      # PV 오브젝트가 삭제됨
$ docker run ... nsenter -t 1 -m -- ls /var/tmp/t9-pv1
f.txt                                       # 하지만 노드의 파일은 남아 있음
```

- PV 오브젝트는 사라졌지만 **hostPath 디렉터리의 파일은 지워지지 않았다.** 정리 방법(스토리지 쪽 삭제)이 없는 볼륨 종류에서는 `Delete`가 "오브젝트만 지우는" 결과가 될 수 있다. 4절의 동적 프로비저닝 PV는 디렉터리까지 지워졌다 (프로비저너가 삭제를 담당).
- 이 실험 중 한 번은 `Retain`으로 만든 PV 오브젝트가 PVC 삭제 이후 사라져 있었다. 같은 순서를 다시 해서 `Released`로 남는 것을 확인했으나 첫 번째 때 사라진 원인은 확인하지 못했다.

##### 1-5. reclaim 정책

| 정책 | 동작 | 기본값 |
|---|---|---|
| `Retain` | PV와 데이터를 남기고 `Released`. 관리자가 수동으로 정리 | **정적으로 만든 PV** |
| `Delete` | PV와 (가능하면) 실제 저장소를 삭제 | **동적 프로비저닝으로 만든 PV** (이 클러스터의 `hostpath` 클래스도 `Delete`) |
| `Recycle` | `rm -rf` 후 재사용 (폐기됨, 사용하지 않음) | - |

- 이 PV의 `RECLAIM POLICY` 열을 `k get pv`로 확인하는 습관이 필요하다. **동적 PV는 기본이 `Delete`라서 PVC를 지우면 데이터가 함께 사라진다** (4절에서 확인).

##### 1-6. 주의할 점 정리

| # | 주의할 점 | 근거 |
|---|---|---|
| 1 | **PVC의 `storageClassName`을 생략하면 기본 StorageClass가 붙는다.** 정적 PV를 쓰려고 했어도 동적 PV가 새로 만들어진다 | 아래 실측 |
| 2 | 동적 PV의 기본 reclaim은 `Delete`. PVC/네임스페이스를 지우면 데이터도 삭제 | 4절 실측 |
| 3 | **네임스페이스 삭제 → PVC 삭제 → (Delete 정책이면) PV와 데이터 삭제** | 아래 실측 |
| 4 | PVC를 지워도 PV가 `Released`로 남고, 재사용하려면 `claimRef`를 직접 지워야 한다 | 1-4 실측 |
| 5 | 사용 중인 PVC는 `Terminating`에서 대기 (finalizer) | 1-4 실측 |
| 6 | PV의 `capacity`가 **강제되지 않는 종류**가 있다 (hostPath 계열) | 아래 실측 |
| 7 | 노드에 묶이는 볼륨(hostPath, local)은 Pod가 다른 노드로 가면 접근 불가 | 문서 지식 (단일 노드라 확인 불가) |
| 8 | 용량 **축소는 불가**, 확장은 클래스가 허용하고 프로비저너가 지원할 때만 | 4절 실측 |
| 9 | RWO는 노드 단위. 여러 노드의 Pod가 같은 RWO 볼륨을 쓰면 문제 | 문서 지식 |
| 10 | 단순 이름 일치만으로 바인딩되므로 클래스 이름의 오타가 `Pending`으로 이어진다 | 1-3 실측 |

**주의 1: 클래스 생략 vs 빈 문자열**

정적 PV(`storageClassName: ""`)를 만들고 `storageClassName`을 **생략한** PVC와 **`""`로 명시한** PVC를 만들었다.

```
$ k -n t9 get pvc omit nodyn
omit    Bound   pvc-86d6dee4-...   hostpath            # 생략 → 기본 클래스 hostpath가 붙어 새 PV가 동적으로 생성됨
nodyn   Bound   t9-pv-nocls        ""                  # "" 명시 → 동적 프로비저닝 안 함, 정적 PV에 바인딩
$ k -n t9 get events ... involvedObject.name=nodyn      # 정적 PV가 생기기 전
FailedBinding  no persistent volumes available for this claim and no storage class is set
```

- 생략하면 **기본 StorageClass가 자동으로 지정**된다 (어드미션이 채움). 그 결과 만들어 둔 정적 PV가 있어도 쓰지 않고 새 PV를 만든다. 정적 PV에 붙이려면 `storageClassName`을 명시해야 한다.
- `""`(빈 문자열)은 "기본 클래스도 쓰지 않는다"는 뜻이다. 맞는 PV가 없으면 Pending이고, 정적 PV가 생기면 붙는다.

**주의 3: 네임스페이스 삭제**

`t9` 네임스페이스를 지웠더니 동적 PV(`dyn`, `imm`, `omit`)는 PV째 사라졌고, 클래스 `t9-wffc`(Retain)로 만든 PV(`late`)는 `Released`로 남았다.

```
$ k delete ns t9
$ k get pv
pvc-652cd275-...   Retain   Released   t9-wffc       # Retain 클래스로 만든 것만 남음
t9-local-pv  ... Released ,  t9-nfs ... Released,  t9-pv-nocls ... Released   # 내가 만든 정적 PV들 (Retain)
```

**주의 6: 용량이 강제되지 않는다**

500Mi를 요청해 1Gi PV(hostPath)를 붙였지만 컨테이너 안에서 본 크기는 용량 제한이 아니라 노드 파일시스템 크기였다.

```
$ k -n t9 exec pv-user -- df /d
tmpfs     4012576   64   4012512   0%  /d      # 노드(VM) 파일시스템 크기. 1Gi도 500Mi도 아님
```

hostPath/local 계열은 `capacity`가 바인딩 매칭용 **표시 값**일 뿐 쿼터가 아니다. (이 노드의 `/var/tmp`가 `tmpfs`로 마운트된 것도 위 출력에서 보인다. 아래 hostPath 절 참고.) 클라우드 디스크나 CSI 볼륨은 실제 크기로 만들어지는 것이 일반적이지만 이 환경에서 확인하지는 않았다.

#### 2. 로컬 볼륨: emptyDir, hostPath (그리고 local)

로컬 볼륨은 **Pod가 실행되는 노드의 저장소**를 쓰는 볼륨이다. 네트워크를 거치지 않으므로 빠르지만 **노드를 벗어나면 데이터가 따라가지 않는다.**

##### 2-1. emptyDir

**Pod가 노드에 배정될 때 빈 디렉터리로 만들어지고, Pod가 삭제되면 함께 삭제되는 임시 볼륨.** Pod 안의 컨테이너끼리 파일을 공유하거나 임시 작업 공간(캐시, 정렬 파일)으로 쓴다.

```yaml
spec:
  containers:
  - name: a
    image: alpine:3.20
    command: ["sh","-c","echo from-a > /data/a.txt; sleep 3600"]
    volumeMounts: [{name: disk, mountPath: /data}, {name: mem, mountPath: /mem}, {name: lim, mountPath: /lim}]
  - name: b
    image: alpine:3.20
    volumeMounts: [{name: disk, mountPath: /shared}]
  volumes:
  - name: disk
    emptyDir: {}                                    # 노드 디스크
  - name: mem
    emptyDir: {medium: Memory, sizeLimit: 64Mi}     # tmpfs (메모리)
  - name: lim
    emptyDir: {sizeLimit: 10Mi}                     # 노드 디스크 + 용량 제한
```

**컨테이너 간 공유**

```
$ k -n t9 exec ed -c b -- cat /shared/a.txt
from-a                                              # a가 쓴 파일을 b가 읽음
```

**실제 저장 위치와 종류**

```
$ k -n t9 exec ed -c a -- mount | grep -E " /data | /mem | /lim "
/dev/vda1 on /data type ext4 (rw,relatime,discard)           # 노드 디스크
tmpfs     on /mem  type tmpfs (rw,relatime,size=65536k,noswap)  # medium: Memory → 64Mi tmpfs
/dev/vda1 on /lim  type ext4 (rw,relatime,discard)

$ docker inspect -f '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}' <컨테이너>
/var/lib/kubelet/pods/426c8026-.../volumes/kubernetes.io~empty-dir/disk -> /data
/var/lib/kubelet/pods/426c8026-.../volumes/kubernetes.io~empty-dir/mem  -> /mem
/var/lib/kubelet/pods/426c8026-.../volumes/kubernetes.io~empty-dir/lim  -> /lim
```

- 기본 emptyDir은 **노드의 `/var/lib/kubelet/pods/<pod uid>/volumes/kubernetes.io~empty-dir/<볼륨 이름>`** 디렉터리를 컨테이너에 바인드 마운트한 것이다.
- `medium: Memory`는 **tmpfs**다. `sizeLimit: 64Mi`가 tmpfs 크기(`size=65536k`)로 적용되었다.

**수명: 컨테이너 재시작에는 살아남고, Pod 삭제에는 사라진다**

```
$ k -n t9 exec ed -c a -- sh -c 'echo persist > /data/p.txt'
$ docker kill <컨테이너 a>                            # 컨테이너만 죽임
$ k -n t9 get pod ed -o jsonpath='{...restartCount}'    →  1
$ k -n t9 exec ed -c a -- ls /data                      →  a.txt p.txt       # 재시작 후에도 파일이 있음

$ k -n t9 delete pod ed ; k apply -f ed.yaml            # Pod를 지우고 다시 생성
$ k -n t9 exec ed -c a -- ls /data                      →  a.txt              # p.txt는 사라짐 (a.txt는 컨테이너가 다시 만든 것)
```

**`sizeLimit`: 쓰는 순간이 아니라 나중에 퇴출(Evict)한다**

`/lim`(sizeLimit 10Mi)에 20MB를 썼다.

```
$ k -n t9 exec ed -c a -- dd if=/dev/zero of=/lim/big bs=1M count=20     # 쓰기는 성공 (20971520 bytes)
(41초 뒤)
$ k -n t9 get pod ed
ed   0/2   Error   0   47s
$ k -n t9 get pod ed -o jsonpath='{.status.reason}: {.status.message}'
Evicted: Usage of EmptyDir volume "lim" exceeds the limit "10Mi".
```

- **쓰기가 막히지 않는다.** kubelet이 주기적으로 사용량을 점검하다가 한도를 넘은 Pod를 **통째로 퇴출**했다 (이 실험에서는 41초 뒤). 즉 `sizeLimit`은 쿼터(디스크 용량 제한)가 아니라 **초과 시 Pod 퇴출 조건**이다. Pod가 죽는다는 점이 중요하다.

**`medium: Memory`는 컨테이너 메모리로 계산된다**

컨테이너 `memory: 64Mi` 제한 + `emptyDir: {medium: Memory}`에 100MB를 쓰게 했다.

```
$ k -n t9 logs memvol
Killed          # dd가 메모리 한도 초과로 종료 (OOM)
written         # 쉘은 살아남아 다음 명령 실행
```

- tmpfs에 쓴 데이터는 메모리를 쓰므로 **컨테이너의 메모리 한도에 포함**되어, 한도를 넘자 쓰던 프로세스가 `Killed` 되었다. 메모리 볼륨은 한도와 `sizeLimit`을 함께 고려해야 한다 (OOM이 일어난 정확한 cgroup 경로까지는 확인하지 않았다).

**용도 정리**: 컨테이너 간 파일 교환(사이드카), 임시 캐시/스크래치, 체크포인트처럼 "Pod가 살아있는 동안만" 필요한 데이터. **영속 데이터에는 쓰지 않는다.**

##### 2-2. hostPath

**노드(호스트)의 파일/디렉터리를 Pod에 그대로 마운트하는 볼륨.** 도커의 `-v /host/path:/container/path` 바인드 마운트(2장)에 해당한다.

```yaml
volumes:
- name: h
  hostPath: {path: /var/tmp/t9-hp, type: DirectoryOrCreate}
```

**데이터가 노드에 남는다**

```
$ k -n t9 exec hp1 -- sh -c 'cat /h/f.txt; mount | grep " /h "'
hello-hp
tmpfs on /h type tmpfs (rw,nosuid,nodev,noexec,relatime,size=4012576k,mode=755)
$ k -n t9 delete pod hp1
$ docker run ... nsenter -t 1 -m -- cat /var/tmp/t9-hp/f.txt
hello-hp                                  # Pod가 없어도 노드에 남음
$ k -n t9 logs hp2                         # 새 Pod가 같은 hostPath를 마운트
hello-hp                                  # 이전 Pod의 파일을 그대로 봄
```

- Pod를 지워도 노드에 데이터가 남고 같은 경로를 마운트한 다른 Pod가 이어서 쓸 수 있다 (emptyDir과 다른 점).
- **여기서 `hostPath`의 "호스트"는 Mac이 아니라 Docker Desktop VM이다.** Mac에서 `ls /var/tmp/t9-hp`는 `No such file or directory`였다. 노드가 VM 안에 있어서, 노드의 파일시스템이 VM의 파일시스템이다.
- 이 VM의 `/var/tmp`는 `tmpfs`(위 `mount` 출력)다. **VM이 재시작되면 사라질 것으로 보이나 재시작은 시험하지 않았다.**

**`type` 검사**

| type | 의미 |
|---|---|
| (생략) | 검사 없음 (기본) |
| `DirectoryOrCreate` / `FileOrCreate` | 없으면 만든다 (권한 0755 / 0644, 소유자 root) |
| `Directory` / `File` | 이미 있어야 하고 종류가 맞아야 한다 |
| `Socket`, `CharDevice`, `BlockDevice` | 해당 종류여야 한다 |

```
$ k apply ... hostPath: {path: /var/tmp/nope, type: Directory}      # 경로가 없음
Warning FailedMount  MountVolume.SetUp failed for volume "h" : hostPath type check failed: /var/tmp/nope is not a directory
$ k apply ... hostPath: {path: /var/tmp/t9-hp, type: File}          # 디렉터리인데 File로 지정
Warning FailedMount  ... hostPath type check failed: /var/tmp/t9-hp is not a file
```

Pod는 `ContainerCreating`에 머문다. 메시지가 "is not a directory/file"이라서, 없는 경로도 같은 표현으로 나온다는 점에 주의.

**위험성: 노드 전체가 노출될 수 있다**

`hostPath: {path: /}`를 읽기 전용으로 마운트하는 Pod를 만들었다.

```
$ k -n t9 logs hp5
bin boot bpf-legacy.o bpf.o containers dev dpkg.orig etc home host-network.o host_mnt init lib media mmap.o mnt ...
checkpoints config.yaml cpu_manager_state device-plugins kubeadm-flags.env       # /var/lib/kubelet 내용
```

- 아무 제한 없이 노드의 루트 파일시스템과 `/var/lib/kubelet`(다른 Pod의 볼륨, 토큰)이 보였다. 쓰기 가능하면 노드 설정 변경, 다른 Pod 데이터 접근, 컨테이너 탈출 경로가 된다. **일반 애플리케이션에는 쓰지 말고** 노드 로그 수집기, 모니터링 에이전트(`/var/log`, `/proc`, `/sys`) 같은 시스템 컴포넌트에 한정한다. 실무에서는 Pod Security Admission 같은 어드미션 정책으로 hostPath를 막는 것이 일반적이다 (문서 지식. 이 환경에서는 제한 없이 마운트되었다).

**hostPath의 문제점**

| 문제 | 설명 |
|---|---|
| 노드 종속 | Pod가 다른 노드에 뜨면 데이터가 없다 (멀티 노드 확인은 못 함) |
| 보안 | 호스트 파일시스템 노출 |
| 권한 | 만들어진 디렉터리 소유자가 root라서 비-root 컨테이너가 쓰기 못 할 수 있다 |
| 이식성 | 노드 경로를 가정하므로 클러스터마다 다르다 (이 환경은 VM 경로) |
| 용량 | 용량 제한/격리 없음 (1-6의 주의 6) |

##### 2-3. local 볼륨 (hostPath의 보완)

`local`은 **PV로 쓰는 노드 로컬 저장소**다. hostPath와 달리 **`nodeAffinity`가 필수**이고, 스케줄러가 이를 알고 Pod를 데이터가 있는 노드에 배치한다.

```yaml
kind: StorageClass
metadata: {name: t9-local}
provisioner: kubernetes.io/no-provisioner          # 동적 생성 없음
volumeBindingMode: WaitForFirstConsumer
---
kind: PersistentVolume
spec:
  capacity: {storage: 1Gi}
  accessModes: [ReadWriteOnce]
  storageClassName: t9-local
  local: {path: /var/tmp/t9-local}
  nodeAffinity:
    required:
      nodeSelectorTerms:
      - matchExpressions: [{key: kubernetes.io/hostname, operator: In, values: [docker-desktop]}]
```

```
$ k -n t9 get pvc loc            # Pod를 만들기 전
loc   Pending                    # event: waiting for first consumer to be created before binding
$ (Pod 생성 후)
loc   Bound   t9-local-pv
```

- `WaitForFirstConsumer`라서 PVC는 **Pod가 생기기 전까지 Pending**이었고, Pod가 스케줄된 뒤 바인딩되었다. 단일 노드라 노드 선택 효과는 확인하지 못했다.
- 정리하면 emptyDir은 **임시**, hostPath는 **PV 없이 Pod에 직접 지정하는 노드 경로**, local은 **PV/PVC로 관리되는 노드 경로**다.

| 구분 | emptyDir | hostPath | local PV |
|---|---|---|---|
| 수명 | Pod | 노드 (Pod와 무관) | PV (reclaim 정책) |
| PV/PVC | 사용 안 함 | 사용 안 함 (PV 정의에는 가능) | 사용 |
| 노드 고정 | 자연히 노드 로컬 | 노드 로컬 (스케줄러는 모름) | `nodeAffinity`로 명시 |
| 용도 | 임시 공유/캐시 | 시스템 컴포넌트 | 고성능 로컬 디스크가 필요한 상태 저장 앱 |

#### 3. 네트워크 볼륨

##### 3-1. 개념

**Pod가 실행되는 노드가 아니라 네트워크 너머의 저장소를 마운트하는 볼륨.** 데이터가 노드 바깥에 있으므로 **Pod가 어느 노드로 옮겨 가도 같은 데이터를 쓸 수 있다.** 대가는 네트워크 지연과 저장소 서버 의존성이다.

##### 3-2. 종류

쿠버네티스 API가 정의한 볼륨 소스를 이 서버에서 `k explain pod.spec.volumes`로 조회했다.

```
awsElasticBlockStore azureDisk azureFile cephfs cinder configMap csi downwardAPI emptyDir ephemeral fc flexVolume
flocker gcePersistentDisk gitRepo glusterfs hostPath image iscsi nfs persistentVolumeClaim photonPersistentDisk
portworxVolume projected quobyte rbd scaleIO secret storageos vsphereVolume
```

| 분류 | 종류 | 성격 | 접근 모드 (일반적) |
|---|---|---|---|
| 네트워크 파일시스템 | **nfs**, cephfs, azureFile, glusterfs | 파일 단위 공유 | **RWX** 가능 |
| 네트워크 블록 스토리지 | iscsi, fc, rbd(Ceph RBD), awsElasticBlockStore, gcePersistentDisk, azureDisk | 디스크 하나를 노드에 붙임 | 보통 **RWO** |
| CSI | `csi` | 외부 드라이버(EBS CSI, EFS CSI 등)로 위임하는 표준 방식 | 드라이버마다 다름 |
| 쿠버네티스 내부 | configMap, secret, downwardAPI, projected | 7장 | - |
| 로컬 | emptyDir, hostPath, (local은 PV 전용) | 2절 | - |

- 이 표의 접근 모드, 분류는 일반적인 설명이고 **실제로 마운트해 본 것은 NFS뿐**이다.
- `explain`이 보여 주는 것은 **API 스키마에 있는 필드 이름이고, 모두 실제로 동작한다는 뜻이 아니다.** 클라우드 종속 인트리 플러그인(awsElasticBlockStore, gcePersistentDisk, azureDisk 등)은 현재 **CSI 드라이버로 위임(migration)** 되고 있고 일부는 제거되었다. 아래 실험으로 확인했다.

**인트리(in-tree) 볼륨과 CSI**

`awsElasticBlockStore` 볼륨을 쓰는 Pod를 만들었다 (`volumeID: vol-123`).

```
$ k -n t9 get pod ebs
ebs   0/1   ContainerCreating   0   45s        # 45초 동안 시작하지 못함
$ k -n t9 get events ...
Warning  FailedAttachVolume  pod/ebs  AttachVolume.Attach failed for volume "ebs.csi.aws.com-vol-123" :
                            timed out waiting for external-attacher of ebs.csi.aws.com CSI driver to attach volume
$ k -n kube-system logs kube-controller-manager-docker-desktop | grep ebs
"attacherDetacher.AttachVolume started" volumeName="kubernetes.io/csi/ebs.csi.aws.com^vol-123" ...
```

- 인트리 `awsElasticBlockStore`는 **`ebs.csi.aws.com` CSI 드라이버를 부르는 요청으로 변환**되었다 (볼륨 이름이 `kubernetes.io/csi/ebs.csi.aws.com^vol-123`). 이 클러스터에는 그 드라이버가 없어서 attach가 시간 초과로 실패했다 (`k get csidrivers`는 비어 있고 `csinode`의 `DRIVERS`가 0).
- 즉 **네트워크 볼륨의 현재 표준은 CSI**이고, 신규 구성에서는 CSI 드라이버를 설치해서 쓴다 (문서 지식. 이 환경에서는 CSI 드라이버를 설치해 보지 않았다).

##### 3-3. NFS 실습

NFS 서버가 있어야 하므로 클러스터 안에 **NFS 서버 Pod**(`itsthenetwork/nfs-server-alpine:12`, privileged)를 띄우고, 서버의 저장소는 그 Pod의 `emptyDir`로 했다 (실습용이며 서버 Pod가 죽으면 데이터도 사라진다). 서버 IP는 `10.1.1.34`였다.

```yaml
apiVersion: v1
kind: PersistentVolume
metadata: {name: t9-nfs}
spec:
  capacity: {storage: 1Gi}
  accessModes: [ReadWriteMany]
  persistentVolumeReclaimPolicy: Retain
  storageClassName: nfs-static
  nfs: {server: 10.1.1.34, path: /}
  mountOptions: [nfsvers=4.1]
---
kind: PersistentVolumeClaim
metadata: {name: nfs-claim, namespace: t9}
spec: {accessModes: [ReadWriteMany], storageClassName: nfs-static, resources: {requests: {storage: 500Mi}}}
```

**RWX: 두 Pod가 동시에 같은 볼륨에 쓴다**

```
$ k -n t9 get pvc nfs-claim
nfs-claim   Bound   t9-nfs   RWX
(Pod nfs-w1이 /d/w1.txt, nfs-w2가 /d/w2.txt를 씀)
$ k -n t9 exec nfs-w1 -- sh -c 'ls /d; cat /d/w2.txt; mount | grep " /d "'
w1.txt
w2.txt
from-w2                                                       # w2가 쓴 파일을 w1이 읽음
10.1.1.34:/ on /d type nfs4 (rw,relatime,vers=4.1,rsize=1048576,wsize=1048576,namlen=255,hard,proto=tcp,timeo=600,retrans=2,sec=sys,...)
$ k -n t9 exec nfs-server -- ls -la /exports
-rw-r--r--  w1.txt
-rw-r--r--  w2.txt                                            # 서버 쪽에도 파일이 있음
```

- **같은 PVC를 쓴 Pod 둘이 서로의 파일을 봤다.** 이것이 emptyDir/hostPath와 다른 점이다 (네트워크 파일시스템이라 RWX가 가능).
- 마운트 옵션에 `hard`, `timeo=600`이 있다. `nfsvers=4.1`을 PV의 `mountOptions`로 지정한 것이 `vers=4.1`로 반영되었다.

**마운트 위치와 구조**

```
$ docker run ... nsenter -t 1 -m -- mount | grep nfs
10.1.1.34:/ on /var/lib/kubelet/pods/abd44375-.../volumes/kubernetes.io~nfs/t9-nfs type nfs4 (rw,...)
```

- **노드(kubelet)가 NFS를 노드의 `/var/lib/kubelet/pods/<uid>/volumes/kubernetes.io~nfs/<PV 이름>`에 마운트**하고, 그 디렉터리를 컨테이너에 바인드 마운트한다. 컨테이너가 NFS 클라이언트를 가지고 있는 것이 아니다. 그래서 **노드에 NFS 클라이언트 지원이 있어야** 한다 (이 환경의 VM에는 있었다).

**클라이언트 Pod가 없어져도 데이터는 서버에 남는다**

```
$ k -n t9 delete pod nfs-w1 nfs-w2
$ k -n t9 run nfs-r ... (같은 PVC 마운트)     →  ls /d   =   w1.txt w2.txt
```

**주의: 서버가 죽으면 쓰기가 멈추고 Pod 삭제도 막힌다**

```
$ k -n t9 delete pod nfs-server               # NFS 서버를 먼저 삭제 (클라이언트 Pod nfs-live는 실행 중)
$ k -n t9 exec nfs-live -- sh -c 'timeout 8 sh -c "echo x > /d/new.txt"; echo rc=$?'
Terminated
rc=143                                         # 8초 안에 쓰기가 끝나지 않음 (timeout이 강제 종료)
$ k -n t9 delete pod nfs-live
nfs-live   1/1   Terminating                   # 약 30초 이상 Terminating에서 멈춤
```

- 마운트가 **`hard`** 라서 서버가 응답하지 않으면 I/O가 에러 없이 **계속 대기**한다. 프로세스가 멈추고 Pod 종료도 지연된다. 이 실험에서는 노드에서 `umount -f -l`(강제 + 지연 언마운트)로 정리했고, 그 뒤 Pod가 `Error` 상태를 거쳐 삭제되었다 (언마운트 없이도 결국 정리되었을지는 확인하지 못했다).
- NFS 서버는 **단일 장애점**이고, 네트워크 볼륨을 쓰는 Pod 전체의 가용성에 영향을 준다. `soft` 옵션은 데이터 손상 위험이 있다는 문서 설명이 있으나 이 환경에서 시험하지 않았다.

##### 3-4. 네트워크 볼륨 선택 시 고려

| 고려 | 내용 |
|---|---|
| 공유 필요 | 여러 Pod가 동시에 쓰려면 RWX가 되는 **파일시스템**(NFS, CephFS, EFS) |
| 성능/지연 | 네트워크 블록 스토리지(EBS 등)가 일반적으로 파일 공유보다 유리 (문서 지식) |
| 노드와 위치 | 블록 스토리지는 보통 **같은 가용 영역**의 노드에만 붙는다 (문서 지식, 그래서 `WaitForFirstConsumer`가 필요, 4절) |
| 장애 | 서버/네트워크 장애가 Pod에 미치는 영향 (위 `hard` 마운트 실험) |
| 운영 | in-tree 대신 **CSI 드라이버** 사용 |

#### 4. 다이나믹 프로비저닝

##### 4-1. 개념

**PVC를 만들면 PV를 관리자가 미리 만들어 두지 않아도 자동으로 만들어 주는 방식.** 정적 프로비저닝은 PV를 미리 만들어야 하므로 용량을 예측해야 하고 관리자가 매번 개입해야 한다. 동적 프로비저닝은 **StorageClass**가 "어떤 프로비저너로, 어떤 파라미터로 PV를 만들지"를 정의하고, 프로비저너가 PVC 요청에 맞춰 PV와 실제 저장소를 만든다.

| 구분 | 정적 | 동적 |
|---|---|---|
| PV 생성 | 관리자가 미리 | PVC 생성 시 자동 |
| StorageClass | 이름 매칭용 (오브젝트 없어도 됨) | **필수** (프로비저너와 파라미터 정의) |
| 용량 | 미리 만든 PV 크기 (남으면 낭비) | 요청한 크기로 생성 |
| 기본 reclaim | `Retain` | `Delete` |

##### 4-2. StorageClass

이 클러스터의 기본 StorageClass:

```
$ k get storageclass
NAME                 PROVISIONER          RECLAIMPOLICY   VOLUMEBINDINGMODE   ALLOWVOLUMEEXPANSION
hostpath (default)   docker.io/hostpath   Delete          Immediate           false
$ k get sc -o yaml
metadata: {annotations: {storageclass.kubernetes.io/is-default-class: "true"}, name: hostpath}
provisioner: docker.io/hostpath
reclaimPolicy: Delete
volumeBindingMode: Immediate
```

| 필드 | 의미 |
|---|---|
| `provisioner` | PV를 만드는 주체 (이 환경: `docker.io/hostpath`, `kube-system`의 `storage-provisioner` Pod, 이미지 `docker/desktop-storage-provisioner:v2.0`) |
| `reclaimPolicy` | 이 클래스로 만든 PV의 reclaim 정책 (기본 `Delete`) |
| `volumeBindingMode` | `Immediate` 또는 `WaitForFirstConsumer` (4-4) |
| `allowVolumeExpansion` | PVC 크기 확장을 허용할지 (4-5) |
| `parameters` | 프로비저너에 전달하는 값 (디스크 종류, IOPS 등. 여기서는 사용 안 함) |
| `is-default-class` 어노테이션 | `storageClassName`을 생략한 PVC에 자동 지정되는 기본 클래스 |

##### 4-3. 동작 순서 (실측)

`storageClassName` 없이 PVC만 만들었다.

```
$ cat dyn.yaml                       # 클래스 지정 없음, 200Mi RWO
$ k apply -f dyn.yaml ; sleep 4
$ k -n t9 get pvc dyn
dyn   Bound   pvc-90de27c8-961e-4c03-bc93-8f7255f151fb   200Mi   RWO   hostpath      # 4초 만에 Bound
$ k get pv
pvc-90de27c8-...   200Mi   RWO   Delete   Bound   t9/dyn   hostpath
$ k get pv pvc-90de27c8-... -o jsonpath='{.spec.hostPath}'
{"path":"/var/lib/k8s-pvs/dyn/pvc-90de27c8-961e-4c03-bc93-8f7255f151fb"}
$ k -n t9 get events --field-selector involvedObject.name=dyn
Normal  ExternalProvisioning   Waiting for a volume to be created either by the external provisioner 'docker.io/hostpath' or manually ...
Normal  Provisioning           External provisioner is provisioning volume for claim "t9/dyn"
Normal  ProvisioningSucceeded  Successfully provisioned volume pvc-90de27c8-...
$ k -n kube-system logs storage-provisioner
provision "t9/dyn" class "hostpath": volume "pvc-90de27c8-..." provisioned
provision "t9/dyn" class "hostpath": succeeded
Trying to save persistentvolume "pvc-90de27c8-..." / persistentvolume "pvc-90de27c8-..." saved
```

```
① PVC 생성 (storageClassName 생략) ─▶ 어드미션이 기본 클래스(hostpath)를 채움
② PV 컨트롤러가 "외부 프로비저너가 만들 것"으로 판단 ─▶ PVC 이벤트 ExternalProvisioning
③ 프로비저너(storage-provisioner Pod)가 PVC를 watch로 감지 ─▶ 저장소 생성 (노드에 디렉터리 /var/lib/k8s-pvs/<PVC 이름>/pvc-<PVC uid>)
④ 프로비저너가 PV 오브젝트 생성 (claimRef로 PVC를 지정, annotation pv.kubernetes.io/provisioned-by)
⑤ PV 컨트롤러가 PVC와 PV를 바인딩 ─▶ Bound
```

- PV 이름은 **`pvc-<PVC의 uid>`** 형식이다. 이름이 PVC 이름과 다르다.
- PVC에는 `volume.kubernetes.io/storage-provisioner: docker.io/hostpath`, `pv.kubernetes.io/bind-completed: yes`, `pv.kubernetes.io/bound-by-controller: yes` 어노테이션이 붙었다. PV에는 `pv.kubernetes.io/provisioned-by: docker.io/hostpath`가 붙었다.
- **프로비저너는 Pod로 떠 있는 컨트롤러**이고 8장에서 본 Watch API를 쓴다. 인그레스 컨트롤러처럼 "클래스가 자기 담당인 오브젝트"만 처리한다.
- 프로비저너가 실제로 저장소를 만드는 방식은 구현마다 다르다. 이 환경은 노드 디렉터리 생성, 클라우드에서는 디스크 API 호출이다.

**데이터 쓰기와 노드 쪽 확인**

```
$ (Pod dyn-user가 PVC dyn을 마운트해서 /d/f.txt에 dyn-data 기록)
$ docker run ... nsenter -t 1 -m -- ls -la /var/lib/k8s-pvs/dyn/pvc-90de27c8-...
-rw-r--r--  1 root root  9  f.txt
$ docker run ... cat .../f.txt     →  dyn-data
```

**삭제: PVC를 지우면 PV와 데이터도 지워진다 (`Delete`)**

```
$ k -n t9 delete pod dyn-user ; k -n t9 delete pvc dyn          # 6초 뒤
$ k get pv                       →  No resources found
$ ls /var/lib/k8s-pvs/dyn/pvc-90de27c8-...   →  No such file or directory       # 노드 디렉터리까지 삭제됨
```

- 1-4의 정적 hostPath PV(`Delete`)에서는 PV 오브젝트만 사라지고 파일은 남았지만, **프로비저너가 만든 PV는 디렉터리까지 삭제**되었다. 삭제를 프로비저너가 처리하기 때문이다.
- 데이터를 지키려면 클래스의 `reclaimPolicy: Retain`으로 하거나, 이미 만들어진 PV의 `persistentVolumeReclaimPolicy`를 `Retain`으로 바꿔 둔다 (바꾸는 것은 이 환경에서 동적 PV에 대해서는 시험하지 않았다).

**(참고) PVC보다 StorageClass가 나중에 만들어지면**

`manual`, `other`처럼 존재하지 않는 클래스를 지정한 PVC는 `ProvisioningFailed: storageclass.storage.k8s.io "..." not found` 이벤트와 함께 Pending에 머문다 (1-3). 클래스를 만들면 처리되는지는 확인하지 않았다.

##### 4-4. volumeBindingMode: Immediate vs WaitForFirstConsumer

| 모드 | 동작 | 문제/용도 |
|---|---|---|
| `Immediate` (기본) | PVC를 만들면 **즉시** PV를 만들고 바인딩 | Pod가 어느 노드에 뜰지 모르는 채로 저장소 위치가 정해져서, 노드/가용 영역이 안 맞을 수 있다 |
| `WaitForFirstConsumer` | **PVC를 쓰는 Pod가 스케줄링될 때까지 바인딩/생성을 미룬다.** 스케줄러가 노드를 고른 뒤 그 노드 기준으로 PV를 만든다 | 노드에 묶이는 볼륨(local, 영역별 블록 스토리지)에 필요 |

클래스 `t9-wffc`(`WaitForFirstConsumer`, 프로비저너는 같은 `docker.io/hostpath`)와 기본 클래스(`Immediate`)로 PVC를 동시에 만들었다.

```
$ k -n t9 get pvc
imm    Bound     pvc-903dd401-...   100Mi   RWO   hostpath      # Immediate: 바로 Bound
late   Pending                                     t9-wffc       # WaitForFirstConsumer: Pending
nodyn  Pending                                                   # (참고) 클래스 ""
$ k -n t9 get events --field-selector involvedObject.name=late
Normal  WaitForFirstConsumer  waiting for first consumer to be created before binding

$ (PVC late를 쓰는 Pod late-user 생성 후)
$ k -n t9 get pvc late
late   Bound   pvc-652cd275-...   100Mi   RWO   t9-wffc
$ k -n t9 get pvc late -o jsonpath='{.metadata.annotations.volume\.kubernetes\.io/selected-node}'
docker-desktop                          # 스케줄러가 고른 노드가 PVC 어노테이션에 기록됨
```

- Pod가 생기기 **전**에는 `late`가 `Pending`이었고 이벤트에 `waiting for first consumer`가 남았다. 이것은 **오류가 아니라 정상 대기**다.
- Pod가 스케줄되면 스케줄러가 노드(`docker-desktop`)를 PVC의 `volume.kubernetes.io/selected-node` 어노테이션에 기록하고, 프로비저너가 그 노드 기준으로 PV를 만들었다. 단일 노드라서 "노드가 달라 실패하는 경우를 피했는지"는 확인하지 못했다.
- `local` 볼륨(2-3)도 같은 모드를 썼고 같은 순서(Pod 생성 전 Pending → 생성 후 Bound)였다.

##### 4-5. 볼륨 확장

```
# 기본 클래스(allowVolumeExpansion: false)
$ k -n t9 patch pvc imm -p '{"spec":{"resources":{"requests":{"storage":"300Mi"}}}}'
Error from server (Forbidden): persistentvolumeclaims "imm" is forbidden: only dynamically provisioned pvc can be resized and the storageclass that provisions the pvc must support resize

# allowVolumeExpansion: true 로 만든 클래스(t9-wffc)
$ k -n t9 patch pvc late -p '{"spec":{"resources":{"requests":{"storage":"300Mi"}}}}'
persistentvolumeclaim/late patched
$ k -n t9 get pvc late           # 6초 뒤
late   Bound   ...   100Mi                  # CAPACITY는 그대로 100Mi

# 축소
$ k -n t9 patch pvc late -p '{"spec":{"resources":{"requests":{"storage":"50Mi"}}}}'
The PersistentVolumeClaim "late" is invalid: spec.resources.requests.storage: Forbidden: field can not be less than status.capacity
```

- 확장은 **StorageClass가 `allowVolumeExpansion: true`** 여야 API가 받아 준다. 기본 클래스에서는 `Forbidden`이었다.
- 클래스가 허용해도 **실제로 늘리는 일은 프로비저너(리사이저)** 가 한다. 이 환경의 프로비저너는 확장을 수행하지 않아서 요청만 받아지고 용량은 100Mi로 남았다 (이 환경에서는 조건 필드로도 진행 표시를 확인하지 못했다).
- 축소는 허용되지 않는다.
- 정적으로 만든 PV(`only dynamically provisioned pvc can be resized`)는 확장 대상이 아니다.

##### 4-6. 정리: 전체 흐름

```
개발자: PVC 작성 (크기 + 접근 방식 [+ 클래스])
   │
   ├─ 클래스 생략 → 기본 StorageClass 지정
   ▼
StorageClass ──provisioner──▶ 프로비저너 Pod (PVC를 watch)
                                  │  (WaitForFirstConsumer면 Pod 스케줄링 후)
                                  ▼
                       실제 저장소 생성 (디스크/디렉터리/NFS 공유 ...)
                                  ▼
                       PV 생성 + claimRef로 PVC에 바인딩
                                  ▼
                       Pod가 PVC를 마운트 ──▶ kubelet이 노드에 마운트
                                  ▼
          PVC 삭제 ──▶ reclaimPolicy: Delete면 PV와 저장소 삭제 / Retain이면 Released로 보존
```

#### 5. 확인하지 못한 부분

- **멀티 노드 동작**: Pod가 다른 노드로 옮겨 갈 때 hostPath/local이 접근 불가해지는 것, RWO의 "노드 하나" 제한, `WaitForFirstConsumer`가 노드/가용 영역 불일치를 막는 효과는 단일 노드라 확인하지 못했다.
- **접근 모드 ROX, RWOP**와 RWO를 여러 Pod가 같은 노드에서 쓰는 경우는 실험하지 않았다.
- **PV spec의 변경 가능 범위**: `capacity`나 `hostPath` 경로를 바꿨을 때의 거부 여부는 PV가 이미 사라진 상태에서 시도해서 확인하지 못했다.
- **정적 `Retain` PV가 PVC 삭제 후 사라진 사건**: 첫 시도에서 `Retain` PV가 없어졌는데 원인을 찾지 못했다. 같은 절차를 다시 했을 때는 `Released`로 남았다.
- **PV의 `Delete` 정책이 hostPath에서 파일을 지우지 않은 이유**: 관찰만 했고 쿠버네티스 쪽 구현 이유는 확인하지 않았다.
- **동적 PV의 reclaim을 `Retain`으로 바꾸는 것**과, 클래스가 `Retain`일 때 PVC 삭제 후 `Released` 상태에서 데이터가 남는 것은 `late`(Retain 클래스) PV가 `Released`로 남은 것만 확인했다. 노드의 데이터 내용 확인은 하지 않았다.
- **StorageClass를 PVC보다 나중에 만들었을 때** Pending PVC가 자동 처리되는지는 확인하지 않았다.
- **볼륨 확장**: 이 환경의 프로비저너는 확장을 수행하지 않아 실제 확장(파일시스템 확장 포함)은 확인하지 못했다.
- **hostPath의 보안 제한**: Pod Security 설정이 이 클러스터에서 hostPath를 막는지는 확인하지 않았다 (실험에서는 막히지 않았다).
- **VM 재시작 후 `/var/tmp` 휘발**: 이 노드의 `/var/tmp`가 tmpfs인 것까지만 확인했고 재시작은 하지 않았다.
- **CSI 드라이버 설치와 사용, 클라우드 디스크(EBS, GCE PD 등), iSCSI, Ceph**: 설치/실행하지 않았다. NFS 외 네트워크 볼륨의 접근 모드와 동작은 문서 지식이다.
- **NFS `soft` 마운트, NFS 서버 복구 후 클라이언트의 자동 재개, 서버 장애 시 `umount -f -l` 없이 Pod 삭제가 끝나는지**는 확인하지 않았다.
- **`medium: Memory` 볼륨의 OOM**이 어느 cgroup에 계산되는지 직접 확인하지 않았다 (`Killed` 출력만 확인).
- **Pod 보호 정책과 `volumeMode: Block`, `volumeAttributesClass`, 스냅샷/복제**는 다루지 않았다.
