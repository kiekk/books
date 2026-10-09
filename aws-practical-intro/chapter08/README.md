### 8장. CloudFront

## 1부. CDN과 CloudFront 개념

#### CDN (Content Delivery Network)이란

**전 세계에 분산된 캐시 서버 네트워크**. 사용자와 **지리적으로 가까운 서버**에서 콘텐츠를 제공해 **지연을 줄이고 원본 부하를 줄임**.

```
❌ CDN 없음:
서울 사용자 ──── 수천 km ────▶ 미국 서버 (지연 200ms+)

✅ CDN 있음:
서울 사용자 → 서울 Edge (10ms) → (캐시 miss 시만) → 미국 서버
```

**CDN이 해결하는 문제**:
- **네트워크 지연** — 지리적 거리 제거
- **대역폭 비용** — 원본 서버에서 매번 전송 안 함
- **원본 부하** — 캐시로 처리
- **DDoS 방어** — Edge가 1차 방어선

#### CloudFront = AWS의 CDN

**특징**:
- **600+ Points of Presence (PoP)** — 전 세계 90+ 도시 (2024 기준)
- **AWS 서비스와 통합** (S3, ALB, Lambda, Media 등)
- **HTTPS + HTTP/2 + HTTP/3** 지원
- **DDoS 방어** 기본 (AWS Shield Standard)
- **AWS WAF** 통합 가능
- **Edge Computing** (Lambda@Edge, CloudFront Functions)

---

## 2부. Edge Location과 Origin ⭐

### Edge Location (엣지 로케이션)

**사용자 가까이에 있는 캐시 서버**. CloudFront의 **최전선**.

- 전 세계 **600+ PoP** (Points of Presence)
- **ISP(통신사) 네트워크 근처**에 위치 → 사용자까지 1~2 hop
- 콘텐츠 복제본 저장 (캐시)

```
한국의 Edge Location 예:
- Seoul (3곳)
- Busan
```

#### Regional Edge Cache (지역 캐시) — 2차 계층

Edge Location보다 **더 큰 캐시**. 자주 쓰이지 않는 콘텐츠도 보관.

```
요청 흐름 (3계층):

User (서울)
    ↓ 요청
Edge Location (서울 Edge)
    ├─ Hit? → 즉시 반환 ⚡
    └─ Miss?
         ↓ 상위에 요청
       Regional Edge Cache (서울/도쿄 Regional)
            ├─ Hit? → 반환 + Edge에 저장
            └─ Miss?
                 ↓ Origin으로
               Origin (S3 Seoul 등)
                 → 응답
                 → Regional/Edge 양쪽에 저장
                 → User 반환
```

**효과**:
- 각 Edge가 **독립적으로** Origin 호출 안 함 → Origin 부하 ↓
- Edge Location에서 사라진 콘텐츠도 Regional에 남아있을 가능성 ↑

### Origin (오리진)

**원본 콘텐츠 저장소**. CloudFront가 캐시 miss 시 데이터 가져오는 곳.

**지원하는 Origin 종류**:

| Origin | 사용처 |
|-------|------|
| **S3 Bucket** | 정적 파일 (이미지, HTML, JS, CSS, 비디오) |
| **S3 Static Website** | S3를 웹사이트로 (리다이렉트 지원) |
| **EC2 / ALB** | 동적 콘텐츠 (API 응답, 서버 렌더링 HTML) |
| **NLB** | TCP 레벨 서비스 |
| **Lambda Function URL** | 서버리스 API |
| **MediaStore / MediaPackage** | 비디오 스트리밍 |
| **외부 HTTP(S) 서버** | On-prem, 다른 클라우드 |

#### Origin Group (장애 전환)

여러 Origin을 **primary/secondary**로 묶어 자동 failover:

```
Primary: S3 ap-northeast-2 (서울)
   │ 실패 (5xx) 시
   ▼
Secondary: S3 us-east-1 (버지니아)
```

→ Origin 장애 시 자동으로 다른 Origin으로 요청.

#### Origin Access Control (OAC) ⭐

**S3를 Public으로 열지 않고 CloudFront 통해서만** 접근 허용.

```
❌ Bad: S3 bucket을 public으로 → 누구나 직접 접근 가능
✅ Good: S3 private + CloudFront OAC
         → CloudFront만 S3 접근 가능
         → 사용자는 CloudFront URL로만 접근
```

