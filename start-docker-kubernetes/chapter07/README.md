### 7장. 쿠버네티스 리소스의 관리와 설정

> 정리 범위: 네임스페이스(Namespace), 컨피그맵(ConfigMap) (Kustomize 포함), 시크릿(Secret)의 개념과 동작
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드 `docker-desktop`, 런타임 `docker://28.0.4`), kubectl v1.35.2
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 실험용 네임스페이스(`t7`, `t7-a`, `t7-b`)는 끝나고 모두 삭제했다.

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. 네임스페이스
2. 컨피그맵
3. 시크릿
4. 컨피그맵 vs 시크릿
5. 확인하지 못한 부분

---

#### 1. 네임스페이스

##### 1-1. 개념

**하나의 클러스터를 논리적으로 나누는 칸막이**다. 오브젝트 이름이 **네임스페이스 안에서만 유일**하면 되고, 팀/환경(dev, stg)/애플리케이션별로 리소스를 분리하거나 권한(RBAC), 사용량 제한(ResourceQuota)을 네임스페이스 단위로 걸 때 쓴다.

```
$ k get ns
default           Active   126d
kube-node-lease   Active   126d
kube-public       Active   126d
kube-system       Active   126d
```

| 기본 네임스페이스 | 용도 (이 환경에서 확인한 내용) |
|---|---|
| `default` | 네임스페이스를 지정하지 않으면 들어가는 곳 |
| `kube-system` | 쿠버네티스 시스템 컴포넌트 (apiserver, etcd, scheduler, coredns, kube-proxy …). `get pod -A`에서 9개 |
| `kube-public` | 인증 없이 읽을 수 있는 공개 정보. `cluster-info` ConfigMap이 있다 |
| `kube-node-lease` | 노드마다 하나씩 있는 `Lease` 오브젝트(노드 하트비트)를 보관. `lease/docker-desktop`이 있다 |

- 네임스페이스를 만들면 `kube-root-ca.crt` ConfigMap과 `default` ServiceAccount가 자동으로 생긴다 (6장 etcd 키 목록에서도 확인).
- 모든 네임스페이스는 `kubernetes.io/metadata.name=<이름>` 라벨을 자동으로 가진다.

##### 1-2. 네임스페이스에 속하는 것과 속하지 않는 것

- 속하는 것: pods, services, deployments, replicasets, configmaps, secrets, PVC, ingress …
- **속하지 않는 클러스터 범위** 리소스(`api-resources --namespaced=false`): namespaces, nodes, persistentvolumes, storageclasses, clusterroles, clusterrolebindings, customresourcedefinitions, ingressclasses …
- 오브젝트를 명령으로 조회할 때 `-n <네임스페이스>`, 전체는 `-A`(`--all-namespaces`)를 쓴다. 지정하지 않으면 컨텍스트에 설정된 네임스페이스, 설정이 없으면 `default`다.

##### 1-3. 같은 이름을 공존시킬 수 있다

```
$ k create ns t7-a ; k create ns t7-b
$ k -n t7-a create deploy web --image=nginx:alpine ; k -n t7-b create deploy web --image=nginx:alpine
$ k -n t7-a expose deploy web --port=80 ; k -n t7-b expose deploy web --port=80
```

`t7-a`와 `t7-b`에 이름이 같은 Deployment `web`과 Service `web`이 문제없이 생겼다.

##### 1-4. 네임스페이스는 네트워크 격리가 아니다

`t7-a`의 `cli` Pod에서 확인했다.

```
$ k -n t7-a exec cli -- cat /etc/resolv.conf
nameserver 10.96.0.10
search t7-a.svc.cluster.local svc.cluster.local cluster.local

$ k -n t7-a exec cli -- wget -qO- http://web                    # 같은 네임스페이스의 web (이름만)
$ k -n t7-a exec cli -- wget -qO- http://web.t7-b               # 다른 네임스페이스의 web
$ k -n t7-a exec cli -- nslookup web.t7-b.svc.cluster.local     # → 10.99.190.13
$ k -n t7-a exec cli -- wget -qO- http://<t7-b Pod IP>          # Pod IP 직접 접근
```

- 네 요청 모두 `<!DOCTYPE html>` 응답이 왔다. **다른 네임스페이스의 Service를 `<서비스>.<네임스페이스>`로, 심지어 Pod IP로 직접 접근할 수 있었다.**
- DNS의 `search` 목록에 자기 네임스페이스(`t7-a.svc.cluster.local`)가 맨 앞에 들어 있어서 **같은 네임스페이스의 Service는 이름만으로 해석**되고, 다른 네임스페이스는 `<서비스>.<네임스페이스>`로 써야 한다. 전체 이름(FQDN)은 `<서비스>.<네임스페이스>.svc.cluster.local`이다.
- 네임스페이스는 **이름과 권한/자원 관리의 경계**이지 네트워크 차단 장치가 아니다. 통신을 막으려면 NetworkPolicy가 필요하다 (이 환경의 CNI가 NetworkPolicy를 지원하는지는 확인하지 않았다).

##### 1-5. 네임스페이스 단위의 자원 제한: ResourceQuota

`t7-b`에 `pods=1, configmaps=3`으로 제한을 걸고 Deployment를 3개로 늘렸다.

