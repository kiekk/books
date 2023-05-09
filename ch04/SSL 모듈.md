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