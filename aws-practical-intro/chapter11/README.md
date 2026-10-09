### 11장. CI/CD 파이프라인

## 1부. CI/CD 개념

### CI/CD란

**Continuous Integration / Continuous Delivery (Deployment)** — 코드 변경을 **자주/자동으로** 통합·테스트·배포하는 개발 방식.

```
개발자 ─push─▶ 저장소 ─▶ 자동 빌드 ─▶ 자동 테스트 ─▶ 자동 배포 ─▶ Production
                           │              │              │
                           └── 실패 시 알림 → 수정 ───────┘
```

### CI (Continuous Integration) — 지속적 통합

**"코드 변경을 자주 통합하고 자동으로 검증"**

**실천 사항**:
- 작은 단위로 **자주 커밋/머지**
- 매 커밋마다 **자동 빌드 + 테스트**
- 통합 문제 **조기 발견**
- Pull Request 기반 코드 리뷰

**해결하는 문제**:
- "통합 지옥" (long-lived branch 머지 시 큰 충돌)
- 긴 feedback loop
- 품질 저하

### CD (Continuous Delivery) — 지속적 전달

**"언제든 production 배포 가능한 상태 유지"** (실제 배포는 수동)

- 코드가 통합/테스트 통과 → **staging 자동 배포**
- **production은 사람이 승인** 후 배포 (버튼 누르기)
- 안정성 중시 조직 (금융, 의료)

### CD (Continuous Deployment) — 지속적 배포

**"production까지 자동 배포"** (CD의 더 발전된 형태)

- 테스트 통과 → **production 즉시 배포**
- 사람 승인 없음 → 자동화 완벽해야
- Netflix, Facebook, Amazon 등이 사용

### Continuous Delivery vs Deployment

```
Delivery:   build → test → staging deploy → [수동 승인] → prod deploy
Deployment: build → test → staging deploy → [자동]      → prod deploy
```

**신뢰도 레벨**:
- 수동 배포 → Continuous Delivery → Continuous Deployment
- 보통 조직은 **Continuous Delivery까지가 현실적**

### CI/CD의 가치

- **빠른 피드백** — 문제를 커밋 직후 발견
- **배포 리스크 감소** — 작은 변경, 롤백 쉬움
- **배포 빈도 증가** — 월 1회 → 일 10회+
- **품질 향상** — 자동 테스트 강제
- **개발자 생산성** — 수동 작업 제거

---

## 2부. AWS CodeCommit — 현재 상태 ⚠️

### CodeCommit 개념

AWS의 **Git 호환 private 저장소 서비스**. GitHub/GitLab과 유사한 역할.

**특징**:
- Git 프로토콜 호환 (git clone/push/pull 그대로)
- Private repository (public 호스팅 불가)
- IAM 통합 (사용자/권한 관리)
- Encryption at rest (KMS)
- Multi-region 지원
- CloudTrail 감사

### ⚠️ 2024년 7월 — 신규 사용자 받지 않음

**중요한 변화**: AWS는 2024년 7월 25일 **CodeCommit에 신규 고객을 받지 않는다**고 공식 발표.

> *"AWS stopped onboarding new customers to AWS CodeCommit"*

- 2024-07-25 이전 사용자: **계속 사용 가능**
- 신규 AWS 계정: **CodeCommit 리포 생성 불가**
- 실질적으로 **"deprecated 상태"**

**함께 중단된 서비스**:
- **AWS CodeStar** (완전 지원 중단, 2024-07-31)
- **AWS Cloud9** (신규 가입 중단, EOL 예정)
- **AWS CodeCommit** (신규 가입 중단)
- **AWS CloudSearch** (신규 가입 중단)

→ 책의 CodeCommit 실습은 **역사적 자료**로 볼 것. 실무에선 GitHub/GitLab 사용.

### CodeCommit vs GitHub 비교