```
$ k -n t7-b create quota q --hard=pods=1,configmaps=3
$ k -n t7-b scale deploy web --replicas=3
$ k -n t7-b get deploy web
web   1/3   1     1                                       # 1개만 생성됨
$ k -n t7-b get rs -o jsonpath='{.items[0].status.conditions[0].message}'
pods "web-5bd64f6ccb-kqzwv" is forbidden: exceeded quota: q, requested: pods=1, used: pods=1, limited: pods=1
$ k -n t7-b describe quota q
configmaps  1     3          # kube-root-ca.crt가 이미 1개를 차지
pods        1     1

$ for i in 1 2 3; do k -n t7-b create cm c$i --from-literal=a=b; done
configmap/c1 created
configmap/c2 created
error: ... configmaps "c3" is forbidden: exceeded quota: q, requested: configmaps=1, used: configmaps=3, limited: configmaps=3
```

- 한도를 넘는 요청은 API 서버가 거부한다. Deployment는 거부당한 Pod를 계속 만들려고 시도하므로 ReplicaSet 상태 조건에 `forbidden` 메시지가 남는다.
- 자동 생성된 `kube-root-ca.crt`도 ConfigMap 개수에 포함되어 3개 중 1개를 쓰고 시작한다.

##### 1-6. 컨텍스트의 기본 네임스페이스

`-n`을 매번 쓰지 않으려면 컨텍스트에 기본 네임스페이스를 설정한다. 원래 kubeconfig를 건드리지 않으려고 복사본(`KUBECONFIG`)에서 확인했다.

```
$ kubectl config set-context docker-desktop --namespace=t7-a      # 복사본 kubeconfig에서
$ kubectl --context docker-desktop get pod                        # -n 없이
cli
web-5bd64f6ccb-r8v8w
```

##### 1-7. 네임스페이스를 삭제하면

```
$ k delete ns t7-b --wait=true
$ k get ns t7-b
Error from server (NotFound): namespaces "t7-b" not found
$ k -n t7-b get all
No resources found in t7-b namespace.
```

네임스페이스를 지우면 **그 안의 Deployment, Pod, Service, ConfigMap, Quota 등이 모두 함께 삭제**된다. 되돌릴 수 없으므로 운영에서는 특히 조심해야 한다.

#### 2. 컨피그맵 (ConfigMap)

##### 2-1. 개념

**설정 값을 컨테이너 이미지와 분리해서 키-값으로 저장하는 오브젝트**다. 이미지를 다시 빌드하지 않고 환경별(dev/stg/prod)로 설정을 바꿔 끼울 수 있다. 도커에서 `-e`, `--env-file`, 바인드 마운트로 하던 일을 쿠버네티스 오브젝트로 선언한 것이다. **민감하지 않은 데이터용**이다.

##### 2-2. 만드는 방법 세 가지

```
$ k -n t7 create cm lit  --from-literal=GREETING=hello --from-literal=LANG=ko
$ k -n t7 create cm file --from-file=app.conf                  # 파일 하나가 키 하나 (키 = 파일 이름)
$ k -n t7 create cm envf --from-env-file=app.env               # KEY=VALUE 줄마다 키 하나

lit  => {"GREETING":"hello","LANG":"ko"}
file => {"app.conf":"server {\n  listen 8080;\n}\n"}
envf => {"LOG_LEVEL":"debug","MODE":"dev"}
```

| 옵션 | 키 | 값 | 용도 |
|---|---|---|---|
| `--from-literal` | 지정한 이름 | 지정한 문자열 | 간단한 값 |
| `--from-file` | 파일 이름 | 파일 내용 전체 | 설정 파일 통째로 |
| `--from-env-file` | 줄의 KEY | 줄의 VALUE | env 파일 |

- 바이너리 파일(`--from-file`로 16바이트 난수 파일)을 넣으면 `data`가 아니라 **`binaryData`에 base64로** 들어갔다 (`{.data}` 출력이 비고 `{.binaryData}`에 `{"bin.dat":"2leZ…=="}`).
- 키 이름은 영문자, 숫자, `-`, `_`, `.`만 허용된다 (`a b=1`은 `is not a valid key name`으로 거부).

##### 2-3. 파드에서 쓰는 방법 세 가지

```yaml
spec:
  containers:
  - name: c
    image: alpine:3.20
    env:                                  # ① 키 하나를 환경 변수 하나로
    - name: MY_GREETING
      valueFrom: {configMapKeyRef: {name: lit, key: GREETING}}
    envFrom:                              # ② 모든 키를 환경 변수로
    - configMapRef: {name: envf}
      prefix: CFG_
    volumeMounts:                         # ③ 볼륨으로 마운트
    - {name: vol, mountPath: /etc/cfg}
    - {name: sub, mountPath: /etc/one.conf, subPath: app.conf}   # 파일 하나만
  volumes:
  - {name: vol, configMap: {name: lit}}
  - {name: sub, configMap: {name: file}}
```

```
$ k -n t7 exec p -- env | grep -E "MY_|CFG_"
CFG_LOG_LEVEL=debug
MY_GREETING=hello
CFG_MODE=dev

$ k -n t7 exec p -- ls -la /etc/cfg
drwxr-xr-x  ..2026_10_21_13_21_33.494506344
lrwxrwxrwx  ..data -> ..2026_10_21_13_21_33.494506344
lrwxrwxrwx  GREETING -> ..data/GREETING
lrwxrwxrwx  LANG -> ..data/LANG

$ k -n t7 exec p -- mount | grep -E "/etc/cfg|one.conf"
/dev/vda1 on /etc/cfg type ext4 (ro,relatime,discard)
/dev/vda1 on /etc/one.conf type ext4 (ro,relatime,discard)
```

