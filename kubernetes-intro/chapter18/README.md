### 18장. 오토스케일링

> 정리 범위: 오토스케일링 기준, HPA와 컨트롤러 매니저의 동작 방식
>
> 실측 환경: 16, 17장과 같은 **임시 kind 클러스터 `t16`**(v1.35.0, control-plane 1 + worker 1, 별도 kubeconfig)에 metrics-server(앱 v0.9.0, 수집 주기 15초)가 설치된 상태에서 실험했다. 부하는 CPU를 계속 쓰는 busybox 루프(`while true; do :; done`, requests=limits=100m)라서 **Pod 하나의 CPU 사용률이 항상 약 100%**다. 이 방식은 실제 서비스 부하를 흉내 낸 것이 아니라 HPA의 계산을 단순하게 보려는 장치다. 네임스페이스는 `t16`이다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: 메트릭 수집(metrics-server, `metrics.k8s.io`)은 [17장](../chapter17/README.md), Deployment 롤아웃은 [6장](../chapter06/README.md)에서 다뤘다.

#### 목차

1. 오토스케일링의 종류와 기준
2. HPA 동작 방식 (컨트롤러 매니저)
3. 스케일 계산 공식과 실측
4. 안정화와 behavior (스케일 속도 제어)
5. 메트릭 종류와 여러 메트릭
6. 실패 상황
7. 선택 가이드와 함정
8. 확인하지 못한 부분

---

#### 1. 오토스케일링의 종류와 기준

| 종류 | 무엇을 조절 | 기준(입력) | 이 장 |
|---|---|---|---|
| **HPA** (Horizontal Pod Autoscaler) | **Pod 개수** (replicas) | CPU/메모리 사용률, 커스텀/외부 메트릭 | **실험함** |
| **VPA** (Vertical Pod Autoscaler) | Pod의 **requests/limits 크기** | 실제 사용량 이력 | 설치하지 않음 |
| **Cluster Autoscaler / Karpenter** | **노드 개수** | Pending Pod, 노드 사용률 | 설치하지 않음 |

- 기준이 되는 값: HPA의 CPU/메모리 "사용률(Utilization)"은 **Pod가 요청한 값(requests) 대비 현재 사용량의 비율**이다. 절대값(`AverageValue`)이나 Pod 단위 외 객체 메트릭도 쓸 수 있다.
- HPA는 `scale` 서브리소스가 있는 대상(Deployment, StatefulSet, ReplicaSet 등)의 `replicas`를 바꾼다. DaemonSet은 scale 서브리소스가 없어 대상이 아니다(문서 지식).

---

#### 2. HPA 동작 방식 (컨트롤러 매니저)

HPA는 **`kube-controller-manager` 안의 컨트롤러 루프**다. 별도 프로세스가 아니다.

```
(매 sync 주기마다)
 HPA 오브젝트 읽기 ─▶ 대상의 scale 조회(현재 replicas) ─▶ 메트릭 조회(metrics.k8s.io)
   ─▶ 원하는 replicas 계산 ─▶ (안정화 윈도우, min/max, 정책 적용) ─▶ scale 서브리소스 갱신
```

##### 2-1. 컨트롤러 매니저 설정값 (실측: `kube-controller-manager --help`)

이 클러스터의 controller-manager 매니페스트에는 HPA 관련 플래그가 **명시돼 있지 않아** 기본값을 쓴다.

| 플래그 | 기본값 | 의미 |
|---|---|---|
| `--horizontal-pod-autoscaler-sync-period` | **15s** | HPA를 평가하는 주기 |
| `--horizontal-pod-autoscaler-tolerance` | **0.1** | 계산 결과가 현재와 10% 이내로 차이 나면 스케일하지 않음 |
| `--horizontal-pod-autoscaler-downscale-stabilization` | **5m0s** | 줄이기 전에 최근 5분의 권고값 중 가장 큰 값을 따름 |
| `--horizontal-pod-autoscaler-cpu-initialization-period` | **5m0s** | Pod 시작 후 이 기간은 CPU 샘플을 건너뛸 수 있음 |
| `--horizontal-pod-autoscaler-initial-readiness-delay` | **30s** | Pod 시작 직후 이 기간의 Ready 변화는 "초기 준비"로 취급 |
| `--concurrent-horizontal-pod-autoscaler-syncs` | (확인 안 함) | 동시에 처리하는 HPA 개수 |

