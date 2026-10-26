### 21장. 헬름

> 정리 범위: Helm이란, 차트/컨피그(values)/릴리스의 특징, 사용 사례(GitOps 아키텍처 등)
>
> 실측 환경: macOS (Apple Silicon), **Helm v4.1.4**, 임시 kind 클러스터 `t16`(v1.35.0, 16~19장과 같은 클러스터, 별도 kubeconfig). 실험용 차트는 `helm create mychart`로 만든 뒤 ConfigMap 템플릿, 체크섬 애너테이션, pre-install 훅 Job을 추가해서 수정한 것이다(이미지 `nginx:alpine`). GitOps 부분은 같은 클러스터에 **Argo CD(차트 10.11.x, v3.5.6)** 를 설치하고 클러스터 안의 `git daemon` Pod를 Git 저장소로 썼다(`git://` 프로토콜). 네임스페이스는 `t21`(Helm 직접 실험), `app-prod`(Argo CD가 배포). 실험이 끝난 뒤 클러스터와 kubeconfig는 삭제했다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: Argo CD와 Git 기반 배포의 기본 동작은 [10장](../chapter10/README.md) 7절(ESO + Argo CD)에서 다뤘다. 이 장은 **Helm 차트를 Argo CD로 배포**하는 부분이 새로운 내용이다. ConfigMap의 변경이 Pod에 반영되는 방식은 10장이다.

#### 목차

1. Helm이란
2. 차트 (Chart)
3. 컨피그 (values)
4. 릴리스 (Release)
5. 훅, 테스트, 서브차트
6. 사용 사례: GitOps 아키텍처
7. 선택 가이드와 함정
8. 확인하지 못한 부분

---

#### 1. Helm이란

**쿠버네티스 애플리케이션의 패키지 매니저.** 여러 YAML(Deployment, Service, ConfigMap …)을 **차트(Chart)** 하나로 묶고, 템플릿과 설정값(values)으로 환경별 차이를 처리하며, 설치된 인스턴스를 **릴리스(Release)** 로 관리(설치/업그레이드/롤백/삭제)한다.

| 개념 | 설명 | 비유 |
|---|---|---|
| **Chart** | 템플릿 + 기본 values + 메타데이터 묶음 | 패키지(`.deb`, `.rpm`) |
| **Values (config)** | 템플릿에 채워 넣는 설정값 | 설치 옵션 |
| **Release** | 차트를 클러스터에 설치한 **인스턴스** (이름, 리비전 이력) | 설치된 프로그램 |
| **Repository / OCI 레지스트리** | 차트 저장소 | 패키지 저장소 |

핵심 가치: 같은 차트를 **다른 이름/다른 values로 여러 번 설치**할 수 있고, 변경 이력을 **리비전**으로 남겨 **롤백**할 수 있다.

---

#### 2. 차트 (Chart)

##### 2-1. 구조 (실측, `helm create mychart`)

```
mychart/
├── Chart.yaml            # 차트 메타데이터
├── values.yaml           # 기본 설정값
├── .helmignore
├── charts/               # 서브차트(의존성) 위치 (나중에 helm dependency 가 채움)
└── templates/
    ├── _helpers.tpl      # 재사용 템플릿 조각(이름/레이블 규칙)
    ├── deployment.yaml, service.yaml, serviceaccount.yaml, ingress.yaml, hpa.yaml, httproute.yaml
    ├── NOTES.txt         # 설치 후 안내 메시지
    └── tests/test-connection.yaml   # helm test 용 훅 Pod
```

`Chart.yaml`(발췌):

```yaml
apiVersion: v2
name: mychart
description: A Helm chart for Kubernetes
type: application        # application | library
version: 0.1.0           # 차트 버전
appVersion: "1.16.0"     # 앱(이미지) 버전 표기용
```

- `version`(차트 자체의 버전)과 `appVersion`(패키징한 앱의 버전)은 **별개**다. `helm list`의 `CHART`/`APP VERSION` 열이 각각이다.
- `type: library`는 템플릿 조각만 제공하는 차트다(시험하지 않음).

##### 2-2. 템플릿

Go 템플릿 문법과 `.Values`, `.Release`, `.Chart`, `.Template` 등 내장 객체를 쓴다. `deployment.yaml` 발췌:

```yaml
metadata:
  name: {{ include "mychart.fullname" . }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
```

