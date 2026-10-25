### 1장. 도커란?

> 정리 범위: 가상 머신과 도커 컨테이너의 아키텍처 차이, 그리고 컨테이너와 OS 커널의 관계
>
> 실측 환경: macOS 15.6.1 (Apple Silicon), Docker Desktop, Docker Engine 28.0.4, VM 커널 `6.10.14-linuxkit`, 이미지 `alpine:3.20` / `ubuntu:24.04`

#### 목차

1. 가상 머신 아키텍처
2. 도커 컨테이너 아키텍처
3. 한눈에 비교
4. 컨테이너를 만드는 커널 기능
5. OS 커널과의 관계 (실측)
6. 도커 구성요소와 실행 흐름
7. 정리: 오해하기 쉬운 지점
8. 확인하지 못한 부분

---

#### 1. 가상 머신 아키텍처

가상 머신(VM)은 **하드웨어를 가상화**한다. 하이퍼바이저가 CPU·메모리·디스크·NIC를 가상 장치로 쪼개 주고, 각 VM은 그 위에서 **자기만의 게스트 OS(커널 포함)** 를 부팅한다.

```
 ┌────────┐ ┌────────┐ ┌────────┐
 │  App A │ │  App B │ │  App C │
 │ Bins/  │ │ Bins/  │ │ Bins/  │
 │ Libs   │ │ Libs   │ │ Libs   │
 │Guest OS│ │Guest OS│ │Guest OS│   ← VM마다 커널이 따로 있다
 │(kernel)│ │(kernel)│ │(kernel)│
 ├────────┴─┴────────┴─┴────────┤
 │         Hypervisor           │   ← 하드웨어를 가상화
 ├──────────────────────────────┤
 │      Host OS (Type 2일 때)    │
 ├──────────────────────────────┤
 │          Hardware            │
 └──────────────────────────────┘
```

- **Type 1 (bare-metal)**: 하드웨어 위에 하이퍼바이저가 직접 올라간다. 예) KVM, Xen, ESXi, Hyper-V
- **Type 2 (hosted)**: 호스트 OS 위에서 앱처럼 동작한다. 예) VirtualBox, VMware Workstation
- 게스트 OS의 커널이 하드웨어 접근 명령을 실행하면 하이퍼바이저가 가로채 처리한다. 현대 CPU는 VT-x/AMD-V 같은 하드웨어 가상화 지원으로 이 비용을 줄인다.
- **강한 격리**: VM 간에는 커널이 분리되어 있어서, 한 VM의 커널 취약점이 다른 VM으로 곧바로 번지지 않는다.
- **무겁다**: VM 하나마다 OS 전체(커널, init, 시스템 데몬)를 부팅하므로 이미지가 수 GB이고, 기동에 수십 초~분 단위가 걸리며, 메모리를 미리 할당한다.

#### 2. 도커 컨테이너 아키텍처

컨테이너는 **OS를 가상화**한다. 하드웨어를 흉내 내지 않고, **호스트 커널을 공유**하면서 프로세스 단위로 "보이는 범위"와 "쓸 수 있는 자원"만 제한한다.

```
 ┌────────┐ ┌────────┐ ┌────────┐
 │  App A │ │  App B │ │  App C │
 │ Bins/  │ │ Bins/  │ │ Bins/  │   ← 컨테이너 = 앱 + 라이브러리 + 파일시스템
 │ Libs   │ │ Libs   │ │ Libs   │     (커널은 없다)
 ├────────┴─┴────────┴─┴────────┤
 │   Docker Engine (dockerd …)   │
 ├──────────────────────────────┤
 │  Host OS Kernel (공유)        │   ← namespace / cgroup / overlayfs
 ├──────────────────────────────┤
 │          Hardware            │
 └──────────────────────────────┘
```

- 컨테이너는 **격리된 환경에서 도는 호스트의 평범한 프로세스**다. 게스트 OS를 부팅하지 않으므로 기동이 빠르고(실측 약 0.35~0.8초) 이미지가 작다(alpine 8.8MB).
- "컨테이너 안의 OS"처럼 보이는 것(`ubuntu`, `alpine`)은 **커널이 아니라 유저 공간 파일시스템(rootfs)** 이다. 배포판 차이는 libc, 패키지 관리자, 기본 도구 차이일 뿐이다.
- 이미지는 읽기 전용 **레이어**의 스택이고, 실행하면 맨 위에 쓰기 가능한 레이어 하나가 얹힌다 (union filesystem, 기본 `overlay2`).
- 호스트 커널을 공유하므로 **격리 강도는 VM보다 약하다.** 커널 취약점은 모든 컨테이너의 공통 공격면이다.

