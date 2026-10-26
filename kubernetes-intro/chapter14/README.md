### 14장. 데이터 저장

> 정리 범위: 볼륨 타입(emptyDir, hostPath, nfs), PV 라이프사이클, 동적 프로비저닝과 정적 프로비저닝의 특징/차이
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop. 노드 여러 개와 NFS 서버가 필요해서 **임시 kind 클러스터**(`t14`, v1.35.0, control-plane 1 + worker 2, 별도 kubeconfig, 네임스페이스 `t14`)에서 주로 실험했다. kind의 기본 StorageClass `standard`는 `rancher.io/local-path`(`WaitForFirstConsumer`, reclaim `Delete`)다. NFS 서버는 같은 클러스터에 `itsthenetwork/nfs-server-alpine` Pod(privileged)를 띄워 Service(ClusterIP)로 노출했다. Docker Desktop 내장 쿠버네티스(v1.32.2)에서는 기본 StorageClass 동작만 `--context docker-desktop`으로 확인했다(네임스페이스 `t14dd`, 삭제함).
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: `emptyDir`/ConfigMap/Secret 볼륨의 기본 사용법은 [5장](../chapter05/README.md), [10장](../chapter10/README.md), [11장](../chapter11/README.md), StatefulSet의 `volumeClaimTemplates`는 [6장](../chapter06/README.md)이다. `시작하세요! 도커/쿠버네티스` [10장](../../start-docker-kubernetes/chapter10/README.md)에서 다룬 PV/PVC 기본과 겹치는 부분은 짧게 쓰고 이번에 새로 확인한 동작을 중심으로 적었다(해당 장 번호는 링크 대상으로 가정했으며 열어 확인하지 않았다).

#### 목차

1. 볼륨이 필요한 이유와 분류
2. 볼륨 타입: emptyDir
3. 볼륨 타입: hostPath
4. 볼륨 타입: nfs
5. PV와 PVC
6. PV 라이프사이클
7. 정적 프로비저닝과 동적 프로비저닝
8. 선택 가이드와 함정
9. 확인하지 못한 부분

---

#### 1. 볼륨이 필요한 이유와 분류

컨테이너 파일시스템은 컨테이너가 재시작되면 사라진다. 볼륨은 컨테이너의 수명을 넘어 데이터를 유지하거나 컨테이너끼리 데이터를 공유하는 방법이다.

| 분류 | 예 | 수명 |
|---|---|---|
| Pod 수명과 같음 | `emptyDir`, `configMap`, `secret`, `downwardAPI` | Pod가 사라지면 같이 사라짐 |
| 노드에 묶임 | `hostPath`, `local` | 노드 디스크에 남지만 **그 노드에서만** 보임 |
| 네트워크 스토리지 | `nfs`, 클라우드 디스크(EBS 등), CSI | Pod와 노드와 무관하게 유지 |
| 영구 볼륨 추상화 | **PV + PVC** | Pod와 독립 (PV 정책에 따름) |

---

#### 2. 볼륨 타입: emptyDir

**Pod가 노드에 배정될 때 만들어지는 빈 디렉터리.** Pod 안 컨테이너끼리 공유한다.

##### 2-1. 컨테이너 간 공유, 컨테이너 재시작 vs Pod 삭제 (실측)

- 컨테이너 `w`가 `/data/w.txt`를 쓰고, 다른 컨테이너 `r`이 `/shared`로 같은 볼륨을 마운트해서 읽었다 → `from-w` 확인(`/dev/vda1 on /shared type ext4`, 노드 디스크).
- **컨테이너만 재시작**(`crictl stop`으로 종료, restartCount 1): 데이터 유지(`marker`, `marker` 두 줄이 로그에 남음 = 재시작 전 쓴 파일이 있어 두 번째 실행에서 한 줄이 추가됨).
- 노드의 실제 경로: `/var/lib/kubelet/pods/<pod uid>/volumes/kubernetes.io~empty-dir/<볼륨명>`.
- **Pod를 삭제하면** 그 디렉터리가 삭제됐다(`No such file or directory`). 즉 수명은 Pod와 같다.

(처음 시도에서 컨테이너 안에서 `kill 1`로 재시작을 흉내 냈지만 PID 1이 신호를 무시해 재시작되지 않았다(restartCount 0). 그래서 `crictl stop`으로 다시 실험했다.)

