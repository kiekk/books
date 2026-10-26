### 15장. 클러스터 네트워킹 구성

> 정리 범위: 도커 컨테이너 네트워킹(NAT, eth0, docker0, veth), pause 컨테이너
>
> 실측 환경: macOS (Apple Silicon), Docker Desktop (Docker Engine 28.0.4, 내장 쿠버네티스 v1.32.2). **도커 컨테이너의 네트워크 장치는 Mac이 아니라 Docker Desktop의 리눅스 VM 안에 있다.** 그래서 VM의 네트워크 네임스페이스를 볼 때는 `docker run --rm --net=host --privileged alpine …`(호스트 네트워크로 VM 자체를 들여다봄)를 썼다. 쿠버네티스 쪽 확인은 `--context docker-desktop`, 네임스페이스 `t15`(삭제함)로 했다. 이 장은 새 클러스터를 만들지 않았다.
>
> 표기: `$` 명령과 출력은 이 환경에서 직접 실행한 결과다. 직접 실행하지 못한 내용은 마지막 "확인하지 못한 부분"에 모았다.
>
> 겹치는 내용: pause 컨테이너의 종료/IP 변화는 [5장](../chapter05/README.md), Service/kube-proxy의 NAT는 [7장](../chapter07/README.md)에서 다뤘다. 이 장은 그 밑바탕이 되는 컨테이너 네트워크 기본 구조를 확인한 것이다. 이 실험의 일부(busybox `ip -br` 미지원으로 인한 재실행 등)와 다른 책의 도커 네트워크 장은 열어 비교하지 않았다.

#### 목차

1. 한눈에 보기: 컨테이너 한 개의 네트워크 경로
2. eth0, veth, docker0 (브리지)
3. 컨테이너 ↔ 컨테이너, 이름 해석
4. NAT: 나가는 트래픽(MASQUERADE)과 들어오는 트래픽(포트 공개/DNAT)
5. 네트워크 모드: bridge, host, none, container
6. pause 컨테이너와 파드 네트워크
7. 쿠버네티스 파드 네트워크와의 비교
8. 확인하지 못한 부분

---

#### 1. 한눈에 보기: 컨테이너 한 개의 네트워크 경로

```
컨테이너 안                           VM(호스트) 안
 eth0 (172.17.0.2) ──┐               ┌─ docker0 (172.17.0.1, 리눅스 브리지)
        (veth 쌍의 한쪽) ├───────────▶ vethXXXX ─┤
                                       └─ iptables(nat) ── eth0 (192.168.65.3) ──▶ 인터넷
```

- 컨테이너마다 **독립 네트워크 네임스페이스**(자기 `eth0`, 라우팅 테이블)를 갖는다.
- 그 `eth0`는 **veth 쌍**의 한쪽이고 다른 한쪽(`vethXXXX`)은 호스트(VM)의 **브리지 `docker0`** 에 꽂혀 있다.
- 바깥으로 나갈 때는 호스트의 iptables가 **소스 IP를 호스트 주소로 바꾼다(NAT)**.

---

#### 2. eth0, veth, docker0 (브리지)

##### 2-1. VM 쪽 (실측)

```
$ docker run --rm --net=host --privileged alpine ip route
10.1.0.0/16 dev cni0 scope link  src 10.1.0.1          ← 쿠버네티스 파드용 브리지
172.17.0.0/16 dev docker0 scope link  src 172.17.0.1   ← 기본 도커 브리지
192.168.16.0/20 dev br-21989c25b4aa scope link ...     ← 사용자 정의 네트워크 브리지(다른 컴포즈/네트워크)
192.168.65.0/24 dev eth0 scope link                    ← VM의 실제 외부 인터페이스
$ docker network inspect bridge
172.17.0.0/16 gw=172.17.0.1 bridge=docker0
```

(이 실험의 busybox `ip`는 `-br` 옵션을 지원하지 않아 이 출력의 일부는 `ip route`와 `/sys/class/net`으로 대체했다.)