#### 3. 한눈에 비교

| 항목 | 가상 머신 | 도커 컨테이너 |
|---|---|---|
| 가상화 대상 | 하드웨어 | OS (커널 기능으로 프로세스 격리) |
| 커널 | VM마다 별도 게스트 커널 | **호스트 커널 공유** |
| 격리 경계 | 하이퍼바이저 (하드웨어 수준) | namespace / cgroup / capability / seccomp |
| 격리 강도 | 강함 | 상대적으로 약함 (커널 공유) |
| 이미지 크기 | 수 GB | 수 MB ~ 수백 MB |
| 기동 시간 | 수십 초 ~ 분 (OS 부팅) | 1초 안팎 (프로세스 시작) |
| 자원 할당 | 시작 시 고정 할당이 일반적 | 필요한 만큼 사용, 상한만 지정 (cgroup) |
| 다른 OS 실행 | 가능 (Windows 위의 Linux 등) | 불가 (호스트와 같은 커널 계열만) |
| 한 호스트의 밀도 | 수 ~ 수십 | 수십 ~ 수백 |
| 보안 커널 격리 필요 시 | 적합 | gVisor / Kata Containers 같은 샌드박스 런타임 병용 |

VM과 컨테이너는 대체 관계가 아니라 **조합**하는 경우가 많다. 클라우드에서는 VM(EC2) 위에 컨테이너를 올리고, Docker Desktop도 아래 5절처럼 Linux VM 안에서 컨테이너를 돌린다.

#### 4. 컨테이너를 만드는 커널 기능

도커가 새로 만든 기술이 아니라, **리눅스 커널의 기존 기능을 묶어 쓰기 편하게 만든 도구**다.

| 기능 | 역할 | 컨테이너에서 보이는 효과 |
|---|---|---|
| **namespace** | 프로세스가 "볼 수 있는" 자원의 범위를 분리 | PID 1부터 시작, 독립된 호스트명/네트워크/마운트 |
| **cgroup** | 프로세스 그룹이 "쓸 수 있는" 자원량 제한·계측 | `-m`, `--cpus`, `--pids-limit` |
| **union FS (overlayfs)** | 읽기 전용 레이어 + 쓰기 레이어를 하나의 파일시스템으로 합침 | 이미지 레이어 공유, 변경분만 저장 |
| **capabilities** | root 권한을 세분화한 권한 단위 | 컨테이너 root라도 `mount`, `sysctl` 불가 |
| **seccomp** | 허용할 시스템콜 필터 | 위험한 syscall 차단 (기본 프로필) |

**namespace 종류**: `pid`, `net`, `mnt`, `uts`, `ipc`, `user`, `cgroup`, `time`

- `pid`: 컨테이너 안에서는 자기 프로세스가 PID 1. 호스트에서는 다른 번호로 보인다.
- `net`: 독립된 네트워크 스택(인터페이스, 라우팅, iptables)
- `mnt`: 독립된 마운트 테이블 → 컨테이너마다 다른 루트 파일시스템
- `uts`: 호스트명 분리 (`--hostname`)
- `ipc`: SysV IPC, POSIX 메시지 큐 분리
- `user`: UID 매핑 (기본 도커 설정에서는 사용하지 않음, 실측에서 호스트와 같은 ns)
- `cgroup`, `time`: cgroup 뷰, 시계 오프셋 분리

**cgroup**은 CPU, 메모리, PID 수, 블록 I/O 등을 제한한다. v2에서는 `/sys/fs/cgroup/` 아래 `memory.max`, `cpu.max`, `pids.max` 같은 파일로 제어한다. 메모리 상한을 넘으면 커널의 OOM killer가 컨테이너 프로세스를 종료한다 (exit code 137 = 128 + SIGKILL 9).

#### 5. OS 커널과의 관계 (실측)

##### 5-1. 컨테이너는 호스트 커널을 쓴다

Mac 터미널에서 확인한 결과:

