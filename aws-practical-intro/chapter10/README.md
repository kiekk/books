### 10장. API Gateway

## 1부. API Gateway 개념

#### API Gateway란

AWS의 **완전 관리형 API 서비스**. 클라이언트와 백엔드 사이에서 **API를 설계/배포/운영**하는 플랫폼.

```
Client ──▶ API Gateway ──▶ Backend
            │               ├─ Lambda
            │               ├─ EC2/ALB
            │               ├─ Step Functions
            │               ├─ 다른 AWS 서비스 (DynamoDB, S3)
            │               └─ 외부 HTTP 서버
            │
            └─ 인증, 쿼터, 캐싱, 로깅, 변환
```

**핵심 역할** — "**API의 리셉션 데스크**":
- 클라이언트 요청 **수신** (HTTPS/WebSocket)
- 인증/인가 검증
- Rate limit 체크
- 캐시 조회
- Request/Response **변환**
- 적절한 백엔드로 **라우팅**
- CloudWatch에 메트릭/로그 전송

#### 왜 필요한가 — Backend 직접 노출의 문제

**없이**:
```
Client → Lambda (직접 호출)
```
문제점:
- Lambda는 **HTTPS endpoint 없음** (예전엔)
- 인증 로직을 Lambda마다 작성
- Rate Limit, CORS, 캐싱 수동 구현
- API 버저닝/배포 복잡

**API Gateway 있음**:
```
Client → API Gateway → Lambda
         ↑ HTTPS, 인증, 쿼터, 캐싱, 변환 전부 처리
```

→ Lambda는 **비즈니스 로직만**, 공통 관심사는 API Gateway가 담당.

#### API 유형 3가지

| 유형 | 특징 | 사용처 |
|-----|------|------|
| **REST API** | **기능 최대** (캐싱, API Keys, WAF, Request Validation...) | 복잡한 요구사항, 레거시 호환 |
| **HTTP API** | **간단, 저렴, 빠름** (REST v2, 2019+) | 신규 REST 서비스 (대부분 추천) |
| **WebSocket API** | **양방향 실시간 통신** | 채팅, 실시간 알림, 게임 |

#### REST API vs HTTP API 비교

| 항목 | **REST API** | **HTTP API** |
|-----|------------|-------------|
| 가격 | 비쌈 ($3.5 per 1M) | **70% 저렴** ($1 per 1M) |
| 성능 | 보통 | **더 빠름** (낮은 지연) |
| JWT Authorizer | ❌ (Lambda Authorizer만) | ✅ **네이티브** |
| API Keys / Usage Plan | ✅ | ❌ (제한적) |
| Request Validation | ✅ | ❌ |
| WAF 통합 | ✅ | ❌ (CloudFront 통해 가능) |
| Caching | ✅ (API Gateway 자체) | ❌ (CloudFront로) |
| Private Integration | VPC Link | VPC Link (더 유연) |

**선택**:
- **신규 서비스** → **HTTP API** (대부분)
- **고급 기능 필요** (API Key 과금, WAF 직접 통합, 캐싱) → **REST API**

#### 주요 구성 요소

```
API Gateway API
├─ Resources (/users, /orders)
│  └─ Methods (GET, POST, PUT, DELETE)
│     ├─ Request (Headers, Query, Body)
│     ├─ Integration (어떤 backend로)
│     ├─ Authorization (누가 호출 가능)
│     └─ Response (status, headers, body)
├─ Stages (dev, staging, prod)
├─ Deployments (각 stage의 특정 시점 snapshot)
├─ Custom Domain (api.example.com)
├─ Usage Plans (API Key별 쿼터)
└─ Models / Validators
```

### 핵심 기능 7가지

#### ① 인증/인가 (Authorization)

| 방식 | 설명 |
|------|------|
| **IAM** | AWS SigV4 서명 (내부 서비스 간) |
| **Cognito User Pools** | 사용자 로그인 (사용자명/비밀번호, OAuth, SNS 로그인) |
| **Lambda Authorizer** | 커스텀 로직 (JWT 검증, 외부 OAuth) |
| **JWT Authorizer** | HTTP API만. JWKS로 자동 검증 |
| **API Keys** | B2B API 발급, Usage Plan 연계 |
| **Mutual TLS (mTLS)** | 클라이언트 인증서 검증 |

#### ② Throttling (Rate Limiting)

**Burst + Rate** 2단계 제어:
- **Burst**: 순간 최대 (예: 5000 req)
- **Rate**: 초당 평균 (예: 1000 req/s)

**적용 레벨**:
- Account 레벨 (전체)
- Stage 레벨 (prod 등)
- Method 레벨 (특정 endpoint)
- API Key 레벨 (사용자별)

→ 공격/과부하 방어, 공정 사용.