- `include "mychart.fullname" .`는 `_helpers.tpl`의 이름 규칙을 사용한다. 릴리스 이름이 `web`이면 리소스 이름이 `web-mychart`로 만들어졌다(`rel1`이면 `rel1-mychart`). 그래서 **같은 차트를 다른 릴리스 이름으로 여러 번 설치해도 이름이 충돌하지 않는다.**
- `helm lint mychart`: 문법과 구조를 검사한다(결과: `1 chart(s) linted, 0 chart(s) failed`, `icon is recommended` 안내 1건).

##### 2-3. 렌더링 미리보기 (클러스터 없이)

```
$ helm template rel1 mychart --set config.message=hi
kind: ServiceAccount  name: rel1-mychart
kind: ConfigMap       name: rel1-mychart-config   message: "hi"
kind: Service         name: rel1-mychart
kind: Deployment      name: rel1-mychart   (annotations checksum/config: 9d20f1e2…)
kind: Pod             name: rel1-mychart-test-connection      ← helm test 훅
kind: Job             name: rel1-mychart-pre                  ← pre-install 훅
$ helm template rel1 mychart -s templates/service.yaml        ← 파일 하나만
```

- `helm template`은 **서버에 접속하지 않고 YAML을 출력**한다(GitOps에서 Argo CD가 이 방식을 쓴다, 6절).

##### 2-4. 패키징

```
$ helm package mychart -d .
Successfully packaged chart and saved it to: .../mychart-0.1.0.tgz
$ helm show chart mychart-0.1.0.tgz  → apiVersion v2, name mychart, type application ...
```

차트는 `.tgz` 하나로 배포된다(저장소나 OCI 레지스트리에 올릴 수 있으나 이번에 올려 보지는 않았다).

---

#### 3. 컨피그 (values)

##### 3-1. 우선순위 (실측)

차트 기본 `values.yaml` < `-f 파일` < `--set` 순으로 덮어쓴다.

```
$ helm upgrade web mychart -f over.yaml --set config.env=prod
(over.yaml: replicaCount: 2, config.message: from-file, config.env: staging)
$ helm get values web
USER-SUPPLIED VALUES:
config: {env: prod, message: from-file}      ← env 는 --set 이 파일을 이김
replicaCount: 2
$ kubectl get cm web-mychart-config → {"env":"prod","message":"from-file"}
```

- `helm get values`는 **사용자가 준 값만**, `-a`(`--all`)는 기본값까지 합친 계산 결과(`COMPUTED VALUES`)를 보여 준다.

##### 3-2. 값이 사라지는 함정: `--reuse-values`

| 명령 | 이전 릴리스에서 준 값 | 결과 (실측) |
|---|---|---|
| `helm upgrade web mychart --set image.tag=alpine` (옵션 없음) | **버려진다** | user values가 `image.tag`만 남고, `replicas=1`로 돌아감 (이전 `replicaCount: 2`가 사라짐) |
| `helm upgrade … --reuse-values --set replicaCount=3` | 유지되고 새 값만 덮어씀 | `config.env: staging`, `config.message: from-file` 유지 + `replicaCount: 3` |

- **upgrade는 기본적으로 이전에 `--set`/`-f`로 준 값을 이어받지 않는다.** 차트 기본값 + 이번에 준 값으로 다시 계산한다. 매번 같은 `-f values.yaml`을 주거나 `--reuse-values`를 쓴다. (GitOps에서는 values 파일이 Git에 있어 문제가 없다.)

##### 3-3. 설정 변경이 Pod에 반영되게 하는 패턴: 체크섬 애너테이션

ConfigMap만 바뀌면 Deployment의 Pod 템플릿은 안 바뀌어 롤아웃이 안 일어난다(10장). 이 차트의 Deployment에 다음을 추가했다.

```yaml
annotations:
  checksum/config: {{ include (print $.Template.BasePath "/configmap.yaml") . | sha256sum }}
```

```
$ helm upgrade web mychart --set config.message=changed
checksum changed: yes → 새 ReplicaSet/Pod 롤아웃 (Pod 이름 해시 변경, 이전 Pod Completed/종료)
$ helm upgrade web mychart --set config.message=changed      (같은 값 다시)
REVISION 3, ReplicaSets 2 (새 RS 안 만들어짐)
```

- ConfigMap 내용이 바뀌면 템플릿의 해시가 바뀌어 **롤링 업데이트가 자동 발생**한다. 같은 값으로 다시 upgrade하면 해시가 같아 롤아웃이 없다(하지만 **리비전은 하나 더 생긴다**: revision 3).