| 방식 | 특징 |
|---|---|
| `env` + `configMapKeyRef` | 필요한 키만 골라 환경 변수 이름을 직접 지정 |
| `envFrom` + `configMapRef` | 모든 키를 한 번에. `prefix`로 이름 충돌 방지. 환경 변수 이름으로 쓸 수 없는 키는 건너뛴다 |
| 볼륨 마운트 | 키가 **파일**이 되고 값이 파일 내용. 설정 파일(nginx.conf 등)에 적합 |
| `subPath` | 디렉터리 전체를 덮지 않고 **파일 하나만** 기존 디렉터리에 끼워 넣을 때 사용 |

- 볼륨으로 마운트하면 각 키가 **심볼릭 링크**(`GREETING → ..data/GREETING`)이고, `..data`가 타임스탬프 이름의 실제 디렉터리를 가리킨다. 이 구조가 뒤에 나오는 "자동 갱신"의 원리다.
- ConfigMap 볼륨은 **읽기 전용**(`ro`)이다. 같은 Pod에서 시크릿 볼륨은 tmpfs였던 것(3절)과 달리 ConfigMap은 노드 디스크(`ext4`)에 마운트되었다.

##### 2-4. 변경 반영: 볼륨은 갱신되고, 환경 변수는 갱신되지 않는다

위 Pod가 실행 중인 상태에서 ConfigMap 두 개를 수정했다 (`lit`의 GREETING을 `HELLO-V2`로, `file`의 app.conf를 9090으로).

```
$ k -n t7 patch cm lit  -p '{"data":{"GREETING":"HELLO-V2"}}'
$ k -n t7 patch cm file --type=merge -p '{"data":{"app.conf":"server { listen 9090; }\n"}}'
(3초 간격으로 폴링)
volume updated after 70s: HELLO-V2

$ k -n t7 exec p -- sh -c 'echo env=$MY_GREETING; cat /etc/one.conf'
env=hello                          # 환경 변수: 그대로
server {
  listen 8080;                     # subPath 마운트: 그대로
}
```

| 소비 방식 | 변경 반영 (실측) |
|---|---|
| 볼륨 마운트 | **약 70초 뒤 반영** (kubelet이 주기적으로 동기화하고 `..data` 심볼릭 링크를 새 디렉터리로 교체) |
| 환경 변수 (`env`, `envFrom`) | **반영 안 됨**. 컨테이너가 시작될 때 한 번 주입되므로 **Pod 재시작이 필요** |
| 볼륨 + `subPath` | **반영 안 됨** (심볼릭 링크 구조를 거치지 않는 직접 바인드 마운트라서) |

- 70초는 이 환경에서 1회 측정한 값이다. 지연은 kubelet의 동기화 주기와 캐시에 좌우되어 환경마다 다를 수 있다.
- 갱신 후 `/etc/cfg/..data`가 새 타임스탬프 디렉터리(`..2026_10_21_13_22_52.146225483`)를 가리키는 것을 확인했다. 파일 내용이 아니라 링크를 통째로 바꾸기 때문에 읽는 쪽에서 반쯤 쓰인 파일을 볼 일이 없다.
- 환경 변수로 쓰는 설정을 바꾸고 싶다면 Deployment를 재시작(`rollout restart`)해야 한다. 파일이 바뀐 것을 프로세스가 스스로 다시 읽는지는 애플리케이션에 달려 있다.

##### 2-5. 존재하지 않는 ConfigMap을 참조하면

```
$ k apply -f pod2.yaml        # configMapKeyRef가 없는 ConfigMap "nope"를 참조
$ k -n t7 get pod p2
p2    0/1   CreateContainerConfigError   0     6s
  Warning  Failed  kubelet  Error: configmap "nope" not found

$ k -n t7 create cm nope --from-literal=GREETING=late              # 나중에 생성
$ k -n t7 get pod p2          # 20초 뒤
p2    1/1   Running   0     26s                                    # 자동으로 시작됨
```

- 컨테이너가 시작되지 못하고 `CreateContainerConfigError`로 남는다. ConfigMap을 만들면 kubelet이 재시도해서 **Pod를 다시 만들지 않아도 시작**되었다.
- **ConfigMap은 네임스페이스에 속하므로 다른 네임스페이스의 ConfigMap은 참조할 수 없다.** `t7`의 `lit`을 `t7-a`의 Pod가 참조하자 `configmap "lit" not found`로 `CreateContainerConfigError`가 났다.

##### 2-6. immutable과 크기 제한

```
$ k -n t7 patch cm imm -p '{"immutable":true}'
$ k -n t7 patch cm imm -p '{"data":{"a":"2"}}'
The ConfigMap "imm" is invalid: data: Forbidden: field is immutable when `immutable` is set

$ k -n t7 create cm sz1048576 --from-file=b.txt      # 1,048,576 바이트(1 MiB) → created
$ k -n t7 create cm sz1100000 --from-file=b.txt      # 1,100,000 바이트
error: ... is invalid: []: Too long: may not be more than 1048576 bytes
```

- `immutable: true`를 켜면 `data`를 바꿀 수 없다 (삭제 후 재생성만 가능). 실수로 바꾸는 것을 막고, kubelet이 변경 감시(watch)를 하지 않아 API 서버 부하도 줄어든다 (감시를 안 한다는 부분은 문서 설명이고 실측하지 않았다).
- 크기 한도는 **1 MiB(1,048,576바이트)** 이고 초과하면 거부된다. 큰 파일은 ConfigMap에 맞지 않는다.