(도움말의 `default` 값을 읽었고, 일부는 한 줄이 길어 앞부분만 확인했다.)

##### 2-2. HPA 오브젝트의 상태 (실측)

```
$ kubectl describe hpa burner
Conditions:
  AbleToScale     True   SucceededRescale   the HPA controller was able to update the target scale to 6
  ScalingActive   True   ValidMetricFound   ... calculate a replica count from cpu resource utilization (percentage of request)
  ScalingLimited  False  DesiredWithinRange the desired count is within the acceptable range
Events:
  SuccessfulRescale  New size: 2; reason: cpu resource utilization (percentage of request) above target
```

- `AbleToScale`(scale 서브리소스 접근 가능), `ScalingActive`(메트릭을 읽어 계산 가능), `ScalingLimited`(min/max에 의해 막힘) 세 조건으로 상태를 본다.
- `kubectl get hpa`의 `TARGETS`가 `<unknown>/50%`이면 메트릭을 못 읽는 것이다(6절).

---

#### 3. 스케일 계산 공식과 실측

```
desiredReplicas = ceil( currentReplicas × currentMetricValue / desiredMetricValue )
```

단, `currentMetricValue / desiredMetricValue`가 **1.0에서 tolerance(0.1) 이내이면 현재 replicas를 유지**한다.

##### 3-1. 스케일 업 (실측, target 50%, min 1, max 6)

| 시각 | HPA 상태 | 설명 |
|---|---|---|
| +0s | `/0` (메트릭 없음) | 생성 직후 |
| +15s | `1/0` | 아직 메트릭 없음 (`FailedGetResourceMetric: no metrics returned from resource metrics API` 경고) |
| +30s | `1/2`, cpu=100% | ceil(1 × 100/50) = **2** |
| +46s | `2/2` | 새 Pod Ready |
| +92s | `2/4`, cpu=100% | ceil(2 × 100/50) = **4** |
| +107s | `4/4` | |
| +122s | `4/6` | ceil(4 × 100/50) = 8 → **max 6으로 제한** (`ScalingLimited`) |

- 사용률 100%, 목표 50%이므로 비율 2.0이라 **한 번에 2배씩** 늘었다(1 → 2 → 4 → 8이어야 하나 6으로 제한).
- 처음 메트릭이 없는 구간(약 15~30초)은 17장의 수집 지연이다. 메트릭이 생긴 뒤 약 15초 안에 첫 스케일이 일어났다.
- 단계 사이는 약 60초였다. 새 Pod가 뜨고 Ready가 되고 메트릭이 쌓이는 시간이 필요했다.

##### 3-2. tolerance (실측)

3개 Pod, 사용률 100% 상태에서 `scaleUp/scaleDown stabilizationWindow=0`으로 두고:

| target | 비율 (100/target) | 결과 |
|---|---|---|
| 92% | 1.087 (< 1.1) | **3/3 유지** (50초 뒤에도 변화 없음) |
| 85% | 1.176 (> 1.1) | **4/4로 증가** (ceil(3 × 1.176) = 4) |

10% 이내의 작은 변동은 무시한다(진동 방지).

---

#### 4. 안정화와 behavior (스케일 속도 제어)

##### 4-1. 기본 스케일 다운 안정화 (5분, 실측)

6 replicas, 사용률 100% 상태에서 target을 50% → 200%로 올렸다(필요 replicas = ceil(6 × 100/200) = 3). 스케일 업 쪽 대기는 없다.

| 경과 | 상태 |
|---|---|
| 0 ~ 약 300초 | **6/6 유지** (계산상 3이 필요하지만 줄이지 않음) |
| 약 313초 | `6/4` (4로 감소 시작) |
| 약 337초 | `4/3` |
| 약 352초 | `3/3` |