##### 2-2. `medium: Memory`와 `sizeLimit` (실측)

```
volumes: [{name: m, emptyDir: {medium: Memory, sizeLimit: 16Mi}}, {name: l, emptyDir: {sizeLimit: 10Mi}}]
$ mount | grep -E " /(mem|lim) "
tmpfs on /mem type tmpfs (rw,relatime,size=16384k,noswap)
/dev/vda1 on /lim type ext4 (rw,relatime,discard)
```

| 시험 | 결과 |
|---|---|
| Memory 볼륨에 20Mi 쓰기 (한도 16Mi) | **16Mi에서 멈춤**(`16+0 records out`) — tmpfs 크기가 `sizeLimit`으로 제한됨 |
| 디스크 emptyDir에 20Mi 쓰기 (`sizeLimit: 10Mi`) | **쓰기는 성공**(20Mi 모두 기록) → 약 48초 뒤 Pod가 **`Failed / Evicted`**: `Usage of EmptyDir volume "l" exceeds the limit "10Mi".` |

- 디스크 기반 emptyDir의 `sizeLimit`은 쓰기를 즉시 막지 않고 **kubelet이 주기적으로 확인해서 초과 시 Pod를 퇴출(Evict)**한다(이 실험에서는 약 48초 안에).
- `medium: Memory`는 노드 메모리를 쓰므로 **컨테이너 메모리 한도에도 영향**을 줄 수 있다고 알려져 있다(이 실험의 메모리 한도 256Mi 안에서 16Mi만 썼기에 영향은 확인하지 못했다).
- Memory emptyDir의 데이터는 노드가 재부팅되면 사라진다(재부팅은 실험하지 않았다).

##### 2-3. 용도

- 컨테이너 간 파일 공유(사이드카가 로그 읽기, init 컨테이너가 준비한 파일 전달 — 5장)
- 캐시, 임시 작업 공간, 정렬 같은 스크래치 공간
- **중요한 데이터를 두지 않는다** (Pod가 지워지거나 퇴출되면 사라짐)

---

#### 3. 볼륨 타입: hostPath

**노드의 파일/디렉터리를 Pod에 마운트.** 데이터가 Pod 삭제 후에도 노드에 남는다.

##### 3-1. `type` 검사 (실측)

| `type` | 경로 상태 | 결과 |
|---|---|---|
| `DirectoryOrCreate` | 없음 | 디렉터리가 만들어져 `Running` (`drwxr-xr-x root`) |
| `Directory` | 없음 | **`ContainerCreating`에 머묾** — `FailedMount ... hostPath type check failed: /data/nodir is not a directory` |
| (생략, 빈 `type`) | 없음 | `Running` (경로는 컨테이너 런타임이 만들었고 소유자 root, 퍼미션 `drwxr-xr-x`) |
| `File` | `/etc/hostname`(있음) | 노드 호스트명(`t14-worker`)이 읽힘 |

- `type`을 생략하면 검사를 하지 않는다. 의도한 경로인지 검사하려면 `Directory`/`File`/`DirectoryOrCreate`처럼 지정한다.

##### 3-2. 노드 종속성 (실측)

```
hp1 (t14-worker) 가 /data/hp/f.txt 에 "hello-from-hp1" 기록
$ kubectl delete pod hp1                → 노드의 /data/hp/f.txt 그대로 (hello-from-hp1)
hp2 (t14-worker2, 같은 hostPath /data/hp)  → ls 결과 비어 있음
```

- Pod를 삭제해도 데이터는 노드에 남지만, **다른 노드에서 뜬 Pod는 그 데이터를 볼 수 없다**(노드마다 별개의 디렉터리).
- 그래서 hostPath는 단일 노드 개발/테스트나 노드 에이전트(로그 수집기, 모니터링)에 쓴다. 운영 데이터 저장소로는 부적합하다.
- **보안**: 노드 파일시스템을 컨테이너에 노출하므로 `/`, `/var/run/docker.sock` 같은 경로를 마운트하면 노드 탈취 경로가 된다고 알려져 있다(이 실험에서 시험하지 않았다). 읽기 전용(`readOnly: true`)과 `type` 지정, 경로 제한 정책이 권장된다.

##### 3-3. Docker Desktop에서의 hostPath