##### 2-7. etcd에는 평문으로 저장된다

```
$ k -n kube-system exec etcd-docker-desktop -- etcdctl ... get /registry/configmaps/t7/lit | strings | grep -E "GREETING|HELLO"
GREETING
HELLO-V2
```

ConfigMap의 값은 etcd에 그대로 저장된다 (설정 값이 평문으로 보이는 것이 정상이다). 민감한 정보를 넣는 곳이 아니다.

##### 2-8. Kustomize로 ConfigMap 관리하기

###### 개념

**Kustomize**는 YAML 템플릿 없이 **"기본(base) YAML 위에 변경 사항을 덧씌우는" 방식**으로 매니페스트를 만들어 주는 도구다. 쿠버네티스 SIG-CLI가 만들었고 **kubectl에 내장**되어 있어 따로 설치할 필요가 없다 (이 환경: `kubectl version --client` → `Kustomize Version: v5.7.1`).

- `kustomization.yaml` 파일이 있는 디렉터리 하나가 한 단위다. 이 파일에 "어떤 YAML을 모을지(`resources`)", "무엇을 생성할지(`configMapGenerator`)", "무엇을 바꿀지(`patches`, `namePrefix`, `images` …)"를 적는다.
- 원본 YAML 파일은 수정하지 않고, 실행할 때 결과물을 만들어 낸다. 환경별 차이는 `overlays`로 분리한다. Helm이 **템플릿에 변수를 채우는** 방식이라면 Kustomize는 **완성된 YAML에 패치를 얹는** 방식이다.
- ConfigMap과 Secret에서 Kustomize가 특히 쓸모 있는 이유는 **`configMapGenerator` / `secretGenerator`가 내용 해시를 이름에 붙여 주기** 때문이다. 2-4절에서 본 "ConfigMap을 바꿔도 환경 변수는 갱신되지 않고, Pod도 재시작되지 않는" 문제를 해결한다.

| 명령 | 동작 |
|---|---|
| `kubectl kustomize <디렉터리>` | 클러스터에 적용하지 않고 **최종 YAML만 출력** (렌더링) |
| `kubectl apply -k <디렉터리>` | 렌더링 결과를 적용 |
| `kubectl diff -k <디렉터리>` | 적용했을 때 바뀔 내용을 미리 비교 |
| `kubectl delete -k <디렉터리>` | 렌더링된 모든 리소스를 삭제 |

###### 기본 사용법: configMapGenerator

```
base/
├── deploy.yaml            # Deployment (ConfigMap web-config를 envFrom과 볼륨으로 참조)
├── app.properties         # listen=8080
└── kustomization.yaml
```

```yaml
# base/kustomization.yaml
resources:
- deploy.yaml
configMapGenerator:
- name: web-config
  literals:                 # --from-literal에 해당
  - GREETING=hello
  - LANG=ko
  files:                    # --from-file에 해당
  - app.properties
```

`configMapGenerator`는 `literals`, `files`, `envs`(`--from-env-file`에 해당)를 지원해서 2-2절의 `kubectl create cm` 세 가지 방법과 대응된다.

```
$ kubectl kustomize base
apiVersion: v1
data:
  GREETING: hello
  LANG: ko
  app.properties: |
    listen=8080
kind: ConfigMap
metadata:
  name: web-config-cm6m864f88          # ← 이름 뒤에 내용 해시가 붙음
---
apiVersion: apps/v1
kind: Deployment
...
        envFrom:
        - configMapRef:
            name: web-config-cm6m864f88   # ← Deployment의 참조도 자동으로 바뀜
...
      volumes:
      - configMap:
          name: web-config-cm6m864f88     # ← 볼륨 참조도 마찬가지
```

- 원본 `deploy.yaml`에는 `name: web-config`라고만 썼는데, 렌더링 결과에서는 **ConfigMap 이름과 이를 참조하는 모든 곳이 해시가 붙은 이름으로 치환**되었다. 참조하는 곳(`envFrom`, `volumes`)을 Kustomize가 알아서 찾아 바꾼다.
- 해시는 **내용에서 계산**된다. 값을 바꿨다가 원래대로 돌리면 해시도 원래 값(`cm6m864f88`)으로 돌아온다 (같은 내용이면 같은 이름).

###### 핵심 효과: 설정이 바뀌면 자동으로 롤링 업데이트된다

```
$ k -n t7k apply -k base
configmap/web-config-cm6m864f88 created
deployment.apps/web created

# GREETING=hello → hello-v2 로 수정 후
$ kubectl kustomize base | grep "name: web-config"
  name: web-config-fddd96ddbm                     # 해시가 달라짐

$ k -n t7k apply -k base
configmap/web-config-fddd96ddbm created           # 새 ConfigMap이 생성됨 (기존 것을 수정하지 않음)
deployment.apps/web configured                    # Deployment의 참조가 바뀜 → Pod 템플릿 변경

$ k -n t7k get cm,rs
configmap/web-config-cm6m864f88                   # 이전 ConfigMap은 그대로 남아 있음
configmap/web-config-fddd96ddbm
replicaset.apps/web-7cdf6bf94d   0 0 0            # 이전 ReplicaSet (replicas 0)
replicaset.apps/web-7d94957b6d   1 1 1            # 새 ReplicaSet
$ k -n t7k exec deploy/web -- env | grep GREETING
GREETING=hello-v2                                  # 새 값이 환경 변수로 반영됨
```

