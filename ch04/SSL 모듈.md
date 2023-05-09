### SSL 모듈
```
SSL 모듈은 HTTPS를 지원합니다.
정확히 말하면 SSL/TLS 보안 계층 위로 HTTP를 전송합니다.
```

| 지시어                       | 설명                                                                                                        |
|---------------------------|-----------------------------------------------------------------------------------------------------------|
| ssl                       | 구형 버전에서만 사용 가능하며, 엔진엑스 1.15.0 이후에는 listen의 ssl 인자를 대신 사용합니다.<br/>listen 443 ssl이나 listen port ssl과 동등합니다. |
| ssL_certificate           | PEM 인증서의 경로를 정하며, 다른 유형의 인증서를 지정하도록 여러번 사용할 수 있습니다.                                                       |
| ssl_certificate_key       | PEM 비밀 키 파일 경로를 정하며, 다른 유형의 인증서를 지정하도록 여러번 사용할 수 있습니다.                                                    |
| ssl_client_certificate    | 클라이언트 PEM 인증서의 경로를 정합니다.                                                                                  |
| ssl_crl                   | 엔진엑스에게 인증서의 폐기 상태를 확인할 수 있는 인증서 폐기 목록(CRL, Certificate Revocation List) 파일을 읽도록 지시합니다.                    |
| ssl_dhparam               | 디피 헬만(Diffie-Hellman) 매개변수 파일의 경로를 정ㅎ바니다.                                                                 |
| ssl_protocols             | 사용할 프로토콜을 지정합니다.                                                                                          |
| ssl_cipers                | 사용할 암호화 방식을 지정합니다.<br/>사용할 수 있는 암호화 방식은 셸에서 openssl ciphers 명령을 실행시켜 얻을 수 있습니다.                           |
| ssl_prefer_server_ciphers | 서버의 암호화 방식을 클라이언트의 암호화 방식보다 선호할지 여부를 지정합니다.                                                               |
| ssL_verify_client         | 클라이언트에서 보내온 인증서를 검증해서 그 결과를 $ssl_client_verify 변수에 저장합니다.                                                 |
| ssl_verify_depth          | 클라이언트 인증 사슬(certificate chain)의 검증 깊이를 지정합니다.                                                             |
| ssl_session_cache         | SSL 세션의 캐시를 구성합니다.                                                                                        |
| ssl_session_timeout       | SSL 세션이 활성화됐을 경우 이 지시어는 세션 데이터의 제한시간을 정합니다.                                                               |
| ssl_password_phrase       | 비밀 키의 비밀번호를 담고 있는 파일을 지정합니다.<br/>각 비밀번호는 별도 줄에 지정되며 인증서 키를 읽을 때 첫 비밀번호부터 하나씩 시도하게 됩니다.                    |
| ssl_buffer_size           | SSL을 통해 요청을 제공할 때 사용할 버퍼 크기를 지정합니다.                                                                       |
| ssl_session_tickets       | TSL 세션 티켓을 활성화합니다.<br/>이 티켓을 통해 클라이언트가 다시 협상하는 과정 없이 신속히 재접속할 수 있게 됩니다.                                   |
| ssl_session_ticket_key    | TSL 세션 티켓을 암/복호활 때 사용되는 키 파일의 경로를 설정합니다.<br/>기본적으로 임의의 값이 생성됩니다.                                          |
| ssl_trusted_certificate   | 클라이언트 인증서의 신뢰성을 확인할 뿐 아니라 OSCP 응답에 스테이플링(stapling)하는 데도 사용할 신뢰할 수 있는 인증서(PEM 형식)의 경로를 설정합니다.              |


### 변수
- `$ssl_cipher`: 현재 요청에 사용된 암호 방식
- `$ssl_client_serial`: 클라이언트 인증서의 일련번호
- `$ssl_client_s_dn와 $ssl_client_i_dn`: 클라이언트 인증서의 주체와 발행자 DN 값 (RFC 2253 형식)
- `$ssl_client_s_dn_legacy와 $ssl_client_i_dn_legacy`: 클라이언트 인증서의 주체와 발행자 DN 값 (기존 형식)
- `$ssl_protocol`: 현재 요청에 사용된 프로토콜
- `$ssl_client_cert, $ssl_client_raw_cert, ssl_client_escaped_cert`: 각각 클라이언트 인증서 데이터와 가공되지 않은 원천 인증서 데이터와 PEM 형태로 인코딩된 인증서 데이터
- `$ssl_client_verify`: 클라이언트 인증서가 성공적으로 검증되면 SUCCESS 값을, 실패할 때 "FAILED:certificate has expired" 같이 실패한 이유를 갖습니다.
- `$ssl_client_v_start`: 클라이언트 인증서 시작일
- `$ssl_client_v_end`: 클라이언트 인증서 만료일
- `$ssl_client_v_remain`: 클라이언트 인증서 만료일까지 남은 일 수
- `$ssl_session_id`: SSL 세션의 ID

### SSL 스테이플링
```
온라인 인증 프로토콜(OSCP, Online Certificate Status Protocol) 스테이플링으로도 불리우는 SSL 스테이플링은 인증기관에 접속할 필요 없이
클라이언트가 SSL/TLS 서버와 접속하거나 세션을 재개하기 쉽게 해주는 기술로, SSL 협상 시간을 단축합니다.
보통 OSCP 트랜잭션은 클라이언트가 일반적으로 인증기관에 접속해서 서버의 인증서 폐기 상태를 확인하기 때문에 인증기관 서버에 엄청난 부하를 줄 수 있습니다.
이런 문제를 위해 설계된 해법이 스테이플링입니다.
OSCP 기록은 서버가 인증기관에서 정기적으로 취득합니다.
그리고 클라이언트와 교환하려고 스테이플됩니다.
인증기관과 통신을 줄이고자 OSCP 기록이 서버에 최대 48시간까지 캐시됩니다.

SSL 스테이플링을 사용하면 방문객과 서버 간의 통신 속도가 올라갑니다.
엔진엑스에 이를 적용하는 건 상대적으로 단순합니다.
실제 필요한 작업은 다음과 같은 세 지시어를 server 블록에 추가하고 신뢰하는 인증서 사슬 파일을 인증기관에서 얻는 것 뿐입니다.
```

- `ssl_stapling on`: server 블록 내에 SSL 스테이플링 활성화
- `ssl_stapling_verify on`: OSCP 응답 검증 활성화
- `ssl_trusted_certificate 파일 경로`: 신뢰할 전체 인증서 파일 경로 지정(확장자는 .pem 이어야 합니다.)

`이 모듈의 동작을 변경하는 데 선택적으로 사용할 수 있는 두 지시어가 있습니다.`
- `ssl_stapling_file 파일명`: 캐시된 OSCP 기록의 경로, 인증서 파일에 지정된 OSCP 서버 응답의 기록을 대체합니다.
- `ssl_stapling_responder url`: url은 인증기관의 OSCP 서버 URL이며, 인증서 파일에 지정된 URL 값을 대체합니다.