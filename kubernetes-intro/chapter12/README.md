### 12장. 파드 스케줄링

> 정리 범위: 노드 셀렉터, node affinity, pod affinity/anti-affinity, taint/toleration, cordon/drain
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop. 노드가 여러 개 필요해서 Docker Desktop 클러스터(노드 1개)는 쓰지 않고 **임시 kind 클러스터**(`t12`, v1.35.0, control-plane 1 + worker 3, 별도 kubeconfig)에서 실험하고 삭제했다. 실험용 노드 레이블: `t12-worker`(zone=a, disk=ssd, cpu=8), `t12-worker2`(zone=a, disk=hdd, cpu=16), `t12-worker3`(zone=b, disk=ssd, cpu=4). control-plane 노드는 기본 taint(`node-role.kubernetes.io/control-plane:NoSchedule`)가 있어 일반 Pod는 worker 3대에만 배치됐다. 네임스페이스는 `t12`.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: 레이블/`nodeSelector` 기초는 [9장](../chapter09/README.md), Pod 종료는 [5장](../chapter05/README.md), 컨트롤러 재생성은 [6장](../chapter06/README.md)이다.

#### 목차

1. 스케줄링 한눈에 보기
2. 노드 셀렉터 (`nodeSelector`)
3. Node Affinity
4. Pod Affinity / Anti-Affinity
5. Taint와 Toleration
6. Cordon과 Drain
7. 선택 가이드와 함정
8. 확인하지 못한 부분

---

#### 1. 스케줄링 한눈에 보기

스케줄러(`kube-scheduler`)는 Pod마다 **필터링**(조건에 안 맞는 노드 제외) → **점수 매기기**(남은 노드 순위) → 한 노드에 바인딩 순으로 동작한다. 아래 기능은 모두 이 과정에 조건을 추가하는 방법이다.

| 기능 | 누가 정하나 | 방향 | 하드/소프트 |
|---|---|---|---|
| `nodeSelector` | Pod | Pod → 노드 레이블 | 하드 |
| node affinity | Pod | Pod → 노드 레이블 | 하드(`required`) / 소프트(`preferred`) |
| pod affinity / anti-affinity | Pod | Pod → **다른 Pod가 있는 위치** | 하드 / 소프트 |
| taint / toleration | **노드**가 밀어내고 Pod가 허용 | 노드 → Pod | `NoSchedule`(하드) / `PreferNoSchedule`(소프트) / `NoExecute`(+퇴출) |
| cordon / drain | 관리자 | 노드 전체 | 스케줄 중지 / 중지 + Pod 퇴출 |

핵심 구분: **affinity/nodeSelector는 "Pod가 원하는 곳"으로 끌어당기는 힘**, **taint는 "노드가 받기 싫은 Pod"를 밀어내는 힘**이다.

---

#### 2. 노드 셀렉터 (`nodeSelector`)

노드의 레이블과 정확히 일치하는 노드에만 배치한다. 가장 단순한 방식이다.

| 설정 | 결과 (실측) |
|---|---|
| `disk: ssd` | `t12-worker` (ssd 노드 중 하나) |
| `disk: ssd, zone: b` (여러 키는 **AND**) | `t12-worker3` |
| `disk: nvme` (없는 값) | **Pending** |
| `kubernetes.io/hostname: t12-worker2` | `t12-worker2` (노드를 직접 지정하는 효과) |

Pending 이벤트: `0/4 nodes are available: 1 node(s) had untolerated taint(s), 3 node(s) didn't match Pod's node affinity/selector.` — control-plane 노드는 taint 때문에, worker 3대는 셀렉터 불일치로 탈락했다는 뜻이다.

- 연산자가 없고 **같다(equality)만** 된다. `In`, `NotIn`, `Gt` 같은 표현이 필요하면 node affinity를 쓴다.
- `nodeName`을 직접 지정하면 스케줄러를 거치지 않는다. 이 실험의 일부 Pod(`nodeName: t12-worker`)가 그렇게 배치됐다(아래 5, 6절).

---

#### 3. Node Affinity

`nodeSelector`를 확장한 방식으로 연산자와 소프트 조건을 쓸 수 있다.

```yaml
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:      # 하드
      nodeSelectorTerms:
      - matchExpressions:
        - {key: disk, operator: In, values: [hdd]}
    preferredDuringSchedulingIgnoredDuringExecution:     # 소프트
    - weight: 100
      preference: {matchExpressions: [{key: zone, operator: In, values: [b]}]}
```