- 설정이 바뀌면 **ConfigMap 이름이 바뀌고 → Deployment의 Pod 템플릿이 바뀌므로 → 6장에서 본 롤링 업데이트가 자동으로 일어난다.** `rollout restart`를 따로 할 필요가 없다. 환경 변수도, `subPath` 마운트도 새 Pod가 새 ConfigMap을 쓰므로 모두 반영된다.
- **롤백이 정확하다.** 기존 ConfigMap을 덮어쓰지 않고 새로 만들기 때문에, `rollout undo`가 이전 ReplicaSet과 **이전 ConfigMap**을 함께 되살린다.

```
$ k -n t7k rollout undo deploy/web
$ k -n t7k exec deploy/web -- env | grep GREETING
GREETING=hello                                     # 이전 설정으로 정확히 복귀
```

  이름 고정 ConfigMap을 덮어쓰는 방식이라면 롤백해도 ConfigMap은 새 값 그대로라서 설정과 이미지 버전이 어긋난다.
- 적용 전에 `kubectl diff -k`로 변경 내용을 확인할 수 있다.

```
$ k -n t7k diff -k base          # GREETING=hello-v3로 바꾼 상태
-            name: web-config-cm6m864f88
+            name: web-config-b5mbm55d9m
+  GREETING: hello-v3
```

###### 단점: 오래된 ConfigMap이 쌓인다

위 로그에서 `web-config-cm6m864f88`이 새 버전 적용 후에도 남아 있다. 기본 동작은 **이전 ConfigMap을 지우지 않는다.**

- 롤백을 위해 남겨 두는 것이 장점이지만, 설정을 바꿀 때마다 ConfigMap이 계속 쌓인다.
- 정리하려면 라벨을 달고 `--prune`을 쓸 수 있다. 라벨 `env=dev`가 붙은 리소스 중 이번 렌더링에 없는 것이 삭제된다.

```
$ k apply -k overlays/dev --prune -l env=dev
configmap/dev-web-config-7dmck2kmg6 created
deployment.apps/dev-web configured
configmap/dev-web-config-g7mdk5fck6 pruned          # 이전 ConfigMap 삭제
```

  단, 정리하면 그 ConfigMap을 쓰던 이전 ReplicaSet으로 롤백할 때는 **ConfigMap이 없어서** 롤백이 정상 동작하지 않는다 (이 경우 실험하지 않았다). 또한 `--prune` 사용 시 kubectl이 "Deprecated: kubectl apply will no longer prune non-namespaced resources by default …" 경고를 출력했다.

###### `generatorOptions`: 해시 끄기, 라벨/어노테이션 붙이기

```yaml
generatorOptions:
  disableNameSuffixHash: true      # 이름에 해시를 붙이지 않음
  labels: {team: platform}         # 생성되는 모든 ConfigMap/Secret에 라벨
  annotations: {note: generated}
```

- 해시를 끄면 이름이 `plain`, `envcm`처럼 고정된다. 라벨과 어노테이션은 생성된 리소스에 모두 붙는다 (렌더링으로 확인).
- **해시를 끄면 자동 롤링 업데이트가 사라진다.** 해시 없이 값만 바꿔 적용해 봤다.

```
$ k -n t7k-nh apply -k nh          # GREETING=v1 → v2
configmap/web-config configured
deployment.apps/web unchanged       # Deployment가 변하지 않음
$ k -n t7k-nh get pod
pod/web-595fb67c46-l9mgg            # 같은 Pod
$ k -n t7k-nh exec deploy/web -- env | grep GREETING
GREETING=v1                         # 환경 변수는 옛 값 (ConfigMap은 v2)
$ k -n t7k-nh rollout restart deploy/web
$ k -n t7k-nh exec deploy/web -- env | grep GREETING
GREETING=v2                         # 재시작해야 반영
```

  즉 이름 고정 방식은 2-4절에서 본 동작(ConfigMap만 바뀌고 Pod는 그대로)과 같다. 설정 변경을 **배포 이벤트로 만들고 싶다면 해시를 켜 두는 것(기본값)** 이 낫다.

###### `secretGenerator`

`configMapGenerator`와 같은 형식이고 같은 해시 규칙이 적용된다.

```yaml
secretGenerator:
- name: db
  literals: [password=S3cr3t!]
  type: Opaque
```

```
$ kubectl kustomize sec
data:
  password: UzNjcjN0IQ==             # 자동으로 base64 인코딩
kind: Secret
metadata:
  name: db-mk758f54bf                 # 해시 접미사
type: Opaque
```

- 평문을 `kustomization.yaml`에 직접 쓰므로 **Git에 올리면 비밀번호가 노출된다.** `files`로 파일을 가리키게 하고 그 파일을 Git에서 제외하거나, 별도의 시크릿 관리 도구를 쓰는 편이 안전하다 (이 부분은 실험하지 않았다).

###### overlay: 환경별로 같은 base를 재사용

```
base/                      # 공통
overlays/
├── dev/kustomization.yaml
└── prod/kustomization.yaml
```

```yaml
# overlays/dev/kustomization.yaml
namespace: t7k-dev
namePrefix: dev-
labels:
- pairs: {env: dev}
  includeSelectors: false      # 셀렉터에는 넣지 않고 메타데이터 라벨에만
resources:
- ../../base
configMapGenerator:
- name: web-config
  behavior: merge              # base의 ConfigMap과 키 단위로 병합
  literals:
  - GREETING=hello-dev         # 덮어쓰기
  - EXTRA=only-dev             # 추가
patches:
- target: {kind: Deployment, name: web}
  patch: |-
    - op: replace
      path: /spec/replicas
      value: 2
```

