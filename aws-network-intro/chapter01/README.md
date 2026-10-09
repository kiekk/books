### 1장. AWS 인프라

## 1부. Region, AZ, Edge — AWS 인프라 계층

AWS의 **글로벌 인프라 3단계 계층**. 성능/가용성/비용/규제를 결정하는 가장 중요한 토대.

```
┌───────────── Global ──────────────┐
│                                   │
│  ┌──── Region (리전) ────────┐    │
│  │  법적/네트워크 격리         │    │
│  │                            │    │
│  │  ┌── AZ ──┐ ┌── AZ ──┐    │    │
│  │  │ 물리  │ │ 물리  │    │    │
│  │  │ 데이터│ │ 데이터│   │    │
│  │  │센터  │ │ 센터  │    │    │
│  │  └───────┘ └───────┘    │    │
│  │                            │    │
│  │  AZ 간: <1~2ms 전용망      │    │
│  └────────────────────────────┘    │
│                                   │
│  ┌── Edge Location ──┐             │
│  │ 캐시 서버 (CDN)   │ x600+       │
│  └────────────────────┘            │
│                                   │
└───────────────────────────────────┘
```

### Region (리전)

**AWS가 서비스를 제공하는 "격리된 지리적 지역"**.

**특징**:
- 전 세계 **32+개 리전** (2024 기준)
- 각 리전은 **완전히 격리** (법적/네트워크/장애)
- Region 간 통신은 **인터넷 또는 전용 백본** (AWS 내부망)
- 리전마다 다른 Region ID (`ap-northeast-2`, `us-east-1` 등)

**리전 선택 기준** ⭐:
1. **지리적 지연** — 사용자와 가까운 리전
2. **법적 규제** — 데이터 주권 (한국 → ap-northeast-2 Seoul)
3. **가격** — 리전별 가격 다름 (us-east-1 가장 저렴)
4. **서비스 가용성** — 신규 서비스는 특정 리전만 먼저 출시 (주로 us-east-1)
5. **재해 복구** — Multi-Region 설계 시 거리 고려

**주요 리전**:
| Region ID | 이름 | 특징 |
|-----------|-----|------|
| `us-east-1` | N. Virginia | **가장 큰 리전**. 신규 서비스 첫 출시 |
| `us-west-2` | Oregon | 서부 미국 |
| `eu-west-1` | Ireland | 유럽 중심 |
| **`ap-northeast-2`** | **Seoul** | **한국** |
| `ap-northeast-1` | Tokyo | 일본 |