#### ③ Caching (REST API)

- API Gateway 자체 캐시 (0.5 GB ~ 237 GB)
- TTL 설정 (최대 1시간)
- Cache Key: method + path + query (설정 가능)
- 캐시 Hit → Backend 호출 없이 즉시 응답

**비용 vs 효과**:
- 캐시 비싼 편 → **HTTP API + CloudFront** 조합이 더 저렴
- 캐시 히트로 Lambda 호출 수 감소

#### ④ Request/Response Transformation

**Request**:
- Header 추가/삭제
- Query string 변환
- Body 매핑 (Velocity Template Language)
- 외부 호환 포맷 ↔ 내부 포맷 변환

**Response**:
- Status code 재지정
- Header 추가 (CORS, security)
- Body 템플릿

→ 백엔드와 **느슨한 결합** (backend 바꿔도 API 스펙 유지).

#### ⑤ Monitoring & Logging

- **CloudWatch Metrics**: Count, Latency, 4xxError, 5xxError, IntegrationLatency, CacheHitCount
- **Access Logs**: 요청 상세 (CloudWatch Logs or Firehose)
- **Execution Logs**: 내부 처리 로그 (디버깅)
- **X-Ray Tracing**: 분산 추적

#### ⑥ CORS (Cross-Origin Resource Sharing)

**브라우저가 다른 도메인 API 호출 시 필수**.

```
Client (web.example.com)
    ↓ CORS Preflight (OPTIONS)
API Gateway → Access-Control-Allow-Origin: *
```

API Gateway에서 **설정만으로 자동 처리**.

#### ⑦ Custom Domain + Certificate

```
api.example.com → API Gateway (default: xxx.execute-api.region.amazonaws.com)
```
- Route 53 + ACM 인증서 조합
- Base Path Mapping으로 여러 API를 한 도메인에

---

## 2부. 사용 사례 ⭐

### 1. Serverless API (가장 흔함)

**아키텍처**:
```
Client → API Gateway → Lambda → DynamoDB
```

**예시** — 블로그 API:
- `GET /posts` → ListPosts Lambda
- `POST /posts` → CreatePost Lambda
- `GET /posts/{id}` → GetPost Lambda
- `PUT /posts/{id}` → UpdatePost Lambda
- `DELETE /posts/{id}` → DeletePost Lambda

**장점**:
- 서버 운영 X
- 자동 스케일링
- 사용량 기반 과금

### 2. Microservice API Gateway (BFF 패턴)

**Backend for Frontend**:
```
┌────────┐  ┌──────────┐  ┌──────────┐
│Mobile  │  │Web (SPA) │  │Partner   │
└───┬────┘  └────┬─────┘  └────┬─────┘
    ▼            ▼              ▼
┌────────┐  ┌────────┐  ┌────────┐
│Mobile  │  │Web API │  │Partner │  ← 각 클라이언트용 BFF
│API GW  │  │GW      │  │API GW  │
└───┬────┘  └───┬────┘  └───┬────┘
    │           │            │
    └───────────┴────────────┘
                ▼
        ┌───────────────┐
        │ Microservices │
        │  (Lambda,     │
        │   ECS,        │
        │   EC2...)     │
        └───────────────┘
```

- 클라이언트별로 다른 API Gateway
- 각자 다른 응답 포맷, 인증 방식
- 백엔드 마이크로서비스는 공유

### 3. Legacy API Modernization

**레거시 백엔드에 modern API layer**:
```
Modern Client (JSON, OAuth)
    ↓
API Gateway (변환)
    ↓ (Request Transformation: JSON → SOAP XML)
Legacy SOAP/EC2 Backend
```

- 레거시 수정 없이 **modern 클라이언트 지원**
- 점진적 마이그레이션 (레거시 → Lambda 분리)

### 4. WebSocket API — 실시간 양방향

**사용 사례**:
- **채팅** — 메시지 push
- **실시간 알림** — 주문 상태, 결제
- **협업 도구** — 커서 공유, 변경 반영
- **게임** — 멀티플레이어 상태 동기화
- **IoT 대시보드** — 센서 데이터 실시간

**아키텍처**:
```
Client ─WebSocket─▶ API Gateway WebSocket API
                      ├─ $connect     → Lambda (연결 처리)
                      ├─ $disconnect  → Lambda (해제)
                      ├─ $default     → Lambda (메시지)
                      └─ sendMessage  → Lambda
                           ↓
                      DynamoDB (connection ID 저장)
```

### 5. B2B API 발급 (API Key + Usage Plan)

**파트너사에게 API 제공** — 플랜별 과금.