Docker Desktop의 노드는 VM 안에 있으므로 hostPath는 **Mac 파일시스템이 아니라 VM 안의 경로**다(예: 이 클러스터의 동적 PV 경로 `/var/lib/k8s-pvs/<pvc>/…`, 아래 7-4).

---

#### 4. 볼륨 타입: nfs

**NFS 서버의 디렉터리를 마운트.** 네트워크 스토리지라 **Pod가 어느 노드에 뜨든 같은 데이터**를 본다. 여러 Pod가 동시에 읽고 쓸 수 있다(RWX).

```yaml
volumes:
- name: nv
  nfs: {server: 10.96.28.200, path: /, readOnly: false}
```

##### 4-1. 실측 (kind, NFS 서버 Pod + Service ClusterIP)

| 시험 | 결과 |
|---|---|
| 서로 다른 노드의 Pod 2개가 같은 NFS를 마운트 | `10.96.28.200:/ on /mnt/nfs type nfs4 (rw,… vers=4.2, … hard, proto=tcp, …)`, 한쪽이 쓴 `from-nfs1`을 다른 쪽이 읽음 |
| 두 Pod 삭제 후 새 Pod(다른 노드)에서 마운트 | 데이터 유지(`from-nfs1`) |
| `readOnly: true`로 마운트 후 쓰기 | 실패(`exit code 1`) |
| 서버 쪽 확인 | NFS 서버 Pod의 `/exports/shared.txt` = `from-nfs1` |
| 존재하지 않는 서버 IP(`10.96.99.99`) | 25초 후에도 **`ContainerCreating`** (마운트 대기) |
| 서버를 **Service DNS 이름**(`nfs.t14.svc.cluster.local`)으로 지정 | 75초 후에도 **`ContainerCreating`**, Events에 아무 것도 남지 않았다 |

- 마운트는 **노드(kubelet)** 가 수행한다. 노드에 `mount.nfs`가 있어야 한다(kind 노드에는 있었음).
- DNS 이름 지정은 이 실험에서 **성공하지 못했다**. 노드(kubelet)의 이름 해석 컨텍스트는 Pod의 CoreDNS가 아니므로 클러스터 DNS 이름을 못 푸는 것으로 보이지만, Events가 없어 **원인을 확인하지는 못했다**. 이 실험에서는 서버를 **ClusterIP**로 지정해 성공했다.
- NFS 서버는 이 실험에서 `OOMKilled`로 한 번 재시작됐고(원인 미조사) 서버 쪽 emptyDir은 컨테이너 재시작에 살아남아 데이터가 유지됐다.
- 서버 주소를 PV에 박아 두므로 서버 이전이 어렵고, 동시 쓰기의 일관성은 NFS와 앱 책임이다.

---

#### 5. PV와 PVC

| 오브젝트 | 의미 | 범위 | 누가 만드나 |
|---|---|---|---|
| **PV** (PersistentVolume) | 클러스터에 존재하는 **스토리지 한 조각** | 클러스터 (네임스페이스 없음) | 관리자(정적) 또는 프로비저너(동적) |
| **PVC** (PersistentVolumeClaim) | 사용자의 **스토리지 요청** | 네임스페이스 | 사용자 |
| **StorageClass** | 동적 프로비저닝 방식/정책 | 클러스터 | 관리자 |

Pod는 PV를 직접 쓰지 않고 **PVC를 참조**한다. 인프라 세부(NFS 주소, 디스크 종류)와 사용자(필요한 용량/모드)를 분리하는 구조다.

##### 5-1. 바인딩 조건 (정적, 실측)

PV `pv-a`: 용량 1Gi, `ReadWriteOnce`, `storageClassName: manual`, 레이블 `tier=gold`, reclaim `Retain`, hostPath.

| PVC 요청 | 결과 |
|---|---|
| 2Gi | **Pending** (PV가 더 작음) |
| `storageClassName: other` | Pending — `storageclass ... "manual"(여기서는 요청한 class) not found` 이벤트(`ProvisioningFailed`) |
| `ReadWriteMany` | Pending (PV는 RWO만) |
| selector `tier=silver` | Pending (PV 레이블 불일치) |
| `storageClassName: ""` | Pending (class가 `manual`인 PV와 다름) |
| **500Mi, RWO, class manual, selector `tier=gold`** | **Bound**, `VOLUME pv-a`, **CAPACITY `1Gi`**(요청 500Mi가 아니라 PV 크기) |
| 이미 Bound된 PV에 다른 PVC(100Mi) | Pending (1 PV : 1 PVC) |

