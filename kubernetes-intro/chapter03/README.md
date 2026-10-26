### 3장. 쿠버네티스로 컨테이너 실행하기

> 정리 범위: kubectl 주요 명령어, netshoot 컨테이너 개념
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드 `docker-desktop`), kubectl v1.35.2, `nicolaka/netshoot:latest`(arm64, 653MB)
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 주의: 이 PC의 kubectl 기본 컨텍스트는 다른 클러스터를 가리키므로 모든 명령에 `--context docker-desktop`을 명시했다 (아래에서는 `k`로 줄여 적음). 컨텍스트/네임스페이스 변경 실험(`config use-context`, `set-context`)은 **kubeconfig 복사본(`KUBECONFIG`)** 에서만 했고 원본은 건드리지 않았다. 실험은 `t3k` 네임스페이스에서 했고 끝나고 삭제했다 (netshoot 이미지도 삭제).
>
> 다른 책 `시작하세요! 도커/쿠버네티스`와 겹치는 부분은 짧게 적고 링크했다: kubectl이 내부에서 보내는 HTTP 요청([10장](../../start-docker-kubernetes/chapter10/README.md)), 명령형/선언적 차이([12장](../../start-docker-kubernetes/chapter12/README.md)).

```bash
k() { kubectl --context docker-desktop "$@"; }
```

#### 목차

1. kubectl 주요 명령어
2. netshoot 컨테이너
3. 확인하지 못한 부분

---

#### 1. kubectl 주요 명령어

##### 1-1. 기본 형식

```
kubectl [command] [TYPE] [NAME] [flags]
  예)  kubectl  get  pods  web  -n t3k  -o wide
```

| 요소 | 의미 | 예 |
|---|---|---|
| command | 동작 | `get`, `create`, `delete`, `logs` |
| TYPE | 리소스 종류 (단수/복수/약어 모두 가능) | `pod`/`pods`/`po`, `deployment`/`deploy`, `service`/`svc` |
| NAME | 이름 (생략하면 전체) | `web` |
| flags | `-n`(네임스페이스), `-o`(출력), `-l`(라벨), `-A`(모든 네임스페이스) | |

`kubectl --help`는 명령을 용도별로 묶어 보여 준다.

| 그룹 | 명령 |
|---|---|
| Basic (Beginner) | `create`, `expose`, `run`, `set` |
| Basic (Intermediate) | `explain`, `get`, `edit`, `delete` |
| Deploy | `rollout`, `scale`, `autoscale` |
| Cluster Management | `certificate`, `cluster-info`, `top`, `cordon`, `drain`, `taint` |
| Troubleshooting & Debugging | `describe`, `logs`, `attach`, `exec`, `port-forward`, `proxy`, `cp`, `auth`, `debug`, `events` |
| Advanced | `diff`, `apply`, `patch`, `replace`, `wait`, `kustomize` |
| Settings | `label`, `annotate`, `completion` |
| Other | `api-resources`, `api-versions`, `config`, `plugin`, `version` |

(`--help` 출력은 앞 3개 그룹만 확인했고 나머지 그룹 구성은 문서 지식이다.)

##### 1-2. 리소스 만들기: run, create, expose

```
$ k -n t3k run web --image=nginx:alpine --port=80 --labels=app=web,tier=front     →  pod/web created
$ k -n t3k create deployment api --image=nginx:alpine --replicas=2                →  deployment.apps/api created
$ k -n t3k expose deployment api --port=80 --target-port=80 --name=api-svc        →  service/api-svc exposed
```

| 명령 | 만드는 것 | 비고 |
|---|---|---|
| `run <이름> --image=...` | **Pod 하나** | 컨트롤러가 없어서 지우면 되살아나지 않는다 (docker run과 비슷) |
| `create deployment <이름> --image=... --replicas=N` | Deployment (+ReplicaSet, Pod) | 6장(도커/쿠버네티스 책)의 소유 관계 |
| `expose <리소스> --port=...` | Service | 대상의 라벨 셀렉터를 그대로 사용. `--type=NodePort`로 유형 지정 |
| `create -f` / `apply -f <파일>` | 파일에 정의된 모든 리소스 | 운영의 기본은 `apply` (12장 링크 참고) |

**`--dry-run=client -o yaml`: YAML 뼈대를 자동으로 만든다**

```
$ k -n t3k create deployment gen --image=nginx:alpine --replicas=2 --dry-run=client -o yaml
apiVersion: apps/v1
kind: Deployment
metadata: {labels: {app: gen}, name: gen, namespace: t3k}
spec:
  replicas: 2
  selector: {matchLabels: {app: gen}}
  ...
$ k -n t3k run tmp --image=alpine:3.20 --dry-run=client -o yaml --command -- sleep 3600
  containers:
  - command: [sleep, "3600"] ...
```

- 명령은 서버에 아무것도 만들지 않고 **YAML만 출력**한다. 처음부터 YAML을 쓰는 대신 이것을 파일로 저장해서 고치면 빠르다 (`> gen.yaml`).
- `run`에서 `--command -- <명령>`은 컨테이너의 `command`로, `--` 뒤에 `--command` 없이 쓰면 `args`로 들어간다 (이번에는 `--command` 쪽만 확인했다).

##### 1-3. 조회: get, describe, events, explain

**get의 출력 옵션**