---

#### 4. 릴리스 (Release)

##### 4-1. 설치와 확인 (실측)

```
$ helm install web mychart -n t21 --wait
NAME: web   STATUS: deployed   REVISION: 1   DESCRIPTION: Install complete   (NOTES: 출력)
$ helm list -n t21
web  t21  1  ...  deployed  mychart-0.1.0  1.16.0
$ kubectl get deploy,svc,cm,sa → web-mychart, web-mychart-config 등
$ kubectl get deploy web-mychart -o jsonpath='{.metadata.labels}'
{"app.kubernetes.io/instance":"web","app.kubernetes.io/managed-by":"Helm","app.kubernetes.io/name":"mychart", ... "helm.sh/chart":"mychart-0.1.0"}
annotations: meta.helm.sh/release-name: web, meta.helm.sh/release-namespace: t21
```

- 리소스에는 `app.kubernetes.io/managed-by: Helm` 레이블과 `meta.helm.sh/release-name/-namespace` 애너테이션이 붙는다(어느 릴리스 소유인지 표시).
- 설치 후 `kubectl get pod` 목록에 **pre-install 훅 Job은 남아 있지 않았다**(훅 삭제 정책, 5절).

##### 4-2. 릴리스 정보는 Secret에 저장된다 (실측)

```
$ kubectl -n t21 get secret -l owner=helm
sh.helm.release.v1.web.v1   helm.sh/release.v1   1
$ (decode: base64 → base64 → gzip → JSON)
keys: ['name','info','chart','manifest','hooks','version','namespace','apply_method']
info: status=deployed, description="Install complete"     manifest 2422 bytes, hooks 2
```

- 릴리스의 **리비전마다 Secret 하나**(`sh.helm.release.v1.<이름>.v<리비전>`)가 만들어지고 렌더링된 매니페스트, 차트 내용, values가 들어 있다. 이 Secret 이력이 **롤백의 원천**이다. Helm 서버(예전 Tiller)는 필요 없다.
- 그래서 릴리스 정보는 **네임스페이스에 속한다**(`-n`으로 조회). 비밀 정보가 values에 있으면 이 Secret에 평문(압축된 JSON)으로 남는다(이 실험에서는 비밀 값을 넣지 않았다).

##### 4-3. 업그레이드, 이력, 롤백 (실측)

```
$ helm history web -n t21
1  superseded  Install complete
2  superseded  Upgrade complete
...
7  deployed    Upgrade complete
$ helm rollback web 2 -n t21 --wait
Rollback was a success!
$ helm history web | tail -2
7  superseded  Upgrade complete
8  deployed    Rollback to 2          ← 롤백도 **새 리비전(8)** 을 만든다
$ kubectl get cm web-mychart-config  → {"env":"dev","message":"changed"}   (리비전 2의 값)
```

- 롤백은 과거 상태로 "되돌리기"가 아니라 **과거 리비전의 내용을 새 리비전으로 다시 배포**하는 것이다.
- 이력은 기본적으로 쌓인다(`--history-max`로 제한할 수 있으나 시험하지 않았다).

##### 4-4. 실패 처리 (실측)

| 상황 | 결과 |
|---|---|
| 없는 이미지 태그로 `helm upgrade … --wait --timeout 40s` | `UPGRADE FAILED: resource Deployment/t21/web-mychart not ready … context deadline exceeded`. 릴리스 STATUS **`failed`**(revision 9). **이전 Pod는 계속 Running**, 새 Pod는 `ErrImagePull` — 롤링 업데이트라 기존 Pod가 보호됨 |
| 같은 상황에서 `--atomic` | `release web failed, and has been rolled back due to rollback-on-failure being set`. history에 revision 10 `failed`, 그리고 **revision 11 `Rollback to 8`**(deployed)이 생겨 이전 상태로 복구 |

- `--wait`는 리소스가 Ready가 될 때까지 기다린다(없으면 매니페스트 적용 직후 성공으로 본다).
- `--atomic`(Helm 4 메시지에서는 `rollback-on-failure`)은 실패 시 **자동 롤백**한다.

##### 4-5. 라이브 드리프트는 어떻게 되나 (실측)

설치 후 `kubectl scale web-mychart --replicas=5`, ConfigMap `env=manual-edit`으로 **차트 밖에서 수정**하고, 차트/값이 그대로인 `helm upgrade --reuse-values`를 했다.

