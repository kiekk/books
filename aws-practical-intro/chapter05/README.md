### 5장. S3

## 1부. S3 개념

#### S3 (Simple Storage Service)

AWS의 **객체 스토리지(Object Storage) 서비스**. "클라우드의 무한 하드디스크".

**핵심 특징**:
- **사실상 무한 용량** (버킷당 무제한, 객체당 5 TB까지)
- **11 nines 내구성** — `99.999999999%` (연간 1,000만 객체 중 1개 손실 수준)
- **99.99% 가용성** (Standard 기준)
- **리전 범위** 서비스 — 버킷은 특정 Region에 생성
- **글로벌 네임스페이스** — 버킷 이름은 **전 세계 유일**
- **HTTPS/HTTP** 접근 (S3 API, REST)

#### 다른 스토리지와 비교

| 유형 | 특징 | AWS 제품 |
|-----|-----|---------|
| **블록 스토리지** | 블록 단위 I/O, OS가 파일 시스템 올림 | EBS |
| **파일 스토리지** | 파일/디렉토리, NFS/SMB 프로토콜 | EFS, FSx |
| **객체 스토리지** | 객체 단위, HTTP API, 메타데이터 | **S3** |

**객체 스토리지 특성**:
- 수정 불가 (**전체 교체만** 가능, 부분 업데이트 X)
- 메타데이터 풍부
- HTTP 기반 → 어디서든 접근 가능
- 대규모 분산 저장에 최적

---

## 2부. 오브젝트 구성 요소

### 객체(Object) 구조

```
┌─────────── Object ───────────┐
│  Key:        files/photo.jpg  │  ← 식별자 (파일명 역할)
│  Value:      [raw bytes]      │  ← 실제 데이터 (0 bytes ~ 5 TB)
│  Metadata:                    │
│    - Content-Type: image/jpeg │
│    - Content-Length: 2048000  │
│    - Last-Modified: ...       │
│    - ETag: "a4e3..."          │
│    - x-amz-meta-author: alice │  ← 사용자 정의 metadata
│  Version ID: null or v123     │  ← 버저닝 활성화 시
│  ACL:        bucket-owner     │  ← Access Control List
└───────────────────────────────┘
```

#### ① Key (키) — "파일 이름"

- 버킷 내 **유일한 식별자**
- 최대 1,024 바이트 (UTF-8)
- "폴더" 개념 없음 → **prefix로 흉내**:
  ```
  photos/2024/jan/img001.jpg
  photos/2024/jan/img002.jpg
  photos/2024/feb/img003.jpg
  ```
  → 콘솔에서는 `photos/2024/jan/` 폴더처럼 보이지만, 실제로는 **그냥 긴 Key 문자열**.

#### ② Value (값) — 실제 데이터

- **최대 5 TB** per 객체
- **5 GB 넘으면 Multipart Upload 필수** (여러 part로 쪼개 업로드)
- 바이너리/텍스트 상관없음

#### ③ Metadata (메타데이터)

**System-defined metadata** (AWS가 관리):
- `Content-Type`, `Content-Length`, `Last-Modified`, `ETag`, `Content-Encoding`

**User-defined metadata** (사용자 정의):
- `x-amz-meta-*` prefix
- 예: `x-amz-meta-author: alice`
- **생성 시만 설정 가능** (수정하려면 객체 재생성)

#### ④ Version ID (버전)

- 버저닝 활성화 시 **같은 Key로 PUT해도 새 버전**으로 저장
- `null` → 버저닝 전 객체
- 삭제도 "**Delete Marker**" 버전으로 처리 (실제 데이터는 보존)

#### ⑤ ACL (Access Control List) — 레거시

- 객체/버킷 단위 권한 설정
- **최근엔 Bucket Policy + IAM 사용 권장**
- 2023+ AWS 기본은 ACL 비활성화

### 버킷(Bucket) 구조

```
Bucket (my-company-logs)
├─ 리전: ap-northeast-2 (서울)
├─ 버저닝: Enabled
├─ 암호화: SSE-KMS
├─ Lifecycle rule: 90일 후 Glacier로 이동
├─ 버킷 정책: ...
└─ Objects:
    ├─ logs/2024/01/app.log
    ├─ logs/2024/01/error.log
    └─ logs/2024/02/app.log
```