**장점**:
- S3 직접 호출 차단 (CloudFront 우회 방지)
- 모든 트래픽이 CloudFront 거침 → WAF, 로깅 일관
- Signed URL 등 추가 보안

---

## 3부. CloudFront 장점 ⭐

### 1. 낮은 지연 (Low Latency)

- 사용자 → Edge (수십 ms 이하)
- 원본 서버가 다른 대륙에 있어도 **지역 균일한 응답 속도**

**체감 예시**:
- 미국 서버로 직접 접근 (서울 사용자): 200~300 ms
- CloudFront 거침: **20~50 ms**

### 2. Origin 부하 감소

- 캐시 Hit rate 90% → Origin 요청 10%만
- S3 요청 비용 절감 (요청 수 과금)
- EC2/ALB 부하 분산

### 3. 비용 효율

- **S3 Egress 비용 절감** — CloudFront → S3 **동일 Region 내 데이터 전송 무료**
- CloudFront의 데이터 전송 가격이 S3보다 저렴한 경우도

### 4. 보안 강화

- **HTTPS Termination** at Edge (TLS 1.3 지원)
- **AWS Shield Standard** (DDoS 방어 기본)
- **AWS Shield Advanced** 옵션 (추가 비용)
- **AWS WAF** 통합 (SQL Injection, XSS 방어)
- **Signed URL / Cookie** (임시 접근 토큰)
- **Field-Level Encryption** (특정 필드만 암호화)
- **Geo-Restriction** (국가 차단)

### 5. 글로벌 균일 성능

- 전 세계 어디서 접근해도 **유사한 응답 속도**
- 서비스를 Multi-Region으로 복제 안 해도 됨 (정적 콘텐츠는)

### 6. HTTP/2, HTTP/3 지원

- Multiplexing으로 여러 요청 동시 처리
- QUIC 기반 HTTP/3 (더 빠른 연결)

### 7. Edge Computing

**CloudFront Functions / Lambda@Edge**:
- 요청/응답을 **Edge에서 변환**
- A/B 테스트 라우팅
- 인증/인가 (JWT 검증)
- URL 재작성
- 사용자 지역별 콘텐츠

| | CloudFront Functions | Lambda@Edge |
|---|---------------------|-------------|
| 언어 | JavaScript만 | Node.js, Python |
| 실행 시간 | 1 ms 이하 | 수십 ms |
| 사용처 | 간단한 변환 (헤더/URL) | 복잡한 로직 |

### 8. 분석 통합

- **CloudWatch Metrics** (요청 수, 캐시 hit rate, 지연)
- **Standard Logs** (S3로 상세 로그)
- **Real-time Logs** (Kinesis로 실시간 전송)

---

## 4부. CloudFront 사용 사례 ⭐

### 1. 정적 콘텐츠 배포 (가장 흔함)

```
S3 (정적 파일) + CloudFront
```

- 이미지, JavaScript, CSS, HTML, 폰트
- **SPA 호스팅** — React/Vue/Angular 앱

**아키텍처**:
```
Client → CloudFront → S3 (SPA + 정적 자산)
            ↓
      API 요청만 ──▶ API Gateway → Lambda
```

### 2. 비디오 스트리밍

**On-demand (VOD)**:
- 영상 파일을 S3에 저장
- CloudFront로 배포
- HLS/DASH 지원

**Live Streaming**:
- MediaLive → MediaPackage → CloudFront
- 전 세계 수백만 명 시청

### 3. 소프트웨어 다운로드

- 큰 설치 파일 (수백 MB~GB)
- 전 세계 다운로드 트래픽 분산
- 예: Steam 게임 패치, 모바일 앱 업데이트

### 4. API 가속화 (동적 콘텐츠)

```
Client → CloudFront → API Gateway → Lambda
          ↑
          짧은 TTL (예: 1분) 또는 캐시 없이 지연만 감소
```

- TLS termination at Edge → connection reuse → 빠른 응답
- 캐시 가능한 응답은 짧게 캐싱

### 5. 전 세계 서비스

- 멀티 리전 서비스에서 모든 지역에 균일 성능
- 예: 글로벌 SaaS, 뉴스 미디어

### 6. 보안 레이어

- **DDoS 방어** — Shield + WAF
- **Bot 차단** — Rate Limit
- **Geo-restriction** — 특정 국가 차단 (저작권/규제)
- **인증 통합** — Lambda@Edge로 JWT 검증

### 7. Edge Computing 사례