- `docker0`: 도커가 만든 **가상 스위치(리눅스 브리지)**. 기본 네트워크(`bridge`)의 게이트웨이가 `172.17.0.1`이다.
- `cni0`: 쿠버네티스 CNI가 파드용으로 만든 브리지(`10.1.0.0/16`). 같은 VM에 두 브리지가 함께 있었다.

##### 2-2. 컨테이너 2개를 띄웠을 때 (실측)

| | n15a | n15b |
|---|---|---|
| 컨테이너 안 `eth0` IP | `172.17.0.2/16` | `172.17.0.3/16` |
| 기본 경로 | `default via 172.17.0.1 dev eth0` | 같음 |
| `/sys/class/net/eth0/iflink` | `394` | `395` |

호스트(VM) 쪽에서:

```
vethc3bc266  ifindex=394  peer(iflink)=11  master=docker0     ← n15a의 eth0 (컨테이너 안 ifindex 11 ↔ 호스트 394)
vethf0b0fa4  ifindex=395  peer(iflink)=11  master=docker0     ← n15b
veth700b6a8f ifindex=22   ... master=cni0                       ← 쿠버네티스 파드들의 veth
vethc7126b89 ifindex=24 / vethf82301b2 ifindex=23 ... master=cni0
```

- **짝 찾는 법**: 컨테이너 안 `eth0`의 `iflink` 값(394)이 호스트 쪽 `veth`의 `ifindex`다. 반대로 호스트 veth의 `iflink`(11)는 컨테이너 안 `eth0`의 `ifindex`다. 둘이 서로를 가리킨다.
- 두 veth 모두 `master=docker0`, 즉 같은 브리지에 꽂혀 있다.
- veth는 **한쪽에 보낸 패킷이 다른 쪽으로 나오는 가상 케이블**이다.

---

#### 3. 컨테이너 ↔ 컨테이너, 이름 해석

```
$ docker exec n15a ping -c1 172.17.0.3     → 1 packets received, 0% packet loss  (0.123 ms)
$ docker exec n15a ping -c1 n15b           → ping: bad address 'n15b'
```

- 같은 브리지에 꽂힌 컨테이너끼리는 **IP로 직접 통신**한다(브리지가 L2 스위치처럼 전달).
- **기본 `bridge` 네트워크에서는 컨테이너 이름으로 통신할 수 없다**(`bad address`). 이름 해석은 사용자 정의 브리지 네트워크가 내장 DNS로 제공한다고 알려져 있다(이 실험에서 사용자 정의 네트워크로 확인하지는 않았다).

---

#### 4. NAT

##### 4-1. 나가는 트래픽: MASQUERADE (실측)

```
$ iptables -t nat -S POSTROUTING            (VM 안)
-A POSTROUTING -m comment --comment "kubernetes postrouting rules" -j KUBE-POSTROUTING
-A POSTROUTING -o docker0 -m addrtype --src-type LOCAL -j MASQUERADE
-A POSTROUTING -s 172.17.0.0/16 ! -o docker0 -j MASQUERADE        ← 172.17.0.0/16 이 docker0 밖으로 나갈 때 MASQUERADE
-A POSTROUTING -o br-21989c25b4aa -m addrtype --src-type LOCAL -j MASQUERADE
```

컨테이너 `172.17.0.2`가 `example.com:80`에 접속한 뒤 VM의 conntrack 항목:

```
tcp  TIME_WAIT
  src=172.17.0.2 dst=172.66.147.243 sport=45466 dport=80          ← 보낼 때(컨테이너 주소)
  src=172.66.147.243 dst=192.168.65.3 sport=80 dport=45466         ← 돌아올 때(목적지는 VM 주소 192.168.65.3)
```

- 컨테이너의 사설 IP(172.17.0.2)는 외부에 보이지 않고 **VM의 IP(192.168.65.3)로 바뀌어 나간다**(SNAT/MASQUERADE). 응답은 conntrack 표를 보고 컨테이너로 되돌려진다.
- 이 VM 주소(`192.168.65.3`) 뒤에서 한 번 더 Mac의 주소로 NAT되는지는 확인하지 않았다(Docker Desktop의 네트워크 계층).
- 8장에서 본 `xff=192.168.65.1`, 7장의 `10.1.0.1`(노드 게이트웨이)이 이 NAT/브리지의 흔적이다.

