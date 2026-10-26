### 10장. 컨피그맵

> 정리 범위: 컨피그맵(ConfigMap)의 개념과 특징, 그리고 EKS에서 ConfigMap과 AWS Secrets Manager를 연동해서 쓰는 방법(ArgoCD 포함)
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2. ConfigMap 실험은 이 클러스터에서 `--context docker-desktop`을 명시해 했다(`k`로 줄여 적음, 네임스페이스 `t10`, 실험 후 삭제). **AWS 연동 부분은 실제 EKS/AWS 계정이 없어서 같은 구조를 임시 kind 클러스터(v1.35.0)에서 재현**했다. External Secrets Operator(차트 2.14.0)와 Argo CD(차트 10.11.1, v3.5.6)를 설치하고, AWS Secrets Manager 자리에는 ESO의 **`fake` 프로바이더**를 썼으며 Git 저장소는 클러스터 안의 `git daemon` Pod로 대신했다(실험 후 클러스터와 kubeconfig 삭제).
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. **AWS 쪽(IAM, IRSA, Pod Identity, 실제 Secrets Manager 호출)은 실행하지 못했고** 웹 검색 결과와 일반 지식에 근거한 부분을 3절에서 따로 표시했다.

```bash
k() { kubectl --context docker-desktop -n t10 "$@"; }
```

#### 목차

1. 컨피그맵 개념
2. 만드는 방법
3. 제약 (키 이름, 크기, 네임스페이스)
4. 사용하는 방법 4가지와 갱신 동작
5. 없는 컨피그맵/키를 참조할 때
6. immutable
7. EKS에서 ConfigMap과 AWS Secrets Manager 연동 (ArgoCD)
8. 선택 가이드와 함정
9. 확인하지 못한 부분

---

#### 1. 컨피그맵 개념

**컨테이너 이미지와 설정을 분리**하기 위한 오브젝트. 환경별로 달라지는 설정(로그 레벨, 호스트, 설정 파일)을 key-value로 저장해 두고 Pod가 환경변수, 인자, 파일로 가져다 쓴다. 이미지를 다시 빌드하지 않고 설정만 바꿀 수 있다.

- 평문 저장이다. **비밀 정보는 Secret**(11장)에 넣는다.
- 네임스페이스에 속한 리소스다.
- 데이터는 `data`(문자열)와 `binaryData`(base64 바이너리) 두 필드에 둔다.

---

#### 2. 만드는 방법

```
$ k create cm lit --from-literal=a=1 --from-literal=b=two
$ k create cm file --from-file=app.properties
$ k create cm filekey --from-file=custom.txt=app.properties
$ k create cm envf --from-env-file=app.env
$ k create cm dir --from-file=.
```

| 방식 | 결과 (실측) |
|---|---|
| `--from-literal` | 키=값 그대로 |
| `--from-file=파일` | **키는 파일명**, 값은 파일 전체 내용(`{"app.properties":"color=blue\nsize=10\n"}`) |
| `--from-file=키=파일` | 키를 직접 지정 (`custom.txt`) |
| `--from-env-file` | 파일의 **각 줄이 키=값으로 분리** (`{"LOG":"debug","MODE":"fast"}`) |
| `--from-file=디렉터리` | 디렉터리의 파일 하나가 키 하나 (`app.env`, `app.properties`, `big.txt`, 스크립트 파일 이름들 …) |

- 디렉터리를 통째로 넣었을 때 `bin.dat`(바이너리 파일)는 **`binaryData`**로 들어갔다(UTF-8 텍스트가 아니면 자동 분류).
- 같은 키가 `data`와 `binaryData`에 동시에 있으면 거부된다: `data[k]: Invalid value: "k": duplicate of key present in binaryData`.

---

#### 3. 제약

##### 3-1. 키 이름

| 키 | 결과 |
|---|---|
| `a b`, `a/b` | 거부 (영숫자, `-`, `_`, `.`만) |
| `..` | 거부 (`must not be '..'`) |
| `.hidden` | 통과 |
| `UPPER_x-1.y` | 통과 |

##### 3-2. 크기

```
$ k create cm big --from-file=big.txt        (1048577 바이트)
ConfigMap "big" is invalid: []: Too long: may not be more than 1048576 bytes
$ k create cm mid --from-file=mid.txt        (1000000 바이트)  → created
```