```
$ k -n t3k get pods
NAME                   READY   STATUS    RESTARTS   AGE
api-77b967b758-7x2v7   1/1     Running   0          1s
web                    1/1     Running   0          1s
$ k -n t3k get pods -o wide                       # IP, NODE 등 추가 열
$ k -n t3k get pod web -o name                    →  pod/web
$ k -n t3k get pods --show-labels                 →  api-...  app=api,pod-template-hash=77b967b758 / web  app=web,tier=front
$ k -n t3k get pods -L app,tier                   # 라벨 값을 열로 (app, tier 열)
$ k -n t3k get pods -l app=web                    # 라벨 셀렉터
$ k -n t3k get pods -l 'app in (web,api)'         # 집합 표현식 → 3개
$ k -n t3k get pods --field-selector status.phase=Running
$ k -n t3k get pod web -o jsonpath='{.status.podIP} {.spec.nodeName} {.status.qosClass}{"\n"}'
10.1.1.197 docker-desktop BestEffort
$ k -n t3k get pods -o custom-columns=NAME:.metadata.name,IP:.status.podIP,NODE:.spec.nodeName,IMAGE:'.spec.containers[0].image'
$ k -n t3k get pods --sort-by=.metadata.creationTimestamp -o name
$ k -n t3k get deploy,svc,pod -o name             # 여러 종류를 한 번에
$ k get pods -A --no-headers | awk '{print $1}' | sort | uniq -c      # kube-system 9, t3k 3
```

| 옵션 | 쓰임 |
|---|---|
| `-o wide` | 추가 열 (IP, 노드 등) |
| `-o yaml` / `-o json` | 전체 오브젝트 |
| `-o name` | `종류/이름`만 (스크립트에서 다른 명령에 전달) |
| `-o jsonpath='{...}'` | 특정 필드만 추출 |
| `-o custom-columns=열이름:경로,...` | 원하는 열로 표 구성 |
| `-l`, `--field-selector` | 라벨/필드 선택자 (6장 라벨 셀렉터) |
| `--show-labels`, `-L <키>` | 라벨 표시 |
| `--sort-by=<경로>` | 정렬 |
| `-A` (`--all-namespaces`), `-n <ns>` | 네임스페이스 범위 |
| `-w` (`--watch`) | 변경을 실시간으로 이어서 출력 (8장 Watch API) |
| `--no-headers` | 헤더 제거 (스크립트용) |
| `--raw <경로>` | API 경로 직접 호출 (`k get --raw /healthz` → `ok`) |

- `get -w`는 이 환경에서 Pod `w1`이 `0/1 ContainerCreating` → `1/1 Running`으로 바뀌는 줄을 이어서 출력했다.
- `kubectl get all`은 **모든 리소스가 아니라** `categories: all`에 속한 일부(pod, service, deployment, replicaset 등)만 보여 준다 (12장 CRD의 `categories`와 같은 개념). ConfigMap, Secret, PVC 등은 나오지 않는다.

**describe: 사람이 읽기 좋은 상세 + 이벤트**

```
$ k -n t3k describe pod web
Name | Namespace | Priority | Service Account | Node | Start Time | Labels | Annotations | Status | IP | IPs |
Containers | Conditions | Volumes | QoS Class | Tolerations | Events
...
Events:
  Normal  Scheduled  default-scheduler  Successfully assigned t3k/web to docker-desktop
  Normal  Pulled     kubelet            spec.containers{web}: Container image "nginx:alpine" already present on machine
  Normal  Created    kubelet            spec.containers{web}: Created container: web
  Normal  Started    kubelet            spec.containers{web}: Started container web
```

- **Pod가 안 뜰 때 가장 먼저 보는 곳이 `describe`의 `Events`** 다 (6장: 스케줄러, kubelet이 남긴 이벤트). 위 이벤트가 6장에서 본 생성 순서(Scheduled → Pulled → Created → Started)다.
- `get -o yaml`은 `spec`과 `status` 전체, `describe`는 요약과 이벤트라는 점이 다르다.

**events, explain, api-resources**

```
$ k -n t3k get events --sort-by=.lastTimestamp      # 시간순으로 정렬해서 마지막 이벤트 확인
$ k -n t3k events --for pod/web                       # 특정 오브젝트의 이벤트
LAST SEEN   TYPE     REASON      OBJECT    MESSAGE
24s         Normal   Pulled      Pod/web   Container image "nginx:alpine" already present on machine
$ k explain pod.spec.containers.livenessProbe --recursive      # 필드 구조
$ k explain deployment.spec.strategy.type                       # ENUM: Recreate / RollingUpdate
$ k api-resources --namespaced=true --no-headers | wc -l        # 31 (네임스페이스 리소스 종류)
$ k api-versions | head                                         # admissionregistration.k8s.io/v1 ...
```

- `kubectl explain`은 **서버 버전에 맞는 필드 설명**을 보여 준다. YAML을 쓸 때 문서를 찾는 대신 쓴다 (`--recursive`는 하위 필드를 펼침).
- `api-resources`는 이 클러스터가 제공하는 리소스 종류(약어, API 버전, 네임스페이스 여부)를 보여 준다.

##### 1-4. 변경: apply, diff, scale, set, rollout

```
$ k apply -f gen.yaml                                          →  deployment.apps/gen created
(gen.yaml에서 replicas: 1 → 3 로 수정)
$ k diff -f gen.yaml
-  generation: 1        +  generation: 2        -  replicas: 1        +  replicas: 3      ← 적용 전에 변경될 내용을 비교
$ k apply -f gen.yaml                                          →  deployment.apps/gen configured
$ k -n t3k scale deploy gen --replicas=2                       →  deployment.apps/gen scaled
$ k -n t3k set image deploy/gen nginx=nginx:1.27-alpine        →  deployment.apps/gen image updated
$ k -n t3k rollout status deploy/gen                           →  deployment "gen" successfully rolled out
$ k -n t3k rollout history deploy/gen                          →  REVISION 1, 2
$ k -n t3k rollout undo deploy/gen                             →  deployment.apps/gen rolled back
$ k -n t3k get deploy gen -o jsonpath='{.spec.template.spec.containers[0].image}'   →  nginx:alpine
$ k -n t3k rollout restart deploy/gen                          →  deployment.apps/gen restarted
$ k -n t3k rollout history deploy/gen                          →  REVISION 2, 3, 4
```

