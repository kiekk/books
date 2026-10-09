### 7장. Lambda

## 1부. 서버리스(Serverless) 개념

#### "서버가 없는 게 아니라, 서버를 신경 안 쓰는 것"

**서버리스 = "No ops"** — 서버는 있지만, **사용자가 운영/관리할 필요가 없음**.

AWS가 책임지는 것:
- 인프라 프로비저닝 (서버 구매/준비)
- OS 설치 및 패치
- Runtime 설치/업그레이드
- 스케일링 (요청 많으면 자동 증설)
- 가용성 (장애 시 자동 복구)

사용자가 책임지는 것:
- **코드**만

#### 서버리스의 핵심 특징

1. **사용량 기반 과금** — idle 시 **0원**
2. **자동 확장** — 트래픽 N배면 자동으로 N배 처리
3. **고가용성 기본** — AWS가 Multi-AZ로 자동 운영
4. **빠른 배포** — 코드만 업로드하면 즉시 실행

#### 전통적 서버(EC2) vs 서버리스(Lambda)

| | **전통적 서버 (EC2)** | **서버리스 (Lambda)** |
|---|---------------------|-------------------|
| 운영 책임 | 사용자 (OS 패치, 스케일링, HA) | **AWS** |
| 과금 | **시간 단위** (idle도 지불) | **호출 당** (idle 0) |
| 확장 | 수동 or Auto Scaling | **자동, 즉시** |
| 최대 실행 시간 | 무제한 | **15분** |
| 응답 시간 | 즉시 | **Cold start** 가능 (100ms~수초) |
| 상태 보유 | 가능 (디스크, 메모리) | **Stateless** |
| 사용처 | 상시 운영 서비스, 복잡한 상태 관리 | 이벤트 처리, API, 배치 |

#### AWS 서버리스 서비스 패밀리

| 유형 | 서비스 |
|-----|------|
| **컴퓨팅** | **Lambda**, Fargate |
| **스토리지** | S3 |
| **DB** | DynamoDB, Aurora Serverless |
| **메시징** | SQS, SNS, EventBridge |
| **API** | API Gateway, AppSync (GraphQL) |
| **인증** | Cognito |
| **분석** | Athena, Glue |

→ **"서버리스 아키텍처"** = 이 서비스들을 조합해 **서버 하나 안 띄우고** 완성.

---

## 2부. Lambda 개념과 특징

### Lambda란

**AWS의 서버리스 컴퓨팅 서비스**. **코드만 업로드**하면 이벤트 발생 시 자동 실행.

### 핵심 구성 요소

```
┌──────────── Lambda Function ────────────┐
│                                          │
│  ① Code         (ZIP or Container Image) │
│  ② Handler      (진입점: file.function)  │
│  ③ Runtime      (Node.js, Python, Java...│
│  ④ Memory       (128 MB ~ 10 GB)         │
│  ⑤ Timeout      (최대 15분)              │
│  ⑥ Execution Role (IAM Role)             │
│  ⑦ Env Vars     (환경변수)               │
│  ⑧ Layers       (공통 코드/의존성)        │
│                                          │
└──────────────────────────────────────────┘
         ↑
         │ 호출
    ┌────┴─────┐
    │ Trigger  │  (S3 / API Gateway / SNS / EventBridge / ...)
    └──────────┘
```

#### ① Code
- **ZIP 파일** 업로드 (최대 50 MB 압축, 250 MB 압축해제)
- **Container Image** 지원 (최대 10 GB, ECR에 저장)

#### ② Handler
```python
# Python 예시: file "lambda_function.py", function "handler"
# 설정: Handler = "lambda_function.handler"
def handler(event, context):
    name = event['name']
    return {'statusCode': 200, 'body': f'Hello {name}'}
```

- `event`: 트리거가 전달한 데이터 (JSON)
- `context`: Lambda 메타정보 (request ID, 남은 시간 등)

#### ③ Runtime 종류

| Runtime | 특징 |
|--------|------|
| **Node.js** | 가장 가볍고 cold start 빠름 |
| **Python** | 데이터/스크립트 처리에 많이 사용 |
| **Java** | JVM init 때문에 cold start 느림 → SnapStart로 완화 |
| **Go** | 바이너리 컴파일, 빠른 cold start |
| **Ruby** | 가능하지만 사용 적음 |
| **.NET** | C#. 느린 cold start |
| **Custom Runtime** | Rust, PHP 등 임의 언어 (Runtime API) |

#### ④ Memory & CPU

- **128 MB ~ 10,240 MB** (10 GB)
- **CPU는 메모리에 비례** 자동 할당 — 메모리 2배면 CPU도 2배
- "Memory"를 조정하면 사실상 "성능 다이얼" 역할
- **1,769 MB에서 vCPU 1개** 상당 (기준점)