```yaml
# overlays/prod/kustomization.yaml
namespace: t7k-prod
namePrefix: prod-
labels:
- pairs: {env: prod}
  includeSelectors: false
resources:
- ../../base
images:
- name: nginx
  newTag: 1.27-alpine           # 이미지 태그만 교체
configMapGenerator:
- name: web-config
  behavior: replace            # base의 ConfigMap을 통째로 교체
  literals:
  - GREETING=hello-prod
patches:
- path: resources.yaml          # 전략적 병합 패치(Deployment 일부 필드만 적은 파일)
```

```
$ k create ns t7k-dev ; k create ns t7k-prod        # namespace 필드는 네임스페이스를 만들어 주지 않음
$ k apply -k overlays/dev ; k apply -k overlays/prod

# dev: ConfigMap dev-web-config-g7mdk5fck6, Deployment dev-web, replicas 2
$ k -n t7k-dev exec deploy/dev-web -- sh -c 'env | grep -E "GREETING|EXTRA|LANG"; cat /etc/app/app.properties'
EXTRA=only-dev
GREETING=hello-dev          # 덮어씀
LANG=ko                     # base의 값이 유지됨 (merge)
listen=8080                 # base의 파일도 유지됨

# prod: ConfigMap prod-web-config-mtd7kb946t, Deployment prod-web, replicas 3
$ k -n t7k-prod exec deploy/prod-web -- sh -c 'env | grep -E "GREETING|EXTRA|LANG"; cat /etc/app/app.properties'
GREETING=hello-prod
cat: can't open '/etc/app/app.properties': No such file or directory     # replace라 base의 키가 모두 사라짐
$ k -n t7k-prod get deploy prod-web -o jsonpath='{.spec.template.spec.containers[0].image}{" "}{.spec.template.spec.containers[0].resources.limits}'
nginx:1.27-alpine {"memory":"128Mi"}
```

| 기능 | 효과 (이 실험) |
|---|---|
| `namespace` | 모든 리소스를 지정한 네임스페이스로 (네임스페이스를 만들지는 않는다) |
| `namePrefix` / `nameSuffix` | 리소스 이름에 접두/접미 (`web` → `dev-web`). 참조하는 곳도 함께 바뀜 |
| `labels` | 모든 리소스에 라벨 추가. `includeSelectors: false`로 셀렉터에는 넣지 않음 |
| `images` | 이미지 이름/태그만 교체 (`nginx` → `nginx:1.27-alpine`) |
| `patches` | JSON 6902 패치(`op/path/value`) 또는 전략적 병합 패치(YAML 일부) |
| `behavior: merge` | 부모(base)의 같은 이름 ConfigMap과 **키 단위로 병합** (덮어쓰기+추가) |
| `behavior: replace` | 부모의 ConfigMap을 **통째로 교체** (base의 키가 사라짐) |
| `behavior: create` (기본) | 같은 이름이 이미 있으면 충돌 (이 실험에서는 시도하지 않음) |

- overlay는 `resources`에 base 디렉터리를 가리키기만 한다. base는 수정하지 않는다.
- 같은 base에서 dev는 해시 `g7mdk5fck6`, prod는 `mtd7kb946t`로 서로 다른 ConfigMap 이름이 만들어졌다 (내용과 이름 접두어가 다르므로).
- `labels`에서 `includeSelectors: false`를 쓴 이유: Deployment/ReplicaSet의 `selector`는 만든 뒤 바꿀 수 없으므로(ReplicaSet은 6장에서 확인), 라벨 추가가 셀렉터를 건드리면 이미 있는 리소스에 적용할 때 거부될 수 있다. 이 부분은 문서 지식이고 Kustomize 라벨로는 실험하지 않았다.
- 과거 `commonLabels` 필드는 셀렉터에도 라벨을 넣는데, 최근에는 `labels` 필드를 쓰는 것이 권장된다 (이 환경에서 `commonLabels`는 사용하지 않았다).

###### 정리: 언제 무엇을 쓰나

| 상황 | 방법 |
|---|---|
| 한 번 만들고 끝 | `kubectl create cm ...` |
| 설정을 YAML/Git으로 관리 | ConfigMap YAML 직접 작성 |
| 설정 변경 시 Pod가 자동으로 새 설정으로 교체되길 원함 | **`configMapGenerator`** (해시 접미사) |
| dev/stg/prod 환경별 차이 관리 | **overlay** + `namePrefix`, `patches`, `images`, `behavior: merge` |

#### 3. 시크릿 (Secret)

##### 3-1. 개념

**패스워드, 토큰, 키처럼 민감한 데이터를 저장하는 오브젝트**다. 구조와 사용법은 ConfigMap과 거의 같지만 (키-값, env/볼륨), 값이 **base64로 인코딩**되어 저장되고 몇 가지 안전장치가 더 붙는다. 3장의 스웜 시크릿에 해당하는 쿠버네티스의 개념이다.

##### 3-2. 가장 중요한 오해: base64는 암호화가 아니다

```
$ k -n t7 create secret generic db --from-literal=username=admin --from-literal=password='S3cr3t!'
$ k -n t7 get secret db -o jsonpath='{.data}'
{"password":"UzNjcjN0IQ==","username":"YWRtaW4="}
$ k -n t7 get secret db -o jsonpath='{.data.password}' | base64 -d
S3cr3t!
```