한 ConfigMap의 합계는 **1MiB(1048576바이트) 이하**다. 큰 파일은 이미지나 볼륨으로 둔다.

##### 3-3. 네임스페이스

같은 네임스페이스의 ConfigMap만 참조할 수 있다.

```
$ (t10)     Pod envFrom configMapRef lit  → Running
$ (default) Pod envFrom configMapRef lit  → CreateContainerConfigError
  Warning  Failed  kubelet  Error: configmap "lit" not found
```

---

#### 4. 사용하는 방법 4가지와 갱신 동작

테스트 ConfigMap `app`(`color=blue`, `size=10`, `conf.ini=[a]\nx=1`)을 한 Pod에서 모두 사용했다.

| 방법 | 설정 | 결과 |
|---|---|---|
| 개별 키 → 환경변수 | `env[].valueFrom.configMapKeyRef` | `COLOR=blue` |
| 전체 → 환경변수 | `envFrom[].configMapRef`, `prefix: CFG_` | `CFG_color=blue`, `CFG_size=10`, **`CFG_conf.ini=[a]`** (점이 있는 키도 환경변수로 들어감) |
| 볼륨(전체) | `volumes[].configMap` | 키마다 파일 하나 |
| 볼륨(일부 키, 경로/권한 지정) | `items: [{key, path, mode}]` | `/etc/sel/sub/app.ini` 만 생성 |
| 볼륨 + `subPath` | `volumeMounts[].subPath` | 파일 하나만 마운트 |
| 명령 인자 | `$(VAR)` | env로 가져온 값을 args에서 참조 (5장) |

##### 4-1. 볼륨의 구조

```
/etc/full:
drwxr-xr-x  ..2026_10_25_13_45_52.4060016238          ← 실제 파일이 있는 타임스탬프 디렉터리
lrwxrwxrwx  ..data -> ..2026_10_25_13_45_52.4060016238
lrwxrwxrwx  color -> ..data/color
lrwxrwxrwx  conf.ini -> ..data/conf.ini
/dev/vda1 on /etc/full type ext4 (ro,relatime,discard)    ← 읽기 전용
```

- 각 키 파일은 심볼릭 링크이고, `..data` 링크를 새 타임스탬프 디렉터리로 **원자적으로 바꾸는** 방식으로 갱신한다.
- 마운트는 읽기 전용이다.
- `items`에 `mode: 0400`을 주면 개별 파일 권한을, `defaultMode: 0600`을 주면 전체 파일 권한을 바꾼다(`defaultMode: 0600`으로 `-rw-------` 확인).

##### 4-2. ConfigMap을 바꾸면 Pod에는 언제 반영되나 (실측)

`color=blue`를 `red`로 patch하고 5초 간격으로 확인했다.

| 사용 방식 | 반영 |
|---|---|
| **볼륨** (`/etc/full/color`) | **87초 뒤 `red`** (이 실험의 1회 측정값, kubelet 동기화 주기에 따라 달라짐) |
| **환경변수** (`COLOR`, `CFG_color`) | **안 바뀜** (`blue blue`) |
| **볼륨 + `subPath`** (`/etc/single/color.txt`) | **안 바뀜** (`blue`) |
| `items`로 가져온 볼륨 | 이번 실험에서는 `conf.ini`를 바꾸지 않아 갱신 여부는 보지 않았다 |

- 환경변수와 `subPath`는 Pod를 다시 만들어야(`rollout restart`) 새 값이 된다.
- 볼륨 갱신 후 앱이 파일을 **다시 읽어야** 한다. 앱이 시작 시 한 번만 읽으면 값이 안 바뀐다.
- ConfigMap이 바뀌어도 Deployment는 롤아웃을 하지 않는다. 설정 변경을 롤아웃으로 이어가려면 Pod 템플릿의 애너테이션에 ConfigMap 해시를 넣거나(Helm의 흔한 패턴), 별도 도구를 쓴다(이 실험에서는 확인하지 않음).

---

#### 5. 없는 컨피그맵/키를 참조할 때