#### ⑤ Timeout
- 기본 3초, **최대 15분** (900초)
- 15분 넘는 작업엔 Lambda 부적합 → **Step Functions** or **ECS/Fargate**

#### ⑥ Execution Role
- Lambda가 **다른 AWS 서비스를 호출할 때 쓰는 권한**
- IAM Role 반드시 attach
- 예: S3 쓰려면 `s3:PutObject` 권한 필요

#### ⑦ Environment Variables
- Function 설정에서 key-value 지정
- **Secrets Manager / Parameter Store** 연계 권장 (민감 정보)

#### ⑧ Layers
- 여러 Function이 **공유하는 라이브러리/코드**
- 예: 공통 유틸리티, SDK, 특정 바이너리 (ffmpeg 등)
- 하나의 Function에 **최대 5 Layer** attach

### 실행 모델 3가지

Lambda 호출 방식:

#### ① Synchronous (동기) — Request/Response
```
Caller → Lambda → 응답 받음
```
- API Gateway, ALB, 사용자 코드가 직접 호출
- Lambda 결과를 호출자가 바로 받음
- 에러 시 **호출자가 재시도 책임**

#### ② Asynchronous (비동기) — Event
```
Caller → Lambda queue (내부) → Lambda 실행
          ↑ Caller는 즉시 "접수 완료" 응답 받음
```
- S3, SNS, EventBridge
- Lambda가 **자동 재시도** (최대 2회)
- 실패 시 **Dead Letter Queue (DLQ)** or **Destination**

#### ③ Poll-based (Stream)
```
Lambda가 소스에서 데이터 polling → 처리
```
- Kinesis, DynamoDB Streams, SQS, MSK
- Lambda가 자동으로 record 가져와 처리
- Batch 단위 (여러 record 한 번에)

### Cold Start vs Warm Start ⭐

**Cold Start**:
```
요청 → 컨테이너 생성 → Runtime 초기화 → 코드 로드 → Handler 실행
       ↑ 이 전체 과정이 cold start
```
- 각 Lambda **첫 호출**이나 **오래 유휴** 후 호출
- **100 ms ~ 수초**
- 특히 **Java/C#**은 JVM/.NET init로 느림

**Warm Start**:
```
요청 → Handler 실행 (컨테이너 재사용)
```
- 최근 사용된 Lambda는 컨테이너가 메모리에 유지됨
- 수 ms 안에 응답

#### Cold Start 완화 방법

| 방법 | 설명 |
|------|------|
| **Provisioned Concurrency** | 미리 N개 컨테이너 준비 (cold start 0, 비용 ↑) |
| **SnapStart** | JVM/Python 상태를 snapshot으로 저장 (Java/Python 지원, cold start 10x 감소) |
| **작은 패키지** | 코드/의존성 작게 → 로드 빨라짐 |
| **Keep-Warm** | 주기적으로 호출해서 유휴 상태 방지 (EventBridge cron) |
| **언어 선택** | 성능 민감하면 Node.js/Python/Go |

### Concurrency (동시성)

**Concurrency** — 같은 시점에 **몇 개 요청이 동시 실행 중**인지.

- **Account 당 기본 1,000** (region별, 상향 가능)
- Reserved Concurrency: Function에 **전용 할당** (다른 Function이 뺏어가지 못하게)
- Provisioned Concurrency: 미리 **warm 상태 유지** (cold start 제거)

**Throttling**: 할당량 초과 시 → 요청 거부 (sync) 또는 큐에 쌓임 (async).

---

## 3부. Lambda 사용 사례 ⭐

### 1. API 백엔드 (가장 흔함)

```
Client → API Gateway → Lambda → DynamoDB/RDS
```

- RESTful API 서버
- **사용 사례**: Spring Boot 서버 대체, 트래픽 변동 큰 서비스
- **장점**: 서버 운영 X, 자동 스케일링
- **단점**: 복잡한 상태 관리 어려움

### 2. 이벤트 처리 (파일 업로드 등)

```
S3 (이미지 업로드) → S3 Event → Lambda → 썸네일 생성 → S3
```

- 사진 리사이즈, 영상 트랜스코딩, PDF 변환
- 바이러스 스캔
- 메타데이터 추출

### 3. 데이터 처리 (ETL)

```
Kinesis Stream → Lambda → DynamoDB
                 ↓
                 S3 (archival)
```

- 실시간 로그 처리
- 데이터 정제 후 DB 적재
- CDC (Change Data Capture) 처리

### 4. 스케줄 작업 (cron 대체)

```
EventBridge (cron: 매일 2am) → Lambda → 작업 실행
```

- 야간 리포트 생성
- 데이터 백업
- 만료 데이터 정리
- 서버 자동 종료 (비용 절감)

