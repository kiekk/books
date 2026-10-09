### 2장. IAM

## 1부. IAM 개념과 목적

#### IAM (Identity and Access Management)이란

AWS 리소스에 대한 **접근 제어** 서비스. "**누가(Who) 무엇을(What) 어떻게(How) 사용할 수 있는가**"를 정의.

**특징**:
- **전역 서비스** — region 무관 (US East, Seoul 등 상관없이 하나의 IAM 설정)
- **무료** (모든 IAM 기능)
- AWS 모든 서비스의 **보안 기반**

#### 왜 필요한가 — 3가지 목적

1. **인증 (Authentication)** — 요청자가 **누구인지** 확인
   - Access Key, 비밀번호, MFA 등으로 신원 증명

2. **인가 (Authorization)** — 그 요청자가 **어떤 작업이 가능한지** 결정
   - Policy로 허용/거부 규칙 명시

3. **감사 (Audit)** — **누가 언제 뭘 했는지** 기록
   - CloudTrail과 연계해 모든 API 호출 로그

#### 최소 권한 원칙 (Principle of Least Privilege, PoLP) ⭐

**핵심 원칙**: 각 identity에게 **업무 수행에 필요한 최소한의 권한만** 부여.

**왜 중요한가**:
- **보안 사고 영향 최소화** — 자격 증명 유출 시 피해 범위 제한
- **실수 방지** — 잘못된 작업으로 인한 사고 가능성 감소
- **감사 용이** — 각 identity의 책임 범위 명확

**실천 방법**:
- 처음엔 **거부가 기본**, 필요한 것만 허용
- `*` 와일드카드 사용 자제 (`Action: "*"`, `Resource: "*"` 금지)
- 정기적인 권한 리뷰 (Access Analyzer 활용)
- **Permission Boundary**로 상한선 설정
- 임시 작업엔 **Role**로 임시 권한 (영구 User 피하기)

**Bad / Good 예시**:
```json
// Bad ❌ — 모든 S3 작업 허용
{
  "Effect": "Allow",
  "Action": "s3:*",
  "Resource": "*"
}

// Good ✅ — 특정 버킷의 읽기만
{
  "Effect": "Allow",
  "Action": ["s3:GetObject", "s3:ListBucket"],
  "Resource": [
    "arn:aws:s3:::my-bucket",
    "arn:aws:s3:::my-bucket/*"
  ]
}
```

---

## 2부. IAM이 관리하는 4가지 리소스

### ① User (사용자)

**AWS 계정 내 개별 사용자**:
- 각 사용자마다 **자격 증명** 보유:
  - 비밀번호 (콘솔 접근)
  - Access Key ID + Secret Access Key (API/CLI 접근)
- 사람 1명 = IAM User 1개 (팀원이 10명이면 10개 User)

**주의**:
- **Root account 사용 지양** — 모든 권한 가진 "원래 계정"
- Root는 **IAM User 생성 + 결제 설정**에만 쓰고, 평소엔 IAM User로 로그인
- Root에 **MFA 필수**

### ② Group (그룹)

**사용자들의 묶음** — 역할/팀별로 묶어서 정책 일괄 관리.

```
"Developers" 그룹 (정책: EC2 full, S3 read)
├─ user: alice
├─ user: bob
└─ user: charlie
```

**장점**:
- 신규 입사자 → 그룹에 추가만 하면 됨
- 권한 변경 → 그룹 정책만 수정 (10명 유저에 각자 수정 X)
- 조직 변경 시 유저를 그룹 간 이동

**제약**:
- Group은 **중첩 불가** (그룹 안에 그룹 X)
- User는 **여러 그룹에 속할 수 있음** (최대 10개)

### ③ Role (역할) ⭐ 가장 중요한 리소스

**임시 자격 증명을 발급받는 신원** — "AssumeRole"로 역할을 "맡는다".

**User와의 차이**:
| | User | Role |
|---|-----|-----|
| 자격 증명 | 영구 (Access Key) | **임시** (STS 토큰, 수 분~수 시간) |
| 소속 | 사람 | 서비스/코드/다른 계정 |
| 유출 위험 | 큼 (장기 유효) | 작음 (수명 짧음) |