| 명령 | 동작 | 비고 |
|---|---|---|
| `apply -f` | 선언적 적용 | 12장 링크 |
| `diff -f` | **적용했을 때 바뀔 내용** 비교 | 운영 적용 전 확인용 |
| `scale deploy X --replicas=N` | 개수 변경 | Deployment, ReplicaSet, StatefulSet 대상 |
| `set image deploy/X <컨테이너>=<이미지>` | 이미지 교체 → 롤링 업데이트 | 6장 롤링 업데이트 |
| `rollout status` | 롤아웃이 끝날 때까지 대기/표시 | CI에서 배포 완료 확인 |
| `rollout history` / `undo` | 리비전 조회 / 이전으로 | `undo` 후 리비전 번호는 새로 올라간다 (6장) |
| `rollout restart` | **설정 변경 없이 Pod를 순차 재시작** | ConfigMap 환경 변수 반영 등 (7장) |

- `rollout undo`는 이미지가 `nginx:alpine`(리비전 1)으로 돌아갔고, `rollout restart`는 리비전을 하나 더 만들었다 (`2, 3, 4`).
- `kubectl diff`의 종료 코드(차이가 있으면 1 등)는 확인하지 못했다 (문서 지식: 차이가 있을 때 1을 반환해 CI에서 쓸 수 있다).

**label, annotate, patch, edit**

```
$ k -n t3k label pod web env=dev            →  pod/web labeled
$ k -n t3k label pod web env=prod --overwrite   →  pod/web labeled
$ k -n t3k label pod web env-                   →  pod/web unlabeled      # 키 뒤에 -
$ k -n t3k annotate pod web owner=me            →  pod/web annotated
$ k -n t3k patch deploy gen -p '{"spec":{"replicas":1}}'     →  deployment.apps/gen patched
$ k -n t3k label pod web env=prod ; k -n t3k label pod web env=x
error: 'env' already has a value (prod), and --overwrite is false
```

- 이미 있는 라벨 값을 바꾸려면 **`--overwrite`가 필요**하고 없으면 오류다 (실수 방지). 제거는 **키 뒤에 `-`** (`env-`).
- `patch`의 종류(strategic merge, merge, json)는 도커/쿠버네티스 책 12장에서 비교했다. `kubectl edit`(편집기 열기)은 터미널 편집기가 필요해서 이번에 시험하지 않았다.

**delete**

```
$ k -n t3k delete pod cordon-test                    →  pod "cordon-test" deleted from t3k namespace
$ k -n t3k delete pod cordon-test --ignore-not-found      # 없어도 오류 없이 통과 (스크립트용)
$ k -n t3k delete -f gen.yaml                         # 파일에 정의된 리소스 삭제
$ k -n t3k delete pod -l grp=x                        # 라벨로 여러 개 삭제 (d1, d2 삭제)
$ k -n t3k delete pod --all                           # 네임스페이스의 모든 Pod
$ k -n t3k delete deploy api --cascade=orphan         # 소유한 ReplicaSet/Pod는 남김
   → replicaset.apps/api-77b967b758, pod/api-77b967b758-7x2v7, pod/api-77b967b758-sz8tz 가 남았다
```

**삭제 시간: `--grace-period`**

SIGTERM을 무시하는 컨테이너(`sleep`이 PID 1)의 Pod를 삭제했다.

| 명령 | 걸린 시간 |
|---|---|
| `delete pod slow` | **31초** (기본 `terminationGracePeriodSeconds` 30초를 기다린 뒤 SIGKILL, 11장) |
| `delete pod slow --grace-period=1` | **4초** |
| `delete pod web --now` | 즉시 삭제 요청 (`--grace-period=1`과 같은 의미, 시간은 재지 않았다) |

- Pod가 `Terminating`에서 오래 머무는 것은 대개 이 유예 시간 때문이다. 급할 때 `--grace-period=0 --force`로 API에서 즉시 지울 수 있지만 컨테이너가 실제로 종료되지 않은 상태에서 지워질 수 있어 위험하다 (이 옵션은 시험하지 않았다).

**wait**

```
$ k -n t3k wait --for=condition=Ready pod/web --timeout=60s     →  pod/web condition met
$ k -n t3k wait --for=condition=available deploy/gen --timeout=30s   →  deployment.apps/gen condition met
```

- `wait`는 조건이 충족될 때까지 기다리다가 시간이 초과되면 오류로 종료한다. 스크립트에서 `sleep`으로 시간을 때우는 대신 쓴다 (이 장의 실험 스크립트가 이 방식으로 Pod 준비를 기다렸다).

##### 1-5. 디버깅: logs, exec, cp, port-forward

