### 3장. EC2

## 1부. EC2 개념

#### EC2 (Elastic Compute Cloud)

AWS의 **가상 머신(VM) 서비스**. "클라우드에서 Linux/Windows 서버를 빌려 쓰는 것".

**핵심 개념**:
- **인스턴스(Instance)** — 실행 중인 VM 하나
- **AMI (Amazon Machine Image)** — OS + 사전 설치된 소프트웨어 템플릿. 인스턴스 복제의 기반
- **인스턴스 타입** — CPU/메모리/네트워크 조합 (예: `t3.micro`, `m5.large`, `c5.xlarge`)
- **키 페어** — SSH 접근용 공개키/비공개키
- **보안 그룹(Security Group)** — 가상 방화벽 (인바운드/아웃바운드 규칙)

#### 인스턴스 타입 패밀리

| 패밀리 | 특징 | 적합한 워크로드 |
|-------|-----|-------------|
| **t** (burstable) | CPU 크레딧 기반, 평상시 저가 | 개발/테스트, 트래픽 변동 큰 웹 |
| **m** (general purpose) | CPU/메모리 균형 | 범용 애플리케이션 서버 |
| **c** (compute optimized) | 고성능 CPU | 배치, 미디어 인코딩, 과학 계산 |
| **r** (memory optimized) | 많은 메모리 | 인메모리 DB, Redis, Elasticsearch |
| **i** (storage optimized) | NVMe SSD 로컬 스토리지 | NoSQL, 분산 파일 시스템 |
| **g / p** (GPU) | GPU 포함 | ML 학습/추론, 그래픽 |

**이름 규칙**: `<family><generation>.<size>` — 예: `m5.large` = m 패밀리, 5세대, large 크기.

---

## 2부. EC2 지불 방식 — OnDemand / Reserved / Spot

| | OnDemand | Reserved | Spot |
|---|---------|----------|------|
| **약정** | 없음 | **1년 or 3년** | 없음 |
| **할인율** | 0% (기본가) | **최대 72%** | **최대 90%** |
| **중단 위험** | 없음 (사용자가 종료) | 없음 | ⚠️ AWS가 2분 전 알림 후 회수 |
| **유연성** | 매우 높음 | 낮음 | 매우 높음 (단 가용성 변동) |
| **사용 사례** | 짧은 사용, 테스트, 예측 불가 트래픽 | 상시 운영 서버 | 배치, 분산 처리, fault-tolerant |

#### ① OnDemand (온디맨드)

- **시간/초 단위 과금** (Linux 1초, Windows 1시간)
- 가장 **비싸지만 가장 유연**
- 쓸 때만 지불, 안 쓸 때 종료 (`stop` or `terminate`)
- **사용처**: 개발/테스트, 트래픽 예측 불가, 신규 서비스

#### ② Reserved Instance (예약 인스턴스, RI)

- **1년 or 3년 약정** → 큰 할인
- **최대 72% 할인** (3년 전체 선결제 기준)

**종류**:
- **Standard RI** — 큰 할인, 인스턴스 타입 변경 불가
- **Convertible RI** — 할인율 낮지만 **인스턴스 타입 변경 가능**

**지불 방식 3가지**:
- **All Upfront** — 전체 선결제 (최대 할인)
- **Partial Upfront** — 일부 선결제
- **No Upfront** — 선결제 없이 월 균등

**사용처**: 베이스라인 트래픽 (24/7 상시 운영 서버)

**(참고) Savings Plans** — RI의 유연한 버전
- "**시간당 $N 사용 약정**" (인스턴스 특정 X)
- EC2 Savings Plans / Compute Savings Plans
- 더 유연, 할인율은 RI 수준

#### ③ Spot Instance (스팟 인스턴스)

- **AWS 유휴 리소스 활용** → 최대 **90% 할인**
- **중단 가능**: AWS가 2분 전 알림 후 회수 (가격 급등 or 수요 급증 시)
- **중단 처리 메커니즘**: `aws:ec2:instance-interruption-notification` 이벤트 → 2분 내 graceful shutdown

**사용 사례** (fault-tolerant 작업):
- 배치 처리 (ETL, 리포트 생성)
- 분산 작업 (Hadoop, Spark)
- CI/CD 빌드
- 미디어 변환
- ML 학습 (checkpoint 기반)

**사용처 X**:
- DB 서버
- 상태 보유 서비스
- 중단 시 복구 비용 큰 작업

#### 3가지 조합 전략