##### 3-1. 연산자와 조합 (실측)

| 조건 | 결과 |
|---|---|
| `disk In [hdd]` | `t12-worker2` |
| `zone NotIn [a]` | `t12-worker3` |
| `gpu Exists` (그런 레이블이 없음) | **Pending** |
| `cpu Gt ['10']` (노드 cpu=8, 16, 4) | `t12-worker2` (16만 해당, 값은 숫자 문자열) |
| 한 `matchExpressions` 안의 조건 여러 개 (`disk In [hdd]` + `zone In [b]`) | **AND** → 해당 노드가 없어 Pending |
| `nodeSelectorTerms`를 두 개로 나눔 (`disk In [hdd]` / `zone In [b]`) | **OR** → `t12-worker2` (hdd 쪽 조건 해당) |
| `nodeSelector: zone=a` + `required` affinity `disk In [ssd]` | **둘 다 만족**하는 `t12-worker` |

- 연산자: `In`, `NotIn`, `Exists`, `DoesNotExist`, `Gt`, `Lt`.
- **terms 사이는 OR, 한 term 안의 expressions는 AND**다.

##### 3-2. `preferred`는 점수만 올린다

`zone In [b]`를 weight 100으로 선호하는 Pod 6개 → **6개 모두 `t12-worker3`**. 선호 조건이 맞는 노드가 있는 한 몰렸다(노드 자원이 충분했기 때문이고, 가득 차면 다른 노드로 간다). 다른 점수 항목(분산 등)보다 선호 가중치가 이 실험에서는 우세했다.

##### 3-3. `IgnoredDuringExecution`의 의미

이름의 `IgnoredDuringExecution`은 **이미 배치된 Pod는 이후 노드 레이블이 바뀌어도 쫓아내지 않는다**는 뜻이다.

```
na-req (disk In [hdd]) 가 t12-worker2 에서 실행 중
$ kubectl label node t12-worker2 disk=nvme --overwrite   (더 이상 조건 불일치)
→ 5초 뒤에도 na-req Running on t12-worker2
```

스케줄링 시점에만 평가한다.

---

#### 4. Pod Affinity / Anti-Affinity

**다른 Pod의 위치를 기준으로** 배치를 정한다. 핵심 필드는 `labelSelector`(대상 Pod)와 `topologyKey`(같은 위치의 기준이 되는 노드 레이블)다.

- `topologyKey: kubernetes.io/hostname` → "같은/다른 **노드**"
- `topologyKey: zone` → "같은/다른 **zone**" (노드 레이블 값이 같으면 같은 도메인)

##### 4-1. Anti-Affinity (실측)

| 설정 | 결과 |
|---|---|
| Deployment 4개, 자기 자신(`app=web`) required anti-affinity, `hostname` | **3개 Running(노드마다 1개), 1개 Pending**. `3 node(s) didn't match pod anti-affinity rules` |
| 3개, anti-affinity `topologyKey: zone` (zone a: worker, worker2 / zone b: worker3) | **2개 Running(zone a 1개 + zone b 1개), 1개 Pending** — zone이 2개뿐이라 |
| 5개, **preferred** anti-affinity (weight 100, hostname) | 5개 모두 Running: `worker` 2 / `worker2` 2 / `worker3` 1 (**가능하면 분산하되 안 되면 겹침**) |

- required는 조건을 못 맞추면 **Pending**으로 남는다. 가용 도메인 수보다 replicas가 많으면 Pending이 생긴다.
- 고가용성을 노드/zone 단위로 분산할 때 쓴다. 단순 분산이면 `topologySpreadConstraints`가 더 적합한 경우가 많지만 이번 실험에는 포함하지 않았다.

##### 4-2. Affinity (실측)

| 설정 | 결과 |
|---|---|
| `cache` Pod가 `app=web`과 같은 노드(required, hostname) | `web` Pod가 있는 `t12-worker2`에 배치 |
| 일치하는 Pod가 **없는** 대상(`app: nosuch`) | **Pending** — `3 node(s) didn't match pod affinity rules` |

- 필요한 대상 Pod가 먼저 떠 있어야 한다. 대상이 없으면 영원히 Pending이다.

##### 4-3. 네임스페이스 범위

`other-web`(`t12b` 네임스페이스, `t12-worker2`에 고정)을 대상으로 affinity를 걸었다.