- `data` 값은 **단순 인코딩**이라 `base64 -d` 한 번으로 풀린다. Secret을 읽을 권한이 있으면 값을 그대로 볼 수 있다.
- etcd에서도 평문으로 저장되었다 (이 환경에서 확인).

```
$ k -n kube-system exec etcd-docker-desktop -- etcdctl ... get /registry/secrets/t7/db | strings
/registry/secrets/t7/db
Secret
...
password
S3cr3t!
username
```

  이 클러스터(Docker Desktop)에는 **저장 시 암호화(encryption at rest)가 설정되어 있지 않아** etcd에 값이 평문으로 들어 있다. etcd 접근 권한이나 백업 파일이 있으면 읽을 수 있다. 운영에서는 API 서버에 `EncryptionConfiguration`을 설정하거나 외부 시크릿 저장소를 쓴다 (이 설정은 직접 해 보지 않았다).
- `kubectl describe`는 값을 숨기고 크기만 보여준다.

```
$ k -n t7 describe secret db
Data
====
password:  7 bytes
username:  5 bytes
```

  다만 `get -o yaml/jsonpath`로는 그대로 나오므로 화면 가림일 뿐 보안 장치가 아니다.

##### 3-3. 종류 (type)

```
$ k create secret --help
  docker-registry   Create a secret for use with a Docker registry
  generic           Create a secret from a local file, directory, or literal value
  tls               Create a TLS secret

$ k -n t7 get secret -o custom-columns=NAME:.metadata.name,TYPE:.type
db     Opaque
reg    kubernetes.io/dockerconfigjson
tlss   kubernetes.io/tls
```

| type | 만드는 명령 | 용도 | 필수 키 |
|---|---|---|---|
| `Opaque` | `create secret generic` | 임의의 사용자 데이터 (기본값) | 없음 |
| `kubernetes.io/dockerconfigjson` | `create secret docker-registry` | 프라이빗 레지스트리 인증 (`imagePullSecrets`에서 사용) | `.dockerconfigjson` |
| `kubernetes.io/tls` | `create secret tls` | TLS 인증서와 키 (Ingress, 8장) | `tls.crt`, `tls.key` |

- type을 `kubernetes.io/tls`로 지정하고 필수 키를 빼면 API 서버가 거부한다.

```
$ k -n t7 apply -f - <<EOF     # type: kubernetes.io/tls, stringData: {foo: bar}
The Secret "badtls" is invalid:
* data[tls.crt]: Required value
* data[tls.key]: Required value
```

- 이 외에 ServiceAccount 토큰, basic-auth, ssh-auth 같은 타입이 더 있으나 직접 만들어 보지는 않았다.

##### 3-4. `stringData`: base64를 직접 하지 않고 쓰기

```
$ k -n t7 apply -f - <<EOF
apiVersion: v1
kind: Secret
metadata: {name: db, namespace: t7}
stringData: {password: "NEW-pass"}
EOF
$ k -n t7 get secret db -o jsonpath='{.data}'
{"password":"TkVXLXBhc3M=","username":"YWRtaW4="}
```

- YAML에 `stringData`로 평문을 쓰면 API 서버가 base64로 바꿔 `data`에 저장한다 (`stringData`는 쓰기 전용이라 조회 시 나오지 않는다).
- 기존 `data`의 다른 키(`username`)는 유지되고 지정한 키(`password`)만 바뀌었다 (`apply`가 병합).
- 이 Secret을 `kubectl create`로 만들어서 `apply` 했기 때문에 `last-applied-configuration` 어노테이션이 없다는 경고가 떴다.

##### 3-5. 파드에서 쓰기

ConfigMap과 같은 방식이고 `secretKeyRef`, `secret.secretName`을 쓴다.

```yaml
    env:
    - name: DB_PASS
      valueFrom: {secretKeyRef: {name: db, key: password}}
    volumeMounts:
    - {name: sec, mountPath: /etc/secret, readOnly: true}
  volumes:
  - name: sec
    secret: {secretName: db, defaultMode: 0400}
```

```
$ k -n t7 exec sp -- sh -c 'echo env=$DB_PASS; ls -lL /etc/secret; mount | grep /etc/secret'
env=S3cr3t!
-r--------   root root   8 password
-r--------   root root   5 username
tmpfs on /etc/secret type tmpfs (ro,relatime,size=7922748k,noswap)
```

- **볼륨은 tmpfs(메모리)** 로 마운트되어 노드 디스크에 남지 않는다. ConfigMap 볼륨이 노드의 ext4였던 것과 대비된다.
- `defaultMode: 0400`이 파일 권한 `-r--------`로 적용되었다.
- 모든 Pod에는 시스템이 만든 ServiceAccount 토큰도 `/run/secrets/kubernetes.io/serviceaccount`에 tmpfs로 마운트된다 (`ca.crt`, `namespace`, `token`).

##### 3-6. 환경 변수는 볼륨보다 노출 면이 넓다

```
$ k -n t7 get pod sp -o jsonpath='{.spec.containers[0].env}'
[{"name":"DB_PASS","valueFrom":{"secretKeyRef":{"key":"password","name":"db"}}}]    # Pod 정의에는 참조만

$ docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' <컨테이너 ID> | grep DB_PASS
DB_PASS=S3cr3t!                                                                      # 컨테이너 설정에는 값이 평문으로
```