- **기본값은 최근 5분의 권고값 중 최대값을 따라** 줄어든다. 목표를 바꾼 직후에는 5분 전의 권고값(6)이 윈도우에 남아 있어 줄어들지 않았다.
- 5분이 지나 윈도우에서 옛 권고값이 빠지자 줄어들기 시작했다. 한 번에 3으로 간 것이 아니라 `6 → 4 → 3` 순으로 감소했다(중간에 4가 된 이유는 윈도우가 슬라이딩하는 동안 권고값이 섞였기 때문으로 보이나, 개별 권고값은 관찰하지 못했다).
- **스케일 업은 즉시, 스케일 다운은 느리게**가 기본 설계다(트래픽이 잠깐 줄었다 늘 때 Pod가 계속 뜨고 지워지는 현상 방지).

##### 4-2. behavior로 속도 조절 (실측)

```yaml
behavior:
  scaleUp:   {stabilizationWindowSeconds: 0,  policies: [{type: Pods, value: 1, periodSeconds: 30}]}
  scaleDown: {stabilizationWindowSeconds: 30, policies: [{type: Pods, value: 1, periodSeconds: 20}]}
```

| 구간 | 결과 (max 8, target 50%, 사용률 100%) |
|---|---|
| 스케일 업 | 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8로 **한 번에 하나씩**, 약 30초 간격 (기본이라면 2배씩 늘어 훨씬 빨랐을 것) |
| target을 400%로 올린 뒤 스케일 다운 | **약 30초 지연** 후(윈도우) 8 → 7 → 6 → 5 → 4로 **하나씩**, 약 30~40초 간격 |

- `policies`의 `type`은 `Pods`(개수)나 `Percent`(비율)이고 `periodSeconds` 동안 최대 이만큼만 변하게 제한한다.
- 정책이 `periodSeconds: 20`이었어도 실제 단계는 약 30~40초였다. 평가 주기(15초)와 윈도우, Pod 변화가 겹친 결과로 보이며 정확한 간격 산식은 확인하지 못했다.
- `selectPolicy`(여러 정책 중 선택), 기본 정책 값(예: 스케일 업 100%/15s와 4 Pods/15s 중 큰 것을 선택하는 기본값)은 문서 지식이며 이번에 확인하지 않았다.

---

#### 5. 메트릭 종류와 여러 메트릭

| `type` | 의미 | 실측 |
|---|---|---|
| `Resource` + `Utilization` | **requests 대비 사용률(%)** | 위 3, 4절 |
| `Resource` + `AverageValue` | Pod당 평균 절대값(`50m`, `100Ki`) | `noreq`에 `cpu: 50m` → `cpu: 0/50m` 정상 표시 |
| `Pods`, `Object`, `External` | 커스텀/외부 메트릭 | 이번에 시험하지 못함(17장 설명) |
| `ContainerResource` | 컨테이너 단위 | 시험하지 않음 |

##### 5-1. 여러 메트릭은 가장 큰 값을 채택 (실측)

```yaml
metrics:
- {type: Resource, resource: {name: cpu,    target: {type: Utilization, averageUtilization: 200}}}
- {type: Resource, resource: {name: memory, target: {type: AverageValue, averageValue: 100Ki}}}
```

```
TARGETS: cpu: 100%/200%, memory: 316Ki/100Ki      REPLICAS 10 (max 10)
```

- CPU만 보면 필요량이 줄어드는 방향(100/200)이지만 메모리 비율이 약 3.16이어서 **둘 중 더 많은 replicas를 요구하는 쪽**(메모리)이 이겨 최대치 10까지 늘었다. 여러 메트릭 중 **계산된 replicas의 최대값**을 쓴다.
- 100Ki는 일부러 아주 작은 값으로 잡은 시험용 수치다.

---

#### 6. 실패 상황

