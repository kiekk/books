### Story 09. IO에서 발생하는 병목 현상

## 1부. I/O 처리 기본 — 왜 Kernel을 거쳐야 하나

#### User space와 Kernel space 분리

OS는 보안/안정성을 위해 메모리를 두 영역으로 분리:
- **User space**: 애플리케이션 코드/데이터
- **Kernel space**: OS 커널, 하드웨어 드라이버

→ **User 프로세스는 디스크/네트워크 하드웨어에 직접 접근할 수 없음**. 반드시 **system call**로 커널에 요청.

#### I/O 요청 플로우 (전통적 `read()`)

```
┌────────────┐  ① read(fd, buf, size) syscall
│ User space │ ─────────────────────────────────┐
│  (JVM)     │                                   │
│  user buf  │                                   ▼
└─────▲──────┘                            ┌─────────────┐
      │                                   │ Kernel space│
      │ ④ 커널→유저 복사                  │             │
      │                                   │  kernel buf │
      │                                   └─────▲───────┘
      │                                         │ ③ DMA
      │                                         │
      └─────────────────────────────────┐       │
                                        │       │
                                        │  ┌────▼─────┐
                                        └──┤ Hardware │
                                        ② 데이터│ (disk,   │
                                           fetch│  NIC)    │
                                                └──────────┘
```

**4단계**:
1. `read()` syscall → user mode → kernel mode 전환
2. 커널이 하드웨어에 데이터 요청
3. **DMA**(Direct Memory Access)로 커널 버퍼에 데이터 복사 (CPU 쉼)
4. 커널 버퍼 → 유저 버퍼 복사 (CPU 사용)
5. kernel mode → user mode 복귀, `read()` 리턴

**비용 발생 지점**:
- **Mode switch** (user ↔ kernel): 수십~수백 ns
- **Memory copy** (kernel ↔ user): 바이트 수에 비례
- **DMA 대기** (디스크 seek, 네트워크 latency): 수백 μs ~ ms
- **Blocking**: 데이터 올 때까지 스레드 멈춤 → CPU 유휴

---

## 2부. 일반적인 Java I/O (`java.io`)

#### 종류 정리

```
java.io
├── InputStream / OutputStream     — 바이트 단위 (binary)
│   ├── FileInputStream / FileOutputStream
│   ├── BufferedInputStream / BufferedOutputStream
│   ├── DataInputStream / DataOutputStream  (int, long, UTF 등 primitive)
│   ├── ObjectInputStream / ObjectOutputStream  (직렬화)
│   └── ByteArrayInputStream / ByteArrayOutputStream  (in-memory)
│
├── Reader / Writer                — 문자 단위 (char + encoding)
│   ├── FileReader / FileWriter
│   ├── BufferedReader / BufferedWriter
│   ├── InputStreamReader / OutputStreamWriter  (byte ↔ char 변환)
│   └── StringReader / StringWriter
│
└── RandomAccessFile               — 파일의 임의 위치 접근
```

**Stream vs Reader/Writer 선택**:
- 바이너리 (이미지, 파일 복사) → **Stream**
- 텍스트 (문자 인코딩 다룸) → **Reader/Writer**

#### 왜 느린가 — 4가지 이유

| 이유 | 설명 | 상대 비중 |
|-----|------|---------|
| **System call overhead** | 매 read/write마다 kernel mode 전환 (수 μs) | 중간 |
| **데이터 복사** | kernel buf ↔ user buf 복사. CPU + 메모리 대역폭 소비 | 큼 |
| **Blocking** | 데이터 올 때까지 스레드 WAITING. CPU 유휴 | 매우 큼 |
| **메모리 할당/GC** | byte[] 반복 생성 → GC 압박 | 작음 |

#### Buffered가 왜 빨라지나

```java
// Bad — 1 byte씩 syscall. 100KB 파일 = 100,000번 syscall
try (FileInputStream fis = new FileInputStream("file")) {
    int b;
    while ((b = fis.read()) != -1) {   // 1 byte씩 kernel 왕복
        process((byte) b);
    }
}

// Good — 8KB씩 syscall. 100KB 파일 = 약 13번 syscall
try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream("file"))) {
    int b;
    while ((b = bis.read()) != -1) {   // 버퍼에서 꺼냄. 버퍼 비면 syscall 1회
        process((byte) b);
    }
}
```

- `BufferedInputStream` 기본 버퍼 크기 **8192 bytes** (8KB)
- Syscall 횟수가 100K → 13으로 축소 → **거의 전부 메모리 접근**이 됨
- **읽기/쓰기 모두 적용**. `BufferedWriter.flush()` 안 하면 데이터 손실 가능 주의.