```
helm get manifest  → replicas: 1 (릴리스 기록상 값)
helm upgrade 후:  replicas=5,  cm {"env":"manual-edit","message":"changed"}      ← 수동 수정이 되돌려지지 않았다
```

- Helm은 **이전 릴리스 매니페스트와 새 매니페스트의 차이**를 중심으로 적용하므로, 두 매니페스트가 같은 필드는 라이브 상태를 건드리지 않았다. 즉 Helm은 **드리프트를 자동으로 교정하지 않는다**(Argo CD와 대비, 6절).
- 이 동작은 Helm 4.1.4와 이 클러스터에서 관찰한 것이다. 버전/옵션(서버 측 적용 등)에 따라 다를 수 있다(확인하지 않음).

##### 4-6. 삭제 (실측)

```
$ kubectl create cm extra ...(차트 밖 리소스) ; 차트에 `helm.sh/resource-policy: keep` ConfigMap 추가
$ helm uninstall web --keep-history
→ deployment, service, 차트의 configmap, pod 등 삭제(Terminating), extra(차트 밖)와 web-mychart-keep(keep 정책)은 남음
$ helm status web → STATUS: uninstalled      (history 보존: sh.helm.release.v1.web.v8/v9 …)
$ helm rollback web → 성공, helm list에 web 14 deployed       (삭제 후에도 이력으로 복구)
$ helm uninstall web                         (history 삭제)
→ helm history web → Error: release: not found,  release Secret 0개
```

- 차트가 만든 리소스만 삭제되고 **차트 밖에서 만든 리소스는 남는다**.
- `helm.sh/resource-policy: keep` 애너테이션이 달린 리소스는 uninstall에서 보존된다.
- `--keep-history`로 지우면 이력이 남아 `helm rollback`으로 되살릴 수 있다. 끝에서 이력까지 삭제하면 복구할 수 없다.

---

#### 5. 훅, 테스트, 서브차트

##### 5-1. 훅 (Hooks)

```yaml
annotations:
  "helm.sh/hook": pre-install,pre-upgrade
  "helm.sh/hook-weight": "0"
  "helm.sh/hook-delete-policy": before-hook-creation,hook-succeeded
```

- 설치/업그레이드 전후에 Job 같은 리소스를 실행한다. 이 실험의 `pre` Job은 성공한 뒤 삭제 정책(`hook-succeeded`)으로 사라졌다(설치 후 목록에 없음). 훅 리소스는 **릴리스 매니페스트(`helm get manifest`)의 관리 대상이 아니고** `helm get hooks`로 따로 본다(`hooks: 2`: 테스트 Pod와 pre Job).

##### 5-2. 테스트

```
$ helm test web -n t21
TEST SUITE: web-mychart-test-connection   Phase: Succeeded
$ kubectl get pod  → web-mychart-test-connection  Completed   (테스트 Pod는 삭제 정책이 없어 남음)
```

##### 5-3. 서브차트(의존성)와 global (실측)

같은 `mychart`를 `alias`로 두 번 의존성에 넣은 상위 차트(`file://../mychart`)를 만들었다.

```
$ helm dependency update parent9
Saving 2 charts   → parent9/charts/mychart-0.1.0.tgz,  Chart.lock(digest 포함)
$ helm template p parent9
Deployment p-frontend (replicas 2, message "front"), Deployment p-backend (replicas 1, message "back")
$ helm template p parent9 --set backend.enabled=false   → Deployment 1개만
```

- 서브차트의 값은 상위 values의 **서브차트 이름(또는 alias) 키 아래**에 넣어 덮어쓴다(`frontend.replicaCount: 2`).
- `condition: backend.enabled`로 서브차트를 켜고 끌 수 있다.
- `global:` 값은 모든 서브차트에서 공유되도록 설계되었다(이번에는 `global.team`을 정의만 했고 템플릿에서 읽어 보지는 않았다).

---

#### 6. 사용 사례: GitOps 아키텍처

##### 6-1. 구조

```
개발자 ──(PR/merge)──▶ Git 저장소 ──(감시)──▶ Argo CD ──(helm template 렌더 + apply)──▶ 쿠버네티스 클러스터
                         ├ charts/mychart/        (차트)             └─ 드리프트 감지/자동 복구
                         └ envs/prod/values.yaml  (환경별 값)
```