| 상황 | 결과 |
|---|---|
| 없는 ConfigMap을 `env`로 참조 | `CreateContainerConfigError` — `configmap "nosuchcm" not found` |
| ConfigMap은 있고 없는 **키**를 참조 | `CreateContainerConfigError` — `couldn't find key missing in ConfigMap t10/haskeys` |
| 없는 ConfigMap을 **볼륨**으로 참조 | `ContainerCreating`에 머묾 — `FailedMount ... configmap "nosuchcm3" not found` |
| `optional: true` + 없는 ConfigMap 볼륨 | Pod는 **Running** |
| `env`의 `optional: true` + 없는 키 | 변수가 비어 있는 채로 시작 |
| 실행 중에 ConfigMap 삭제 | 실행 중 Pod는 영향 없음 (환경변수 `a=1` 유지) |
| 삭제 후 새 Pod 생성 | `CreateContainerConfigError` → ConfigMap을 다시 만들면 **약 20초 안에 Running** (재시작 없이 kubelet이 재시도) |

- `envFrom`으로 들어온 `bad-key=2`(하이픈 포함 키)는 **환경변수로 그대로 주입됐다**(`bad-key=2` 확인). 환경변수 이름 검증이 완화돼 있는 이 버전의 동작으로 보이지만 그 이유(버전별 기능 게이트)는 확인하지 못했다. 쉘에서 `$bad-key`로 참조할 수 없으므로 이런 키 이름은 피한다.
- `optional: true` 볼륨 마운트의 디렉터리에는 항목이 2개 있었는데(`ls -A | wc -l = 2`) 무엇인지는 열어 보지 않았다.

---

#### 6. immutable

```
$ (immutable: true 인 ConfigMap)
$ k patch cm imm -p '{"data":{"k":"v2"}}'
The ConfigMap "imm" is invalid: data: Forbidden: field is immutable when `immutable` is set
$ k patch cm imm -p '{"immutable":false}'
The ConfigMap "imm" is invalid: immutable: Forbidden: field is immutable when `immutable` is set
$ k patch cm imm -p '{"metadata":{"labels":{"x":"y"}}}'   → patched (레이블은 가능)
$ k delete cm imm   → deleted
```

- `immutable: true`이면 `data`, `binaryData`, `immutable` 자체를 바꿀 수 없고 **삭제 후 재생성만** 가능하다. 메타데이터(레이블)는 바꿀 수 있었다.
- 의도치 않은 변경을 막고, kubelet이 이 ConfigMap을 watch 하지 않아 API 서버 부하가 줄어든다고 알려져 있다(부하 감소는 실측하지 않음). 버전을 이름에 넣고(`app-config-v2`) Deployment가 새 이름을 참조하게 하는 방식과 함께 쓴다.

---

#### 7. EKS에서 ConfigMap과 AWS Secrets Manager 연동 (ArgoCD)

##### 7-1. 먼저 정리: 둘은 역할이 다르다

| | ConfigMap | AWS Secrets Manager |
|---|---|---|
| 담는 것 | 비밀이 아닌 설정 | DB 비밀번호, API 키 등 |
| 저장소 | etcd(평문) | AWS 관리형 서비스(KMS 암호화, IAM 접근 제어) |
| 쿠버네티스 연결 | 직접 참조 | **Secret으로 동기화**하거나 **볼륨으로 마운트** |

"ConfigMap과 Secrets Manager를 연동"한다는 것은 보통 **비밀이 아닌 값은 ConfigMap(Git에 저장), 비밀은 Secrets Manager(Git에 안 저장)에 두고 Pod에서 같이 쓰는 구조**를 가리킨다. Secrets Manager의 값을 ConfigMap에 넣는 것은 평문 저장이라 권장되지 않는다(이 문서에서 다루지 않음).

##### 7-2. 방식 비교

| | **External Secrets Operator (ESO)** | **Secrets Store CSI Driver + AWS 프로바이더 (ASCP)** |
|---|---|---|
| 동작 | 컨트롤러가 Secrets Manager를 읽어 **Kubernetes Secret을 만든다** | Pod가 CSI 볼륨을 마운트할 때 **파일로 마운트**, 선택적으로 Secret 동기화 |
| Git에 두는 것 | `ExternalSecret`(참조만 있음), `SecretStore` | `SecretProviderClass`(참조만 있음) |
| 값 갱신 | `refreshInterval` 마다 자동 | 볼륨 갱신은 드라이버 로테이션 옵션(이 문서에서 검증 안 함) |
| Secret 오브젝트 | 항상 생김 | **Pod가 마운트해야** 생김(동기화 켠 경우) |
| 이 실험 | **재현함 (fake 프로바이더)** | 안 함 |