| 항목 | **CodeCommit** | **GitHub** |
|-----|---------------|-----------|
| **현재 상태** | ⚠️ **신규 가입 중단 (2024-07)** | ✅ 활발 (사실상 표준) |
| 소유 | AWS | Microsoft (2018 인수) |
| 저장소 유형 | Private만 | Private + Public |
| 사용자 수 | 소수 (AWS 환경 전용) | 1억+ (전 세계) |
| 커뮤니티 | 없음 | **방대한 오픈소스 생태계** |
| 인증 | IAM | GitHub 계정 (SSO 가능) |
| 요금 | 사용자 5명까지 무료, 이후 과금 | 무료 (private 포함, 제한적) |
| **CI/CD** | CodePipeline 통합 | **GitHub Actions** (강력, 유연) |
| **Pull Request 리뷰** | 기본적 | 풍부한 기능 (Draft PR, Suggestion 등) |
| **Code Search** | 기본적 | 전역 검색, Copilot 통합 |
| **IDE 통합** | AWS IDE 플러그인 | **거의 모든 IDE 네이티브 지원** |
| **외부 서비스 통합** | 제한적 (AWS 생태계) | **수천 개 marketplace apps** |
| **Issue / Project Mgmt** | ❌ | ✅ Issues, Projects, Milestones |
| **Wiki / Pages** | ❌ | ✅ |

### 왜 CodeCommit이 사장됐나

1. **GitHub 생태계 압도** — Marketplace, Actions, Copilot
2. **커뮤니티 부재** — AWS 사용자만, 네트워크 효과 없음
3. **기능 격차** — Pull Request, Code Search, Discussions 등 뒤처짐
4. **Microsoft의 GitHub 인수** (2018) → 투자 가속화
5. **AWS 자체도 GitHub 사용** — Amplify, SAM 등 소스를 GitHub에

**현재 추천**:
- **GitHub** — 사실상 표준
- **GitLab** — 자체 호스팅 가능, DevOps 통합
- **Bitbucket** — Atlassian 생태계

---

## 3부. AWS CodeDeploy

### CodeDeploy 개념

**자동 배포 서비스**. 다양한 플랫폼에 **애플리케이션을 자동으로 배포**.

**지원 플랫폼**:
- **EC2 / On-premises** — 서버에 애플리케이션 설치
- **Lambda** — 새 함수 버전 트래픽 전환
- **ECS** — 컨테이너 서비스 업데이트

### 핵심 개념

**Application**: 배포 대상 애플리케이션 (논리적 그룹)
**Deployment Group**: 배포될 서버/서비스 묶음 (환경 — dev, staging, prod)
**Deployment**: 특정 revision을 배포하는 작업
**Revision**: 배포될 코드 버전 (S3 ZIP, GitHub tag 등)

### appspec.yml — 배포 설정 파일

```yaml
version: 0.0
os: linux

files:
  - source: /
    destination: /opt/myapp

permissions:
  - object: /opt/myapp
    mode: 755

hooks:
  BeforeInstall:
    - location: scripts/stop_server.sh
      runas: root
  AfterInstall:
    - location: scripts/install_deps.sh
  ApplicationStart:
    - location: scripts/start_server.sh
  ValidateService:
    - location: scripts/health_check.sh
      timeout: 300
```

**Hook 단계**:
1. **BeforeInstall** — 설치 전 (서비스 중단, 백업)
2. **AfterInstall** — 설치 후 (의존성 설치, 설정 변경)
3. **ApplicationStart** — 서비스 시작
4. **ValidateService** — 정상 동작 검증 (health check)

### 배포 전략 5가지

#### ① In-Place Deployment

```
배포 전: [server1 v1] [server2 v1]
배포 중: [server1 v2 설치중...] [server2 v1]
배포 후: [server1 v2] [server2 v2]
```

- **기존 서버에 새 버전 설치**
- 설치 중 **다운타임 가능**
- 리소스 효율적 (추가 서버 없음)
- 롤백 느림 (재배포)

#### ② Blue/Green Deployment ⭐

```
Blue (v1 운영): [server1 v1] [server2 v1]
Green (v2 준비): [server3 v2] [server4 v2]
    ↓ 트래픽 전환 (ELB)
Green이 운영: [server3 v2] [server4 v2] (사용자 트래픽)
Blue는 대기: [server1 v1] [server2 v1] (즉시 롤백 가능)
    ↓ 안정 확인
Blue 종료
```

- **새 환경(Green) 구성 → 트래픽 전환**
- **다운타임 없음**
- **즉시 롤백 가능** (트래픽만 Blue로 되돌림)
- 리소스 2배 (잠깐)

