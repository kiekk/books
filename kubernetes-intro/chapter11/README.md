### 11장. 시크릿

> 정리 범위: 시크릿(Secret)의 개념과 타입
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop 내장 쿠버네티스 v1.32.2 (단일 노드, 이 클러스터의 etcd 직접 조회 포함). 모든 명령에 `--context docker-desktop`을 명시했다(`k`로 줄여 적음, 네임스페이스 `t11`, 실험 후 삭제).
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: 컨피그맵의 사용 방식/갱신 동작은 [10장](../chapter10/README.md), 외부 시크릿 저장소(AWS Secrets Manager + ESO)는 10장 7절에서 다뤘다. 여기서는 Secret 자체의 특징과 타입을 중심으로 적었다.

```bash
k() { kubectl --context docker-desktop -n t11 "$@"; }
```

#### 목차

1. 시크릿 개념
2. 만들기와 base64
3. 타입 정리
4. 사용 방법과 갱신
5. 제약과 immutable
6. 보안 관점: Secret은 암호화가 아니다
7. 선택 가이드와 함정
8. 확인하지 못한 부분

---

#### 1. 시크릿 개념

**비밀번호, 토큰, 키 같은 민감한 데이터를 저장하는 오브젝트.** 사용법은 ConfigMap과 거의 같고(환경변수, 볼륨), 다음이 다르다.

| 구분 | ConfigMap | Secret |
|---|---|---|
| 용도 | 일반 설정 | 민감한 데이터 |
| 데이터 인코딩 | 평문(`data`) | **base64**(`data`) 또는 평문 입력(`stringData`) |
| `kubectl describe` | 값을 보여 줌 | **값을 숨김** (바이트 수만) |
| 볼륨 마운트 | 노드 디스크(ext4) | **tmpfs(메모리)** |
| 타입 | 없음 | **있음** (`type`) |
| 크기 제한 | 1MiB | 1MiB |

---

#### 2. 만들기와 base64

```
$ k create secret generic g --from-literal=user=admin --from-literal=pw='p@ss w0rd'
$ k get secret g -o jsonpath='{.data}'
{"pw":"cEBzcyB3MHJk","user":"YWRtaW4="}
$ k get secret g -o jsonpath='{.data.pw}' | base64 -d
p@ss w0rd
$ k describe secret g
Type:  Opaque
Data
====
pw:    9 bytes
user:  5 bytes
```

- `data`의 값은 **base64 인코딩**일 뿐이다. 누구나 `base64 -d`로 풀 수 있다.
- `describe`는 값 대신 바이트 수만 보여 준다.
- `data`에 base64가 아닌 값을 넣으면 거부된다: `illegal base64 data at input byte 3`.

##### 2-1. `stringData`는 쓰기 전용 편의 필드

```
data: {b: <three의 base64>}      stringData: {a: plain, b: two}
$ k get secret sd -o jsonpath='{.data}'         → {"a":"cGxhaW4=","b":"dHdv"}      (dHdv = "two")
$ k get secret sd -o jsonpath='{.stringData}'   → (빈 값)
```

- 평문으로 적은 `stringData`는 저장될 때 base64로 변환돼 `data`로 합쳐지고 `stringData`는 읽을 수 없다.
- 같은 키가 `data`와 `stringData`에 모두 있으면 **`stringData`가 이겼다**(`b`가 `three`가 아니라 `two`).

---

#### 3. 타입 정리

`type` 필드는 Secret이 담는 데이터의 용도를 알리고, 일부 타입은 **필수 키를 API 서버가 검사**한다. 타입은 만든 뒤 **바꿀 수 없다**(`type: Invalid value: "Opaque2": field is immutable`).