```
$ k -n t3k port-forward pod/web 18083:80 &        # 로컬 18083 → Pod 80
$ curl -s -o /dev/null -w "%{http_code}" localhost:18083/        →  200
$ k -n t3k logs web --tail=2                      # 마지막 2줄
127.0.0.1 - - [25/Oct/2026:10:05:32 +0000] "GET /hello HTTP/1.1" 404 153 ...
$ k -n t3k logs web --since=1m | wc -l                              →  30
$ k -n t3k logs deploy/api | head -1                                # Deployment의 Pod 하나를 골라서
Found 2 pods, using pod/api-77b967b758-7x2v7                        ← 여러 Pod면 하나만 보여 준다는 안내(stderr)
$ k -n t3k logs -l app=api --prefix --tail=1                        # 라벨로 전체 Pod, 줄 앞에 Pod/컨테이너 이름
[pod/api-77b967b758-7x2v7/nginx] 2026/10/25 10:05:15 [notice] 1#1: start worker process 41
[pod/api-77b967b758-sz8tz/nginx] 2026/10/25 10:05:15 [notice] 1#1: start worker process 41
$ k -n t3k logs -f web                                              # 이어서 출력 (follow)
127.0.0.1 - - ... "GET /followed HTTP/1.1" 404 ...                  ← 새 요청이 들어오는 즉시 출력
$ k -n t3k exec web -- nginx -v                                     →  nginx version: nginx/1.31.7
$ k -n t3k exec web -- sh -c 'echo hi > /tmp/f.txt; hostname'       →  web
$ k -n t3k cp t3k/web:/tmp/f.txt ./f.txt                            # Pod → 로컬 (tar: removing leading '/' from member names)
$ k -n t3k cp ./up.txt web:/tmp/up.txt                              # 로컬 → Pod
```

| 명령 | 쓰임 | 주의 |
|---|---|---|
| `logs <pod>` | 컨테이너 표준 출력/에러 | 컨테이너가 여럿이면 `-c <컨테이너>`. 재시작 전 로그는 `--previous` |
| `logs -f`, `--tail`, `--since` | 실시간/마지막 N줄/최근 N분 | |
| `logs deploy/<이름>` | **Pod 하나만** 선택해서 출력 | 전체를 보려면 `-l <라벨> --prefix` |
| `exec <pod> -- <명령>` | 컨테이너 안에서 명령 실행 | `--` 뒤가 컨테이너 명령. 셸이 필요하면 `sh -c '...'`. `-it`는 TTY가 필요 (이 환경에서는 시험하지 못함) |
| `cp <원본> <대상>` | 파일 복사 | 컨테이너 이미지에 `tar`가 있어야 한다 (문서 지식, `nginx:alpine`에서는 동작). `네임스페이스/Pod:경로` 형식 |
| `port-forward <리소스> 로컬:원격` | **로컬 포트를 Pod/Service로 연결** | 포그라운드로 계속 실행되는 명령. 8, 9장에서 외부 접근이 안 될 때 확인용으로 썼다 |

- `port-forward`는 `pod/web`뿐 아니라 `svc/<이름>`, `deploy/<이름>`도 대상으로 쓸 수 있다 (`svc` 대상은 앞선 책의 6장에서 사용).

##### 1-6. `kubectl debug`와 `top`, `auth`

`kubectl debug`의 자세한 사용은 2절(netshoot)에서 다룬다.

```
$ k -n t3k top pod
error: Metrics API not available                       ← metrics-server가 없는 클러스터 (이 환경 기본)
$ k auth can-i create deployments -n t3k               →  yes
$ k auth can-i delete nodes --as=nobody                →  no          # 다른 신원의 권한을 시험 (--as)
$ k auth whoami                                        →  Username docker-for-desktop / Groups [system:masters system:authenticated]
```

- `top`은 metrics-server가 있어야 동작한다 (도커/쿠버네티스 책 14장에서 설치해서 확인).
- `auth can-i`는 RBAC를 점검하는 명령이다 (도커/쿠버네티스 책 10장).

##### 1-7. 클러스터와 컨텍스트: config, cluster-info, version, cordon

**kubeconfig 컨텍스트 (복사본에서 시험)**

```
$ kubectl config get-contexts -o name                 # 등록된 컨텍스트 목록 (이 PC에는 7개)
$ kubectl config current-context                      # 현재 컨텍스트 (이 PC의 기본은 EKS QA 클러스터)
$ kubectl config use-context docker-desktop           →  Switched to context "docker-desktop".
$ kubectl config set-context --current --namespace=t3k      # 컨텍스트의 기본 네임스페이스 지정
$ kubectl get pods --no-headers | wc -l               →  4        ← -n 없이 t3k의 Pod가 나온다
$ kubectl config view --minify -o jsonpath='ns={.contexts[0].context.namespace}'      →  ns=t3k
```

- 컨텍스트는 **클러스터 + 사용자 + 기본 네임스페이스**의 조합이다 (도커/쿠버네티스 책 10장).
- **여러 클러스터를 쓸 때 `use-context`는 위험하다.** 이 PC의 기본 컨텍스트가 운영 클러스터를 가리키는 것처럼, 컨텍스트를 헷갈리면 엉뚱한 클러스터에 명령이 간다. 그래서 이 노트의 모든 명령은 `--context`를 명시했다. 위험한 명령(`delete`, `apply`) 앞에서는 `kubectl config current-context`로 확인하는 습관이 필요하다.

**cluster-info, version**

```
$ k cluster-info
Kubernetes control plane is running at https://127.0.0.1:6443
CoreDNS is running at https://127.0.0.1:6443/api/v1/namespaces/kube-system/services/kube-dns:dns/proxy
$ k version -o json     →  client v1.35.2, server v1.32.2          ← 마이너 버전 차이가 1을 넘으면 경고
```

**cordon / uncordon: 노드를 스케줄 대상에서 제외**

```
$ k cordon docker-desktop                 →  node/docker-desktop cordoned
$ k get node docker-desktop               →  Ready,SchedulingDisabled
$ k -n t3k run cordon-test --image=nginx:alpine ; (4초 뒤)
cordon-test   Pending
Warning  FailedScheduling  default-scheduler  0/1 nodes are available: 1 node(s) were unschedulable. ...
$ k uncordon docker-desktop               →  node/docker-desktop uncordoned
cordon-test   Running                      ← 해제되자 바로 스케줄됨
```