**버킷 네이밍 규칙**:
- 3~63자, 소문자/숫자/하이픈/점만
- IP 주소 형식 X
- `xn--` 로 시작 X
- **전 세계 유일** (한 번 쓴 이름은 다른 사람이 못 씀)

---

## 3부. 스토리지 클래스 (버킷 "타입")

실은 **버킷** 타입이 아니라 **객체별 스토리지 클래스** — 같은 버킷 안에 다른 클래스 객체 혼재 가능.

| 클래스 | 저장 비용 | 조회 비용 | 가용성 | 액세스 지연 | 최소 보관 | 사용처 |
|-------|---------|---------|------|---------|---------|------|
| **S3 Standard** | 기본 | 저렴 | 99.99% | ms | — | 자주 접근 |
| **S3 Intelligent-Tiering** | 중간 | 중간 | 99.9% | ms | — | 접근 패턴 모름 |
| **S3 Standard-IA** | 저렴 | 비쌈 | 99.9% | ms | 30일 | 가끔 접근 |
| **S3 One Zone-IA** | 더 저렴 | 비쌈 | 99.5% | ms | 30일 | 재생성 가능한 데이터 |
| **S3 Glacier Instant Retrieval** | 매우 저렴 | 비쌈 | 99.9% | **ms** | 90일 | 분기별 접근 아카이브 |
| **S3 Glacier Flexible Retrieval** | 매우 저렴 | 저렴 | 99.99% | **분~시간** | 90일 | 1년에 몇 번 |
| **S3 Glacier Deep Archive** | **최저가** | 가장 비쌈 | 99.99% | **12시간** | 180일 | 7년 이상 장기 보관 |

### 선택 가이드

| 접근 빈도 | 즉시 조회 필요? | 추천 |
|---------|-------------|------|
| 매일 | ✅ | **Standard** |
| 월 1회 미만 | ✅ (ms 이내) | **Standard-IA** |
| 분기 1회 미만 | ✅ (ms 이내) | **Glacier Instant Retrieval** |
| 년 1회 미만 | 분~시간 OK | **Glacier Flexible** |
| 몇 년에 한번 | 12시간 OK | **Glacier Deep Archive** |
| 모름 | 모름 | **Intelligent-Tiering** |

### Lifecycle Rule — 자동 이동

```
Day 0: Standard (활발히 접근)
  ↓ 30일 후
Day 30: Standard-IA (자주 안 쓰임)
  ↓ 90일 후
Day 90: Glacier Flexible (아카이브)
  ↓ 365일 후
Day 365: Glacier Deep Archive (장기 보관)
  ↓ 2555일 후
Day 2555: 삭제
```

→ Lifecycle 규칙으로 **자동 이동 + 자동 삭제** 가능. 비용 최적화 핵심.

---

## 4부. 버킷 정책 (Bucket Policy) ⭐

### Resource-based Policy vs Identity-based Policy

| | **Identity-based (IAM Policy)** | **Resource-based (Bucket Policy)** |
|---|------------------------------|---------------------------------|
| Attach 대상 | User, Group, Role | S3 Bucket |
| 질문 방식 | "**이 user는** 뭐할 수 있나?" | "**이 bucket에** 누가 뭐할 수 있나?" |
| **Principal 필드** | 없음 (identity 자체가 principal) | **필수** (누구에게 적용되는지 명시) |
| Cross-account | 지원 (Role Assume) | **직접 지원** (다른 Account의 ARN 명시) |

→ **같은 접근 제어의 두 가지 관점**. 둘 다 평가되어 **합집합이 허용**.

### 버킷 정책 예시

#### ① 특정 user에게 읽기 권한

```json
{
    "Version": "2012-10-17",
    "Statement": [{
        "Sid": "AllowAliceRead",
        "Effect": "Allow",
        "Principal": {
            "AWS": "arn:aws:iam::123456789012:user/alice"
        },
        "Action": "s3:GetObject",
        "Resource": "arn:aws:s3:::my-bucket/*"
    }]
}
```

#### ② 공개 읽기 (Static Website Hosting)

```json
{
    "Version": "2012-10-17",
    "Statement": [{
        "Sid": "PublicRead",
        "Effect": "Allow",
        "Principal": "*",
        "Action": "s3:GetObject",
        "Resource": "arn:aws:s3:::my-public-bucket/*"
    }]
}
```