| 타입 | 용도 | 필수 키 | 만드는 방법 / 실측 |
|---|---|---|---|
| `Opaque` (기본) | 임의 key-value | 없음 | `create secret generic` |
| `kubernetes.io/dockerconfigjson` | 프라이빗 레지스트리 인증 | `.dockerconfigjson` | `create secret docker-registry`. 내용: `{"auths":{"r.example.com":{"username":"u","password":"p","email":"e@x.com","auth":"dTpw"}}}` |
| `kubernetes.io/tls` | TLS 인증서/키 | `tls.crt`, `tls.key` | `create secret tls` |
| `kubernetes.io/basic-auth` | 기본 인증 | `username`, `password` (실측: 둘 다 필수) | `--type=kubernetes.io/basic-auth` |
| `kubernetes.io/ssh-auth` | SSH 개인키 | `ssh-privatekey` | `--type=kubernetes.io/ssh-auth --from-file=ssh-privatekey=…` |
| `kubernetes.io/service-account-token` | ServiceAccount 토큰 | 애너테이션 `kubernetes.io/service-account.name` | 아래 3-1 |
| `bootstrap.kubernetes.io/token` | 노드 부트스트랩 토큰 | (kube-system에서 쓰임) | 이 실험의 `t11`에는 키 없이도 생성됨 |
| 사용자 정의 (`example.com/my-type`) | 자체 용도 구분 | 없음 | 만들어짐 |
| `kubernetes.io/dockercfg` | 구형 도커 설정 | `.dockercfg` | **실험하지 않음** |

##### 3-1. 필수 키 검증 (실측)

```
--type=kubernetes.io/tls        : [data[tls.crt]: Required value, data[tls.key]: Required value]
--type=kubernetes.io/dockerconfigjson : data[.dockerconfigjson]: Required value
--type=kubernetes.io/basic-auth : [data[username]: Required value, data[password]: Required value]
--type=kubernetes.io/ssh-auth   : data[ssh-privatekey]: Required value
--type=bootstrap.kubernetes.io/token : created   (검증 없음)
--type=example.com/my-type      : created
```

- 필수 키가 없으면 API 서버가 거부한다. 단, **값의 내용**은 검사하지 않는다: `tls.crt=junk`를 넣은 `tls` 타입 Secret이 `Warning: tls: failed to find any PEM data in certificate input` 경고만 나오고 **만들어졌다**.

##### 3-2. ServiceAccount 토큰 Secret

```
$ k create secret generic sat --type=kubernetes.io/service-account-token
error: ... metadata.annotations[kubernetes.io/service-account.name]: Required value

# 애너테이션을 달아 생성
metadata: {name: bot-token, annotations: {kubernetes.io/service-account.name: bot}}
type: kubernetes.io/service-account-token
→ 3초 뒤 data 키: ca.crt, namespace, token   (컨트롤러가 채워 넣음)
$ (token 의 payload)
{"iss":"kubernetes/serviceaccount","kubernetes.io/serviceaccount/namespace":"t11",
 "kubernetes.io/serviceaccount/secret.name":"bot-token", ...}
$ k delete sa bot  → bot-token 도 사라짐 (ServiceAccount에 묶임)
```

- `default` 서비스어카운트에는 자동으로 만들어진 토큰 Secret이 **없었다**(최신 버전은 토큰을 자동 생성하지 않고 `TokenRequest`로 단기 토큰을 발급해 Pod에 주입한다고 알려져 있다. 이 실험에서 Pod에 주입된 토큰 자체는 확인하지 않았다).
- 장기 토큰 Secret은 위처럼 **직접 만들어야** 한다. 장기 토큰은 만료되지 않으므로 가능하면 피한다(문서 권고).

---

#### 4. 사용 방법과 갱신

Secret `g`를 환경변수와 볼륨으로 사용했다.

```
env PW=p@ss w0rd   S_user=admin   S_pw=p@ss w0rd     (envFrom + prefix S_)
$ ls -lL /etc/sec        -rw-r--r-- pw, user          ← 기본 파일 권한 0644
$ ls -lL /etc/sec2/x     -r-------- pw                (defaultMode: 0400, items 로 경로 지정)
$ mount | grep /etc/sec
tmpfs on /etc/sec  type tmpfs (ro,relatime,size=7922748k,noswap)
```