```
API Key: "free-tier"      → Rate: 100 req/s,  Quota: 10,000/month
API Key: "pro-tier"       → Rate: 1,000 req/s, Quota: 1,000,000/month
API Key: "enterprise"     → Rate: unlimited
```

- **Usage Plan**으로 API Key별 쿼터
- 과금 분리 (Stripe 등 연계)

### 6. 외부 서비스 통합 (Private → Public)

**내부 EC2 서비스를 공개 API로**:
```
Internet ─▶ API Gateway (public endpoint)
             ↓ VPC Link
             ALB (private, VPC 내부)
             ↓
             EC2
```

- 내부 서버 private 유지 + 공개 API 노출
- API Gateway가 **보안 레이어**

### 7. Mock API (프로토타입)

```
Method Response: 200
Mock Integration: 
  { "message": "sample response" }
```

- 백엔드 없이 **API 스펙 먼저**
- 프론트엔드 개발 병행
- Postman 대체

### 8. Rate Limit / DDoS 방어

**트래픽 보호**:
```
Attacker ─▶ API Gateway (rate limit) ─X─▶ Lambda (호출 안 됨)
             429 Too Many Requests
```

- Burst/Rate 설정으로 과부하 방어
- API Key별 세밀한 쿼터
- **AWS WAF 통합** (SQL Injection, XSS 차단)

### 9. 다른 AWS 서비스 직접 통합

Lambda 없이 **직접 백엔드 서비스 호출** (AWS Service Proxy):

```
Client → API Gateway → DynamoDB (Lambda 없이)
       → S3 Pre-signed URL 생성
       → SNS 메시지 발행
       → Step Functions 시작
```

**장점**: Lambda 없어서 **더 빠름, 더 저렴**
**단점**: 변환 로직 복잡

### 10. SPA + 서버리스 아키텍처

```
Browser → CloudFront → S3 (SPA)
             │
             └─ /api/* → API Gateway → Lambda → DynamoDB
```

→ **서버 1대 안 띄우고** 완성.

---

## 3부. API Gateway 설계 Best Practice

### API 설계

- [ ] **RESTful 원칙** 준수 (명사 리소스, HTTP verb 활용)
- [ ] **버전 관리** (`/v1/users`, `/v2/users` 또는 Header)
- [ ] **일관된 에러 포맷** (status + code + message)
- [ ] **Pagination** 명시 (`?page=1&limit=20` or cursor-based)
- [ ] **Idempotency** — PUT/DELETE은 멱등, POST는 ID 생성

### 보안

- [ ] **HTTPS Only** (ACM 인증서)
- [ ] **인증 필수** — API Key만으론 부족, OAuth/JWT 추가
- [ ] **Lambda Authorizer** 캐싱 (TTL 설정, 매 요청 재검증 금지)
- [ ] **API Key**는 비밀 — 공개 git 금지
- [ ] **Rate Limit** 설정 (DDoS 방어)
- [ ] **WAF 통합** (SQL Injection, XSS)
- [ ] **CORS**: `*` 피하기 (특정 도메인 명시)

### 운영

- [ ] **Stage 분리** (dev / staging / prod)
- [ ] **Deployment 롤백** 전략
- [ ] **Canary Release** (10% 트래픽만 새 버전)
- [ ] **CloudWatch Alarms** (5xx error rate, latency p99)
- [ ] **Access Logs 수집** (장애 조사)
- [ ] **X-Ray Tracing** (분산 추적)

### 비용

- [ ] **HTTP API 우선 고려** (REST API 대비 70% 저렴)
- [ ] **CloudFront 앞단** (글로벌 + 캐싱)
- [ ] **Caching**은 비싸 — 필요한 경우만
- [ ] **직접 AWS 통합** (Lambda 생략 가능)

---

## 4부. REST API vs HTTP API 선택 플로우

```
신규 서비스인가?
├─ Yes → 아래 조건 중 하나라도 필요?
│         ├─ API Keys / Usage Plan 과금
│         ├─ API Gateway 자체 Caching
│         ├─ Request Validation
│         ├─ WAF 직접 통합
│         └─ Private VPC endpoint (API Gateway)
│       ├─ No → ✅ HTTP API (저렴, 빠름, JWT 네이티브)
│       └─ Yes → ✅ REST API
│
└─ 기존 REST API → 그대로 유지 (migration 비용 크면)
```

---

## 참고 자료

- [API Gateway Developer Guide](https://docs.aws.amazon.com/apigateway/latest/developerguide/)
- [REST API vs HTTP API](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-vs-rest.html)
- [API Gateway Pricing](https://aws.amazon.com/api-gateway/pricing/)
- [Lambda Authorizer](https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-use-lambda-authorizer.html)
- [WebSocket API Guide](https://docs.aws.amazon.com/apigateway/latest/developerguide/apigateway-websocket-api.html)