**실측 (JDK 21, 12KB 파일)**:
- `BufferedReader`: **1638 μs**
- `FileChannel + heap buf`: 94 μs (**17x faster**)
- `FileChannel + DirectBuffer`: 40 μs (**40x faster**)

→ `java.io` + Buffered만 써도 괜찮지만, 성능 민감한 코드엔 NIO가 10~40배 빠름.

---

## 3부. Stream은 왜 꼭 닫아야 하나

#### File Descriptor는 **OS 제한 자원**

OS는 프로세스당 열 수 있는 FD 수를 제한:
- Linux 기본: **1024** (soft) / **4096** (hard). `ulimit -n`으로 변경.
- macOS: 256 ~ 10240
- 데몬 서버에선 65536까지 올리기도

**FD leak 증상**:
```
java.io.FileNotFoundException: /tmp/data.txt (Too many open files)
```
→ 애플리케이션 자체는 "열려는" 거라 생각하지만, OS가 거부. **치명적 버그**.

**leak 원인 TOP 3**:
1. `close()` 호출 안 함
2. 예외 발생 시 `close()` 안 함 (try-finally 없이)
3. `URL.openStream()`, DB `Connection`, `ZipInputStream` 등 잊기 쉬운 것들

#### `finalize()`를 믿을 수 없는 이유

```java
protected void finalize() { close(); }  // ❌ 안 됨
```

- `finalize()`는 **GC가 호출할 때까지 안 돌아감**. 언제인지 보장 X.
- 심지어 **GC가 안 돌면 영원히 호출 안 됨** (앱 종료 시까지)
- JDK 9+ `@Deprecated`, JDK 18+ removal 예정
- **긴급한 리소스 해제엔 절대 쓰면 안 됨**

#### try-with-resources (JDK 7+)

**이전 (JDK 6)** — try-finally 지옥:
```java
InputStream in = null;
OutputStream out = null;
try {
    in = new FileInputStream("in.txt");
    out = new FileOutputStream("out.txt");
    // ... copy ...
} finally {
    if (out != null) {
        try { out.close(); } catch (IOException e) { /* log */ }
    }
    if (in != null) {
        try { in.close(); } catch (IOException e) { /* log */ }
    }
}
```

**JDK 7+** — try-with-resources:
```java
try (InputStream in = new FileInputStream("in.txt");
     OutputStream out = new FileOutputStream("out.txt")) {
    in.transferTo(out);
}
// 자동으로 close() 호출. 선언 역순 (out → in) 으로 닫힘.
```

**조건**: `AutoCloseable` 인터페이스 구현한 리소스만 가능.
- verified: `InputStream`은 `AutoCloseable` 구현 ✅
- `Connection`, `Statement`, `ResultSet`, `Lock`, `ExecutorService` 등 거의 모든 리소스 지원

**Exception 처리 뉘앙스**:
- try 블록에서 예외 발생 + close()에서도 예외 → **suppressed exception**으로 처리됨
- `e.getSuppressed()`로 조회 가능
- 과거 try-finally는 close() 예외가 try 예외를 "덮어쓰는" 문제가 있었음 → 해결

**JDK 9+ 개선**:
```java
InputStream in = new FileInputStream("f.txt");  // 외부에서 받음
try (in) {   // 이미 선언된 변수도 사용 가능
    // ...
}
```

**다중 리소스 Tip**:
```java
try (ZipInputStream zis = new ZipInputStream(new FileInputStream("a.zip"))) {
    // ...
}
// ⚠️ FileInputStream은 명시적 close() 안 됨.
// ZipInputStream.close()가 내부 stream도 닫지만, 중간에 ZipInputStream 생성 실패하면 FileInputStream leak!

// ✅ 둘 다 명시
try (FileInputStream fis = new FileInputStream("a.zip");
     ZipInputStream zis = new ZipInputStream(fis)) {
    // ...
}
```

---

## 4부. NIO (New I/O, JDK 1.4) — Buffer, Channel, Selector

#### 3대 요소

```
┌─────────┐  write   ┌────────┐  write   ┌──────────┐
│ Thread  │ ───────▶ │ Buffer │ ───────▶ │ Channel  │ ──▶ 하드웨어
│         │ ◀─────── │        │ ◀─────── │          │ ◀── 하드웨어
└─────────┘   read   └────────┘   read   └──────────┘
                                             ▲
                                             │ 등록
                                        ┌─────────┐
                                        │Selector │  (한 스레드가 여러 Channel 감시)
                                        └─────────┘
```