- 볼륨은 **tmpfs(메모리)** 다. 노드 디스크에 평문 파일이 남지 않는다. (`size`는 노드 메모리 크기에서 온 값이다.)
- 기본 파일 권한은 `0644`다. 더 좁히려면 `defaultMode`나 `items[].mode`를 쓴다.
- `kubectl describe pod`는 `PW: <set to the key 'pw' in secret 'g'>`로 값을 숨긴다.
- ConfigMap과 마찬가지로 **갱신 동작**이 다르다:

```
$ k patch secret g -p '{"stringData":{"pw":"CHANGED"}}'
볼륨:   약 72초 뒤 CHANGED
환경변수: PW=p@ss w0rd (안 바뀜)
```

- 환경변수는 재시작 전까지 옛 값이다(볼륨은 약 72초, 이 실험 1회 측정).

##### 4-1. imagePullSecrets

```
spec: {imagePullSecrets: [{name: reg}], containers: [{image: r.example.com/priv/app:1}]}
→ ErrImagePull / ImagePullBackOff  (r.example.com 이 실제 레지스트리가 아니어서)
```

`imagePullSecrets`에 `dockerconfigjson` Secret을 지정하면 kubelet이 이미지를 받을 때 그 인증 정보를 쓴다. 이 실험의 레지스트리는 실존하지 않아 **인증이 실제로 통과하는 것**은 확인하지 못했다(시도 자체와 실패만 확인). ServiceAccount에 `imagePullSecrets`를 붙여 Pod마다 쓰지 않아도 되게 하는 방법도 있지만 실험하지 않았다.

---

#### 5. 제약과 immutable

- 크기: ConfigMap과 같은 **1MiB 제한** (`data: Too long: may not be more than 1048576 bytes`).
- `immutable: true`: `data` 수정이 거부된다 (`field is immutable when `immutable` is set`). 변경하려면 삭제 후 재생성이다.
- 네임스페이스 범위이며 다른 네임스페이스의 Secret은 참조할 수 없다(10장 ConfigMap과 같은 규칙으로 알려져 있으나 Secret으로는 실험하지 않았다).

---

#### 6. 보안 관점: Secret은 암호화가 아니다

##### 6-1. etcd에 평문으로 저장된다 (이 클러스터)

```
$ kube-apiserver 인자에서 encryption 검색  → (--encryption-provider-config 없음)
$ etcdctl get /registry/secrets/t11/g | strings
...
CHANGED
user
admin
```

- 이 Docker Desktop 클러스터는 **저장 시 암호화(encryption at rest)가 꺼져 있어** etcd에서 값이 그대로 보였다(`k8s:enc:` 접두사가 아닌 평문). etcd에 접근할 수 있는 사람은 값을 읽는다.
- EKS 등 관리형 서비스의 저장 시 암호화(KMS 연동 등)는 이 실험에서 확인하지 못했다.

##### 6-2. 읽기 권한이 곧 값을 읽는 권한

```
$ k auth can-i get secret/g --as=system:serviceaccount:t11:reader   → no
$ (list 권한만 가진 role 부여 후)
$ k get secrets --as=...:reader -o jsonpath='{.items[0].data}'
{"password":"cA==","username":"dQ=="}      ← list 만 줘도 모든 Secret 값을 읽는다
```

- **`list`/`watch`만 줘도 값이 노출된다.** Secret에 대한 RBAC는 `get`뿐 아니라 `list`도 신중히 준다(13장).
- `kubectl get secret -o yaml`은 base64 값을 그대로 출력한다.

##### 6-3. 우연한 유출 경로

```
$ cat <stringData: {k: supersecret}> | kubectl apply -f -
$ k get secret leak -o jsonpath='{.metadata.annotations}'
{"kubectl.kubernetes.io/last-applied-configuration":"{...\"stringData\":{\"k\":\"supersecret\"}}\n"}
```