- 바인딩 조건: **요청 용량 ≤ PV 용량, 접근 모드 일치, `storageClassName` 일치, (있다면) selector 일치, 1:1.** 요청보다 큰 PV에 붙으면 **PV 전체 용량이 보인다**(낭비).
- PV의 `claimRef`에 PVC의 이름/UID가 기록된다.
- 접근 모드: `ReadWriteOnce`(한 노드에서 읽기/쓰기), `ReadOnlyMany`, `ReadWriteMany`, `ReadWriteOncePod`(이 실험에서는 RWO, RWX만 사용). **RWO는 "노드 하나"에 대한 제한이지 Pod 하나가 아니다**. 이 클러스터에서 한 노드의 여러 Pod가 같은 RWO PVC를 쓰는 실험은 하지 않았다.

---

#### 6. PV 라이프사이클

```
Available ──(PVC 바인딩)──▶ Bound ──(PVC 삭제)──▶ Released ──(reclaim 정책)──▶ Retain: 그대로 / Delete: 삭제 / (실패: Failed)
```

| 상태 | 의미 |
|---|---|
| **Available** | 아직 어떤 PVC에도 묶이지 않음 |
| **Bound** | PVC에 묶임 |
| **Released** | PVC가 삭제됨, 하지만 **`claimRef`가 남아 있어 다른 PVC가 못 씀**, 데이터 처리는 reclaim 정책에 따름 |
| **Failed** | 자동 회수(reclaim)에 실패 |
| (Pending) | PV는 `Pending`도 있으나 이 실험에서는 보지 못했다 |

##### 6-1. Retain (실측)

```
pv-a (Retain), Bound to c-ok. Pod 가 /d/f.txt 에 "precious" 기록
$ kubectl delete pvc c-ok          (Pod가 아직 사용 중)
c-ok  Terminating                  ← 바로 지워지지 않음 (kubernetes.io/pvc-protection finalizer)
$ kubectl delete pod user1         → PVC 삭제 완료
pv-a  Released  CLAIM c-ok         (CLAIM 칸에 이전 PVC 이름이 남음)
$ docker exec t14-worker cat /data/pv-a/f.txt   → precious     (데이터 그대로)
```

- **PVC 보호**: 사용 중인 PVC는 삭제 명령을 줘도 `Terminating`으로 남았다가 **Pod가 사라지면 삭제됨**. PVC와 PV에 각각 finalizer `kubernetes.io/pvc-protection`, `kubernetes.io/pv-protection`이 붙어 있었다.
- PV도 보호됨: `Bound`인 PV에 `kubectl delete pv --wait=false`를 하면 삭제되지 않고 `Bound`로 남았고, **PVC를 삭제한 뒤에야 사라졌다**.
- `Released` PV는 새 PVC가 붙지 않았다 (새 PVC `c-new`가 `Pending`).
- **재사용하려면** `claimRef`를 제거해야 한다:

```
$ kubectl patch pv pv-a --type json -p '[{"op":"remove","path":"/spec/claimRef"}]'
pv-a  Available           → (잠시 뒤) c-new Bound to pv-a
$ cat /data/pv-a/f.txt    → precious   (이전 사용자의 데이터가 새 PVC에 그대로 노출)
```

- Retain에서 `claimRef`만 지우면 **이전 데이터가 그대로 남은 채 다음 사용자에게 붙는다.** 민감 데이터가 있으면 먼저 지워야 한다.
- PV가 `Available`로 바뀌고 새 PVC가 `Bound`가 될 때까지는 몇 초 지연이 있었다(처음 5초 뒤엔 `Pending`, 이후 `Bound`).

##### 6-2. Delete (정적 hostPath PV에서의 실패, 실측)

```
$ kubectl patch pv pv-a -p '{"spec":{"persistentVolumeReclaimPolicy":"Delete"}}'   (정적 hostPath PV)
$ kubectl delete pvc c-del
pv-a  Failed
Message: host_path deleter only supports /tmp/.+ but received provided /data/pv-a
Warning  VolumeFailedDelete  persistentvolume-controller
$ ls /data/pv-a  → f.txt  (데이터는 그대로)
```

