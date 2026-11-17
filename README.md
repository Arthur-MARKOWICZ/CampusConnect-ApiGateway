# Campus Connect — gateway-service
Membros do grupo:  Andre Benato , Arthur Lopes, Gustavo Tissot, Luigi Millani


**Ponto unico de entrada** da API: Spring Cloud Gateway (proxy + CORS + rastreio) e **BFF** (agregacao com WebClient).

Padrao da disciplina ([AULA_ARQSOL_GATEWAY](https://github.com/ricardo-vieira/AULA_ARQSOL_GATEWAY)).

| Item | Valor |
|------|-------|
| Porta (host) | **8090** |
| Stack | Java 21, Spring Boot 3.3, Spring Cloud Gateway (reativo) |
| Swagger | `http://localhost:8090/swagger-ui.html` |
| OpenAPI | `http://localhost:8090/v3/api-docs` |
| Rotas | `http://localhost:8090/actuator/gateway/routes` |

## Topologia

```
[ Cliente ] ---> [ Gateway :8090 ]
                      |-- /api/v1/users/**, /api/v1/auth/**  --> user-service :8081
                      |-- /api/v1/apartments/**              --> apartment-service :8080
                      |-- /api/v1/messages|conversations|/ws --> chat-service :8082
                      |-- /api/v1/rides/**, /api/v1/ride-requests/** --> carona-service :8083
                      +-- /api/bff/**                       --> composicao local (WebClient)
```

## BFF

| Endpoint | Papel |
|----------|-------|
| `GET /api/bff/dashboard` | Agrega users + apartments (`Mono.zip`) |
| `GET /api/bff/apartments/{id}` | Apartamento + proprietario |
| `GET /api/bff/status` | Topologia e CORS (sem auth) |

O BFF propaga o header `Authorization` quando o cliente o envia.

Headers nas respostas roteadas: `X-Gateway-Processed-By`, `X-Gateway-Route`, `X-Gateway-Execution-Time-Ms`, `X-Correlation-Id`.

## Variaveis

| Variavel | Uso (local) | Uso (Compose) |
|----------|-------------|---------------|
| `USER_SERVICE_URL` | `http://localhost:8081` | `http://user-service:8080` |
| `APARTMENT_SERVICE_URL` | `http://localhost:8080` | `http://apartment-service:8080` |
| `CHAT_SERVICE_URL` | `http://localhost:8082` | `http://chat-service:8080` |
| `CARONA_SERVICE_URL` | `http://localhost:8083` | `http://carona-service:8080` |

No Compose, preferir `GATEWAY_*_SERVICE_URL` (ver [`.env.example`](../.env.example)).

## Como executar

```bash
# Via Compose (raiz do repo) — sobe todos os dependentes
docker compose up --build gateway-service

# Local (demais servicos no ar)
cd gateway-service && mvn spring-boot:run
cd gateway-service && mvn test
```

Fluxo tipico: cadastro → login → chama APIs com Bearer via `:8090`. Detalhes: [`../README.md`](../README.md).