- `cordon`은 **새 Pod가 이 노드에 스케줄되지 않게** 한다. 이미 실행 중인 Pod는 그대로 둔다. 노드 점검 전에 사용한다. 기존 Pod까지 다른 노드로 옮기는 `drain`은 노드가 하나인 이 환경에서는 시험하지 않았다.
- 노드 상태에 `SchedulingDisabled`가 붙고, 11장의 스케줄러 필터링 메시지(`1 node(s) were unschedulable`)가 나왔다.

##### 1-8. 자주 쓰는 명령 치트시트

| 상황 | 명령 |
|---|---|
| 지금 뭐가 떠 있나 | `kubectl get pods -A`, `kubectl get all -n <ns>` |
| 안 뜨는 Pod 원인 | `kubectl describe pod <이름>` (Events), `kubectl get events --sort-by=.lastTimestamp` |
| 죽는 컨테이너 로그 | `kubectl logs <pod> --previous`, `kubectl logs <pod> -c <컨테이너>` |
| Pod 안에서 확인 | `kubectl exec <pod> -- <명령>`, `kubectl cp` |
| 로컬에서 접속 | `kubectl port-forward svc/<이름> 8080:80` |
| 필드 확인 | `kubectl explain <리소스>.<필드>` |
| YAML 뼈대 | `kubectl create ... --dry-run=client -o yaml` |
| 적용 전 비교 | `kubectl diff -f <파일>` |
| 배포 상태 | `kubectl rollout status/history/undo/restart deploy/<이름>` |
| 권한 확인 | `kubectl auth can-i <동사> <리소스> --as=<사용자>` |
| 컨텍스트 확인 | `kubectl config current-context` (위험한 명령 전) |
| 디버깅 컨테이너 붙이기 | `kubectl debug` (2절) |

- 자동 완성(`kubectl completion zsh`가 `#compdef kubectl`로 시작하는 스크립트를 출력)과 별칭(`alias k=kubectl`)을 설정하면 입력이 크게 줄어든다 (자동 완성 설정 자체는 시험하지 않았다).

#### 2. netshoot 컨테이너

##### 2-1. 개념

**`nicolaka/netshoot`은 네트워크 문제를 진단하는 도구들을 한 이미지에 모아 둔 디버깅용 컨테이너 이미지**다 (Alpine 기반).

**왜 필요한가**: 운영용 컨테이너 이미지는 보안과 크기를 위해 최소한으로 만든다 (distroless, slim 등). 그런 컨테이너에는 `ss`, `dig`, `tcpdump`, `ping`이 없어서 **문제가 생겨도 안에서 확인할 도구가 없다.** 이때 도구를 모두 가진 netshoot 컨테이너를 **같은 네트워크 환경에 붙여서** 진단한다. 앱 이미지에 도구를 넣어 다시 빌드/배포하지 않아도 된다.

| 항목 (실측) | 값 |
|---|---|
| 이미지 | `nicolaka/netshoot:latest`, **arm64** 이미지가 제공됨 (Apple Silicon에서 그대로 실행) |
| 크기 | **653MB** (레이어 15개). 앱 이미지(`nginx:alpine` 26MB)보다 훨씬 크다 |
| 베이스 | Alpine Linux |
| 기본 실행 | `Entrypoint=[]`, `Cmd=[zsh]`, 사용자 `root` |

이 이미지에 들어 있는 것으로 확인한 도구 (컨테이너 안에서 `command -v`로 조사):

| 분류 | 도구 |
|---|---|
| HTTP/전송 | `curl`, `wget`, `httpie`, `grpcurl`, `fortio` (부하 도구), `openssl`, `swaks`(메일) |
| DNS | `dig`, `nslookup`, `host` |
| 연결/경로 | `ping`, `traceroute`, `mtr`, `nc`(netcat), `nmap` |
| 소켓/인터페이스 | `ss`, `netstat`, `ip`, `ethtool` |
| 패킷 캡처 | **`tcpdump`**, `tshark`, `termshark`, `ngrep` |
| 방화벽 | `iptables`, `nft` |
| 성능 | `iperf`, `iperf3` |
| 기타 | `jq`, `calicoctl`, `ctop` |

(위 목록은 `command -v`로 실제 존재를 확인한 것들이다. 이외에 포함된 도구가 더 있을 수 있다.)

##### 2-2. 사용법 1: 클러스터 안에 독립 Pod로 띄우기

```
$ k -n t3k run netshoot --image=nicolaka/netshoot --command -- sleep infinity
$ k -n t3k exec netshoot -- <진단 명령>
```

`netshoot` Pod(IP 10.1.1.212)와 `web` Pod + `web-svc` Service를 두고 확인했다.

**DNS (7장/도커 책의 서비스 DNS 확인)**

```
$ N cat /etc/resolv.conf
nameserver 10.96.0.10
search t3k.svc.cluster.local svc.cluster.local cluster.local
options ndots:5
$ N dig +short web-svc.t3k.svc.cluster.local         →  10.108.234.2          ← Service ClusterIP
$ N dig +search +short web-svc                        →  10.108.234.2          ← 짧은 이름도 search 도메인으로 해석
$ N dig +short @10.96.0.10 web-svc.t3k.svc.cluster.local    →  10.108.234.2   ← CoreDNS(10.96.0.10)에 직접 질의
$ N dig nosuchsvc.t3k.svc.cluster.local +noall +comments | grep status
;; ->>HEADER<<- opcode: QUERY, status: NXDOMAIN                              ← 없는 이름은 NXDOMAIN
$ N dig web-svc.t3k.svc.cluster.local +noall +answer +stats
web-svc.t3k.svc.cluster.local. 30 IN A 10.108.234.2   ;; Query time: 2 msec   ;; SERVER: 10.96.0.10#53(10.96.0.10)
```