**주요 사용 사례**:

**(1) EC2 Instance Role** — EC2에서 다른 AWS 서비스 접근
```
EC2 인스턴스 → Role attach
             ↓
             자동으로 임시 자격 증명 획득 (metadata 서비스)
             ↓
             S3, DynamoDB 등 호출 (Access Key 하드코딩 불필요)
```

**(2) Lambda Execution Role** — Lambda 함수의 실행 권한
- 모든 Lambda는 Execution Role 보유
- 함수가 S3/DynamoDB/SQS 등 호출할 때 이 Role 사용

**(3) Cross-Account Access** — 다른 AWS 계정 접근
```
Account A의 User → Account B의 Role Assume
                   ↓
                   Account B의 리소스 접근
```

**(4) Federation** — 외부 SSO (Google, Okta 등)
- 사내 SSO 사용자가 임시로 AWS Role 부여받아 접근

**핵심 장점 — 왜 User보다 Role이 선호되는가**:
- **장기 Access Key 불필요** → 유출 위험 ↓
- **자동 로테이션** (STS가 임시 토큰 발급)
- **조건부 접근** (MFA, IP, 시간 제한)
- 사용자가 바뀌어도 Role은 그대로

### ④ Policy (정책)

**JSON 문서로 권한 명시**. User/Group/Role에 attach해서 권한 부여.

**Policy 구조**:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AllowS3Read",
            "Effect": "Allow",
            "Action": [
                "s3:GetObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::my-bucket",
                "arn:aws:s3:::my-bucket/*"
            ],
            "Condition": {
                "IpAddress": {
                    "aws:SourceIp": "203.0.113.0/24"
                }
            }
        }
    ]
}
```

**핵심 필드**:

| 필드 | 의미 |
|-----|------|
| `Version` | **반드시 `"2012-10-17"`** (최신 문법) |
| `Statement` | 권한 규칙 리스트 (여러 개 가능) |
| `Sid` | Statement ID (선택, 가독성용) |
| `Effect` | **`Allow`** or **`Deny`** |
| `Action` | 어떤 작업 (`s3:GetObject`, `ec2:*`, `*`) |
| `Resource` | 어떤 자원 (ARN 형식) |
| `Condition` | 추가 조건 (IP, 시간, MFA 여부, 태그 등) |

**Policy 종류**:

| 종류 | 특징 | 쓸 때 |
|-----|------|------|
| **AWS Managed** | AWS가 제공, 유지보수 | `AmazonS3ReadOnlyAccess` 같은 표준 역할 |
| **Customer Managed** | 사용자가 작성, 재사용 가능 | 조직 특유 권한. **가장 권장** |
| **Inline** | identity에 직접 종속, 재사용 X | 1회성, 다른 identity와 공유 금지 |

**Policy Evaluation Logic (평가 순서)** ⭐:

```
1. 기본값: 모든 요청은 암묵적으로 DENY (default deny)
2. 적용되는 모든 정책 수집 (User + Group + Role + SCP + Resource Policy + Boundary)
3. 명시적 Deny 하나라도 있으면 → DENY (즉시 종료)
4. 명시적 Allow 있으면 → ALLOW
5. 아무 Allow 없으면 → DENY (implicit deny)
```

→ **명시적 Deny > 명시적 Allow > 암묵적 Deny**. Deny가 항상 승리.

---

## 3부. 정책 시뮬레이터로 테스트 ⭐

IAM Policy Simulator — 정책이 실제로 어떻게 평가되는지 **운영 환경에 영향 주지 않고 테스트**.

#### 콘솔 사용법

**접속**: https://policysim.aws.amazon.com

**순서**:
1. 좌측에서 테스트할 **User / Group / Role** 선택
2. 해당 identity에 attach된 정책 자동 로드
3. **Service 선택** (예: S3)
4. **Action 선택** (예: GetObject, ListBucket)
5. **Resource ARN 입력** (예: `arn:aws:s3:::my-bucket/file.txt`)
6. (옵션) Condition 추가 — IP, MFA, 태그 등 시뮬레이션
7. **"Run Simulation"** 클릭
8. 결과:
   - **allowed** ✅ or **denied** ❌
   - **어떤 Statement**가 결정했는지 추적 가능

**활용 포인트**:
- 정책 변경 **전** 영향 확인
- "이 user가 이 작업 할 수 있나?" 디버깅
- 복잡한 정책 조합 (SCP + Permission Boundary + Policy) 결과 미리 확인

#### CLI 사용법

```bash
# User의 action 테스트
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:user/alice \
  --action-names s3:GetObject \
  --resource-arns arn:aws:s3:::my-bucket/file.txt