#### ③ Canary Deployment

```
Step 1: 10% 트래픽 → v2
Step 2: (수 분 대기 + 모니터링)
Step 3: 100% 트래픽 → v2
```

- **일부 트래픽만** 새 버전으로
- 문제 발견 시 **영향 최소화**
- 점진적 확대

#### ④ Linear Deployment

```
10% every 1 min:  10% → 20% → 30% ... → 100%
```

- **일정 비율로 균등 증가**
- Lambda/ECS에 흔히 사용

#### ⑤ AllAtOnce

- **모든 서버에 동시 배포**
- 가장 빠름, 가장 위험
- 다운타임 발생

### Rollback (자동 롤백)

- **CloudWatch Alarm 트리거 시** 자동 롤백
- 수동 롤백도 가능
- Blue/Green은 **매우 빠른 롤백** (트래픽만 전환)

### CodeDeploy의 현재 가치

CodeCommit은 사장됐지만 **CodeDeploy는 여전히 유효**:
- **ECS Blue/Green 배포**의 공식 메커니즘
- **Lambda 트래픽 전환**에도 사용
- **EC2 배포 자동화** (레거시 포함)

---

## 4부. AWS CodePipeline

### CodePipeline 개념

**CI/CD 파이프라인 Orchestration 서비스**. 여러 단계(Source → Build → Test → Deploy)를 **연결**하고 **자동 실행**.

```
┌──────────┬──────────┬──────────┬──────────┐
│  Source  │  Build   │  Test    │  Deploy  │  ← Stage
├──────────┼──────────┼──────────┼──────────┤
│CodeCommit│CodeBuild │CodeBuild │CodeDeploy│  ← Action Provider
│  GitHub  │ Jenkins  │          │          │
│    S3    │          │          │          │
└──────────┴──────────┴──────────┴──────────┘
```

### 구성 요소

**Pipeline**: 전체 플로우 하나
**Stage**: 파이프라인의 단계 (Source, Build, Deploy...)
**Action**: Stage 내 실행 단위
**Transition**: Stage 간 자동/수동 전환
**Artifact**: Stage 간 전달되는 결과물 (S3에 저장)

### Action Provider (각 단계별 가능한 도구)

| Stage | Provider |
|------|---------|
| **Source** | CodeCommit (레거시), **GitHub**, Bitbucket, S3, ECR, CodeStar Connections |
| **Build** | **CodeBuild**, Jenkins, TeamCity |
| **Test** | CodeBuild, Device Farm (모바일), 3rd party |
| **Deploy** | **CodeDeploy**, CloudFormation, ECS, S3, Elastic Beanstalk, Lambda |
| **Approval** | Manual Approval (사람 승인) |
| **Invoke** | Lambda, Step Functions |

### 전형적 Pipeline 예시

**Serverless Web 서비스**:
```
① Source: GitHub (push에 반응)
② Build: CodeBuild
    - npm install, npm run build, 테스트 실행
    - CloudFormation 템플릿 생성
③ Deploy to Staging: CloudFormation
    - API Gateway + Lambda + DynamoDB 업데이트
④ Manual Approval: 담당자 승인 대기
⑤ Deploy to Production: CloudFormation
    - prod 환경 업데이트
```

### CodePipeline vs GitHub Actions

| 항목 | **CodePipeline** | **GitHub Actions** |
|-----|------------------|-------------------|
| 호스팅 | AWS | GitHub |
| 설정 | AWS Console / CDK | `.github/workflows/*.yml` (코드) |
| AWS 통합 | **네이티브** | OIDC로 가능 |
| 외부 통합 | 제한적 | **방대한 marketplace** |
| 가격 | Pipeline당 월 $1 | Public 무료, Private 월 2000분 무료 |
| 복잡도 | 낮음 | 중간 |
| 유연성 | 중간 | **매우 높음** |
| 사용 추세 | ⚠️ 감소 | ✅ **증가 (사실상 표준)** |

**현재 추천**:
- **GitHub 리포 사용 중** → GitHub Actions
- **AWS 깊은 통합** 필요 (CloudFormation, 복잡한 CodeDeploy) → CodePipeline
- **혼합**: GitHub Actions가 빌드 → CodePipeline이 배포 orchestration

---