- `nslookup`보다 `dig`가 응답 코드(`NXDOMAIN` 등), TTL(30), 질의 시간, 서버를 보여 줘서 DNS 문제 진단에 낫다. "서비스 이름이 안 풀린다"는 문제를 이렇게 쪼갠다: ① `resolv.conf`의 네임서버/search가 맞나 ② CoreDNS에 직접 질의하면 되나 ③ 이름 자체가 있나(NXDOMAIN).

**연결성**

```
$ N curl -s -o /dev/null -w "svc http=%{http_code} time=%{time_total}s remote=%{remote_ip}\n" http://web-svc
svc http=200 time=0.063913s remote=10.108.234.2
$ N curl ... http://<web Pod IP>                      →  pod http=200 remote=10.1.1.213
$ N nc -zv -w2 web-svc 80                             →  Connection to web-svc (10.108.234.2) 80 port [tcp/http] succeeded!
$ N nc -zv -w2 web-svc 81                             →  (exit code 1: 연결 실패)
$ N ping -c2 -W1 <web Pod IP>                         →  2 packets transmitted, 2 received, 0% packet loss
$ N traceroute -n -m3 <web Pod IP>                    →  1  10.1.1.213  0.021 ms   ← 같은 노드의 Pod는 한 홉
```

- **Service(ClusterIP)와 Pod IP를 각각 호출**해서 "Service는 안 되는데 Pod는 되나"를 구분한다 (엔드포인트/셀렉터 문제인지, 앱 문제인지). `nc -zv`는 포트가 열려 있는지만 보는 가장 단순한 확인이다 (81번 포트는 닫혀 있어 실패).
- `curl -w`로 **단계별 시간**을 본다.

```
$ N curl -s -o /dev/null -w "dns=%{time_namelookup} connect=%{time_connect} ttfb=%{time_starttransfer} total=%{time_total}\n" http://web-svc
dns=0.004542 connect=0.004720 ttfb=0.005573 total=0.005602
$ N curl -sI http://web-svc | head -3                  # 응답 헤더만
HTTP/1.1 200 OK / Server: nginx/1.31.7 / Date: ...
```

- 느릴 때 어느 단계(DNS, 연결, 첫 바이트)가 문제인지 알 수 있다. 첫 요청이 0.064초였던 것은 첫 호출의 지연이고, 이후 호출은 5ms였다 (한 번씩 측정한 값).

**Pod 안의 네트워크 구성과 TLS**

```
$ N ip -br addr
lo UNKNOWN 127.0.0.1/8 ... / eth0@if231 UP 10.1.1.212/16 ...     ← eth0@if231: 노드 쪽 veth와 쌍 (도커 책 2장의 veth)
$ N ip route
default via 10.1.0.1 dev eth0 / 10.1.0.0/16 dev eth0 proto kernel scope link src 10.1.1.212
$ N sh -c 'echo | openssl s_client -connect kubernetes.default.svc:443 2>/dev/null | openssl x509 -noout -subject -issuer -dates'
subject=CN=kube-apiserver / issuer=CN=kubernetes / notBefore=Jun 17 12:06:03 2026 GMT
```

- `ip -br addr`에는 `tunl0`, `gre0` 같은 DOWN 상태의 터널 인터페이스가 여러 개 보였다 (이미지가 쓰는 커널 모듈이 만든 기본 장치로 보이지만 확인하지 않았다).
- `openssl s_client`로 API 서버의 인증서(`CN=kube-apiserver`, 발급자 `CN=kubernetes`)를 확인했다. 인증서 만료/CA 문제도 이렇게 확인한다.

##### 2-3. 사용법 2: `kubectl debug`로 실행 중인 Pod에 붙이기 (임시 컨테이너)

앱 Pod를 건드리지 않고(재시작/재배포 없이) **같은 Pod 안에 netshoot 컨테이너를 추가**한다. 이것이 쿠버네티스의 **임시 컨테이너(ephemeral container)** 다.

```
$ k -n t3k exec web -- sh -c 'which ss tcpdump dig curl'      # nginx:alpine에는 curl만 있다
/usr/bin/curl
$ k -n t3k debug web --image=nicolaka/netshoot --target=web -c dbg1 -- sh -c '...'
Targeting container "web". If you don't see processes from this container it may be because the container runtime doesn't support this feature.
--profile=legacy is deprecated and will be removed in the future. It is recommended to explicitly specify a profile, for example "--profile=general".
$ k -n t3k logs web -c dbg1
--- ip:
eth0@if232 UP 10.1.1.213/16 ...                               ← web Pod와 같은 IP (같은 네트워크 네임스페이스)
--- listening sockets (web, :80):
LISTEN 0 511 0.0.0.0:80   /   LISTEN 0 511 [::]:80
--- processes visible:
PID USER TIME COMMAND
  1 root  0:00 nginx: master process nginx -g daemon off;
 30 bird  0:00 nginx: worker process ...                      ← --target으로 web 컨테이너의 PID 네임스페이스도 공유
--- localhost curl:
200                                                            ← localhost:80이 web 컨테이너의 nginx
$ k -n t3k get pod web -o jsonpath='ephemeralContainers={.spec.ephemeralContainers[*].name} target={.spec.ephemeralContainers[0].targetContainerName}'
ephemeralContainers=dbg1 target=web
```

| 옵션 | 의미 |
|---|---|
| `kubectl debug <pod> --image=<이미지>` | 대상 Pod에 임시 컨테이너 추가 |
| `--target=<컨테이너>` | 그 컨테이너의 **프로세스 네임스페이스를 공유** (이 환경에서는 `ps`에 nginx가 보였다) |
| `-c <이름>` | 임시 컨테이너 이름 |
| `-it` | 터미널에 붙어서 대화형 (이 환경의 실험에서는 TTY가 없어 `-- <명령>` 후 `logs -c`로 결과를 읽었다) |
| `--profile=general` 등 | 권한 프로파일 (지정하지 않으면 `legacy`라는 경고가 나옴) |