##### 4-2. 들어오는 트래픽: 포트 공개(DNAT) (실측)

```
$ docker run -d --name n15web -p 127.0.0.1:18088:80 nginx:alpine
$ curl localhost:18088                                       → 200
$ iptables -t nat -S DOCKER | grep 18088
-A DOCKER -d 127.0.0.1/32 -p tcp -m tcp --dport 18088 -j DNAT --to-destination 172.17.0.4:80
```

- `-p 호스트포트:컨테이너포트`는 호스트의 `iptables nat` 규칙으로 **목적지 주소를 컨테이너 IP:포트로 바꾼다(DNAT)**.
- 실험에서는 보안을 위해 `127.0.0.1`에만 바인딩했다. `-p 18088:80`처럼 주소를 생략하면 모든 인터페이스에 공개된다.
- `docker-proxy` 프로세스를 VM에서 찾아봤지만 목록에 보이지 않았다(이번 환경에서는 iptables 규칙만 확인).
- 컨테이너 IP(`172.17.0.4`)는 재시작하면 바뀔 수 있어 규칙이 이름이 아닌 IP로 만들어진다.

---

#### 5. 네트워크 모드

| 모드 | 의미 | 실측 |
|---|---|---|
| `bridge` (기본) | 컨테이너마다 독립 네임스페이스 + docker0 | 위 2~4절 |
| `host` | **호스트(VM)의 네트워크 네임스페이스를 그대로 사용** | `--net=host`로 본 `eth0`가 VM의 `192.168.65.3/24` |
| `none` | 네트워크 없음 (`lo`만) | 출력: `lo`(127.0.0.1)와 비활성 `tunl0`만, `eth0` 없음 |
| `container:<이름>` | **다른 컨테이너의 네임스페이스를 공유** | 아래 |

##### 5-1. `--net=container:<대상>` (실측)

```
$ docker run -d --name base alpine sleep 600
$ docker run -d --name side --net=container:base alpine sleep 600
base eth0: inet 172.17.0.2/16
side eth0: inet 172.17.0.2/16                      ← 같은 IP
netns inode: base=net:[4026532560]  side=net:[4026532560]     ← 같은 네임스페이스
base 에서 127.0.0.1:8081 리슨 → side 에서 127.0.0.1:8081 접속 성공 ("connected-via-localhost")
```

두 컨테이너는 **IP, 포트 공간, `localhost`를 공유**한다. 이것이 쿠버네티스 파드의 네트워크 모델과 같다.

---

#### 6. pause 컨테이너와 파드 네트워크

##### 6-1. 왜 필요한가

한 파드의 컨테이너들은 **같은 IP와 `localhost`를 공유**해야 한다. 그런데 네트워크 네임스페이스는 **어떤 프로세스(컨테이너)에 붙어 존재**한다. 앱 컨테이너 중 하나가 재시작되면 그 컨테이너가 네임스페이스의 주인일 경우 IP가 사라진다. 그래서 **아무 일도 하지 않는 pause 컨테이너가 먼저 떠서 네임스페이스를 소유**하고, 앱 컨테이너들은 그 네임스페이스에 **합류**한다.

##### 6-2. 실측 (Docker Desktop 쿠버네티스, 파드 `two` = 컨테이너 a, b)

```
$ docker ps --filter label=io.kubernetes.pod.name=two
7ef8ab1889dd  k8s_a_two_t15_<uid>_0
8f68a334254c  k8s_b_two_t15_<uid>_0
$ docker inspect (각 앱 컨테이너)
io.kubernetes.docker.type:container   NetworkMode=container:a4ee9707ee6b…      ← 둘 다 같은 대상에 합류
$ docker inspect a4ee9707ee6b   →   io.kubernetes.docker.type = podsandbox       ← 이것이 pause(샌드박스) 컨테이너
$ kubectl exec two -c a -- readlink /proc/1/ns/net   → net:[4026532560]
$ kubectl exec two -c b -- readlink /proc/1/ns/net   → net:[4026532560]            ← 같은 네임스페이스
```