- `Delete`는 **볼륨 플러그인이 삭제를 지원해야** 동작한다. 정적 hostPath PV는 `/tmp/…` 아래만 자동 삭제를 지원해서 `Failed`가 됐다. `Failed` PV는 자동으로 해결되지 않으므로 관리자가 직접 정리해야 한다.
- 세 번째 정책 `Recycle`(데이터 삭제 후 재사용)은 **더 이상 권장되지 않는(deprecated)** 방식으로 알려져 있다(이 실험에서 시험하지 않았다).

##### 6-3. 확장과 변경 제약 (실측)

| 시도 | 결과 |
|---|---|
| 바인딩된 PVC의 크기를 늘림 (StorageClass에 `allowVolumeExpansion` 없음) | **거부**: `only dynamically provisioned pvc can be resized and the storageclass that provisions the pvc must support resize` |
| 크기를 줄임 | 거부: `field can not be less than status.capacity` |
| `accessModes` 변경 | 거부: `spec is immutable after creation except resources.requests and volumeAttributesClassName for bound claims` |
| 아직 Pending인 PVC의 크기 변경 | 같은 불변 오류 메시지 (spec immutable) |

- 확장이 허용되는 StorageClass(`allowVolumeExpansion: true`)에서의 확장은 실험하지 않았다.
- **kind의 local-path 프로비저너는 요청 용량을 강제하지 않았다**: 100Mi PVC에 300MB를 기록해도 성공했다(`df`는 노드 디스크 전체 58.4G 표시). 용량 한도는 스토리지 구현에 달려 있다.

---

#### 7. 정적 프로비저닝과 동적 프로비저닝

##### 7-1. 비교

| 구분 | 정적(static) | 동적(dynamic) |
|---|---|---|
| PV 생성 | **관리자가 미리 수동으로** 만든다 | PVC가 생기면 **프로비저너가 자동으로** 만든다 |
| 필요한 것 | PV (스토리지 준비 포함) | StorageClass + 프로비저너(CSI 드라이버 등) |
| PVC 바인딩 | 조건에 맞는 기존 PV 중에서 선택 | 새 PV가 PVC 요청에 맞춰 생성돼 바인딩 |
| 용량 | PV 크기 전체가 보임(낭비 가능) | **요청한 크기로 생성**(구현에 따라) |
| 삭제/회수 | PV의 reclaim 정책 (hostPath는 Delete 불가 → Failed) | StorageClass의 `reclaimPolicy`(기본 `Delete`)가 PV에 복사 |
| 장점 | 기존 스토리지(NFS 등)를 그대로 연결, 세밀한 통제 | 자동화, 셀프서비스 |
| 단점 | 수동 운영, 낭비 | 프로비저너 필요, 삭제 시 데이터도 사라질 수 있음(`Delete`) |

##### 7-2. 동적 프로비저닝 흐름 (kind의 `standard`, 실측)

```
$ kubectl get sc standard
mode=WaitForFirstConsumer  reclaim=Delete  expand=(없음)   annotations: is-default-class=true
$ PVC dyn1 (storageClassName 생략, 200Mi)  → storageClassName: standard (기본 클래스가 자동 채워짐)
  Pending  — "waiting for first consumer to be created before binding"
(Pod 생성) → dyn1 Bound, PV pvc-1dda4ef8-… (200Mi, Delete)
  PV: hostPath /var/local-path-provisioner/pvc-…_t14_dyn1
      nodeAffinity: kubernetes.io/hostname In [t14-worker2]
      annotations: local.path.provisioner/selected-node=t14-worker2, pv.kubernetes.io/provisioned-by=rancher.io/local-path
```