- 임시 컨테이너는 **Pod의 네트워크 네임스페이스를 공유**하므로 netshoot에서 `ip`/`ss`/`curl localhost`가 **앱 컨테이너 입장의 네트워크**를 그대로 보여 준다 (같은 IP `10.1.1.213`, `:80` LISTEN, localhost로 접속). 도커 책 1장의 "네임스페이스를 공유하는 컨테이너" 원리의 쿠버네티스 버전이다 (Pod의 net/ipc 공유, pid는 `--target`일 때).
- Pod `spec.ephemeralContainers`에 기록된다. **임시 컨테이너는 한 번 추가하면 제거하거나 수정할 수 없다** (Pod를 다시 만들 때 사라진다는 것은 문서 지식이고 실험하지 않았다). 앱 컨테이너 `web`의 `restartCount`는 0이었다 (영향 없음).

**tcpdump로 다른 Pod의 트래픽 캡처 (실측)**

다른 Pod(`netshoot`)가 `web-svc`로 curl을 보내는 동안 `web` Pod의 임시 컨테이너에서 `tcpdump`를 실행했다.

```
$ k -n t3k debug web --image=nicolaka/netshoot --target=web -c dbg2 -- sh -c 'timeout 12 tcpdump -nn -i eth0 -c 8 "tcp port 80"'
$ k -n t3k exec netshoot -- curl -s -o /dev/null http://web-svc/capture-me
$ k -n t3k logs web -c dbg2
10:09:20.380095 IP 10.1.1.212.45202 > 10.1.1.213.80: Flags [S] ...        ← SYN (netshoot Pod → web Pod)
10:09:20.380117 IP 10.1.1.213.80 > 10.1.1.212.45202: Flags [S.] ...        ← SYN-ACK
10:09:20.380131 IP 10.1.1.212.45202 > 10.1.1.213.80: Flags [.] ...         ← ACK
10:09:20.380231 IP 10.1.1.212.45202 > 10.1.1.213.80: Flags [P.] ... length 81   ← HTTP 요청
10:09:20.380754 IP 10.1.1.213.80 > 10.1.1.212.45202: Flags [P.] ... length 308  ← HTTP 응답
10:09:20.380943 IP 10.1.1.212.45202 > 10.1.1.213.80: Flags [F.] ...        ← FIN
8 packets captured
```

- **TCP 3-way handshake(SYN → SYN-ACK → ACK) → 요청 → 응답 → FIN**이 그대로 보인다.
- `curl`은 **Service의 ClusterIP(10.108.234.2)로** 보냈는데 `web` Pod의 `eth0`에서 잡힌 패킷의 목적지는 **Pod IP `10.1.1.213`**이다. 6장(도커 책)에서 본 kube-proxy의 DNAT이 이미 적용된 뒤의 패킷이다. 앱 쪽에서는 Service IP가 아니라 클라이언트 Pod IP(`10.1.1.212`)가 소스로 보인다.
- 이런 캡처는 "요청이 앱까지 도달하는지", "재전송/RST가 있는지"를 볼 때 쓴다.

##### 2-4. 사용법 3: 노드를 디버깅 (`kubectl debug node/`)

```
$ k debug node/docker-desktop --image=nicolaka/netshoot --profile=general -- sh -c 'hostname; ip -br addr; ls /host'
Creating debugging pod node-debugger-docker-desktop-kdb2h with container debugger on node docker-desktop.
--- hostname: docker-desktop                                       ← 노드의 호스트명
eth0 UP 192.168.65.3/24 ... / services1@if11 UP 192.168.65.6/32 ...    ← 노드의 인터페이스 (Pod와 다른 네트워크)
--- host root mounted?:  bin boot bpf-legacy.o ...                  ← 노드의 루트 파일시스템이 /host에 마운트됨
$ k get pod node-debugger-... -o jsonpath='hostNetwork={.spec.hostNetwork} hostPID={.spec.hostPID} volumes={.spec.volumes[*].name}'
hostNetwork=true hostPID=true volumes=host-root kube-api-access-pkw9s
```

- 노드에 SSH 없이도 **노드의 네트워크/프로세스/파일시스템**을 볼 수 있는 Pod(`node-debugger-...`)를 만든다 (`hostNetwork`, `hostPID`, 호스트 루트 볼륨). 디버깅 Pod는 현재 네임스페이스(`default`)에 만들어지고 **자동으로 지워지지 않으므로 직접 삭제**해야 한다 (이 실험에서 삭제했다).
- 강력한 권한이므로 이 명령은 관리자만 쓰게 제한해야 한다 (도커 책 9장의 hostPath 위험성과 같은 이유). RBAC로 이 권한을 나누는 실험은 하지 않았다.

##### 2-5. 사용법 4: 도커에서 같은 방식 (`--net container:`)

쿠버네티스가 아닌 단독 도커 컨테이너에도 같은 원리를 쓸 수 있다.

```
$ docker run -d --name t3k-nginx nginx:alpine
$ docker run --rm --net container:t3k-nginx nicolaka/netshoot sh -c 'ip -br addr show eth0; ss -ltn | grep ":80 "; curl -s -o /dev/null -w "localhost:80 -> %{http_code}\n" localhost:80'
eth0@if233 UP 172.17.0.2/16                                       ← nginx 컨테이너와 같은 IP
LISTEN 0 511 0.0.0.0:80
localhost:80 -> 200
$ docker run --rm nicolaka/netshoot ip -br addr show eth0         # 대상 지정 없이: 별도의 네트워크 네임스페이스
eth0@if235 UP 172.17.0.2/16                                       ← (다른 컨테이너라 이전 컨테이너가 삭제된 뒤 같은 주소가 재사용됨)
```