⚠️ `Principal: "*"`는 **인터넷 전체에 공개**. 반드시 의도된 경우만.

#### ③ HTTPS 강제

```json
{
    "Version": "2012-10-17",
    "Statement": [{
        "Sid": "DenyInsecureConnections",
        "Effect": "Deny",
        "Principal": "*",
        "Action": "s3:*",
        "Resource": [
            "arn:aws:s3:::my-bucket",
            "arn:aws:s3:::my-bucket/*"
        ],
        "Condition": {
            "Bool": {
                "aws:SecureTransport": "false"
            }
        }
    }]
}
```

→ HTTP 요청은 모두 거부 (HTTPS만 허용).

#### ④ 특정 IP에서만 접근

```json
{
    "Version": "2012-10-17",
    "Statement": [{
        "Effect": "Allow",
        "Principal": "*",
        "Action": "s3:GetObject",
        "Resource": "arn:aws:s3:::my-bucket/*",
        "Condition": {
            "IpAddress": {
                "aws:SourceIp": ["203.0.113.0/24"]
            }
        }
    }]
}
```

#### ⑤ 다른 AWS 계정에 접근 허용

```json
{
    "Version": "2012-10-17",
    "Statement": [{
        "Effect": "Allow",
        "Principal": {
            "AWS": "arn:aws:iam::ANOTHER-ACCOUNT-ID:root"
        },
        "Action": ["s3:GetObject", "s3:ListBucket"],
        "Resource": [
            "arn:aws:s3:::my-bucket",
            "arn:aws:s3:::my-bucket/*"
        ]
    }]
}
```

### Block Public Access ⭐ (2018+ 안전 가드)

**"실수로 공개하는 사고 방지"** 기능. 4가지 설정이 **AND 조건**으로 적용:

| 설정 | 효과 |
|-----|------|
| Block **public ACLs** | ACL로 public 설정 **거부** |
| Ignore **public ACLs** | 기존 public ACL **무시** |
| Block **public bucket policies** | public bucket policy **거부** |
| Restrict **public buckets** | public bucket의 **접근 자체 거부** |

**2023+ 신규 버킷은 기본 활성화** — "모든 public access 차단" 상태.

→ 공개 버킷 필요 시 **의도적으로 해제** 필요 (실수 방지).

### Policy Evaluation 순서 (S3 접근 결정)

```
요청 발생
    ↓
① Block Public Access 체크 ──Public 요청 && 차단 설정─→ DENY
    ↓
② Explicit Deny 체크 (IAM + Bucket Policy) ─명시적 Deny─→ DENY
    ↓
③ Allow 찾기 (IAM OR Bucket Policy 중 하나라도) ─없으면─→ DENY
    ↓
    있으면 → ALLOW
```

**명시적 Deny > 명시적 Allow > 암묵적 Deny** — IAM 평가 로직과 동일.

---

## 5부. 암호화 ⭐

### Encryption at Rest (저장 시 암호화)

**모든 S3 객체는 2023년 1월부터 기본으로 암호화됨** (SSE-S3). 추가 설정 선택지:

#### ① SSE-S3 (Server-Side Encryption with S3-managed keys) — 기본

- **AWS가 전부 관리** (키 생성/로테이션/삭제)
- **AES-256**
- 추가 비용 없음
- **2023년 1월 이후 모든 버킷 기본 적용** (명시 안 해도 적용)

```
업로드 → S3가 자동 암호화 → 저장
조회  → S3가 자동 복호화 → 반환
```

#### ② SSE-KMS (SSE with AWS KMS keys)

- **KMS (Key Management Service)** 사용
- 사용자가 **키 소유권 보유** (관리자 지정)
- **CloudTrail로 사용 추적** 가능 ("누가 언제 키 사용했는지")
- **자동 로테이션** 설정 가능 (매년)
- 추가 비용 (KMS API 호출당 과금)

**사용처**:
- 감사 요구사항 있는 환경 (금융/의료)
- 키 라이프사이클 통제 필요
- 다른 계정과 키 공유

```json
// 업로드 시 요청
Headers:
  x-amz-server-side-encryption: aws:kms
  x-amz-server-side-encryption-aws-kms-key-id: arn:aws:kms:...
```