**A/B 테스트**:
```
Client → CloudFront Functions
         ├─ 50%: v1 Origin
         └─ 50%: v2 Origin
```

**지역별 다른 콘텐츠**:
```
Client Country Header → Lambda@Edge
                        ├─ KR → Korean content
                        ├─ US → English content
                        └─ ...
```

**이미지 리사이징**:
```
/image.jpg?w=400 → Lambda@Edge → 리사이즈 → 캐시 저장
```

### 8. SPA + 서버리스 아키텍처

```
┌──────────────────────────────────┐
│   사용자 (브라우저)                │
└──────────────────┬───────────────┘
                   ↓
         ┌─────────────────┐
         │  CloudFront      │ ← CDN + 보안
         └────┬────────────┬┘
              ↓            ↓
       ┌──────────┐  ┌──────────────┐
       │ S3 (SPA) │  │ API Gateway  │
       └──────────┘  └──────┬───────┘
                           ↓
                      ┌─────────┐
                      │ Lambda  │
                      └────┬────┘
                           ↓
                      ┌──────────┐
                      │ DynamoDB │
                      └──────────┘
```

→ **서버 1대 안 띄우고** 전 세계 서비스 운영.

---

## 5부. 캐시 동작 상세

### Cache Behavior

**경로별 다른 캐싱 정책** 설정 가능:

```
*.jpg, *.css, *.js    → TTL 1년 (long cache)
/api/*                → No cache (매번 Origin)
/user/*               → TTL 5분 (short cache)
```

### Cache Key — 캐시 식별자

**기본 Cache Key**: URL (호스트 + 경로)
```
GET https://example.com/image.jpg
    → Cache Key: example.com/image.jpg
```

**Query String / Header 포함 가능**:
```
GET /image.jpg?w=400
    → Cache Key: /image.jpg + w=400
```

→ 같은 리소스지만 **다른 파라미터**면 **다른 캐시 엔트리**.

**Cache Policy 종류**:
- `CachingOptimized` — Query/Header 무시, 효율 최대
- `CachingDisabled` — 캐시 완전 비활성
- `UseOriginCacheControlHeaders` — Origin의 `Cache-Control` 헤더 따름
- **Custom** — 세밀한 설정

### TTL (Time to Live)

- **Minimum TTL** — 최소 캐시 시간
- **Default TTL** — 기본 캐시 시간
- **Maximum TTL** — 최대 캐시 시간

Origin이 `Cache-Control: max-age=3600` 보내면 → 1시간 캐시.

### Cache Invalidation (수동 삭제)

```bash
aws cloudfront create-invalidation \
  --distribution-id EXXXXXXXXXXX \
  --paths "/images/*" "/index.html"
```

- 배포에 **수 분 소요**
- **월 1,000 path 무료**, 이후 유료
- 가능하면 **파일명에 버전 포함** (예: `app.v2.js`) → invalidation 불필요

### Behaviors (경로 패턴)

```
Behavior 1: /api/*       → Origin: API Gateway, No cache, 모든 메서드 허용
Behavior 2: /images/*    → Origin: S3, TTL 1년
Behavior 3: /*           → Origin: S3, TTL 1일 (default)
```

→ **우선순위 순서대로** 매칭 (specific → general).

---

## 6부. 실무 체크리스트

- [ ] **Origin Access Control (OAC)** 사용 (S3 private 유지)
- [ ] **HTTPS Only** 설정 (HTTP → HTTPS 자동 redirect)
- [ ] **AWS WAF 통합** (중요 서비스)
- [ ] **Compression 활성화** (Gzip/Brotli)
- [ ] **HTTP/2, HTTP/3 활성화**
- [ ] **버저닝된 URL** 사용 (예: `app.v123.js`) → invalidation 최소화
- [ ] **적절한 Cache-Control 헤더** Origin에서 설정
- [ ] **Access Log** S3에 수집 (장애 추적)
- [ ] **CloudWatch Alarms** — 4xx/5xx rate, Origin latency
- [ ] **Geo-Restriction** 필요 시 적용
- [ ] **Signed URL/Cookie** — 유료 콘텐츠/임시 접근

---

## 참고 자료

- [Amazon CloudFront Developer Guide](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/)
- [CloudFront Edge Locations](https://aws.amazon.com/cloudfront/features/)
- [Origin Access Control](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html)
- [CloudFront Functions vs Lambda@Edge](https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/edge-functions.html)
- [CloudFront Pricing](https://aws.amazon.com/cloudfront/pricing/)