## 5부. AWS CodeStar 서비스 패밀리 전체 요약

| 서비스 | 역할 | 현재 상태 |
|-------|------|---------|
| **CodeCommit** | Git 저장소 | ⚠️ 신규 가입 중단 (2024-07) |
| **CodeBuild** | 빌드/테스트 | ✅ 활발 |
| **CodeDeploy** | 자동 배포 | ✅ 활발 |
| **CodePipeline** | CI/CD orchestration | ✅ 활발 |
| **CodeArtifact** | 아티팩트 저장소 (npm, Maven) | ✅ 활발 |
| **CodeGuru** | 코드 리뷰/프로파일링 (AI) | ✅ 활발 |
| **CodeStar** | 통합 템플릿 | ❌ **완전 종료 (2024-07)** |

**현재 조합 패턴 (2026)**:

**AWS 중심**:
```
GitHub → CodeBuild → CodeDeploy / ECS / Lambda
         (buildspec.yml)   (appspec.yml)
         
Orchestration: CodePipeline or GitHub Actions
```

**GitHub Actions 중심**:
```
GitHub Actions → AWS (OIDC로 인증) → ECR → ECS/Lambda
                 (yml 하나로 전부)
```

---

## 6부. 현대적 CI/CD 조합 추천 (2026)

### Serverless 애플리케이션

```
GitHub → GitHub Actions
       └─ build + test
       └─ CloudFormation / SAM / CDK deploy
```

### 컨테이너 애플리케이션 (ECS/EKS)

```
GitHub → GitHub Actions
       └─ Docker build + push to ECR
       └─ ECS task definition 업데이트
       └─ CodeDeploy Blue/Green (optional)
```

### 전통적 EC2

```
GitHub → GitHub Actions (or CodePipeline)
       └─ ZIP build
       └─ S3 upload
       └─ CodeDeploy to EC2 (In-Place / Blue-Green)
```

### 멀티 환경 배포

```
dev 자동 → staging 자동 → [Manual Approval] → prod
                                              ├─ Canary 10%
                                              └─ 안정 확인 → 100%
```

---

## 7부. CI/CD Best Practice

### 파이프라인 설계

- [ ] **매 커밋마다 자동 실행** (not 수동)
- [ ] **빠른 피드백** (전체 10분 이내 목표)
- [ ] **병렬 실행** (test 종류별, 플랫폼별)
- [ ] **환경 분리** (dev / staging / prod)
- [ ] **Artifact 재사용** — 한 번 빌드, 여러 환경에 배포 (test, staging, prod에 **같은 artifact**)

### 테스트

- [ ] Unit test (빠름, 많이)
- [ ] Integration test (DB/외부 서비스 연계)
- [ ] E2E test (실제 사용자 플로우)
- [ ] Smoke test (prod 배포 후)
- [ ] 테스트 피라미드 지키기 (unit 많이, E2E 적게)

### 배포

- [ ] **Blue/Green or Canary** (In-Place 지양)
- [ ] **자동 롤백** (CloudWatch Alarm 트리거)
- [ ] **Feature Flag** 활용 (코드 배포 ≠ 기능 활성화)
- [ ] **IaC (Infrastructure as Code)** — CloudFormation, Terraform, CDK
- [ ] **Secrets Manager / Parameter Store** (git에 secret 금지)

### 보안

- [ ] **OIDC 인증** (long-lived Access Key 금지)
- [ ] **최소 권한** (배포용 Role은 꼭 필요한 권한만)
- [ ] **SAST** (static analysis) — 코드 취약점 스캔
- [ ] **의존성 취약점 스캔** (Dependabot, Snyk)
- [ ] **Container image 스캔** (ECR Image Scan, Trivy)

---

## 참고 자료

- [AWS CodePipeline Guide](https://docs.aws.amazon.com/codepipeline/latest/userguide/)
- [AWS CodeDeploy Guide](https://docs.aws.amazon.com/codedeploy/latest/userguide/)
- [AWS CodeCommit — No Longer Onboarding (2024-07)](https://aws.amazon.com/blogs/devops/how-to-migrate-your-aws-codecommit-repository-to-another-git-provider/)
- [GitHub Actions AWS](https://github.com/aws-actions)
- "Continuous Delivery" by Jez Humble & David Farley — 고전