**Buffer**:
- 데이터 담는 그릇. position, limit, capacity 세 포인터로 관리
- 종류: `ByteBuffer`, `CharBuffer`, `IntBuffer`, ...
- `flip()`, `clear()`, `rewind()`로 모드 전환

**Channel**:
- 양방향 I/O 대상 (`FileChannel`, `SocketChannel`, `DatagramChannel`)
- `read(buffer)` / `write(buffer)`로 Buffer와 교환
- **Non-blocking 모드 지원** (`configureBlocking(false)`)

**Selector** (JDK 1.4):
- **하나의 스레드가 여러 Channel을 감시** (I/O multiplexing)
- OS의 `epoll` (Linux) / `kqueue` (macOS) / `IOCP` (Windows)에 매핑
- 전통적 "스레드 하나당 소켓 하나" 모델을 **"스레드 하나로 수만 소켓"** 모델로 전환

```java
Selector selector = Selector.open();
ServerSocketChannel server = ServerSocketChannel.open();
server.bind(new InetSocketAddress(8080));
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();  // 이벤트 올 때까지 블록
    for (SelectionKey key : selector.selectedKeys()) {
        if (key.isAcceptable()) { ... }
        if (key.isReadable()) { ... }
    }
}
```

→ Netty, Tomcat NIO Connector 등이 이 패턴.

#### DirectBuffer — Zero-copy에 근접

```java
ByteBuffer heap = ByteBuffer.allocate(4096);        // heap 안
ByteBuffer direct = ByteBuffer.allocateDirect(4096); // off-heap (native memory)
```

**heap Buffer 플로우** (read):
```
disk → kernel buf → user buf (heap) → ByteBuffer
          ↑________복사________↑
```

**DirectBuffer 플로우**:
```
disk → kernel buf → DirectBuffer (native memory)
          ↑__복사 생략__↑
```

- 커널 → 유저 복사 **생략** (JVM이 native memory에 직접 매핑)
- **대용량 I/O**에서 성능 유리 (verified: 40 μs vs 94 μs)
- 단, **생성 비용은 더 비쌈** → 재사용(buffer pooling) 권장
- **GC 대상 아님**. 명시적 해제 or JVM이 참조 끊어진 뒤 Cleaner로 수거

#### MappedByteBuffer — 메모리 매핑 파일

```java
try (FileChannel ch = FileChannel.open(path, READ, WRITE)) {
    MappedByteBuffer mbb = ch.map(FileChannel.MapMode.READ_WRITE, 0, ch.size());
    mbb.putInt(0, 42);     // 파일을 메모리처럼 접근
    mbb.force();           // 디스크 동기화
}
```

- 파일을 **메모리 주소 공간에 매핑** (OS의 `mmap()`)
- read/write syscall 없이 **메모리 접근 instruction만**으로 I/O
- OS가 알아서 페이지 단위로 디스크와 동기화
- 대용량 파일, 랜덤 액세스에 유용 (DB, index 파일 등)

#### `transferTo` — Zero-copy

```java
FileChannel source = FileChannel.open(src);
FileChannel dest = FileChannel.open(dst);
source.transferTo(0, source.size(), dest);
```

- 전통적 복사: `src → kernel → user → kernel → dst` (4번 복사)
- `transferTo`: **커널 안에서 직접** 소스에서 목적지로 (Linux `sendfile()` syscall) — "zero-copy"
- 유저 공간을 전혀 안 거침 → 매우 빠름
- 파일 복사, 네트워크로 파일 전송 등에 사용

---

## 5부. 동기 vs 비동기, Blocking vs Non-blocking

**이 두 개념은 서로 다른 축**이지만 많이 혼동됨. 정리.

#### 두 축의 정의

**Blocking vs Non-blocking** = 호출의 **대기 여부**:
- **Blocking**: 결과 올 때까지 **호출자 스레드가 멈춤** (CPU 유휴)
- **Non-blocking**: 호출자가 **즉시 리턴**. 결과 없으면 "아직 없음" 리턴 (예: EAGAIN)

**Synchronous vs Asynchronous** = 결과 **전달 방식**:
- **Sync**: 호출자가 **직접** 결과를 받음 (리턴값 or 폴링으로 확인)
- **Async**: 호출자가 받지 않음. **콜백/이벤트/Future**로 나중에 전달됨

#### 4가지 조합

| 유형 | **Blocking** | **Non-blocking** |
|------|--------------|------------------|
| **Sync** | ⬛ 전통 `read()` — 데이터 올 때까지 멈춤, 결과 리턴값 | 🔄 polling — 즉시 리턴, 결과 있으면 리턴값 (busy loop) |
| **Async** | ❓ 드물다 — "기다리긴 하는데 결과는 콜백"? 거의 안 씀 | ✅ AIO — 즉시 리턴, 결과는 콜백/Future로 |