```
호스트(macOS)        : Darwin 24.6.0
docker info          : kernel=6.10.14-linuxkit  os=Docker Desktop
alpine 컨테이너      : Linux 6.10.14-linuxkit
ubuntu:24.04 컨테이너: Linux 6.10.14-linuxkit   (PRETTY_NAME="Ubuntu 24.04.5 LTS")
```

- 배포판이 alpine이든 ubuntu든 `uname -r`은 **같은 커널 버전**이다. 컨테이너가 자기 커널을 갖지 않는다는 직접적인 증거다.
- `/proc/version`에도 같은 커널 빌드 문자열이 나온다.
- 호스트는 macOS(Darwin)인데 컨테이너는 Linux 커널이다. **macOS에는 Linux 커널이 없으므로 컨테이너가 macOS 커널을 공유할 수 없다.** 그래서 Docker Desktop은 내부에 **작은 Linux VM(LinuxKit)** 을 띄우고 그 VM의 커널을 컨테이너들이 공유한다.

```
Mac 호스트 (Darwin)
 └─ Docker Desktop
     └─ Linux VM (linuxkit, 커널 6.10.14)   ← 여기에 dockerd/containerd/runc
         ├─ 컨테이너 A  ┐
         ├─ 컨테이너 B  ├ 모두 이 VM의 커널을 공유
         └─ 컨테이너 C  ┘
```

즉 Mac/Windows의 도커는 **"VM 1개 + 그 안의 컨테이너 N개"** 구조다. VM의 비용을 한 번만 내고 그 위에서는 컨테이너의 장점을 쓰는 것이다. Linux 서버에서는 이 VM 계층이 없고 컨테이너가 호스트 커널을 직접 쓴다.

VM 내부 상태:

```
$ docker info --format '... cgroup={{.CgroupDriver}}/v{{.CgroupVersion}} runtime={{.DefaultRuntime}} storage={{.Driver}}'
cgroup=cgroupfs/v2  runtime=runc  storage=overlay2
```

##### 5-2. namespace로 격리되지만 커널 위에서는 보통 프로세스다

컨테이너 안에서:

```
$ ps
PID   USER  COMMAND
  1   root  sh -c ps; ...
  7   root  ps
```

`sleep 300`을 컨테이너로 실행하고, VM 쪽(`--pid=host`)에서 같은 프로세스를 본 결과:

```
docker inspect : pid-on-VM=5908
VM에서 pgrep   : vm-pid=5908
/proc/5908/ns  : pid -> pid:[4026533499]   ← 컨테이너 안에서 본 ns 번호와 동일
                 mnt -> mnt:[4026533474], net -> net:[4026533501], uts -> uts:[4026533497] ...
/proc/5908/cgroup: 0::/../bc87f5bb2410...  ← 컨테이너 ID로 된 cgroup
```

- 컨테이너 안에서 PID 1인 프로세스가 **VM 커널에서는 PID 5908인 일반 프로세스**다. 같은 프로세스를 두 개의 PID 네임스페이스에서 다른 번호로 보는 것이다.
- 컨테이너 안과 밖에서 본 ns inode 번호(`4026533499` 등)가 같다 → "컨테이너"는 별도 개체가 아니라 **프로세스에 붙은 namespace + cgroup 설정의 묶음**이다.
- `user`(`4026531837`)와 `time`(`4026531834`)은 기본 설정에서 호스트와 공유하는 ns로 확인됐다. 즉 기본 컨테이너의 root는 호스트 UID 0과 같은 UID이고, 그래서 capability로 권한을 줄이는 것이 중요하다.
- `ps`로 본 VM의 프로세스 트리에는 `dockerd → containerd → containerd-shim-runc-v2` 구조가 보였다 (6절).

##### 5-3. cgroup으로 자원 상한이 걸린다

`docker run -m 64m --cpus 0.5 --pids-limit 20`:

```
/sys/fs/cgroup/memory.max → 67108864      (= 64MiB)
/sys/fs/cgroup/cpu.max    → 50000 100000  (100ms 주기 중 50ms = 0.5 CPU)
/sys/fs/cgroup/pids.max   → 20
```

- 제한값은 `docker run` 옵션이 **cgroup 파일 값으로 변환**된 것이다. 도커 자체가 제한을 구현하는 것이 아니라 커널이 강제한다.
- 함정: 같은 컨테이너에서 `free -m`은 **VM 전체 메모리(7837MB)** 를 보여줬다. `free`, `top` 같은 도구는 `/proc/meminfo`를 읽는데 이것은 cgroup 제한을 반영하지 않는다. 컨테이너 안에서 "내 메모리가 얼마인지" 알려면 cgroup 파일을 봐야 한다. (JVM 같은 런타임이 컨테이너 인식 옵션을 따로 두는 이유)
- 메모리 32MB 제한 컨테이너에서 100MB를 쓰려 하자 `Killed`, **exit=137**로 종료됐다 (OOM kill).