| 상황 | 결과 (실측) |
|---|---|
| **requests가 없는 Pod에 `Utilization` 목표** | `TARGETS cpu: <unknown>/50%`, `ScalingActive False FailedGetResourceMetric: missing request for cpu in container c of Pod …`. 사용률은 requests가 기준이므로 **requests 필수** |
| 같은 대상에 `AverageValue` 목표 | requests 없이도 동작 (`cpu: 0/50m`) |
| **metrics-server 중지**(replicas 0) | 약 75초 뒤 `cpu: <unknown>/50%`, `ScalingActive False … unable to fetch metrics …`, `APIService AVAILABLE False`, `kubectl top`은 `Metrics API not available`. **replicas는 그대로 유지(6)** — 스케일 업도 다운도 못 한다 |
| 새로 만든 HPA/Pod | 첫 수십 초 동안 `FailedGetResourceMetric ... no metrics returned from resource metrics API`는 일시적 |
| max 도달 | `ScalingLimited True TooManyReplicas`로 표시 |

- `kubectl autoscale deploy noreq --cpu-percent=70 --min=1 --max=2`로 만든 HPA도 **`autoscaling/v2`** 로 저장됐고 메트릭은 `Resource`/`Utilization` 형태였다(명령은 내부적으로 v2 구조를 만든다).

---

#### 7. 선택 가이드와 함정

| 질문 | 답 (이 실험 기준) |
|---|---|
| HPA가 안 늘어난다 | `kubectl get hpa`의 `TARGETS`가 `<unknown>`인지, metrics-server(`APIService`), requests 설정 확인 |
| 갑자기 2배로 늘었다 | 비율이 2면 ceil(현재 × 2). `behavior.scaleUp.policies`로 속도 제한 |
| 줄어들지 않는다 | 기본 5분 안정화. 필요하면 `behavior.scaleDown.stabilizationWindowSeconds` 조정 |
| 작은 변동에도 흔들린다/안 움직인다 | tolerance 10% (플래그, 클러스터 설정) |
| 새 Pod의 CPU가 높게 나와 과다 확장 | 시작 직후 샘플은 initialization period 동안 제외될 수 있음(이번에 재현하지 않음) |
| 메모리 기반 HPA | Pod가 메모리를 해제하지 않는 앱이면 줄어들지 않을 수 있음(이번에 확인 안 함) |
| HPA와 `replicas` 고정 | HPA 대상 Deployment의 `spec.replicas`를 매니페스트에 고정해 두면 apply 때마다 덮어쓴다는 알려진 문제가 있다(GitOps에서 흔함, 이번에 확인하지 않음) |
| 노드가 부족하다 | HPA는 Pod만 늘린다. 노드가 필요하면 Cluster Autoscaler/Karpenter |

---

#### 8. 확인하지 못한 부분

- **VPA, Cluster Autoscaler, Karpenter, KEDA**는 설치하지 않았다. 노드 부족으로 Pod가 Pending이 되는 상황도 만들지 않았다.
- **커스텀/외부 메트릭 HPA**(`Pods`/`Object`/`External`), Prometheus Adapter, 컨테이너 단위(`ContainerResource`)는 시험하지 못했다.
- **실제 요청 트래픽 부하**(HTTP 부하 생성) 기반 스케일링은 하지 않았다. 부하는 항상 약 100%인 CPU 루프였다.
- `horizontal-pod-autoscaler-cpu-initialization-period`/`initial-readiness-delay`가 **과다 확장을 막는지**, 이 플래그 변경 효과는 실험하지 않았다. 컨트롤러 매니저 플래그는 기본값만 읽었다.
- **스케일 다운 5분 안정화**의 정확한 계산(윈도우 내 개별 권고값)과 `6 → 4 → 3`으로 간 이유는 관찰하지 못했다.
- `behavior`의 `selectPolicy`, `Percent` 정책, 기본 정책 값은 시험하지 않았다. `periodSeconds` 대비 실제 간격이 더 길었던 이유도 확인하지 못했다.
- **GitOps 환경에서 `replicas` 충돌**, `kubectl rollout`과 HPA의 상호작용, 스케일 서브리소스를 노출하지 않는 대상은 확인하지 않았다.
- HPA의 `status.currentMetrics`/`desiredReplicas` 외에 **컨트롤러 매니저 로그/메트릭**(`horizontal_pod_autoscaler_*`)은 보지 않았다.
- 메모리 기반 Utilization 목표의 동작, 여러 컨테이너 Pod(사이드카 포함)의 사용률 계산은 확인하지 않았다.