| Pod | 결과 |
|---|---|
| `t12b`의 Pod (같은 네임스페이스, 기본) | `t12-worker2` Running |
| `t12`의 Pod, `namespaces` 지정 없음 | **Pending** |
| `t12`의 Pod, `namespaces: [t12b]` | `t12-worker2` Running |

- 기본값은 **Pod 자신의 네임스페이스**만 대상으로 본다. 다른 네임스페이스를 보려면 `namespaces`(또는 `namespaceSelector`)를 지정한다.

##### 4-4. 비용

Pod affinity/anti-affinity는 다른 Pod를 모두 평가하므로 대규모 클러스터에서 스케줄링 비용이 커진다고 알려져 있다(이 실험에서 측정하지 않음).

---

#### 5. Taint와 Toleration

**노드에 taint(얼룩)를 붙이면 그 taint를 견디는(toleration) Pod만** 배치된다.

```
$ kubectl taint node t12-worker dedicated=gpu:NoSchedule     # 추가
$ kubectl taint node t12-worker dedicated-                    # 삭제
```

형식: `key=value:effect`. effect는 세 가지다.

| effect | 의미 |
|---|---|
| `NoSchedule` | toleration 없는 **새 Pod를 배치하지 않음** (이미 있는 Pod는 유지) |
| `PreferNoSchedule` | 가능하면 배치하지 않음 (소프트) |
| `NoExecute` | 새 Pod 배치 금지 + **이미 실행 중인 Pod도 퇴출** |

##### 5-1. NoSchedule (실측)

`t12-worker`, `t12-worker2`에 `dedicated=gpu:NoSchedule`을 붙이고 Deployment 2개(각 6 replicas)를 만들었다.

| Deployment | 결과 |
|---|---|
| toleration 없음 | **6개 모두 `t12-worker3`** (taint 없는 노드로만) |
| `tolerations: [{key: dedicated, operator: Equal, value: gpu, effect: NoSchedule}]` | `worker` 2 / `worker2` 2 / `worker3` 2 (**taint 노드와 일반 노드 모두 가능**) |
| `operator: Exists`(값 무관) | `t12-worker`에 배치 |
| 값이 다른 toleration (`value: cpu`) + 노드 지정 | **Pending** (`3 node(s) had untolerated taint(s)`) |
| 노드를 정한 뒤 다른 노드에 taint(`late=x:NoSchedule`)를 **나중에** 추가 | 이미 실행 중인 Pod는 그대로 (Running) |

- **toleration은 "허용"일 뿐 "끌어당기지 않는다."** toleration이 있어도 taint가 없는 노드에 갈 수 있다. 특정 노드 전용으로 쓰려면 **taint + toleration + nodeSelector/affinity**를 함께 쓴다.
- `operator: Equal`(기본)은 키와 값이, `Exists`는 키만 일치하면 된다. `effect`를 생략하면 그 키의 모든 effect를 허용한다(문서 규칙, 이번 실험에서는 effect를 명시했다).

##### 5-2. 여러 taint는 모두 견뎌야 한다

`t12-worker`에 `a=1:NoSchedule`, `b=2:NoSchedule` 둘을 붙이고 `a`만 tolerate하는 Pod → **Pending**. 노드의 **모든 taint**를 견뎌야 한다(AND).

##### 5-3. PreferNoSchedule (실측)

`worker`, `worker2`에 `soft=y:PreferNoSchedule` → toleration 없는 Pod 6개가 **모두 `t12-worker3`**. 다른 노드가 남아 있어 피한 것이다. 선택지가 taint 노드뿐이면 거기에도 배치되는 소프트 조건이다(이 부분은 직접 실험하지 않았다).

##### 5-4. NoExecute와 `tolerationSeconds` (실측)

`t12-worker`에 있는 Pod 3개(`nodeName`으로 고정)에 `maint=now:NoExecute`를 붙였다.

| Pod | toleration | 결과 |
|---|---|---|
| `notol` | 없음 | **즉시 삭제** (+5초 첫 확인 때 이미 없음) |
| `sec20` | `NoExecute`, `tolerationSeconds: 20` | 약 20초 뒤 종료(`Succeeded` 상태가 보인 뒤 삭제) |
| `forever` | `NoExecute`, 시간 지정 없음 | **계속 Running** (40초 이상) |