- **Git이 원하는 상태의 단일 출처**. 차트와 환경별 values(`envs/prod/values.yaml`)를 Git에 두고, Argo CD(Application)가 `charts/mychart`를 `valueFiles: [../../envs/prod/values.yaml]`로 렌더링해서 배포한다.
- 사람이 `helm install/upgrade`를 직접 실행하지 않는다.

##### 6-2. Argo CD + Helm (실측)

`Application`(`syncPolicy.automated: {prune: true, selfHeal: true}`)으로 배포한 결과:

| 확인 | 결과 |
|---|---|
| 상태 | `Synced / Healthy`, 관리 리소스: ConfigMap, Service, ServiceAccount, Deployment (replicas 2, ConfigMap `env: prod`) |
| **`helm list -A`에 `web` 릴리스가 있나** | **없다.** `app-prod` 네임스페이스에 `sh.helm.release.v1.*` Secret 0개. (이 목록에는 Argo CD와 metrics-server 설치용 Helm 릴리스만 보임) |
| Argo CD가 붙인 것 | 리소스에 레이블 `app.kubernetes.io/managed-by: Helm`(차트 템플릿의 값)은 있지만 소유 표시는 애너테이션 **`argocd.argoproj.io/tracking-id: web:apps/Deployment:app-prod/web-mychart`** |
| `helm.sh/hook: pre-install` Job | Argo CD가 **`PreSync` 훅**으로 실행: `Job/web-mychart-pre Synced PreSync Succeeded` |
| `helm test` 훅 Pod | 이 환경의 Argo CD 설치 목록(`ConfigMap, Service, ServiceAccount, Deployment`)에 포함되지 않았다 |

- **Argo CD는 Helm을 "템플릿 엔진"으로만 쓴다**(`helm template`처럼 렌더링한 YAML을 `kubectl apply`와 유사하게 적용). 그래서 **`helm list`/`helm rollback`/릴리스 Secret은 존재하지 않는다.** 이력은 Argo CD(`status.history`: 0, 1, …)와 Git 이력이 담당한다.
- Helm 훅은 Argo CD의 훅 단계(PreSync 등)로 **매핑**돼 실행됐다.

##### 6-3. 드리프트와 롤백

| 시도 | 결과 (실측) |
|---|---|
| `kubectl scale web-mychart --replicas=5`, ConfigMap `env=manual` | **5초 안에 `replicas=2`, `env=prod`로 되돌려짐** (`selfHeal: true`). **4-5절의 Helm 직접 사용과 반대** |
| Git의 values 수정(`message: gitops-v2`, `replicaCount: 3`)을 push | 약 15초 안에 `replicas=3`, ConfigMap `message=gitops-v2` 자동 반영 (Argo CD 폴링 주기를 `timeout.reconciliation: 30s`로 설정함, 기본은 120~180s), 체크섬 애너테이션 덕분에 롤아웃도 진행(ReplicaSet 2개) |
| **롤백**: Git에서 해당 커밋을 `git revert` 후 push | 약 25초 안에 `message=gitops-v1`, `replicas=2`로 복구. **`helm rollback`이 아니라 Git 이력을 되돌리는 것**이 롤백 수단 |
| 실험 중 `revert`를 두 번 연속 해서 상태가 v2로 돌아가고 v1 복구가 늦어 보였다(내 조작 실수) | 마지막 한 번의 `revert`로 정상 복구. Argo CD의 반영 지연이라기보다 **내가 `revert`를 두 번 적용해서 최종 상태가 v2였던 것**으로 확인함 |

- GitOps의 장점: 변경 이력이 Git에 남고(감사, 리뷰), 클러스터 상태가 항상 Git과 일치하도록 **자동 복구**되며, 롤백은 `git revert`다.
- 위 시나리오의 `kubectl` 직접 수정은 **차단**되지 않고 단지 **되돌려질** 뿐이다(수정 권한 자체는 RBAC로 막아야 함, 13장).
- 10장에서 본 것처럼 **Secret 값은 Git에 두지 않는다**(ExternalSecret 같은 참조만).

##### 6-4. 배포 도구 비교 (이 장의 실험 범위 기준)