- 기본 StorageClass(`storageclass.kubernetes.io/is-default-class: "true"`)가 있으면 PVC에서 `storageClassName`을 생략해도 채워진다. **`storageClassName: ""`**(빈 문자열)은 "동적 프로비저닝을 쓰지 않고 정적 PV만 쓴다"는 뜻이라 PV가 없으면 Pending이다(위 5-1의 `c-empty`).
- **`volumeBindingMode: WaitForFirstConsumer`**: PVC를 만들어도 **Pod가 스케줄될 때까지 PV를 만들지 않는다**(`Pending`이 정상). Pod의 배치(노드 선택)를 먼저 정한 뒤 그 노드(또는 zone)에 볼륨을 만들기 위해서다. 로컬 디스크나 zone 제약이 있는 스토리지에 필요하다. `Immediate`는 PVC 생성 즉시 만든다.
- 이 프로비저너의 PV는 **노드에 종속**돼 있다(`nodeAffinity`). 이후 같은 PVC를 쓰는 Pod는 **같은 노드(`t14-worker2`)에만** 스케줄된다. Pod를 삭제하고 새로 만들었을 때 데이터(`dyn-data`)가 유지됐고 같은 노드에 떴다.
- PVC도 없고 Pod도 없는 상태에서 Pod가 존재하지 않는 PVC를 참조하면 Pod는 `Pending`, 이벤트는 `FailedScheduling ... persistentvolumeclaim "ghostclaim" not found`였다.

##### 7-3. 회수 정책 (동적, 실측)

| StorageClass | PVC 삭제 후 |
|---|---|
| `standard` (reclaim **Delete**) | **PV 삭제**, 노드의 `/var/local-path-provisioner/…` 디렉터리 삭제 (PV 목록과 디렉터리 모두 비어 있음) |
| 직접 만든 `keep` (reclaimPolicy **Retain**, 같은 프로비저너) | PV **`Released`** (CLAIM keepc), 노드 디렉터리와 데이터(`kept`) **유지** |

- **동적 프로비저닝 + 기본 `Delete`는 PVC를 지우면 데이터도 지운다.** 중요한 데이터는 StorageClass의 `reclaimPolicy: Retain`이나 PV의 reclaim 정책 변경(`kubectl patch pv … persistentVolumeReclaimPolicy`)으로 보호한다.
- 이미 만들어진 PV의 reclaim 정책은 PV에서 바꿀 수 있다(이 실험에서 정적 PV의 Retain → Delete 전환으로 확인).

##### 7-4. Docker Desktop의 기본 StorageClass

```
$ kubectl --context docker-desktop get sc
hostpath (default)   docker.io/hostpath   Delete   Immediate   expansion=false
$ PVC ddp (100Mi, class 생략)  → 4초 안에 Bound (Pod 없이)
  PV: hostPath /var/lib/k8s-pvs/ddp/pvc-…   reclaim Delete
$ PVC 삭제 → PV 삭제 (남은 PV 0개)
```

- Docker Desktop은 `Immediate`라서 Pod 없이도 바로 바인딩된다. kind(`WaitForFirstConsumer`)와 다른 점이다.
- 경로는 Docker Desktop **VM 내부**이다(3-3).

##### 7-5. 정적 NFS PV (실측)

```yaml
kind: PersistentVolume
spec:
  capacity: {storage: 5Gi}
  accessModes: [ReadWriteMany]
  persistentVolumeReclaimPolicy: Retain
  storageClassName: nfs-static
  mountOptions: [nfsvers=4.1]
  nfs: {server: 10.96.28.200, path: /}
```

- PVC(`ReadWriteMany`, `nfs-static`, 1Gi)가 이 PV에 Bound(5Gi 표시)되었고, 서로 다른 노드의 Pod 2개가 같은 PVC를 마운트해 한쪽이 쓴 `via-pvc`를 다른 쪽이 읽었다.
- `mountOptions: [nfsvers=4.1]`이 반영돼 `vers=4.1`로 마운트됐다(인라인 `nfs:` 때는 4.2).

---

#### 8. 선택 가이드와 함정

| 질문 | 선택 |
|---|---|
| 컨테이너끼리 임시 공유 | `emptyDir` |
| 메모리처럼 빠른 임시 공간 | `emptyDir.medium: Memory` (+ `sizeLimit`, 메모리 소모 주의) |
| 노드 에이전트가 노드 파일 접근 | `hostPath` (읽기 전용, `type` 지정) |
| 여러 노드에서 동시에 읽기/쓰기 | NFS 등 RWX 가능한 네트워크 스토리지 |
| 앱 데이터 영구 저장 | PVC (클라우드 디스크/CSI) |
| StatefulSet | `volumeClaimTemplates` (Pod마다 PVC 자동 생성, 6장) |
| 기존 NFS를 쿠버네티스에 연결 | **정적 PV** |
| 개발자 셀프서비스 | **동적 프로비저닝 + StorageClass** |
| 데이터 보호 | `reclaimPolicy: Retain` + 백업 |