```
베이스라인 (24/7 상시):    Reserved Instance (가격 효율)
Peak 처리 (낮에만):         OnDemand (유연성)
배치/분산 작업:              Spot (최대 할인)
```

실무에선 **Mix해서** 사용. Auto Scaling Group의 "Mixed Instances Policy" 활용.

---

## 3부. EBS와 가용 영역

### EBS (Elastic Block Store)

**EC2에 attach하는 블록 스토리지** — "서버의 하드디스크" 역할.

**특징**:
- **독립 리소스**: EC2가 종료돼도 EBS는 유지 가능 (`DeleteOnTermination=false`)
- **특정 AZ에 종속** — 다른 AZ의 EC2에 attach 불가
- **한 번에 하나의 EC2에만 attach** (일반적. io1/io2 Multi-Attach는 예외)
- **Snapshot**으로 백업 → S3에 저장 → 다른 AZ로 복원 가능
- **암호화 지원** (KMS 기반)

**비유**: EC2 = 컴퓨터 본체, EBS = 하드디스크. 본체 바꿔도 디스크 꽂으면 데이터 보존.

### 가용 영역 (Availability Zone, AZ)

**Region 안의 물리적으로 분리된 데이터센터**.

```
Region (지역, 예: ap-northeast-2 서울)
├─ AZ: ap-northeast-2a   ← 데이터센터 1
├─ AZ: ap-northeast-2b   ← 데이터센터 2
├─ AZ: ap-northeast-2c   ← 데이터센터 3
└─ AZ: ap-northeast-2d   ← 데이터센터 4
```

**특징**:
- 같은 Region 내 AZ 간은 **고속/저지연 네트워크** 연결
- 각 AZ는 **독립적 전력/냉각/네트워크** → 하나 장애 나도 다른 AZ 영향 X
- **고가용성(HA) 설계의 기본 단위**

**AZ 설계 패턴**:
```
Multi-AZ 설계:
Load Balancer (여러 AZ)
  ├─ EC2 (AZ-a) + EBS
  ├─ EC2 (AZ-b) + EBS
  └─ EC2 (AZ-c) + EBS
RDS Multi-AZ (자동 Failover)
```

→ 1개 AZ 장애 시에도 서비스 유지.

### EBS 타입

**SSD 계열** (랜덤 I/O, DB):

| 타입 | 특징 | 사용처 |
|-----|------|------|
| **gp2** | Gen 2 범용 SSD (레거시) | gp3로 전환 권장 |
| **gp3** | **Gen 3 범용 SSD** (현재 기본) | 대부분의 워크로드 |
| **io1** | Provisioned IOPS, 고성능 | 고성능 DB |
| **io2** | io1의 개선판, 내구성 향상 | 미션 크리티컬 DB |
| **io2 Block Express** | 극고성능 (SAP HANA 등) | 전문 워크로드 |

**HDD 계열** (순차 I/O, 비용 효율):

| 타입 | 특징 | 사용처 |
|-----|------|------|
| **st1** | Throughput Optimized HDD | 빅데이터, 로그 분석 |
| **sc1** | Cold HDD, 최저가 | 저빈도 접근 아카이브 |

### gp2 vs gp3 ⭐ (사용자님 요청)

| 항목 | gp2 | gp3 |
|-----|-----|-----|
| **출시** | 2014 | 2020 |
| **IOPS 산정** | **스토리지 크기에 비례** (3 IOPS/GB) | **기본 3000 IOPS 보장** (크기 무관) |
| **IOPS 최대** | 16,000 (5,334 GB 이상 시) | 16,000 (별도 구매) |
| **Throughput** | 128~250 MB/s (크기에 비례) | **기본 125 MB/s 보장** |
| **Throughput 최대** | 250 MB/s | 1,000 MB/s (별도 구매) |
| **IOPS 커스터마이즈** | ❌ (스토리지 크기로만 조절) | ✅ **IOPS 따로 구매** |
| **Throughput 커스터마이즈** | ❌ | ✅ **Throughput 따로 구매** |
| **가격** | 기본가 | **~20% 저렴** |

**핵심 차이**:
- **gp2**: "작은 디스크 = 느린 IOPS" (크기와 성능 결합). 성능 더 원하면 **크기 늘리는 수밖에**.
- **gp3**: "**크기와 성능 분리**". 100 GB짜리라도 16000 IOPS 설정 가능. 필요한 만큼만 지불.