- 이벤트 `TaintManagerEviction`: `Marking for deletion Pod …`, 그리고 taint를 제거하면 `Cancelling deletion of Pod …`(삭제 대기 중이던 Pod의 퇴출 취소).
- **컨트롤러가 없는 단독 Pod는 퇴출되면 사라진다**(되살아나지 않음). Deployment의 Pod는 새 노드에 다시 만들어진다(Deployment 3종을 `NoExecute` 노드에 둔 실험은 이름 기반 집계가 모호해서 결론을 내지 못했고, 위 단독 Pod 실험으로 대체했다).
- 노드 장애 시 자동으로 붙는 `node.kubernetes.io/not-ready`, `unreachable`(NoExecute) taint와 기본 `tolerationSeconds: 300`은 [4장](../chapter04/README.md)에서 확인했다.

##### 5-5. control-plane taint

kind의 control-plane 노드에는 `node-role.kubernetes.io/control-plane:NoSchedule`이 기본으로 있어 일반 Pod가 오지 않았다(스케줄링 실패 메시지의 `1 node(s) had untolerated taint(s)`가 그것이다).

---

#### 6. Cordon과 Drain

##### 6-1. cordon: 새 배치만 막는다

```
$ kubectl cordon t12-worker2
$ kubectl get nodes
t12-worker2   Ready,SchedulingDisabled
$ kubectl get node t12-worker2 -o jsonpath='{.spec.unschedulable} {.spec.taints}'
true [{"effect":"NoSchedule","key":"node.kubernetes.io/unschedulable","timeAdded":"..."}]
```

- `spec.unschedulable: true`와 함께 `node.kubernetes.io/unschedulable:NoSchedule` **taint가 자동으로 붙는다**(즉 cordon은 taint의 한 사례).
- Deployment를 6 → 9로 늘리자 **새 Pod 3개는 worker2에 가지 않고**(worker 3 / worker2 2 / worker3 4), **기존 Pod는 그대로** 남았다(`bare`, `dep` 2개, DaemonSet, `emp`).

##### 6-2. drain: cordon + 퇴출

`kubectl drain`은 노드를 cordon하고 Pod를 **eviction API로 퇴출**한다. 플래그 없이 실행하면 막힌다.

```
$ kubectl drain t12-worker2 --timeout=30s
error: unable to drain node "t12-worker2" due to error:
 cannot delete DaemonSet-managed Pods (use --ignore-daemonsets to ignore): kube-system/kindnet-..., kube-system/kube-proxy-..., t12/ds-...
 cannot delete Pods that declare no controller (use --force to override): t12/bare
 cannot delete Pods with local storage (use --delete-emptydir-data to override): t12/emp
```

| 대상 | 플래그 | 이유 |
|---|---|---|
| DaemonSet Pod | `--ignore-daemonsets` | 지워도 DaemonSet이 즉시 다시 만들고 cordon된 노드에도 배치되므로 건드리지 않고 무시 |
| 컨트롤러 없는 Pod | `--force` | 지우면 **복구되지 않으므로** 명시적 동의 필요 |
| `emptyDir` 사용 Pod | `--delete-emptydir-data` | 퇴출되면 emptyDir 데이터가 사라지므로 동의 필요 |

```
$ kubectl drain t12-worker2 --ignore-daemonsets --delete-emptydir-data --force
evicting pod t12/bare, dep-…, dep-…, emp   → evicted ... node/t12-worker2 drained
```

- `--dry-run=client`로 어떤 Pod가 퇴출될지 먼저 볼 수 있다.
- 결과: worker2에는 DaemonSet Pod(kindnet, kube-proxy, `ds-…`)만 남았다. `dep` Pod 2개는 다른 노드(worker 4 / worker3 5)에 다시 만들어졌고, **단독 Pod `bare`와 `emp`는 사라지고 돌아오지 않았다.**
- `kubectl uncordon`으로 스케줄링을 재개해도 **이미 옮겨 간 Pod는 되돌아오지 않는다**(재분배 없음. worker 4 / worker3 5 그대로).

##### 6-3. PodDisruptionBudget(PDB)이 drain을 막는다

`dep` 9개에 `minAvailable: 9` PDB(`ALLOWED DISRUPTIONS 0`)를 걸고 `t12-worker3`를 drain했다.

```
$ kubectl drain t12-worker3 --ignore-daemonsets ... --timeout=25s
error when evicting pods/"dep-…" -n "t12": global timeout reached: 25s   (퇴출 대상 5개 모두)
$ kubectl get node t12-worker3
t12-worker3   Ready,SchedulingDisabled     ← 시간 초과로 실패해도 cordon은 유지됨
```