함정:

- **emptyDir `sizeLimit` 초과는 Pod 퇴출**이다(약 1분 안). 쓰기는 먼저 성공한다.
- hostPath 데이터는 **노드에 종속**. Pod가 다른 노드로 가면 데이터가 안 보인다.
- `Released` PV는 `claimRef`를 지우기 전까지 재사용되지 않고, 지우면 **이전 데이터가 그대로 노출**된다.
- 정적 hostPath PV에 `Delete`를 걸면 `Failed`가 된다.
- 동적 프로비저닝 + `Delete`: **PVC 삭제 = 데이터 삭제**.
- WaitForFirstConsumer에서 PVC가 `Pending`인 것은 정상이다(Pod가 만들어져야 진행).
- 사용 중인 PVC/PV는 삭제해도 `Terminating`으로 남는다(보호 finalizer).
- PVC는 요청한 크기보다 큰 PV에 붙으면 PV 전체 크기가 보이고, 구현에 따라 **크기가 강제되지 않는다**(local-path는 300MB 기록 성공).
- 확장은 StorageClass가 `allowVolumeExpansion: true`여야 하고, 축소와 accessModes 변경은 안 된다.
- NFS 서버를 Pod가 아닌 **노드에서 해석 가능한 주소(IP)**로 지정한다. (이 실험에서 클러스터 DNS 이름은 성공하지 못했다.)

---

#### 9. 확인하지 못한 부분

- **클라우드 스토리지**(EKS의 EBS CSI/EFS CSI, `gp3` StorageClass, AZ 제약, 스냅샷), 실제 CSI 드라이버 설치와 동작은 확인하지 못했다. kind의 local-path와 Docker Desktop의 hostpath 프로비저너만 확인했다.
- **볼륨 확장**: `allowVolumeExpansion: true`인 StorageClass에서의 온라인/오프라인 확장은 실험하지 않았다(거부 케이스만 확인).
- **볼륨 스냅샷, 클론, `volumeAttributesClass`**, `ReadWriteOncePod`, `ReadOnlyMany`, `volumeMode: Block`은 다루지 않았다.
- **같은 노드의 여러 Pod가 RWO PVC를 공유**할 때와 RWO PV를 다른 노드의 Pod가 쓰려 할 때의 동작(Multi-Attach 오류 등)은 확인하지 않았다.
- **`Recycle` 정책**, PV `Pending` 상태, `Failed` PV의 수동 복구 절차는 실험하지 않았다.
- **StatefulSet `volumeClaimTemplates`와 PVC 보존 정책**(`persistentVolumeClaimRetentionPolicy`)은 이번에 실험하지 않았다. 6장은 개념 비교만 다뤘다.
- **emptyDir `medium: Memory`와 컨테이너 메모리 한도의 상호작용**, 노드 재부팅 시 데이터 소실.
- **NFS**: DNS 이름으로 지정했을 때 마운트가 안 된 정확한 원인, 서버 장애/네트워크 단절 시 hang 동작(`hard` 마운트), NFS 권한(UID/GID, root squash), 성능, 서버 Pod의 OOMKilled 원인은 확인하지 못했다. NFS 서버는 데모용(`itsthenetwork/nfs-server-alpine`)이며 운영 구성이 아니다.
- **hostPath 보안**(`/`나 소켓 마운트의 위험)은 문서 지식이며 시험하지 않았다. hostPath `readOnly`의 쓰기 거부도 `File` 타입 로그에서 확인하지 못했다.
- `hostPath`의 `type` 값 중 `Socket`, `CharDevice`, `BlockDevice`, `FileOrCreate`는 실험하지 않았다.
- PV의 `nodeAffinity`/`local` 볼륨 타입을 **직접 정의**하는 방법은 다루지 않았다(프로비저너가 만든 PV의 값을 보기만 했다).
- 실험 종료 시 kind 노드 컨테이너(`t14-worker`, `t14-worker2`)가 한동안 `docker kill`/`rm -f`로 종료되지 않았다(`did not receive an exit event`). 몇 분 뒤 스스로 종료되어 삭제했다. 원인(privileged NFS 서버 Pod 등)은 조사하지 않았다.