- 앱 컨테이너 a, b의 `NetworkMode`가 **`container:<pause의 ID>`**(5-1의 `--net=container:` 와 같은 방식)이고 netns inode가 같다.
- `podsandbox` 타입의 컨테이너가 pause다(레이블 `io.kubernetes.pod.name`으로 필터했을 때 pause는 목록에 나오지 않았으므로 ID로 직접 확인했다. 이미지/명령 줄은 확인하지 못했다).
- 5장에서 확인한 대로 **pause 컨테이너를 죽이면 파드 IP가 바뀌고(`SandboxChanged`) init 컨테이너가 다시 실행**되는 이유가 이 구조다(IP는 pause의 네임스페이스에 달려 있음).
- 파드 IP는 `10.1.2.71`, 파드 안 `eth0`는 `eth0@if402`, 즉 호스트의 `ifindex=402` veth와 짝이고, 그 veth는 **`cni0` 브리지**에 꽂혀 있었다(`master=cni0`).

##### 6-3. pause의 역할 정리

| 역할 | 설명 |
|---|---|
| 네임스페이스 소유 | 파드의 네트워크 네임스페이스(IP, 포트, localhost) 유지 |
| 파드 내 컨테이너 공유 | 앱 컨테이너가 `container:` 방식으로 합류 |
| (그 외) | 프로세스 네임스페이스 공유 시 좀비 프로세스 회수 등은 문서 지식이며 이번에 확인하지 않았다 |

---

#### 7. 쿠버네티스 파드 네트워크와의 비교

| | 도커 기본 네트워크 | 쿠버네티스 파드 네트워크 (이 실험 환경) |
|---|---|---|
| 브리지 | `docker0` (172.17.0.0/16) | `cni0` (10.1.0.0/16), CNI가 관리 |
| 컨테이너/파드의 IP 단위 | 컨테이너마다 | **파드마다** (컨테이너는 공유) |
| 네임스페이스 소유 | 컨테이너 자신 | **pause 샌드박스** |
| veth 쌍 | 컨테이너 ↔ docker0 | 파드 ↔ cni0 |
| 외부 통신 | MASQUERADE | 노드 IP로 SNAT (7장의 `10.1.0.1`) |
| 이름 해석 | 기본 bridge에서는 불가 | CoreDNS (16장) |
| 노드 간 통신 | 별도 구성 필요(기본은 단일 호스트) | CNI가 노드 간 파드 통신을 제공해야 함 (이 실험은 단일 노드라 확인 못 함) |

---

#### 8. 확인하지 못한 부분

- **노드 간 파드 통신(오버레이, 라우팅, VXLAN, CNI 플러그인 종류와 차이)**은 단일 노드 Docker Desktop이라 확인하지 못했다. 이 장의 제목 "클러스터 네트워킹 구성"의 핵심인 CNI 비교(Calico, Flannel, Cilium, AWS VPC CNI)는 다루지 못했다.
- **Docker Desktop VM 바깥(Mac)과의 연결 방식**(VM의 192.168.65.3이 Mac에서 어떻게 보이는지, `host.docker.internal`, 한 번 더의 NAT)은 확인하지 않았다.
- **사용자 정의 브리지 네트워크의 내장 DNS**로 이름 통신이 되는지는 시험하지 않았다(기본 bridge에서 안 되는 것만 확인).
- **`docker-proxy` 프로세스**를 VM에서 찾지 못해 userland proxy의 역할은 확인하지 못했다.
- **IPv6, MTU, VLAN, macvlan/ipvlan 네트워크 드라이버**, `iptables`의 `DOCKER-USER`, 컨테이너 간 격리(ICC) 설정은 다루지 않았다.
- **pause 컨테이너의 이미지/명령/크기**, 프로세스 네임스페이스 공유(`shareProcessNamespace`) 시의 동작은 이 장에서 확인하지 않았다(5장에서 일부 다룸).
- **Linux 호스트(VM이 아닌)** 에서의 `docker0` 동작은 확인하지 못했다. 이 문서의 모든 관찰은 Docker Desktop의 VM 내부다.
- **CNI 플러그인이 파드 IP를 할당하는 과정**(IPAM), `cni0`가 어떤 플러그인이 만든 것인지는 조사하지 않았다.
