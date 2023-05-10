### SSL

| 지시어                           | 설명                                                           |
|-------------------------------|--------------------------------------------------------------|
| proxy_ssl_certificate         | SSL 서버의 인증서를 담고 있는 PEM 파일의 경로를 설정합니다.                        |
| proxy_ssl_certificate_key     | SSL 서버의 인정에 쓰는 보안 키 파일의 경로를 설정합니다.                           |
| proxy_ssl_ciphers             | 뒷단 서버와 SSL 통신을 하는 용도의 암호 알고리즘을 설정합니다.                        |
| proxy_ssl_crl                 | PEM 형식의 인증서 폐기 목록(CRL) 파일 경로를 설정합니다.                         |
| proxy_ssl_name                | 엔진엑스가 뒷단 서버 SSL 인증서의 폐기 상태를 검증할 때 사용할 서버 이름을 직접 지정합니다.       |
| proxy_ssl_password_file       | 인증 키를 읽을 때 하나씩 적용해 볼 비밀번호를 보관하는 파일의 경로를 설정합니다.               |
| proxy_ssl_server_name         | on으로 설정하면 서버 이름이 서버명 표시 프로토콜에 따라 뒷단 서버에 알려집니다.               |
| proxy_ssl_session_reuse       | 엔진엑스에게 뒷단 서버와 통신할 때 기존 SSL 세션을 재사용하도록 지시합니다.                 |
| proxy_ssl_protocols           | SSL 뒷단 서버와 통신할 때 사용할 프로토콜을 설정합니다.                            |
| proxy_ssl_trusted_certificate | 신뢰할 수 있는 인증기관 (PEM 형식) 인증서의 경로를 설정합니다.                       |
| proxy_ssl_verify              | on으로 설정하면 엔진엑스는 SSL 뒷단 서버의 인증서를 검증합니다.                       |
| proxy_ssl_verify_depth        | proxy_ssl_verify 지시어가 on으로 설정되면 이 지시어는 인증서 체인의 검증 깊이를 설정합니다. |