##### 5-4. 커널을 공유하기 때문에 생기는 제약

**① 컨테이너 안에서 커널을 바꿀 수 없다**

```
$ sysctl -w kernel.hostname=x   → Read-only file system
$ mount -t tmpfs none /mnt      → permission denied (are you root?)
$ ls /lib/modules               → No such file or directory
```

- 컨테이너의 `root`지만 기본 capability 집합이 제한되어 있어서(`CapEff: 00000000a80425fb`) `mount`가 막힌다.
- `/lib/modules`가 없다 → 커널 모듈은 컨테이너가 아니라 **호스트(여기서는 VM) 커널의 것**이다. 컨테이너에서 모듈을 올리면 모든 컨테이너에 영향이 가기 때문에 막혀 있다.
- `--cap-drop ALL`을 주면 `CapEff`, `CapBnd`가 전부 `0`이 된다 → 최소 권한 운영의 근거.

**② 같은 커널 계열만 실행된다**

- Linux 컨테이너는 Linux 커널이 있어야 돈다. Windows 컨테이너는 Windows 커널이 필요하다. Mac에서 Linux 컨테이너가 도는 이유는 5-1의 VM 덕분이다.
- **CPU 아키텍처도 맞아야 한다.** 이 Mac은 arm64인데 `--platform linux/amd64`로 실행하면 `uname -m`이 `x86_64`로 출력됐다. 이는 Docker Desktop이 에뮬레이션(Rosetta 또는 QEMU)을 거치기 때문이며, 네이티브보다 느리다. (에뮬레이션 종류와 성능 차이는 측정하지 않았다)

**③ 호스트 커널 버전이 컨테이너의 기능 상한이다**

- 컨테이너 안 프로그램이 쓰는 시스템콜·기능(cgroup v2, io_uring, eBPF 등)은 **호스트 커널이 지원하는 만큼**만 쓸 수 있다. 이미지에 새 커널을 넣어도 소용이 없다.
- 같은 이미지가 호스트 커널에 따라 다르게 동작할 수 있다는 뜻이며, 쿠버네티스 노드 OS/커널 버전 관리가 중요한 이유이기도 하다.

##### 5-5. 이미지는 "커널 없는 파일시스템"이다

```
alpine:3.20  8.82MB  (ADD alpine-minirootfs-3.20.10-aarch64.tar.gz …, CMD ["/bin/sh"])
ubuntu:24.04 101MB
```

- alpine 레이어의 정체는 **minirootfs tarball 하나**다. 커널·부트로더가 없고, 기본 쉘과 libc(musl)와 BusyBox 정도만 있다.
- VM 이미지가 디스크 전체(커널+initrd+rootfs)인 것과 대비된다.
- 이미지가 aarch64 전용 rootfs라는 점에서, 이미지는 **CPU 아키텍처에 종속**되고 커널에는 종속되지 않음(같은 아키텍처의 Linux 커널이면 됨)을 알 수 있다.

#### 6. 도커 구성요소와 실행 흐름

실측 시 VM 안에서 확인한 프로세스:

```
342   273  /usr/local/bin/dockerd --config-file ... --containerd /run/containerd/containerd.sock
1207  756  /usr/local/bin/containerd
686     1  /usr/bin/containerd-shim-runc-v2 -namespace moby -id 46b859d1...
```

```
docker CLI ──REST──▶ dockerd ──gRPC──▶ containerd ──▶ containerd-shim-runc-v2 ──▶ runc ──▶ 컨테이너 프로세스
 (클라이언트)       (이미지 빌드, 네트워크,   (이미지 관리,       (컨테이너마다 1개,        (OCI 런타임:        (namespace/cgroup이
                     볼륨, API)              컨테이너 수명)      부모 역할 유지)          커널 설정 후 exec)   적용된 프로세스)
```