- `--net container:<이름>`은 **대상 컨테이너의 네트워크 네임스페이스에 들어가는** 옵션이다 (도커/쿠버네티스 책 2장의 `--net=container:`). 쿠버네티스의 Pod가 `pause` 컨테이너의 네임스페이스를 공유하는 방식과 같다 (도커 책 6장).

##### 2-6. netshoot 사용 방식 정리

| 방법 | 명령 | 어디의 네트워크가 보이나 | 언제 |
|---|---|---|---|
| 독립 Pod | `kubectl run netshoot --image=nicolaka/netshoot -- sleep infinity` | **새 Pod의 네트워크** (클러스터 내 클라이언트 입장) | DNS, Service 연결성, 외부 통신 확인 |
| 임시 컨테이너 | `kubectl debug <pod> --image=... --target=<컨테이너>` | **대상 Pod의 네트워크** (같은 IP, localhost) | 특정 앱 Pod의 소켓/트래픽(tcpdump) 진단 |
| 노드 디버깅 | `kubectl debug node/<노드> --image=...` | **노드의 네트워크** (hostNetwork) | 노드 레벨 네트워크 문제 |
| 도커 | `docker run --net container:<이름> nicolaka/netshoot` | 대상 컨테이너의 네트워크 | 단독 도커 컨테이너 |

**진단 흐름 예 ("서비스가 안 된다")**

1. 독립 netshoot Pod에서 `dig <서비스>`로 **DNS**가 되는지
2. `curl <서비스>`와 `curl <Pod IP>`로 Service/Pod 중 어디서 끊기는지 (엔드포인트, 6장)
3. `nc -zv <서비스> <포트>`로 **포트**가 열려 있는지
4. 앱 Pod에 임시 컨테이너를 붙여 `ss -ltn`(앱이 그 포트로 LISTEN 하는지, 특히 `127.0.0.1`로만 바인딩하지 않았는지), `tcpdump`(패킷이 도달하는지)
5. 필요하면 `kubectl describe`(이벤트), `kubectl get endpoints`

##### 2-7. 주의점

| 주의 | 이유 |
|---|---|
| 이미지가 크다 (653MB) | 처음 pull이 오래 걸린다 (사설 레지스트리 미러링 필요할 수 있다). 실험 후 로컬에서 삭제했다 |
| `latest` 태그 | 버전이 바뀔 수 있다. 운영 환경에서는 태그/digest 고정 권장 (이번에는 `latest`를 썼다) |
| **root 권한으로 실행, 강력한 도구**(tcpdump, nmap, iptables) | 보안 정책(Pod Security)에서 막힐 수 있고 아무에게나 허용하면 안 된다. 네임스페이스에 남겨 두지 말고 **쓰고 지운다** |
| 임시 컨테이너는 제거 불가 | `kubectl debug`로 붙인 컨테이너는 Pod가 재생성될 때까지 `spec`에 남는다 |
| `kubectl debug node/`는 노드 루트를 마운트 | 매우 강한 권한. 사용 후 디버거 Pod를 반드시 삭제 |
| 클러스터 외부로 나가는 통신을 확인할 때 | 방화벽/네트워크 정책에 영향을 받는다 (이 환경에서 NetworkPolicy는 확인하지 않았다) |

#### 3. 확인하지 못한 부분

- **`kubectl exec -it`, `kubectl run -it`, `kubectl attach`, `kubectl debug -it`**: TTY가 필요한 대화형 사용은 이 환경에서 시험하지 못했다 (명령 실행 후 `logs`로 결과를 읽는 방식으로 대체).
- **`kubectl edit`**(편집기), **`kubectl drain`**(노드 하나라 시험하지 않음), `kubectl taint`, `kubectl autoscale`, `kubectl proxy`, `kubectl attach`, `kubectl replace`, `kubectl certificate`: 이 장에서 시험하지 않았다.
- **`kubectl diff`의 종료 코드**와 `--prune`, `apply --server-side` 등은 확인하지 않았다 (도커/쿠버네티스 책 12장에서 일부 다룸).
- **`delete --grace-period=0 --force`**와 `--now`의 실제 소요 시간.
- **`kubectl plugin`/krew**: `kubectl plugin list`는 이 PC의 PATH에 접근 불가 경로가 있다는 경고만 출력했고 플러그인 확인은 하지 못했다. 자동 완성 설정과 별칭 설정은 시험하지 않았다.
- **netshoot의 나머지 도구**: `iperf3`로 대역폭 측정, `mtr`, `nmap` 스캔, `tshark`/`termshark`, `ngrep`, `grpcurl`, `iptables`/`nft`로 kube-proxy 규칙 조회는 존재만 확인하고 사용해 보지 않았다.
- **`kubectl debug`의 다른 모드**: `--copy-to`(Pod 복사본으로 디버깅), `--share-processes`, 프로파일(`general`, `netadmin`, `sysadmin` 등)의 차이는 시험하지 않았다. 노드 디버깅의 `general` 프로파일에서 `privileged` 값은 비어 있었다(출력 없음). 권한 차이의 의미는 확인하지 않았다.
- **임시 컨테이너 관련 RBAC**(`pods/ephemeralcontainers` 권한)와 임시 컨테이너 종료 후의 Pod 상태.
- **Pod Security/NetworkPolicy가 netshoot 사용을 막는 경우**: 이 클러스터에는 제한이 없어서 막히지 않았다.
- **Pod IP/ClusterIP 값은 실행마다 달라서** 노트의 IP는 해당 실행의 값이다.
