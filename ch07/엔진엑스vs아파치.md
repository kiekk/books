### 엔진엑스 vs 아파치

#### 지시어

| 아파치 지시어                                                                                                 | 엔진엑스 지시어                                                                                                                                           |
|---------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| ServerTokens<br/>응답 헤더에 전송되는 서버 OS와 소프트웨어의 이름과 버전 정보를 구성할 수 있습니다.                                       | server_tokens<br/>엔진엑스에서 HTTP 모듈의 server_tokens 지시어로 서버 정보를 전송하도록 허용하거나 금지할 수 있습니다.                                                                |
| ServerRoot<br/>구성과 로그 폴더가 들어 있는 서버의 최상위 디렉터리를 정할 수 있습니다.                                                | --prefix 컴파일 옵션<br/>엔진엑스에서 이 옵션은 컴파일할 때 구성 스크립트의 --prefix 스위치나 실행할 때 -p 명령행 옵션으로 정의할 수 있습니다.                                                       |
| PidFile<br/>애플리케이션 PID 파일의 경로를 정합니다.                                                                    | pid: 아파치 지시어와 동일합니다.                                                                                                                               |
| TimeOut<br/>GET 요청의 최대 실행 시간<br/>POST와 PUT 요청에서 두 TCP 패킷 간에 허용되는 최대 지연 시간<br/>두 ACK 패킷 간에 허용되는 최대 지연 시간 | send_timeout: 클라이언트의 두 읽기 동작 사이에 허용되는 최대 지연 시간<br/>client_body_timeout: 클라이언트 요청 본문을 읽을 때의 제한시간<br/>client_header_timeout: 클라이언트 요청 헤더를 읽을 때의 제한시간 |
| KeepAlive, MaxKeepAliveRequests, KeepAliveTimeout<br/>아파치의 연결 유지(keep-alive) 동작을 제어합니다.                 | keepalive_requests<br/>아파치 지시어와 동일합니다.<br/>연결 유지 기능을 완전히 비활성화하려면 keepalive_timeout이나 keepalive_requests를 0으로 설정합니다.                                |
| Listen<br/>아파치와 연결을 기다릴 인터페이스와 포트를 정합니다.                                                                | listen<br/>엔진엑스에서 이 지시어는 가상 호스트 수준(server 블록)에서만 정의됩니다.                                                                                            |
| LoadModule<br/>아파치는 동적으로 여러 모듈을 읽을 수 있습니다.                                                              | -with_****_module<br/>엔진엑스는 모듈을 동적으로 읽을 수 없고, 컴파일할 때 포함해야 합니다.                                                                                     |
| Include<br/>다른 구성 파일을 포함하게 하는 지시어로 와일드카드를 지원합니다.                                                        | include: 아파치 지시어와 동일합니다.                                                                                                                           |
| User, Group<br/>데몬이 실행되는 데 사용될 사용자와 그룹을 정할 수 있습니다.                                                      | user<br/>user 지시어로 사용자와 그룹 모두 지정할 수 있습니다.                                                                                                          |
| ServerAdmin, ServerSignature<br/>오류와 점검 페이지에 표시될 서버 관리자의 이메일 주소와 서명 문구를 지정할 수 있습니다.                     | 동등한 지시어 없음                                                                                                                                         |
| UseCanonicalName<br/>아파치가 자기 참조 URL을 만드는 방법을 정의합니다.                                                     | 동등한 지시어 없음                                                                                                                                         |
| DocumentRoot<br/>아파치가 제공할 파일이 저장된 루트 폴더를 정의합니다.                                                         | root<br/>문서 최상위 경로를 정의할 때 http, server, location if 등 모든 수준에서 추가할 수 있습니다.                                                                          |
| DirectoryIndex, IndexOptions, IndexIgnore<br/>디렉터리 색인과 파일 목록 옵션을 정의합니다.                                 | index, autoindex, random_index, fancyindex<br/>엔진엑스도 다양한 색인 관리 옵션을 제공합니다.                                                                          |
| AccessFileName<br/>페이지 수행 시점에 동적으로 포함되는 .htaccess 파일명을 정의합니다.                                           | 동등한 지시어 없음                                                                                                                                         |
| TypesConfig, DefaultType<br/>MIME 타입 옵션을 정의합니다.                                                         | types, default_type<br/>아파치와 동일합니다.                                                                                                                |
| HostNameLookups<br/>로그를 남기거나 접근을 제어하려고 클라이언트의 IP 주소로 호스트 이름을 찾게 할 수 있습니다.                               | 동등한 지시어 없음                                                                                                                                         |
| ErrorLog, LogLevel, LogFormat, CustomLog<br/>로그를 남기도록 활성화하고 형식을 정의합니다.                                  | access_log, log_format<br/>아파치와 동일합니다.                                                                                                             |
| Alias, AliasMatch, ScriptAlias<br/>디렉터리 별칭 옵션입니다.                                                       | alias<br/>alias는 아파치와 동일하지만 나머지는 동등한 지시어가 없습니다.                                                                                                    |


#### 모듈
| 아파치 모듈           | 엔진엑스 모듈     | 상태    | 구성 스위치                            |
|------------------|-------------|-------|-----------------------------------|
| mod_auth_basic   | auth_basic  | 기본 포함 | --without-http_auth_basic_module  |
| mod_autoindex    | autoindex   | 기본 포함 | --without-http_autoindex_module   |
| mod_charset_lite | charset     | 기본 포함 | --without-http_charset_module     |
| mod_dav          | dav         | 옵션    | --without-http_dav_module         |
| mod_deflate      | gzip        | 기본 포함 | --without-http_gzip_module        |
| mod_expires      | headers     | 기본 포함 | 비활성화 불가능                          |
| mod_fcgid        | fastcgi     | 기본 포함 | --without-http_fastcgi_module     |
| mod_headers      | headers     | 기본 포함 | 비활성화 불가능                          |
| mod_include      | ssi         | 기본 포함 | --without-http_ssl_module         |
| mod_log_config   | log         | 기본 포함 | 비활성화 불가능                          |
| mod_proxy        | proxy       | 기본 포함 | --without-http_proxy_module       |
| mod_rewrite      | rewrite     | 기본 포함 | --without-http_rewrite_module     |
| mod_ssl          | ssl         | 옵션    | --without-http_ssl_module         |
| mod_status       | stub_status | 옵션    | --without-http_stub_status_module |
| mod_substitute   | sub         | 옵션    | --without-http_sub_module         |
| mod_uid          | userid      | 기본 포함 | --without-http_userid_module      |