**S3 Bucket Key** (KMS 비용 최적화):
- 매 요청마다 KMS 호출 → 비싸짐
- S3 Bucket Key: 버킷 레벨 키 사용 → KMS 호출 **최대 99% 감소**

#### ③ SSE-C (Customer-provided keys)

- **사용자가 키를 직접 제공** (매 요청마다)
- AWS는 키 저장 X → 사용자가 완전히 관리
- **HTTPS 필수** (키가 전송되므로)

```
PUT /object HTTP/1.1
x-amz-server-side-encryption-customer-algorithm: AES256
x-amz-server-side-encryption-customer-key: <base64 key>
x-amz-server-side-encryption-customer-key-MD5: <MD5>
```

**사용처**: 매우 특수한 규제 요건. 실무에선 거의 안 씀.

#### ④ CSE (Client-Side Encryption)

- **애플리케이션이 암호화 후 업로드**
- AWS는 **암호화된 데이터만** 봄
- 가장 안전 (AWS조차 복호화 불가)
- 단, 애플리케이션 복잡도 ↑

**사용처**: 극도의 보안 요구 (End-to-End 암호화)

### 암호화 종류 비교

| | SSE-S3 | SSE-KMS | SSE-C | CSE |
|---|-------|---------|-------|-----|
| 키 관리자 | AWS | 사용자(KMS) | 사용자 (직접) | 사용자 (app) |
| 암호화 위치 | S3 서버 | S3 서버 | S3 서버 | 애플리케이션 |
| 감사 (CloudTrail) | ❌ | ✅ | ❌ | ❌ |
| 추가 비용 | 없음 | KMS 호출 | 없음 | 없음 |
| 복잡도 | 낮음 | 중간 | 높음 | 가장 높음 |
| 사용 빈도 | **가장 많음** | 두 번째 | 거의 안 씀 | 특수 |

### Encryption in Transit (전송 시 암호화)

- **HTTPS/TLS** — 기본 지원 (S3 endpoint는 HTTPS)
- **HTTP도 허용** → 버킷 정책으로 **HTTPS 강제** 필수:
  ```json
  "Condition": {
      "Bool": { "aws:SecureTransport": "false" }
  }
  ```
  (위 4부 예시 ③ 참조)

### 암호화 Best Practice

- [ ] 모든 버킷에 **기본 암호화** 활성 (SSE-S3 or SSE-KMS)
- [ ] 민감 데이터는 **SSE-KMS** 사용 (감사 추적)
- [ ] **HTTPS 강제** (Deny `aws:SecureTransport: false`)
- [ ] **S3 Bucket Key** 활성화 (KMS 비용 최적화)
- [ ] KMS 키 **자동 로테이션** 활성화
- [ ] 매우 민감한 데이터는 **CSE** 검토
- [ ] 암호화 변경은 **기존 객체 재업로드 or S3 Batch Operations** 필요

---

## 6부. S3 실무 체크리스트

- [ ] **버킷 네이밍**: 의미 있는 prefix (`company-env-service`)
- [ ] **Block Public Access**: 기본 활성, 공개 필요 시만 해제
- [ ] **버저닝**: 중요 데이터는 활성화 (실수 삭제 복구)
- [ ] **Lifecycle Rule**: 비용 최적화 (오래된 데이터 Glacier로)
- [ ] **암호화**: SSE-S3 기본, 민감 데이터는 SSE-KMS
- [ ] **HTTPS 강제**: `aws:SecureTransport` 조건
- [ ] **Access Logging**: 접근 로그 수집 (별도 버킷)
- [ ] **CloudTrail**: Data Events 활성화 (중요 버킷)
- [ ] **CORS**: 웹에서 접근하는 경우 명시
- [ ] **CDN**: 공개 콘텐츠는 CloudFront 앞단 (8장 참고)
- [ ] **MFA Delete**: 중요 버킷의 삭제 보호

---

## 참고 자료

- [Amazon S3 Developer Guide](https://docs.aws.amazon.com/AmazonS3/latest/userguide/)
- [S3 Storage Classes](https://aws.amazon.com/s3/storage-classes/)
- [S3 Bucket Policies Examples](https://docs.aws.amazon.com/AmazonS3/latest/userguide/example-bucket-policies.html)
- [S3 Encryption](https://docs.aws.amazon.com/AmazonS3/latest/userguide/UsingEncryption.html)