### 5. Webhook 처리

```
GitHub/Slack/Stripe → API Gateway → Lambda → 처리
```

- GitHub push → CI 트리거
- Slack 명령어 처리
- Stripe 결제 완료 → DB 업데이트

### 6. IoT 데이터 처리

```
IoT 디바이스 → AWS IoT Core → Lambda → DynamoDB
```

- 센서 데이터 수집/변환
- 이상 탐지
- 실시간 알림

### 7. 챗봇 / Voice Assistant

```
Alexa → Alexa Skill (Lambda) → 응답
```

- Alexa Skills
- Slack bot
- Discord bot

### 8. Edge Computing

**Lambda@Edge / CloudFront Functions**:
```
Client → CloudFront (Edge Location) → Lambda@Edge → origin
                      ↓
                      Request/Response 변환
```

- A/B 테스트 라우팅
- 인증 (JWT 검증)
- URL 재작성
- 이미지 리사이징

### 9. 서버리스 웹 애플리케이션

```
S3 (SPA) + CloudFront + API Gateway + Lambda + DynamoDB
```

- 완전히 서버 없이 웹 서비스 운영
- **AWS Amplify**로 손쉽게 구축

### 10. 자동화 / Ops

- EC2 상태 변경 → Lambda → Slack 알림
- CloudWatch Alarm → Lambda → 자동 복구
- 비용 모니터링 자동화

---

## 4부. Lambda vs 대체제 비교

| | **Lambda** | **Fargate** | **EC2** |
|---|-----------|------------|---------|
| 실행 단위 | 함수 | 컨테이너 | VM |
| 최대 실행 | **15분** | 무제한 | 무제한 |
| 시작 시간 | 100ms~수초 (cold) | 수십초~분 | 분 |
| Cold start | 있음 | 있음 (느림) | 없음 |
| 유휴 비용 | **0원** | 적음 | 많음 |
| 상태 보유 | ❌ Stateless | 가능 | 가능 |
| 스케일링 | **즉시 자동** | 수십 초 | 수 분 |
| 사용처 | **짧은 이벤트** | **상시 서비스** | **완전 제어** |

**실무 선택 가이드**:
- 트리거 기반 짧은 작업 → **Lambda**
- 상시 운영 컨테이너 서비스 → **Fargate**
- 복잡한 설정/상태 보유 필요 → **EC2**

---

## 5부. Lambda 비용 모델

**과금 요소 2가지**:
1. **Request 수** — 호출 1건당 $0.20 per 1M requests
2. **Duration × Memory** — GB-seconds 단위
   - 예: 512 MB × 1초 = 0.5 GB-seconds
   - $0.0000166667 per GB-seconds (지역별 다름)

**무료 Tier** (매월):
- 1,000,000 request
- 400,000 GB-seconds

→ 작은 서비스는 **거의 무료**. 큰 서비스는 EC2보다 저렴할 수도, 비쌀 수도.

**비용 최적화**:
- **메모리 튜닝** — Lambda Power Tuning 툴 사용 ([github.com/alexcasalboni/aws-lambda-power-tuning](https://github.com/alexcasalboni/aws-lambda-power-tuning))
- 짧은 Function으로 쪼개기 (지불 단위 축소)
- Reserved/Savings Plans (Compute Savings Plans 적용 가능)

---

## 6부. Lambda 함정과 주의점

- [ ] **Cold start 민감 서비스** — Provisioned Concurrency 또는 SnapStart 활용
- [ ] **15분 timeout** — 긴 작업은 Step Functions로 분할
- [ ] **Stateless** — 세션/캐시는 외부 (DynamoDB, ElastiCache, S3)
- [ ] **VPC Lambda** — ENI 생성 비용 (초기엔 느림, 2019+ 개선)
- [ ] **동시성 limit** — Reserved Concurrency로 격리 (다른 Function 영향 방지)
- [ ] **외부 API 호출** — timeout 짧게, Lambda 전체 timeout 전에 응답
- [ ] **DB 연결** — Lambda마다 새 커넥션 → **RDS Proxy** 사용 (connection pool)
- [ ] **메모리 설정** — 적정값 찾기 (너무 적으면 OOM/느림, 너무 많으면 비용)
- [ ] **로그 비용** — CloudWatch Logs가 비싸질 수 있음, 보관 기간 설정

---

## 참고 자료

- [AWS Lambda Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/)
- [Serverless Architectures](https://aws.amazon.com/serverless/)
- [Lambda Pricing](https://aws.amazon.com/lambda/pricing/)
- [AWS Lambda Power Tuning](https://github.com/alexcasalboni/aws-lambda-power-tuning)
- [Serverless Patterns](https://serverlessland.com/patterns)