| | `helm install/upgrade` 직접 | Argo CD + Helm 차트 |
|---|---|---|
| 릴리스/이력 저장 | 클러스터의 `sh.helm.release.v1.*` Secret | Argo CD `Application.status`와 Git |
| 롤백 | `helm rollback N` (새 리비전 생성) | `git revert` + 자동 sync |
| 드리프트 | **교정 안 함**(4-5 실측) | `selfHeal`로 **자동 교정**(6-3 실측) |
| Helm 훅 | Helm이 실행 | Argo CD 훅 단계로 매핑(PreSync 확인) |
| `helm test` | `helm test` | (이 실험에서 실행하지 않음) |

---

#### 7. 선택 가이드와 함정

| 질문 | 답 (이 실험 기준) |
|---|---|
| YAML 중복을 줄이고 환경별 값을 분리 | 차트 + values |
| 배포 전 렌더링 확인 | `helm template`, `helm lint` |
| upgrade 때 이전 값이 사라졌다 | `-f values.yaml` 반복 지정 또는 `--reuse-values` |
| ConfigMap만 바꿨는데 Pod가 그대로 | `checksum/config` 애너테이션 패턴 |
| 실패한 업그레이드 | `--atomic`(자동 롤백), 아니면 `helm rollback` |
| 롤백한 이력이 늘어난다 | 롤백도 새 리비전을 만든다(`--history-max` 제한) |
| 수동 `kubectl` 수정 | Helm은 되돌리지 않는다. 필요하면 GitOps(selfHeal) |
| 차트가 만든 리소스 보존 | `helm.sh/resource-policy: keep` |
| 비밀 값 | `values`에 넣지 않는다(릴리스 Secret에 남음, Git에도 평문 금지) |
| GitOps에서 `helm list`가 비어 있다 | 정상. Argo CD는 릴리스 객체를 만들지 않는다 |

함정:

- `helm upgrade`는 이전의 `--set`을 기억하지 않는다.
- `helm uninstall`은 차트 밖에서 만든 리소스를 지우지 않고, `keep` 정책 리소스도 남긴다.
- 같은 values로 upgrade해도 리비전은 늘어난다.
- Argo CD로 관리하는 차트에 `helm rollback`을 쓰려 하면 릴리스가 없다.
- 차트 훅(Job)이 실패하면 설치/동기화가 막힐 수 있다(이번에 실패 케이스를 만들지 않았다).

---

#### 8. 확인하지 못한 부분

- **원격 차트 저장소/OCI 레지스트리**에서 차트를 받아 설치(`helm repo add`, `helm install repo/chart`)는 이 장에서 쓴 차트(로컬 디렉터리)로는 하지 않았다. (Argo CD와 metrics-server는 저장소에서 설치했지만 차트 구조 분석은 하지 않았다.) 차트 서명/검증(provenance)도 확인하지 않았다.
- **`helm diff` 플러그인**, `helm get all`, `helm env`, `--dry-run=server`, `--history-max`, `helm upgrade --install`, `--create-namespace`의 세부 동작은 시험하지 않았다(일부는 일반 지식).
- **Helm 4의 서버 측 적용(Server-Side Apply)과 3-way 병합의 정확한 동작**(4-5의 드리프트 비교가 어떤 메커니즘인지)은 확인하지 못했다. 릴리스 Secret의 `apply_method` 필드가 있었지만 값을 확인하지 않았다.
- **Argo CD의 다양한 Helm 옵션**(`parameters`, `fileParameters`, `skipCrds`, `helm.sh/hook` 상세 매핑, `helm test` 훅 처리)과 App of Apps/ApplicationSet은 시험하지 않았다. `helm test` 훅 Pod가 Argo CD로 배포됐을 때의 동작도 확인하지 못했다.
- **Flux** 등 다른 GitOps 도구와의 비교는 하지 않았다.
- **Kustomize**와 Helm의 비교, 차트 템플릿 작성의 고급 문법(`range`, `tpl`, `lookup`, named templates 상세)은 다루지 않았다.
- **차트의 `values.schema.json` 검증**, CRD 설치(`crds/` 디렉터리) 동작은 시험하지 않았다.
- **서브차트 `global` 값의 전달**은 정의만 했을 뿐 템플릿에서 읽지 않았다.
- Argo CD 폴링 주기 30초는 이번 실험을 위해 바꾼 값이다(기본 동작 시간과 다름). **웹훅 기반 즉시 동기화**, 다중 클러스터, 프라이빗 저장소 인증은 확인하지 못했다.
- 위 6-3의 "`revert`를 두 번 적용" 건은 실험 조작 실수의 기록이며, 롤백 동작 확인은 마지막 한 번의 `revert`로 이루어졌다.