(두 방식의 비교 중 CSI 쪽은 웹 검색 결과에 근거한다: 출처는 아래 7-5.)

##### 7-3. ESO + ArgoCD 구조 (재현한 부분)

```
Git (ExternalSecret, SecretStore, ConfigMap, Deployment)
  └─ ArgoCD가 sync ──▶ 클러스터
        ExternalSecret ──(ESO가 읽음)──▶ 외부 저장소(AWS SM / 여기서는 fake)
                              └──▶ Secret(db-secret) 생성
        Deployment ── secretKeyRef / secret 볼륨 ──▶ db-secret
```

Git에 올린 매니페스트(요약):

```yaml
# store.yaml (sync-wave -1)        # 실제 EKS에서는 provider.aws 로 바뀐다 (7-4)
kind: SecretStore
spec: {provider: {fake: {data: [{key: /prod/app/db, value: '{"username":"admin","password":"pw-v1"}'}]}}}
# externalsecret.yaml (sync-wave 0)
kind: ExternalSecret
spec:
  refreshInterval: 15s
  secretStoreRef: {name: fake-aws, kind: SecretStore}
  target: {name: db-secret, creationPolicy: Owner, deletionPolicy: Retain}
  data:
  - {secretKey: DB_PASSWORD, remoteRef: {key: /prod/app/db, property: password}}
  - {secretKey: DB_USER,     remoteRef: {key: /prod/app/db, property: username}}
# app.yaml (Deployment: sync-wave 1) ConfigMap(app-config) envFrom + db-secret 을 env/볼륨으로 사용
```

Application은 `syncPolicy.automated: {prune: true, selfHeal: true}`로 만들었다. `external-secrets.io/v1` API가 설치된 CRD에 있었다(`v1`, `v1beta1`).

##### 7-3-1. 실측 결과

| 확인 | 결과 |
|---|---|
| 최초 sync | `Synced / Healthy`, ESO가 `db-secret`(Opaque, 키 2개) 생성, Pod Running (sync wave 순서대로 적용되도록 애너테이션을 달았지만 시작 순서 자체를 측정하지는 않았다) |
| Argo CD가 관리하는 리소스 목록 | ConfigMap, Deployment, **ExternalSecret, SecretStore**. **`db-secret`은 목록에 없다** |
| `db-secret` 메타데이터 | `ownerReferences: ExternalSecret/db`, ArgoCD 추적 애너테이션 `tracking-id`가 `…ExternalSecret:app/db`(ExternalSecret의 것을 **복사**해서 가진다) |
| Secret을 사람이 `HACKED`로 수정 | **5초 안에 `pw-v1`로 되돌려짐** (ESO가 Secret 변경을 감지) |
| Secret 삭제 | **4초 안에 재생성** |
| 외부 값을 `pw-v2`로 변경(Git에 반영) | Secret은 갱신됨. **환경변수는 `pw-v1` 그대로**, 볼륨 파일은 이후 약 72초에 `pw-v2` |
| 원격 키 이름을 틀리게 수정(`property: nope`) | ExternalSecret `SecretSyncedError (could not get secret data from provider)`, **기존 Secret 값(`pw-v2`)은 유지**, 앱 Pod 영향 없음, Argo CD App은 **Degraded** |
| ExternalSecret을 Git에서 삭제(prune) | ExternalSecret과 `db-secret`이 함께 삭제됨(ownerReference 가비지 컬렉션). 이미 실행 중인 Pod는 계속 동작(환경변수/볼륨은 남음) |
| 그 뒤 `rollout restart` | 새 Pod가 `FailedMount ... secret "db-secret" not found`로 `ContainerCreating`에 머묾 |

##### 7-3-2. ArgoCD에서 겪은 함정