- 퇴출(eviction)은 PDB를 따르므로 허용 중단 수가 0이면 **계속 재시도하며 대기**하다 시간 초과로 실패했다.
- `minAvailable`을 6으로 낮추자 drain이 끝났다(`node/t12-worker3 drained`).
- drain이 실패해도 **cordon 상태는 남는다**. 작업을 포기하면 직접 `uncordon` 해야 한다.
- 반대로 `kubectl delete pod`는 PDB와 무관하게 삭제됐다(PDB `minAvailable: 9` 상태에서 Pod 하나 삭제 성공). PDB는 **eviction API 경로**에만 적용된다.

##### 6-4. 정리

| | cordon | drain |
|---|---|---|
| 새 Pod 배치 | 막음 | 막음 (cordon 포함) |
| 기존 Pod | 유지 | **퇴출** |
| 되돌리기 | `uncordon` | `uncordon` (Pod는 안 돌아옴) |
| 용도 | 점검 예정, 일단 배치 중지 | 노드 업그레이드/교체 전 비우기 |

---

#### 7. 선택 가이드와 함정

| 하고 싶은 것 | 방법 |
|---|---|
| 특정 종류 노드에 배치 (단순) | `nodeSelector` |
| 연산자나 우선순위 조건 | node affinity (`In`, `Gt`, `preferred`) |
| 같은 노드/zone에 같이 배치 | pod affinity (+ 대상이 먼저 떠 있어야) |
| 노드/zone 분산 | pod anti-affinity (required는 도메인 수보다 replicas가 많으면 Pending) |
| 노드를 전용으로 확보 | **taint + toleration + nodeSelector/affinity** (toleration만으로는 안 끌려옴) |
| 장애/점검 시 퇴출 시간 제어 | `NoExecute` + `tolerationSeconds` |
| 점검 전 노드 비우기 | `cordon` → `drain --ignore-daemonsets --delete-emptydir-data` |
| 중요한 서비스 가용성 보호 | PDB (drain의 eviction이 따름, `kubectl delete`는 안 막음) |

함정:

- required 조건이 맞지 않으면 **Pending에 머문다.** 원인은 `kubectl describe pod`의 `FailedScheduling` 메시지(어떤 조건으로 몇 노드가 탈락했는지 나온다).
- `IgnoredDuringExecution`: 배치 후 조건이 바뀌어도 안 쫓아낸다. 쫓아내려면 `NoExecute` taint나 drain.
- `drain`은 컨트롤러 없는 Pod를 **영구 삭제**한다(`--force`).
- uncordon 후 재분배는 자동으로 되지 않는다.
- `nodeName`/`kubectl run --overrides`처럼 직접 노드를 지정하면 스케줄러의 taint, affinity 검사를 받지 않는다. 이 실험에서는 `nodeName: t12-worker` Pod가 `NoExecute` taint 평가 대상(퇴출)에는 해당했지만 `NoSchedule` 검사 여부는 직접 확인하지 않았다.

---

#### 8. 확인하지 못한 부분

- **`PreferNoSchedule`이 유일한 선택지일 때** 배치되는지(소프트 조건의 폴백)는 확인하지 않았다.
- **Pod affinity의 `IgnoredDuringExecution`**(배치 후 대상 Pod 레이블이 바뀌었을 때)은 실험 중 대상 Pod를 지워 버려서 확인하지 못했다. node affinity에서만 확인했다.
- **`preferred` 조건 여러 개의 점수 합산 방식**, 다른 스코어링 플러그인(자원 균형, `topologySpreadConstraints`)과의 상호작용은 측정하지 않았다.
- **`NoExecute` 시 Deployment Pod의 재배치**: 단독 Pod로 확인했고 Deployment 대상 실험은 집계 방식 문제로 결론을 내지 못했다.
- **`taint`에서 `effect` 생략 toleration**, `tolerationSeconds`의 정확한 퇴출 시각 오차는 확인하지 않았다.
- **스케줄링 우선순위(PriorityClass)와 선점**, 자원 요청 부족으로 인한 Pending, `topologySpreadConstraints`는 다루지 않았다.
- **`kubectl drain`의 `--disable-eviction`, `--grace-period`, `--pod-selector`, `--skip-wait-for-delete-timeout`** 같은 옵션은 실험하지 않았다.
- 클라우드 환경(EKS 노드 그룹 교체, Karpenter, Cluster Autoscaler)의 drain 연동은 확인하지 못했다.
- 대규모 클러스터에서 pod affinity의 스케줄링 비용은 측정하지 않았다.