# 결과 (JSON)
{
    "EvaluationResults": [
        {
            "EvalActionName": "s3:GetObject",
            "EvalResourceName": "arn:aws:s3:::my-bucket/file.txt",
            "EvalDecision": "allowed",
            "MatchedStatements": [...]
        }
    ]
}
```

**정책 직접 테스트** (identity 없이):
```bash
aws iam simulate-custom-policy \
  --policy-input-list file://policy.json \
  --action-names s3:GetObject \
  --resource-arns arn:aws:s3:::my-bucket/file.txt
```

→ 정책 **배포 전**에 미리 로컬 JSON으로 테스트.

#### 조건부(Condition) 시뮬레이션

```bash
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::123456789012:user/alice \
  --action-names s3:GetObject \
  --resource-arns arn:aws:s3:::my-bucket/file.txt \
  --context-entries ContextKeyName=aws:SourceIp,ContextKeyType=ip,ContextKeyValues=203.0.113.5
```

→ **"특정 IP에서 접근했을 때" 결과** 시뮬레이션.

#### 실무 활용 시나리오

| 상황 | 시뮬레이션으로 확인 |
|-----|-------------------|
| 신규 정책 배포 전 | 기존 user가 영향받는지 |
| "접근 안 됨" 문제 디버깅 | 어떤 Deny가 가로막는지 |
| Permission Boundary 설계 | 특정 action이 Boundary를 넘는지 |
| Cross-account 설정 | Role Assume + 리소스 접근 체인 |
| 민감 리소스 보호 확인 | 특정 user가 못 지우는지 (`Deny` 명시 후) |

---

## 4부. 실무 Best Practice

#### 자격 증명 보안

- [ ] **Root account**: MFA 설정, 평상시 사용 금지
- [ ] **IAM User**: MFA 활성화
- [ ] **Access Key**: 코드에 하드코딩 금지 (환경변수, Secrets Manager, Role 사용)
- [ ] **Access Key rotation**: 90일마다 교체 권장
- [ ] **Unused credentials**: 90일 이상 미사용 Access Key 비활성화

#### 권한 설계

- [ ] **Group 사용**: User마다 정책 attach 금지
- [ ] **Role 선호**: EC2/Lambda 등 서비스는 **반드시 Role** (Access Key 하드코딩 X)
- [ ] **Customer Managed Policy** 선호 (Inline 지양, 재사용성)
- [ ] **`*` 와일드카드 피하기**
- [ ] **Permission Boundary** 활용 (상한선 설정)
- [ ] **Access Analyzer** 활용 (사용 안 하는 권한 자동 발견)

#### 모니터링

- [ ] **CloudTrail** 활성화 (모든 API 호출 기록)
- [ ] **Credential Report** 정기 확인 (IAM User 상태)
- [ ] **IAM Access Advisor** — 각 user/role이 **실제로 쓴** 서비스 조회 → 미사용 권한 제거

#### 조직 레벨 (AWS Organizations)

- [ ] **SCP (Service Control Policy)** — 계정 전체 상한선
- [ ] **AWS SSO** / **IAM Identity Center** 사용 (장기 IAM User 지양)
- [ ] **Account 분리** — 프로덕션/스테이징/개발 account 별도

---

## 참고 자료

- [IAM User Guide](https://docs.aws.amazon.com/IAM/latest/UserGuide/)
- [IAM Policy Reference](https://docs.aws.amazon.com/IAM/latest/UserGuide/reference_policies.html)
- [IAM Policy Simulator](https://policysim.aws.amazon.com/)
- [IAM Best Practices](https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html)
- [AWS Access Analyzer](https://docs.aws.amazon.com/IAM/latest/UserGuide/what-is-access-analyzer.html)
