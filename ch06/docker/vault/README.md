# docker-compose 로 vault 실행

```shell
# 먼저 docker desktop 이 실행되어 있는지 확인

docker -v

docker-compose -v
```

```shell
# vault 설정이 작성된 docker-compose 파일을 백그라운드에서 실행
# 이 때 터미널의 경로는 docker-compose 파일이 위치한 경로여야 합니다.

docker-compose up -d
```

```shell
# 실행되었다면 아래와 같은 로그를 볼 수 있는데 그 다음에 vault 로 접속해 환경 변수를 지정해야 합니다.

Attaching to vault
vault  | ==> Vault server configuration:
vault  | 
vault  |                      Cgo: disabled
vault  |    Environment Variables: GODEBUG, HOME, HOSTNAME, PATH
vault  |               Go Version: go1.20.4
vault  |               Listener 1: tcp (addr: "0.0.0.0:8200", cluster address: "0.0.0.0:8201", max_request_duration: "1m30s", max_request_size: "33554432", tls: "disabled")
vault  |                Log Level: 
vault  |                    Mlock: supported: true, enabled: true
vault  |            Recovery Mode: false
vault  |                  Storage: file
vault  |                  Version: Vault v1.13.3, built 2023-06-06T18:12:37Z
vault  |              Version Sha: 3bedf816cbf851656ae9e6bd65dd4a67a9ddff5e
vault  | 
vault  | ==> Vault server started! Log data will stream in below:
vault  | 
vault  | 2024-01-04T11:34:09.284Z [WARN]  unknown or unsupported field file found in configuration at /vault/config/vault.json
vault  | 2024-01-04T11:34:09.284Z [INFO]  proxy environment: http_proxy="" https_proxy="" no_proxy=""
vault  | 2024-01-04T11:34:09.285Z [WARN]  no `api_addr` value specified in config or in VAULT_API_ADDR; falling back to detection if possible, but this value should be manually set 
```

```shell
# vault 환경 변수 설정

docker exec -it vault sh
export VAULT_ADDR="http://127.0.0.1:8200"
```