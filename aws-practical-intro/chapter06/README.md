### 6장. CloudWatch

## 1부. CloudWatch 개념

#### CloudWatch란

AWS의 **통합 모니터링/관측성(Observability) 서비스**. "AWS 환경에서 벌어지는 모든 일을 보는 눈".

**핵심 기능 (한 플랫폼에 통합)**:
- **Metrics** — 수치 지표 (CPU, 네트워크, 디스크, 응답 시간 등)
- **Logs** — 로그 수집/저장/검색
- **Alarms** — 임계값 기반 알림
- **Events (EventBridge)** — 이벤트 기반 자동화
- **Dashboards** — 시각화
- **Synthetics / RUM** — 외부/실제 사용자 모니터링
- **X-Ray 연동** — 분산 추적

**특징**:
- **리전 범위** 서비스
- 거의 **모든 AWS 서비스와 자동 통합** (EC2, RDS, Lambda, S3, ...)
- Push + Pull 모델 혼합
- 보관 기간/해상도별 요금

#### 관측성(Observability)의 3대 요소

```
┌────────────────────────────────────┐
│          Observability             │
│  (시스템 내부 상태 추정 가능성)      │
└────────────────────────────────────┘
    │           │           │
    ▼           ▼           ▼
┌────────┐ ┌────────┐ ┌────────┐
│Metrics │ │ Logs   │ │ Traces │
│ (지표) │ │ (로그)  │ │ (추적) │
└────────┘ └────────┘ └────────┘
    │           │           │
    CloudWatch  CloudWatch   X-Ray
    Metrics     Logs
```

- **Metrics**: 수치. "지금 CPU 몇 %?"
- **Logs**: 텍스트 이벤트. "방금 어떤 에러가?"
- **Traces**: 요청 흐름 추적. "이 요청이 어떤 서비스를 거쳤나?"

**CloudWatch는 이 3가지를 전부 다룸** (Traces는 X-Ray가 전담, CloudWatch에 통합).

---

## 2부. CloudWatch 모니터링 종류 ⭐

### 1. Metrics (지표) — 가장 기본

**수치 데이터**를 시간순으로 저장. "ms 단위 timestamp + 값".

**구조**:
```
Namespace: AWS/EC2             ← 서비스별 분류
Metric name: CPUUtilization    ← 지표 이름
Dimensions:                    ← 필터링 축
  - InstanceId=i-xxxxx
  - InstanceType=t3.micro
Timestamp: 2026-10-09T10:00:00
Value: 42.5
Unit: Percent
```

#### Metric 종류

**① AWS 서비스 Built-in Metrics** (자동 수집):

| 서비스 | 대표 Metrics |
|-------|------------|
| **EC2** | CPUUtilization, NetworkIn/Out, DiskReadOps |
| **RDS** | CPUUtilization, DatabaseConnections, FreeStorageSpace, ReadLatency |
| **ELB** | RequestCount, HTTPCode_Target_5XX_Count, TargetResponseTime |
| **Lambda** | Invocations, Errors, Duration, ConcurrentExecutions |
| **S3** | NumberOfObjects, BucketSizeBytes |
| **DynamoDB** | ConsumedReadCapacityUnits, ThrottledRequests |

**② Custom Metrics** (사용자 정의):

```java
// AWS SDK로 애플리케이션 커스텀 지표 전송
PutMetricDataRequest req = PutMetricDataRequest.builder()
    .namespace("MyApp")
    .metricData(MetricDatum.builder()
        .metricName("OrdersProcessed")
        .value(1.0)
        .unit(StandardUnit.COUNT)
        .build())
    .build();
cloudWatch.putMetricData(req);
```

→ 비즈니스 지표도 CloudWatch로 (주문 수, 로그인 수 등).

#### Basic vs Detailed Monitoring (EC2 기준)

| | Basic | Detailed |
|---|------|---------|
| 수집 주기 | **5분** | **1분** |
| 비용 | 무료 | 인스턴스당 추가 과금 |
| 자동 활성 | ✅ | 수동 활성화 |

**Detailed Monitoring 쓸 때**: 세밀한 Auto Scaling, 짧은 트래픽 스파이크 대응 필요 시.

#### 해상도별 보관 기간

| 수집 주기 | 보관 기간 |
|---------|---------|
| **1초 미만** (고해상도) | 3시간 |
| **1분** | 15일 |
| **5분** | 63일 |
| **1시간** | **15개월** |

→ CloudWatch가 **자동으로 집계(aggregation)** 하면서 오래된 데이터는 저해상도로.

### 2. Logs — 로그 수집/검색