- **docker CLI**: 사용자가 쓰는 명령. 데몬과 소켓(`/var/run/docker.sock`)으로 통신한다. (Mac에서는 `~/.docker/run/docker.sock`)
- **dockerd**: 도커 데몬. 이미지/네트워크/볼륨 관리, API 제공
- **containerd**: 컨테이너 수명주기와 이미지 저장을 담당하는 고수준 런타임 (쿠버네티스도 직접 사용)
- **containerd-shim**: 컨테이너마다 하나씩 남아 있는 중간 프로세스. 데몬이 재시작돼도 컨테이너가 죽지 않게 하고, stdio와 종료 코드를 관리한다. 이 shim의 PPID가 1인 것이 관찰됐다.
- **runc**: OCI 표준을 구현한 저수준 런타임. namespace/cgroup을 설정하고 컨테이너 프로세스를 `exec`한 뒤 **종료**한다 (그래서 상주 프로세스로는 안 보인다).
- **OCI**: 이미지 포맷(Image Spec)과 런타임(Runtime Spec) 표준. 덕분에 도커 이외의 런타임(containerd, CRI-O, podman 등)과 이미지가 호환된다.

#### 7. 정리: 오해하기 쉬운 지점

| 오해 | 실제 |
|---|---|
| "컨테이너는 가벼운 VM이다" | 커널이 없는 **격리된 프로세스**다. VM과 격리 모델이 다르다. |
| "ubuntu 컨테이너는 Ubuntu OS가 돈다" | **rootfs만 Ubuntu**이고 커널은 호스트 것이다. (`uname -r` 동일) |
| "컨테이너 안 root는 안전하다" | root UID가 호스트와 같다(user ns 미사용). capability/seccomp 등으로 제한할 뿐이다. |
| "컨테이너가 제한 메모리를 인식한다" | `free`는 호스트 전체를 보여준다. cgroup 파일을 봐야 한다. |
| "Mac에서 Mac용 컨테이너가 돈다" | Linux VM 안의 **Linux 컨테이너**다. |
| "이미지를 만들면 어디서나 돈다" | 같은 **커널 계열 + CPU 아키텍처**여야 한다. |
| "컨테이너가 사라지면 데이터도 사라지는 건 도커 특성" | 쓰기 레이어가 컨테이너와 함께 삭제되는 것. 볼륨은 별도 (이후 장에서 다룸). |

선택 기준 요약:
- 같은 커널을 신뢰할 수 있고 빠른 배포·높은 밀도가 목표 → **컨테이너**
- 서로 다른 OS가 필요하거나 멀티테넌트에서 강한 커널 격리가 필요 → **VM** (또는 컨테이너 + 샌드박스 런타임)
- 실무 → VM 위에서 컨테이너 (격리 경계는 VM, 배포 단위는 컨테이너)

#### 8. 확인하지 못한 부분

- **VM 기동 시간·메모리 오버헤드의 직접 비교**: 컨테이너 기동 시간(0.35~0.8초, 3회 측정, 이미지가 이미 있는 상태)은 실측했지만, 같은 환경에서 VM을 부팅해 비교하지는 않았다. 표의 "VM 수십 초~분"은 일반적인 설명이며 이 머신의 측정값이 아니다.
- **seccomp 기본 프로필**: 컨테이너 안에서 `/proc/self/status`의 `Seccomp:`가 `0`(필터 없음)으로 나왔다. 2장 실험 중 이 Docker Desktop VM의 `daemon.json`에서 `"seccomp-profile":"unconfined"`가 설정되어 있는 것을 확인했다. 즉 이 환경에서만 기본 프로필이 꺼져 있는 것이며, 일반적인 도커 엔진은 기본 seccomp 프로필을 적용한다고 알려져 있다 (그 동작은 이 환경에서 직접 검증하지 못했다).
- **user namespace 미사용**은 Docker Desktop 기본 설정에서의 관찰이다. Linux 서버의 설정(`userns-remap`)에 따라 다르다.
- **amd64 에뮬레이션 방식(Rosetta/QEMU)과 성능 차이**는 측정하지 않았다.
- **Linux 네이티브 호스트**(VM 계층 없음)에서의 결과는 확인하지 못했다. 5-1의 VM 구조는 Docker Desktop 특성이다.
- 하이퍼바이저 Type 1/2 구분, Kata Containers/gVisor의 동작은 개념 설명이며 직접 실행해보지 않았다.
- 책 본문의 구체적인 서술·그림은 이 노트에 반영하지 않았다 (요청받은 토픽 + 실측 중심).