**[Story 참고 — 이전 책의 EC2 가용 영역 섹션](../../aws-practical-intro/chapter03/README.md#3부-ebs와-가용-영역)**

### AZ (Availability Zone, 가용 영역)

**리전 내의 "물리적으로 분리된 데이터센터"**.

**특징**:
- 리전당 **보통 3~6개 AZ** (서울은 4개: 2a, 2b, 2c, 2d)
- 각 AZ는 **독립적 전력/냉각/네트워크** → 하나 장애 나도 다른 AZ 영향 X
- **AZ 간 거리는 수십 km** (너무 멀면 지연, 너무 가까우면 재해 공유)
- **AZ 간 지연은 < 1~2 ms** (전용 광섬유)
- **AZ 이름 랜덤화** — 내 계정의 `ap-northeast-2a`와 다른 계정의 `ap-northeast-2a`는 **다른 물리 AZ**일 수 있음 (AWS가 균등 분산 위해)

**네트워크 관점 핵심**:
- **VPC는 리전 범위** (여러 AZ를 포함)
- **서브넷은 AZ 범위** (하나의 서브넷은 하나의 AZ 안에만)
- **EC2는 하나의 AZ에 생성** (여러 AZ에 복제 안 됨)
- **ELB는 여러 AZ에 분산 배치 가능**

### Edge Location (엣지 로케이션)

**캐시/분산 서버**. CDN, Route 53의 PoP(Point of Presence).

**특징**:
- 전 세계 **600+ PoP** (90+ 도시, 2024)
- 리전/AZ와는 **다른 개념** — Edge는 "**가장자리의 캐시**", AZ는 "**데이터센터**"
- 사용자 요청의 1차 접점
- 한국 Edge: Seoul (3곳), Busan

**사용 서비스**:
- **CloudFront** (CDN)
- **Route 53** (DNS)
- **Global Accelerator** (글로벌 네트워크)
- **AWS Shield** (DDoS 방어)
- **Lambda@Edge** (Edge 컴퓨팅)

**[Story 참고 — 이전 책의 CloudFront Edge Location 섹션](../../aws-practical-intro/chapter08/README.md#2부-edge-location과-origin)**

### 특수 AWS 인프라 노드

**Local Zone** — Region 하위의 더 작은 지역화 노드
```
us-west-2 (Oregon Region)
  ├─ 2a, 2b, 2c, 2d (일반 AZ)
  └─ Local Zone: Los Angeles, Houston, Boston...
```
- 특정 **도시** 단위
- 저지연 필요한 애플리케이션 (게임, 실시간 미디어)
- 리전과 저지연 전용망 연결

**Wavelength Zone** — 5G 네트워크 통합
- 통신사(Verizon, SKT 등)의 5G 기지국 내 AWS 하드웨어
- **모바일 Edge** 컴퓨팅 (실시간 AR/VR, 자율주행)

**Outposts** — On-premises에 AWS
- AWS 하드웨어 랙을 **고객 데이터센터**에 설치
- 하이브리드 클라우드 (AWS 서비스를 로컬에서)

**AWS Direct Connect Location**
- 전용선 연결점
- 고객 네트워크 ↔ AWS 사설 접속

### 계층별 가용성/복원력 설계

| 레벨 | 장애 범위 | 설계 패턴 |
|-----|---------|---------|
| 서버 1대 | 서버 1대 | **Multi-AZ** (여러 AZ에 배포) |
| AZ 전체 | AZ 1개 | **Multi-AZ** (다른 AZ로 Failover) |
| Region 전체 | 리전 전체 (매우 드묾) | **Multi-Region** (DR Region) |
| 네트워크/인터넷 | 특정 지역 사용자 | **CloudFront + Global Accelerator** |

---

## 2부. CloudFormation 개념

### CloudFormation (CFn)이란

AWS의 **Infrastructure as Code (IaC)** 서비스. "**AWS 리소스를 코드(YAML/JSON)로 정의**해서 자동 생성/관리".

```
Template (YAML/JSON 파일)
    ↓ 업로드
CloudFormation Service
    ↓ 리소스 생성/수정/삭제
Stack = Template의 실체
    │
    ├─ VPC
    ├─ EC2
    ├─ RDS
    └─ ... (수십~수백 리소스)
```

### 왜 IaC가 필요한가

**없이** (콘솔 수동):
- 환경 재현 어려움 (prod/stage 수동으로 똑같이 만들기)
- 변경 추적 안 됨 (누가 언제 뭘 바꿨는지)
- 실수 쉬움 (설정 하나 빼먹음)
- 리뷰 안 됨

**IaC**:
- **코드로 환경 정의** → Git으로 버저닝/리뷰
- **재현 가능** (같은 코드 = 같은 환경)
- **환경 복제 쉬움** (prod 코드 → stage, dev)
- **변경 이력** 자동 (git log)
- **롤백 가능**

### Template 구조

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: 'My First VPC'

Parameters:
  VpcCidr:
    Type: String
    Default: '10.0.0.0/16'
    Description: VPC CIDR 범위

Mappings:
  RegionMap:
    ap-northeast-2:
      AMI: 'ami-xxxxxxxxx'
    us-east-1:
      AMI: 'ami-yyyyyyyyy'

Conditions:
  IsProduction: !Equals [!Ref Environment, 'prod']

Resources:
  MyVPC:
    Type: AWS::EC2::VPC
    Properties:
      CidrBlock: !Ref VpcCidr
      EnableDnsSupport: true
      Tags:
        - Key: Name
          Value: my-vpc

  MySubnet:
    Type: AWS::EC2::Subnet
    Properties:
      VpcId: !Ref MyVPC
      CidrBlock: '10.0.1.0/24'
      AvailabilityZone: 'ap-northeast-2a'

Outputs:
  VpcId:
    Description: 'VPC ID'
    Value: !Ref MyVPC
    Export:
      Name: !Sub '${AWS::StackName}-VpcId'
```

**섹션 설명**:

| 섹션 | 역할 | 필수 |
|------|------|-----|
| **AWSTemplateFormatVersion** | 템플릿 버전 | 선택 |
| **Description** | 템플릿 설명 | 선택 |
| **Parameters** | 입력 변수 (stack 생성 시 입력) | 선택 |
| **Mappings** | 조건부 매핑 (리전별 AMI 등) | 선택 |
| **Conditions** | 조건 정의 (환경별 리소스 생성) | 선택 |
| **Resources** | **생성될 리소스 (필수)** | ✅ |
| **Outputs** | 결과 export (다른 stack에서 참조) | 선택 |

### 핵심 개념

**Stack**:
- Template의 **"인스턴스"**
- Template을 CloudFormation에 올리면 → Stack 생성
- Stack 단위 생성/수정/삭제
- 리전 범위

**Change Set**:
- Stack 변경 전 **미리보기**
- "이 변경이 어떤 리소스를 추가/수정/삭제할지"
- 안전한 운영 환경 변경

**Drift Detection**:
- Stack 리소스가 **콘솔로 수동 변경**됐는지 감지
- IaC 원칙 유지 (코드 ↔ 실제 상태 일치)

**Rollback**:
- Stack 생성/수정 **실패 시 자동 롤백**
- 이전 상태로 복구

**Nested Stacks**:
- Stack 안에서 다른 Stack 호출
- 모듈화 (VPC Stack, EC2 Stack, RDS Stack...)

**StackSet**:
- 여러 **Account/Region**에 같은 Stack 동시 배포
- 조직 전체 설정 (예: 모든 Account에 CloudTrail 활성화)

### 생성/업데이트 흐름

```
① Template 작성 (YAML/JSON)
    ↓
② Template Validate (문법 체크)
   aws cloudformation validate-template --template-body file://t.yaml
    ↓
③ Change Set 생성 (미리보기)
   aws cloudformation create-change-set --change-set-name my-change ...
    ↓
④ Change Set 검토 (어떤 리소스가 영향받나)
    ↓
⑤ Change Set 실행
   aws cloudformation execute-change-set --change-set-name my-change
    ↓
⑥ CREATE_IN_PROGRESS → CREATE_COMPLETE (또는 ROLLBACK)
```

### Stack 상태

| 상태 | 의미 |
|-----|------|
| CREATE_IN_PROGRESS | 생성 중 |
| CREATE_COMPLETE | 생성 완료 |
| CREATE_FAILED | 생성 실패 |
| ROLLBACK_IN_PROGRESS | 실패 → 롤백 중 |
| ROLLBACK_COMPLETE | 롤백 완료 (더 이상 수정 불가, 삭제만) |
| UPDATE_IN_PROGRESS | 업데이트 중 |
| UPDATE_COMPLETE | 업데이트 완료 |
| DELETE_IN_PROGRESS | 삭제 중 |
| DELETE_COMPLETE | 삭제 완료 |

### 내장 함수 (Intrinsic Functions)

| 함수 | 용도 | 예시 |
|-----|------|------|
| `!Ref` | Parameter나 Resource 참조 | `!Ref MyVPC` |
| `!GetAtt` | Resource의 attribute 참조 | `!GetAtt MyVPC.CidrBlock` |
| `!Sub` | 문자열 치환 | `!Sub '${AWS::StackName}-vpc'` |
| `!Join` | 문자열 결합 | `!Join ['-', [a, b, c]]` |
| `!If` | 조건 분기 | `!If [IsProd, prod-val, dev-val]` |
| `!Equals` | 비교 | `!Equals [!Ref Env, prod]` |
| `!ImportValue` | 다른 Stack의 Output 참조 | `!ImportValue 'network-stack-VpcId'` |

### Pseudo Parameters (내장 변수)

AWS가 자동 제공하는 값:
- `AWS::AccountId` — 현재 계정 ID
- `AWS::Region` — 현재 리전
- `AWS::StackName` — Stack 이름
- `AWS::StackId` — Stack ARN
- `AWS::NoValue` — 조건부로 값 생략 시

---

## 3부. CloudFormation vs 대체제 ⭐

| 도구 | 특징 | 사용처 |
|------|------|------|
| **CloudFormation** | AWS 공식, 모든 AWS 리소스 지원, YAML/JSON | AWS 단일 클라우드 |
| **Terraform** | 멀티 클라우드 (AWS/GCP/Azure/K8s), HCL 언어 | **멀티 클라우드, 사실상 표준** |
| **AWS CDK** | **TypeScript/Python/Java로 코드 작성** → CloudFormation 생성 | 개발자 친화적 |
| **Pulumi** | TypeScript/Python 등 범용 언어, 멀티 클라우드 | Terraform의 코드 버전 |
| **AWS SAM** | Serverless에 특화된 CFn 상위 레이어 | Lambda, API Gateway |
| **Serverless Framework** | Serverless 전용, 멀티 클라우드 | Lambda |

### CloudFormation vs Terraform

| 항목 | **CloudFormation** | **Terraform** |
|-----|-------------------|---------------|
| 소유 | AWS 공식 | HashiCorp (Oracle 2024 인수) |
| 지원 클라우드 | AWS만 | **멀티 클라우드** |
| 언어 | YAML/JSON | HCL |
| State 관리 | AWS가 관리 (내부) | **외부 state file** (S3 등) |
| 신규 리소스 지원 | AWS 즉시 | HashiCorp Provider 업데이트 대기 |
| 커뮤니티 | AWS 공식 문서 | **방대한 Module 생태계** |
| 가격 | 무료 | 오픈소스 무료 (Cloud 유료) |
| **현재 추세** | AWS만 쓸 때 | **멀티 클라우드 사실상 표준** |

### CloudFormation vs AWS CDK

```yaml
# CloudFormation (YAML)
Resources:
  MyBucket:
    Type: AWS::S3::Bucket
    Properties:
      BucketName: my-bucket
      VersioningConfiguration:
        Status: Enabled
```

```typescript
// AWS CDK (TypeScript)
import * as s3 from 'aws-cdk-lib/aws-s3';

new s3.Bucket(this, 'MyBucket', {
  bucketName: 'my-bucket',
  versioned: true,
});
```

→ CDK는 **코드로 작성 → CloudFormation으로 변환** → 배포. 반복 로직, 조건부 생성이 **훨씬 자연스러움**.

**현재 추세**: 신규 프로젝트는 **CDK or Terraform**. CloudFormation YAML 직접 쓰는 경우는 줄어듦.

---

## 4부. 실무 체크리스트

### IaC 사용

- [ ] **모든 환경을 코드로** (수동 콘솔 변경 금지)
- [ ] Git으로 버저닝 + 코드 리뷰
- [ ] **Multi-environment** (dev/stage/prod) 같은 템플릿 + Parameters
- [ ] Secret은 **Parameter Store / Secrets Manager** (코드에 하드코딩 금지)

### CloudFormation 사용 시

- [ ] **Change Set으로 미리보기** (운영 환경)
- [ ] **Rollback 조건** 명시 (`RollbackConfiguration`)
- [ ] **Nested Stacks**로 모듈화 (큰 템플릿 분리)
- [ ] **Drift Detection** 정기 수행
- [ ] **DeletionPolicy** 설정 (`Retain`으로 중요 리소스 보호)

### 네트워크 리소스 특히 조심

- [ ] **VPC/Subnet/Route Table**은 한 번 생성 후 수정 어려움 — 신중히 설계
- [ ] **IP 대역 중복 금지** — Multi-Account/Region 설계 시 전역 계획
- [ ] **NAT Gateway** 비용 큼 — 필요한 수만

---

## 참고 자료

- [AWS Global Infrastructure](https://aws.amazon.com/about-aws/global-infrastructure/)
- [AWS Regions and AZs](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using-regions-availability-zones.html)
- [CloudFormation User Guide](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/)
- [CloudFormation Template Reference](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-reference.html)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [AWS CDK Workshop](https://cdkworkshop.com/)