1. **Git 폴링 지연**: 차트 기본 설정에서 `timeout.reconciliation`이 `120s`였다. Git에 push해도 최대 이 정도 뒤에 감지된다(설정에 따라 다름). 즉시 반영하려면 `argocd.argoproj.io/refresh=hard` 애너테이션이나 웹훅을 쓴다.
2. **Degraded 리소스가 sync wave를 막고 sync 작업이 옛 리비전에 묶인다**:
   - 잘못된 ExternalSecret(`property: nope`)을 push해서 sync wave 0의 ExternalSecret이 Degraded가 되자 sync가 `Retrying attempt #4`로 반복했다.
   - 이후 **고친 커밋을 push했는데도 반영되지 않았다.** 진행 중이던 작업(`operation.sync.revision`)이 잘못된 커밋(`0bbf6c9`)에 고정돼 있었기 때문이다.
   - `operation` 필드를 제거해(진행 중 작업 중단) 작업을 끝내자 Argo CD가 최신 커밋을 sync하고 정상이 됐다.
   - 자동 sync와 sync wave를 같이 쓰면 이런 교착이 생길 수 있다. 수동 개입(작업 종료 또는 Sync 재실행)이 필요했다. (원인은 관측한 현상에서 읽은 것이고 Argo CD 내부 구현을 확인한 것은 아니다.)
3. **ExternalSecret이 만든 Secret은 Argo CD의 prune/diff 대상이 아니다**(목록에 없음). Git에 Secret을 두지 않으니 diff로 값이 노출될 일도 없다. 반대로 Argo CD가 `db-secret`의 값을 **관리하지 않으므로** 값 변경은 ESO의 `refreshInterval`이 책임진다.
4. **Pod 재시작은 별도로 해결**해야 한다: Secret 값이 바뀌어도 환경변수는 안 바뀐다. 볼륨 마운트 + 앱의 파일 재읽기, 또는 `rollout restart`/Reloader 같은 도구가 필요하다(Reloader는 설치하지 않아 확인 못 함).

##### 7-4. 실제 EKS에서는 무엇이 달라지나 (실행하지 못함, 웹 검색과 일반 지식에 근거)

`SecretStore`의 프로바이더만 `fake`에서 `aws`로 바뀐다. 개념 구성:

```yaml
kind: ClusterSecretStore            # 또는 네임스페이스 범위 SecretStore
spec:
  provider:
    aws:
      service: SecretsManager
      region: ap-northeast-2        # 예시 값
      auth:
        jwt:
          serviceAccountRef: {name: external-secrets, namespace: external-secrets}   # IRSA
```

필요한 AWS 쪽 준비 (검색 결과에서 확인한 일반 패턴):

1. EKS 클러스터에 IAM OIDC 공급자 연결 → ESO ServiceAccount에 IAM Role을 연결하는 **IRSA**. (검색 결과 중 한 가이드는 eksctl로 IAM Role을 만들고, 정책은 `secretsmanager:GetSecretValue`, `DescribeSecret`을 특정 시크릿 경로 접두사(`production/*` 등)로 제한한다고 설명한다.)
2. Role의 신뢰 정책과 ServiceAccount 애너테이션이 맞아야 한다. 검색 결과는 흔한 장애 원인으로 **ServiceAccount 애너테이션 누락, 신뢰 정책 불일치, Secrets Manager 이름과 ExternalSecret `remoteRef.key` 불일치**를 꼽았다.
3. ArgoCD로 ESO를 배포할 때 CRD를 먼저 설치하려고 sync-wave(예: `-2`)를 쓰는 예가 있다. 이 실험에서는 Helm `installCRDs=true`로 ESO를 따로 설치했고 ESO 자체를 ArgoCD로 배포하는 흐름은 검증하지 못했다.
4. **EKS Pod Identity**(IRSA의 대안): 검색 결과에는 ESO와 Pod Identity에 대한 내용이 없었다. 확인하지 못했다.

Secrets Store CSI 드라이버 방식(검색 결과 기반, 미실행):

- `SecretProviderClass`에 `provider: aws`와 `objects`(가져올 시크릿 이름)을 적고 Pod가 CSI 볼륨으로 마운트한다.
- Kubernetes Secret 동기화는 드라이버 설치 시 `syncSecret.enabled=true`를 줘야 하고, **Pod가 마운트해야** Secret이 만들어진다.
- 검색 결과 중 한 곳(Datadog Operator PR)이 EKS 관리형 애드온 이름을 `aws-secrets-store-csi-driver-provider`로, 인증에 Pod Identity Agent 애드온과 Pod Identity 연결이 필요하다고 적고 있다. **단일 출처이고 공식 EKS 문서 페이지는 확인하지 못했다.**
- `region` 파라미터를 명시하라는 AWS 문서의 안내가 있다(없으면 노드 애너테이션에서 매번 조회).