**CloudWatch Logs** — 애플리케이션/시스템 로그 중앙 수집.

**계층 구조**:
```
Log Group (my-app-prod)
├─ Log Stream (instance-i-aaa)
│   ├─ 2026-10-09 10:00:00 ... log entry
│   ├─ 2026-10-09 10:00:01 ... log entry
├─ Log Stream (instance-i-bbb)
│   └─ ...
└─ Log Stream (lambda-execution-xxx)
```

- **Log Group**: 애플리케이션/서비스 단위
- **Log Stream**: 호스트/실행 단위 (EC2 인스턴스, Lambda 호출 등)

#### 로그 전송 방법

| 소스 | 방법 |
|-----|------|
| **EC2** | **CloudWatch Agent** 설치 (`/var/log/*` 전송) |
| **Lambda** | 자동 전송 (별도 설정 불필요) |
| **ECS/EKS** | awslogs log driver |
| **애플리케이션** | AWS SDK로 직접 `PutLogEvents` |
| **S3** | S3 Access Logs → CloudWatch Logs Subscription |

#### CloudWatch Logs Insights (쿼리) ⭐

SQL-like 쿼리로 로그 검색/집계:

```
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 100
```

더 복잡한 예:
```
fields @timestamp, request_id, status, duration
| filter status >= 500
| stats count() by bin(5m)
```

→ 매 5분당 500 에러 수 집계.

**장점**: 몇 TB 로그도 몇 초 안에 검색. 서버에 SSH 접근 X.

### 3. Alarms — 알람

**Metric 임계값 기반 자동 알림/액션**.

**Alarm 상태 3가지**:
- `OK` — 정상 (조건 미충족)
- `ALARM` — 임계 초과
- `INSUFFICIENT_DATA` — 데이터 부족

**설정 예시**:
```
Metric: EC2 CPUUtilization (InstanceId=i-xxx)
Statistic: Average
Period: 5 minutes
Threshold: > 80%
Evaluation Periods: 2 (consecutive)
Action: SNS Topic → Email/Slack
```

→ **"5분 평균 CPU가 2번 연속 80% 넘으면 알림"**.

**Action 종류**:
- **SNS 알림** (이메일/SMS/Slack 연동)
- **Auto Scaling** (scale out/in 트리거)
- **EC2 Action** (reboot, stop, terminate)
- **Systems Manager** (OpsItem 생성)
- **Lambda 호출** (EventBridge 경유)

#### Composite Alarm (복합 알람)

여러 알람의 **AND/OR 조합**:
```
ALARM("high-cpu") AND ALARM("high-memory")
→ 복합 알람 발동
```

→ 노이즈 감소 (단일 지표 흔들림 무시).

### 4. Events / EventBridge — 이벤트 기반 자동화

**EventBridge** (구 CloudWatch Events) — 이벤트 → 액션 라우팅.

**패턴**:
```
Event Source → Rule → Target
```

**예시**:
- EC2 상태 변경 → Lambda로 알림
- S3 객체 업로드 → SQS로 큐잉
- 매일 오전 2시 → Lambda 트리거 (cron처럼)
- CodePipeline 배포 성공 → Slack 알림

**Scheduled Events** (cron):
```
cron(0 2 * * ? *)   # 매일 오전 2시 UTC
rate(5 minutes)      # 5분마다
```

### 5. Dashboards — 시각화

**여러 Metrics를 한 화면에**. Grafana 대신 CloudWatch 자체 UI 사용.

**Widget 종류**:
- Line / Stacked Area / Number chart
- Log widget (Insights 쿼리 결과)
- Alarm status widget
- Text widget

**실무 Tip**: 서비스별 Dashboard 하나씩 (예: `my-app-prod`, `my-app-stage`).

### 6. Synthetics (Canaries) — 외부 모니터링

**Puppeteer/Selenium 기반 외부 모니터링**. "사용자처럼 접속해서 체크".

**사용 사례**:
- Health check (`GET /health` 매분 호출)
- Login 플로우 자동 테스트
- API Workflow 검증 (여러 API 순차 호출)
- UI 요소 체크 (버튼이 있는지, 폼이 정상인지)

**장점**:
- 외부 관점 (사용자 경험 측정)
- 서비스 자체 다운이어도 알림 가능 (내부 모니터링은 서비스가 죽으면 멈춤)

### 7. RUM (Real User Monitoring)

**브라우저에서 실제 사용자의 성능 데이터 수집**.

**수집 데이터**:
- 페이지 로드 시간
- Core Web Vitals (LCP, FID, CLS)
- JavaScript 에러
- API 호출 지연
- 사용자 세션