**조합별 특징**:

**① Sync + Blocking** (`FileInputStream.read()`):
```java
byte[] data = new byte[1024];
int n = fis.read(data);   // 데이터 올 때까지 블록. 리턴값으로 결과 받음.
```
가장 간단. 스레드 1개 = 소켓 1개 → scalability 안 좋음.

**② Sync + Non-blocking** (NIO Selector 패턴):
```java
channel.configureBlocking(false);
int n = channel.read(buf);   // 데이터 없으면 0 리턴 (block X)
if (n > 0) { ... }           // 결과 즉시 처리
```
Busy polling 피하려면 Selector로 "읽을 수 있을 때 알려줘" 등록.

**③ Async + Non-blocking** (`AsynchronousSocketChannel`, Netty):
```java
AsynchronousFileChannel ch = AsynchronousFileChannel.open(path);
Future<Integer> future = ch.read(buf, 0);
// 즉시 리턴, 완료되면 Future.get() 또는 callback으로 결과

ch.read(buf, 0, attachment, new CompletionHandler<>() {
    public void completed(Integer n, Object attach) { ... }
    public void failed(Throwable t, Object attach) { ... }
});
```
가장 효율. OS가 완료되면 콜백 호출. CPU 유휴 없음.

#### Java 매핑 요약

| 유형 | Java API | 유저 코드 모양 |
|-----|---------|------------|
| Sync Blocking | `java.io` (`InputStream.read`) | `int n = in.read(buf);` 블록 |
| Sync Non-blocking | NIO (`SocketChannel.configureBlocking(false)`) + `Selector` | 폴링 or Selector 이벤트 |
| Async | NIO.2 (`AsynchronousSocketChannel`, `AsynchronousFileChannel`, JDK 7+) | Future / CompletionHandler |
| Virtual Thread (JDK 21+) | Sync 코드지만 내부적으로 async | `int n = in.read(buf);` — JVM이 unmount/mount |

#### Linux syscall 레벨 매핑

| 유형 | Linux syscall |
|-----|--------------|
| Blocking | `read(fd, buf, size)` — 기본 동작 |
| Non-blocking | `read()` with `O_NONBLOCK` flag → 즉시 리턴 (`EAGAIN`) |
| Multiplexing | `select` / `poll` / `epoll` / `kqueue` — 여러 fd 감시 |
| Async | `io_uring` (Linux 5.1+), `aio_read()` — 커널이 완료되면 신호 |

#### 흔한 혼동 예시

**"NIO = Non-blocking I/O"는 반쪽 맞음**:
- NIO는 **non-blocking 모드도 지원**할 뿐, 기본은 blocking
- `channel.configureBlocking(true)` (기본) → blocking
- `configureBlocking(false)` → non-blocking
- NIO가 "항상 빠르다"는 오해 — 저수준 API일 뿐, 쓰는 방식에 따라 다름

**Virtual Thread는 async인가?**:
- 코드 모양은 **blocking** (`in.read(buf)` — 리턴값 기다림)
- 하지만 **JVM이 Carrier에서 unmount**해주므로 OS 레벨에선 async처럼 동작
- → **"가짜 blocking"**. 유저는 간단한 sync 코드로 async 성능을 얻음.

#### 성능 특성 비교

| 모델 | 스레드 수 | 메모리 | 코드 복잡도 | scalability |
|-----|---------|-------|----------|-------------|
| Sync Blocking (java.io) | 소켓당 1개 | 많음 (스택 1MB × N) | 낮음 | 수백 |
| Sync Non-blocking (NIO + Selector) | 1~N (적음) | 적음 | 높음 (상태 머신) | 수만 |
| Async (NIO.2 / Netty) | 적음 | 적음 | 높음 (콜백 지옥 or Future 조합) | 수만 |
| Virtual Thread + 기존 Blocking API | 소켓당 1개 (가상) | 적음 | **낮음** (sync처럼 짬) | 수십만~수백만 |

→ **JDK 21+는 Virtual Thread로 "쉬운 코드 + 좋은 성능"** 조합이 가능. Netty/비동기 라이브러리의 필요성이 많이 줄어듦.

---

## 참고 자료

- `java.io.AutoCloseable`, `java.lang.AutoCloseable` javadoc
- `java.nio.channels` 패키지 javadoc
- JEP 149: Reduce Core-Library Memory Usage (JDK 7)
- JEP 353: Reimplement the Legacy Socket API (JDK 13)
- "The C10K Problem" — classic multiplexing 배경
- Linux `man 2 read`, `man 7 epoll`, `man 2 io_uring_enter`