**gp2 → gp3 전환**:
```bash
aws ec2 modify-volume --volume-id vol-xxx --volume-type gp3
```
- **다운타임 없음** (ModifyVolume)
- 즉시 **~20% 비용 절감**
- 대부분 워크로드엔 gp3 기본 성능으로 충분

→ 신규 EBS는 **gp3 기본**. 기존 gp2는 전환 검토.

**(참고) io1 vs io2**:
- io2는 io1 대비 **내구성 100x** (`99.999%` → `99.9999%`)
- 가격 동일하거나 저렴
- 신규 사용은 **io2 권장**

---

## 4부. ELB (Elastic Load Balancer)

**트래픽을 여러 백엔드로 분산**하는 서비스. 3가지 타입 + 1가지 특수 타입.

#### 타입 비교

| 타입 | Layer | 라우팅 기준 | 사용처 |
|-----|-------|-----------|------|
| **CLB** (Classic LB) | L4 + L7 | 기본적 (레거시) | 신규 비권장 |
| **ALB** (Application LB) | **L7** (HTTP/HTTPS) | URL path, host, header, method | 웹 서비스 (일반적) |
| **NLB** (Network LB) | **L4** (TCP/UDP) | IP:Port | 저지연, 고성능, 비 HTTP |
| **GLB** (Gateway LB) | **L3** | IP 패킷 | 보안 어플라이언스 (방화벽) |

#### ① CLB (Classic Load Balancer) — 레거시

- 2009년 등장 (최초 ELB)
- Layer 4 + Layer 7 혼합
- EC2-Classic 시절 설계
- **신규 사용 비권장**. 기존 CLB는 ALB/NLB로 마이그레이션.

#### ② ALB (Application Load Balancer) ⭐ 가장 많이 쓰임

**Layer 7 (HTTP/HTTPS) 라우팅** — 애플리케이션 수준에서 트래픽 분산.

**라우팅 규칙**:
- **Path-based**: `/api/*` → service A, `/admin/*` → service B
- **Host-based**: `api.example.com` → service A, `admin.example.com` → service B
- **HTTP Header**: 특정 header 값 기반
- **HTTP Method**: GET vs POST
- **Query String**: `?version=v2` 기반

**주요 특징**:
- **WebSocket 지원** — 실시간 통신 가능
- **HTTP/2 지원** — 멀티플렉싱
- **gRPC 지원** (2020+)
- **Target Group**: EC2, Lambda, Container (ECS/EKS), IP 주소
- **Sticky Session** 지원 (쿠키 기반)
- **SSL/TLS 종료** — ACM 인증서로 HTTPS 처리
- **WAF (Web Application Firewall) 연동**
- **인증 통합** — Cognito, OIDC

**사용 사례**:
- 일반 웹 서비스 / API 서버
- 마이크로서비스 (경로별 라우팅)
- 서버리스 (Lambda target)

#### ③ NLB (Network Load Balancer) — 고성능 L4

**Layer 4 (TCP/UDP) 라우팅** — 네트워크 수준에서 초고속 분산.

**주요 특징**:
- **초고성능** — 초당 수백만 요청
- **매우 낮은 지연** — < 100 μs
- **Static IP 지원** — 각 AZ마다 고정 IP 할당 가능
- **Elastic IP** attach 가능
- **UDP 지원** (게임, 실시간 비디오)
- **Preserve Client IP** 가능 (아래 X-Forwarded-For 섹션 참조)
- **PrivateLink 통합**

**사용 사례**:
- TCP 기반 서비스 (SMTP, SSH, 게임 서버)
- IoT (MQTT)
- 금융 거래 (저지연 필수)
- **고성능 API 게이트웨이**

#### ④ GLB (Gateway Load Balancer) — 보안 어플라이언스용

- **Layer 3** (IP 패킷)
- 방화벽, IDS/IPS, Deep Packet Inspection 어플라이언스 분산
- 일반 애플리케이션 트래픽엔 사용 X

#### 선택 가이드

| 상황 | 선택 |
|-----|-----|
| 일반 웹 서비스 (HTTP/HTTPS) | **ALB** |
| 저지연 TCP 서비스 | **NLB** |
| UDP 서비스 (게임, VoIP) | **NLB** |
| 고정 IP 필요 | **NLB** |
| 레거시 유지 | CLB (단, 마이그레이션 계획) |
| 보안 어플라이언스 분산 | GLB |

---

## 5부. X-Forwarded-For 헤더

### 왜 필요한가 — ELB가 가로채는 Client IP

```
Client (203.0.113.5) ──▶ ALB (10.0.1.100) ──▶ EC2 (10.0.2.50)
```