**Synthetics vs RUM**:
- Synthetics: **합성 트래픽** (내가 모니터링용으로 발생)
- RUM: **실제 사용자 트래픽** (수백만 사용자의 경험)

### 8. Container Insights

**ECS / EKS 전용 모니터링**. 컨테이너 레벨 지표/로그 자동 수집.

- Pod/Container CPU/메모리
- Node 상태
- Kubernetes 이벤트

### 9. Lambda Insights

**Lambda 전용 심화 모니터링**.

- Cold start 추적
- 메모리 사용량 (allocated vs used)
- Shutdown 이벤트
- Function의 CPU 사용

### 10. X-Ray 연동 (분산 추적)

**ServiceLens** — CloudWatch + X-Ray 통합 뷰.

- 서비스 간 호출 그래프
- 각 서비스별 지연/에러
- 요청 1건의 전체 경로 추적

---

## 3부. 모니터링 유형별 정리

**4가지 모니터링 축**:

| 유형 | 설명 | 도구 |
|-----|------|------|
| **Infrastructure Monitoring** | 서버/DB/네트워크 등 리소스 상태 | CloudWatch **Metrics** |
| **Application Monitoring** | 애플리케이션 내부 지표/로그 | CloudWatch **Logs** + **Custom Metrics** |
| **Synthetic Monitoring** | 외부 관점 health check | **Synthetics (Canaries)** |
| **Real User Monitoring** | 실제 사용자 경험 | **RUM** |

**4가지를 조합해야 완전한 관측성**.

### 전형적 조합 패턴

**웹 서비스**:
```
Infrastructure: EC2/RDS/ELB Metrics → Dashboard
Application: 애플리케이션 로그 → Logs Insights
             Custom Metric (주문 수, 결제 성공률)
Synthetic: /health 매분 호출 (Canary)
RUM: 사용자 페이지 로드 시간 (RUM)
Alarms: CPU > 80%, 5xx > 1%, 응답 지연 p99 > 500ms
Events: 배포 완료 → Slack 알림
```

---

## 4부. CloudWatch vs 대체제 (현재 관점)

| | CloudWatch | 대체제 |
|---|-----------|-------|
| **Metrics** | Metrics | **Prometheus + Grafana** |
| **Logs** | CloudWatch Logs | **Elasticsearch / OpenSearch, Loki** |
| **APM/Tracing** | X-Ray + ServiceLens | **Datadog, New Relic, Jaeger, OpenTelemetry** |
| **Dashboards** | Dashboards | **Grafana** |

**장단점**:
- **CloudWatch 장점**: AWS 서비스와 즉시 통합, 설정 간단, Managed
- **CloudWatch 단점**: AWS 종속, 비용 (특히 Logs/Custom Metrics), UI 아쉬움
- **대체제 장점**: Vendor-neutral, 더 유연, 멀티 클라우드
- **대체제 단점**: 운영 복잡도, 설정 시간

**실무**:
- AWS만 쓰는 중소 규모: **CloudWatch 중심**
- 복잡한 환경 / 멀티 클라우드: **CloudWatch + Prometheus + Datadog** 혼용

---

## 5부. CloudWatch 비용 주의 ⚠️

**조심해야 할 비용 포인트**:

1. **Custom Metrics** — Metric당 과금. 수만 개 뿌리면 비싸짐.
2. **Logs Ingestion** — GB당 과금. DEBUG 로그 다 보내지 말 것.
3. **Logs Insights 쿼리** — 스캔한 GB당 과금.
4. **Logs 보관** — 장기 보관 시 S3로 export + Lifecycle.
5. **1분 Metrics** — Detailed Monitoring 활성화 시 서버당 추가 비용.
6. **Dashboards** — 3개까지 무료, 이후 과금.

**비용 최적화 체크리스트**:
- [ ] Log 레벨 운영 환경 INFO 이상 (DEBUG 금지)
- [ ] Log Group 보관 기간 설정 (기본 영구 — 꼭 설정)
- [ ] 오래된 Log는 S3로 export + 삭제
- [ ] 꼭 필요한 Custom Metrics만
- [ ] Alarm 통합 (Composite Alarm으로 중복 제거)

---

## 참고 자료

- [Amazon CloudWatch User Guide](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/)
- [CloudWatch Metrics Reference by Service](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CW_Support_For_AWS.html)
- [CloudWatch Logs Insights Query Syntax](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/CWL_QuerySyntax.html)
- [EventBridge](https://docs.aws.amazon.com/eventbridge/)
- [CloudWatch Pricing](https://aws.amazon.com/cloudwatch/pricing/)