- 클라이언트 측 `kubectl apply`로 `stringData`를 적용하면 **평문이 `last-applied-configuration` 애너테이션에 남는다.** `data`로 apply해도 base64 값이 그대로 남는다. 서버 측 apply(`--server-side`)에서는 확인하지 않았다.
- Pod 환경변수는 `describe`엔 숨겨지지만 컨테이너 안(`env`, `/proc`), 로그, 크래시 덤프로 새기 쉽다. 환경변수보다 볼륨 마운트를 권하는 의견이 있으나 이 실험에서 우열을 비교하지는 않았다.

##### 6-4. 정리: 안전하게 쓰려면

| 항목 | 권장 |
|---|---|
| 저장 시 암호화 | 클러스터에서 활성화(KMS 등), 이 클러스터는 꺼져 있음 |
| RBAC | `get`/`list`/`watch`를 최소 범위로 |
| Git | Secret 매니페스트를 평문으로 올리지 않는다 → 외부 시크릿 저장소(10장 7절) 또는 암호화 도구 |
| 사용 방식 | 볼륨(tmpfs) 마운트, 갱신이 필요하면 재읽기 |
| 민감 정보를 애너테이션/ConfigMap에 | 넣지 않는다 |

---

#### 7. 선택 가이드와 함정

| 질문 | 답 (이 실험 기준) |
|---|---|
| base64면 안전한가 | 아니다. 인코딩일 뿐 |
| `stringData`로 편하게 | 쓰기 전용. 읽으면 `data`(base64)로 나옴. 같은 키면 `stringData` 우선 |
| 타입은 필수인가 | 아니다(Opaque 기본). 다만 `tls`, `dockerconfigjson` 등은 필수 키 검사를 받는다. 내용은 검사 안 됨 |
| 타입을 바꾸려면 | 불가. 삭제 후 재생성 |
| 값을 바꿨는데 앱에 반영 안 됨 | 환경변수는 재시작, 볼륨은 약 1분 뒤 갱신 + 앱 재읽기 |
| `describe`에서 값이 안 보임 | 숨김일 뿐이고 `get -o yaml`, 볼륨, etcd에서는 보인다 |
| `list` 권한만 줬다 | 값까지 읽힌다 |
| 장기 SA 토큰 | 직접 만든 `service-account-token` Secret은 만료가 없다 |

---

#### 8. 확인하지 못한 부분

- **저장 시 암호화(EncryptionConfiguration, KMS)**를 켠 클러스터에서의 동작과 EKS의 기본 설정은 확인하지 못했다. 이 Docker Desktop 클러스터는 꺼져 있다는 것만 확인했다.
- **`kubernetes.io/dockercfg`** 타입과 `bootstrap.kubernetes.io/token`의 실제 사용(kube-system, 필수 키 `token-id`, `token-secret` 등)은 실험하지 않았다.
- `imagePullSecrets`의 **인증 성공 경로**(실제 프라이빗 레지스트리)와 ServiceAccount 연결.
- Pod에 자동 주입되는 **TokenRequest 기반 SA 토큰**(projected volume)은 확인하지 않았다. `default` SA에 토큰 Secret이 없다는 것만 확인했다.
- 서버 측 apply(`--server-side`)에서 `last-applied` 애너테이션 유출 여부.
- **Secret 갱신 지연 시간**은 1회 측정(약 72초)뿐이며 `subPath`와 환경변수 외의 경우(`items`)는 확인하지 않았다.
- Secret의 **크기 한도/개수가 etcd 성능에 미치는 영향**, 대량 Secret의 kubelet watch 부하와 `immutable`의 효과는 측정하지 않았다.
- **외부 시크릿 연동**(ESO, Secrets Store CSI Driver, Sealed Secrets, SOPS)은 ESO fake 프로바이더 재현([10장](../chapter10/README.md))만 했다. Sealed Secrets와 SOPS는 설치하지 않았다.
- 다른 네임스페이스의 Secret 참조 불가 여부를 Secret으로는 직접 확인하지 않았다(ConfigMap으로만 확인).