##### 7-5. 출처 (웹 검색 결과, 원문 전체를 읽지 않았고 요약 수준으로 확인)

- [How to Manage Secrets with ArgoCD and AWS Secrets Manager](https://oneuptime.com/blog/post/2026-02-26-argocd-aws-secrets-manager/markdown)
- [How to Deploy the External Secrets Operator with ArgoCD](https://oneuptime.com/blog/post/2026-02-26-how-to-deploy-the-external-secrets-operator-with-argocd/markdown)
- [Leverage AWS secrets stores from EKS Fargate with External Secrets Operator (AWS 블로그)](https://aws.amazon.com/blogs/containers/leverage-aws-secrets-stores-from-eks-fargate-with-external-secrets-operator/)
- [Use Amazon Secrets Manager secrets in Amazon EKS (AWS 문서 중국 리전 미러)](https://docs.amazonaws.cn/en_us/secretsmanager/latest/userguide/integrating_csi_driver.html)
- [SecretProviderClass 구성 (AWS 문서 중국 리전 미러)](https://docs.amazonaws.cn/en_us/secretsmanager/latest/userguide/integrating_csi_driver_SecretProviderClass.html)
- [Datadog Operator PR #3291 (EKS 애드온 이름 언급)](https://github.com/DataDog/datadog-operator/pull/3291)

---

#### 8. 선택 가이드와 함정

| 질문 | 답 (이 실험 기준) |
|---|---|
| 설정을 바꿨는데 Pod에 반영 안 됨 | 환경변수/`subPath`는 재시작 필요. 볼륨은 약 1~1.5분 안에 갱신 |
| 큰 설정 파일 | 합계 1MiB 제한 |
| 오타로 인한 Pod 시작 실패 | 없는 ConfigMap/키는 `CreateContainerConfigError`, 볼륨은 `FailedMount`. `describe pod`의 Events 확인 |
| 설정 잠금 | `immutable: true` + 이름에 버전 |
| 비밀을 ConfigMap에 | 하지 않는다. Secret이나 외부 시크릿 저장소 |
| 비밀을 Git에 | `ExternalSecret`/`SecretProviderClass`처럼 **참조만** Git에 둔다 |
| ArgoCD + ESO | `SecretStore` → `ExternalSecret` → `Deployment` 순서를 sync wave로, `db-secret`은 ArgoCD 추적 밖, Degraded가 sync를 막을 수 있음 |
| 원격 비밀 변경 후 앱 반영 | ESO가 Secret을 갱신해도 환경변수는 그대로. 재시작 또는 볼륨 재읽기 |

---

#### 9. 확인하지 못한 부분

- **실제 AWS 연동 전부**: IAM 정책/Role, IRSA/Pod Identity, 실제 Secrets Manager 호출, 비용, 지연, 레이트 리밋, 리전 장애, KMS는 확인하지 못했다. 7-4는 검색 결과 요약과 일반 지식이다.
- **ESO 자체를 ArgoCD로 배포**(CRD sync-wave, `SkipDryRunOnMissingResource`)와 ArgoCD가 ESO CRD 설치 전 매니페스트를 처리하는 문제는 실험하지 않았다.
- **Secrets Store CSI Driver + ASCP**: 설치와 동작을 전혀 실행하지 않았다.
- **ArgoCD Application의 ignoreDifferences 설정 필요 여부**: 이 실험에서는 필요하지 않았지만 실제 AWS 프로바이더에서 `ExternalSecret`의 기본값 채움 때문에 diff가 생기는지는 모른다.
- ESO `fake` 프로바이더의 동작이 실제 AWS 프로바이더와 같다고 가정할 수 없다(에러 메시지, 권한 오류, 버전 관리).
- **Reloader 등 설정 변경 시 자동 롤아웃 도구**는 설치하지 않았다.
- ConfigMap `items`로 마운트한 볼륨의 **갱신 여부**, 갱신 시간의 분포(1회 측정값 87초), 클러스터 크기별 영향은 측정하지 않았다.
- `immutable`이 API 서버 부하를 줄인다는 것은 문서 지식이며 측정하지 않았다.
- 환경변수 이름에 하이픈이 허용된 이유(기능 게이트)와 `optional: true` 볼륨 디렉터리의 항목 2개의 정체는 확인하지 못했다.