EC2 애플리케이션 입장에서 `request.getRemoteAddr()`를 호출하면:
- **ALB의 IP (10.0.1.100)**이 보임 ❌
- **진짜 Client IP (203.0.113.5)는 안 보임**

→ ALB가 **프록시** 역할을 하기 때문. HTTP 커넥션이 Client-ALB와 ALB-EC2로 **별개**.

### 해결 — X-Forwarded-For 헤더

ALB가 **자동으로 추가**하는 HTTP 헤더:

```http
X-Forwarded-For: 203.0.113.5
X-Forwarded-Proto: https
X-Forwarded-Port: 443
```

- **X-Forwarded-For**: 원본 Client IP
- **X-Forwarded-Proto**: 원본 프로토콜 (http / https)
- **X-Forwarded-Port**: 원본 포트

#### 다중 프록시 체인

Client → CloudFront → ALB → EC2 식으로 **여러 프록시 거치면**:
```http
X-Forwarded-For: 203.0.113.5, 54.230.100.5
```

- 쉼표로 구분, **가장 왼쪽이 원본 Client**
- 각 프록시가 자기 앞의 IP를 추가

#### 애플리케이션 설정

**Spring Boot**:
```yaml
server:
  forward-headers-strategy: native   # Tomcat/Jetty/Netty
```
→ `request.getRemoteAddr()`가 **자동으로 X-Forwarded-For의 원본 IP** 반환.

**Nginx (프록시 역할 시)**:
```nginx
location / {
    proxy_pass http://backend;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

**Node.js (Express)**:
```javascript
app.set('trust proxy', true);  // X-Forwarded-* 신뢰
// req.ip 가 원본 Client IP 반환
```

### ALB vs NLB의 Client IP 처리

| | ALB | NLB |
|---|-----|-----|
| L7 proxy | ✅ 완전 proxy | ❌ (L4) |
| X-Forwarded-For 자동 추가 | ✅ | ❌ |
| Client IP 보존 (TCP 레벨) | ❌ (ALB IP로 교체) | **✅ Preserve Client IP 설정 시** |

**NLB의 "Preserve Client IP"**:
- Target Group 설정에서 활성화
- **TCP source IP를 원본 Client로 유지** → 애플리케이션 코드 수정 없이 `request.getRemoteAddr()`가 Client IP 반환
- 단, 보안 그룹/방화벽 설정 시 **각 Client IP를 허용**해야 함 (ALB 하나만 허용하는 것과 차이)

### ⚠️ 보안 주의 — Header Spoofing

**X-Forwarded-For는 HTTP 헤더** → **클라이언트가 임의 설정 가능** (스푸핑).

```bash
curl -H "X-Forwarded-For: 1.2.3.4" https://my-app
```

→ 애플리케이션이 그냥 `getHeader("X-Forwarded-For")` 쓰면 **IP 위조 공격 당할 수 있음**.

**방어**:
1. **신뢰 가능한 프록시(ALB)에서 온 요청만** X-Forwarded-For 신뢰
2. Spring Boot `forward-headers-strategy=native`는 이걸 자동 처리 (Tomcat RemoteIpValve)
3. 외부 Load Balancer를 거치지 않는 경로는 헤더 **무시**
4. 보안 그룹: EC2는 **ALB의 보안 그룹에서만 트래픽 허용**

### 실무 체크리스트

- [ ] ALB 사용 시 Spring Boot `forward-headers-strategy=native` 설정
- [ ] NLB 사용 시 "Preserve Client IP" 활성화 (가능하면)
- [ ] 애플리케이션이 Client IP를 쓰는 경우 (로깅, Rate Limit) 스푸핑 방어
- [ ] CloudFront 거치는 경우 `CloudFront-Viewer-Address` 헤더 활용 (CF 전용)
- [ ] HTTPS Terminate 위치 명확히 (ALB에서 종료 vs EC2까지 전달)

---

## 참고 자료

- [EC2 Instance Types](https://aws.amazon.com/ec2/instance-types/)
- [EC2 Pricing (OnDemand/Reserved/Spot)](https://aws.amazon.com/ec2/pricing/)
- [EBS Volume Types](https://docs.aws.amazon.com/ebs/latest/userguide/ebs-volume-types.html)
- [ALB vs NLB 비교](https://aws.amazon.com/elasticloadbalancing/features/)
- [X-Forwarded-For (RFC 7239)](https://datatracker.ietf.org/doc/html/rfc7239)