- Pod 정의에는 Secret 이름과 키만 있고 값은 없다.
- 하지만 환경 변수로 주입하면 컨테이너 런타임 쪽 설정(`docker inspect`의 `Config.Env`)에 **값이 평문으로** 보인다. 노드에 접근할 수 있는 사람, 프로세스 환경 덤프, 애플리케이션 로그/에러 리포트로 유출될 수 있다. 가능하면 볼륨 마운트(파일)로 전달하는 편이 낫다.

##### 3-7. 변경 반영

```
$ k -n t7 apply -f secret-newpass.yaml
(폴링)
volume after 85s: NEW-pass
$ k -n t7 exec sp -- sh -c 'echo env=$DB_PASS'
env=S3cr3t!                          # 환경 변수는 그대로
```

ConfigMap과 동일하다. **볼륨은 갱신(85초, 1회 측정)되고 환경 변수는 갱신되지 않는다.**

##### 3-8. 크기

- 1 MiB(1,048,576바이트)짜리 값을 가진 `generic` Secret은 `created`로 만들어졌다. 이 경계의 정확한 의미(디코딩 전/후 크기 중 어느 쪽에 한도가 걸리는지)는 확인하지 않았다. 문서상 한도는 1 MiB다.

#### 4. 컨피그맵 vs 시크릿

| 구분 | ConfigMap | Secret |
|---|---|---|
| 용도 | 일반 설정 | 민감한 데이터 |
| 저장 형식 | `data`(문자열), `binaryData`(base64) | `data`(**base64**), `stringData`(쓰기용 평문) |
| 암호화 | 없음 | **기본은 없음** (이 환경의 etcd에서 평문 확인). 별도로 저장 시 암호화 설정 필요 |
| 종류(type) | 없음 | Opaque, dockerconfigjson, tls 등 (타입별 필수 키 검증) |
| 파드에서 사용 | env, envFrom, 볼륨 | env, 볼륨, `imagePullSecrets` |
| 볼륨 마운트 | 노드 디스크 (ext4, ro) | **tmpfs** (메모리) |
| 파일 권한 | 기본 | `defaultMode`로 제어 (0400 확인) |
| 변경 반영 | 볼륨 갱신 (70초), env는 재시작 필요 | 동일 (85초) |
| `subPath` 마운트 | 갱신 안 됨 | 같은 원리로 갱신 안 될 것이다 (Secret에서는 실험하지 않았다) |
| 크기 | 1 MiB 한도 | 1 MiB 한도 |
| `immutable` | 지원 | 지원 (Secret에서는 실험하지 않았다) |
| 네임스페이스 | 네임스페이스에 속함, **다른 네임스페이스 참조 불가** | 동일 (Secret 교차 참조는 실험하지 않았다) |
| `describe` 출력 | 값 표시 | 값 숨김 (크기만) |

- 둘 다 **네임스페이스 리소스**이고, 같은 네임스페이스의 Pod만 참조할 수 있다.
- **Secret은 "암호화된 저장소"가 아니라 "별도로 접근 제어를 걸 수 있는 오브젝트"** 로 이해해야 한다. 안전해지는 것은 RBAC로 `get secrets` 권한을 제한하고(10장), 저장 시 암호화를 켜고, tmpfs로 마운트하는 등 주변 설정을 했을 때다.

#### 5. 확인하지 못한 부분

- **저장 시 암호화(`EncryptionConfiguration`)**: 이 클러스터에는 설정이 없어서 평문임을 확인한 것까지만 했다. 설정 방법과 암호화된 형태는 확인하지 않았다.
- **RBAC로 Secret 접근 제한**: 10장 범위라 실험하지 않았다.
- **Secret의 `subPath`, `immutable`, 교차 네임스페이스 참조**: ConfigMap에서만 실험했고 Secret은 같은 원리일 것으로 정리했다.
- **그 외 Secret 타입**: ServiceAccount 토큰, basic-auth, ssh-auth, bootstrap token은 직접 만들지 않았다. `imagePullSecrets`로 실제 프라이빗 레지스트리에서 pull하는 것도 하지 못했다.
- **ConfigMap 볼륨 갱신 지연(70초, 85초)**: 각 1회 측정이다. kubelet의 `syncFrequency`와 캐시 설정에 따라 달라지는 값으로 보이지만 해당 설정을 확인하지는 않았다.
- **`immutable`의 감시 부하 감소 효과**는 실측하지 않았다.
- **NetworkPolicy**: 네임스페이스 간 통신을 막는 실험은 하지 않았다. 이 환경의 CNI가 지원하는지도 확인하지 않았다.
- **LimitRange**와 ResourceQuota의 cpu/memory 항목(`requests`/`limits`), 네임스페이스 `Terminating` 상태에서 멈추는 경우(finalizer)는 확인하지 않았다.
- **Kustomize 미확인 항목**: 독립 실행 `kustomize` CLI(kubectl 내장 v5.7.1만 사용), `behavior: create` 충돌, `--prune`으로 정리한 뒤 롤백했을 때 동작, Kustomize `labels`와 셀렉터 충돌, `components`, `replacements`, 원격 base(`github.com/...`), Helm과의 조합, `secretGenerator`에서 `files`/`envs`로 평문을 Git 밖에 두는 방법은 실험하지 않았다. 해시 계산 알고리즘(어떤 필드가 포함되는지)도 확인하지 않았고 "내용이 같으면 같은 해시"까지만 확인했다.
- **Secret/ConfigMap 크기 한도의 정확한 계산 기준**: 1 MiB 값을 넣은 Secret이 만들어졌으므로, base64 인코딩 후 크기가 아니라 디코딩된 값 기준일 수 있다고 추정할 뿐 확인하지 않았다.
