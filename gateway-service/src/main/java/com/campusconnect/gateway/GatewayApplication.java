package com.campusconnect.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class GatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Campus Connect — API Gateway & BFF")
                        .version("1.0.0")
                        .description("""
                                ### Disciplina de Arquitetura de Soluções (Fase 2)

                                Este projeto implementa o padrão **API Gateway** e **Backend For Frontend (BFF)**:
                                - **Roteamento Reverso Transparente:**
                                  - `/api/v1/users/**`, `/api/v1/auth/**` &rarr; user-service (:8081)
                                  - `/api/v1/apartments/**` &rarr; apartment-service (:8080)
                                  - `/api/v1/messages/**`, `/api/v1/conversations/**`, `/ws/**` &rarr; chat-service (:8082)
                                - **CORS Centralizado:** Elimina a necessidade de cada microsserviço gerenciar origens.
                                - **Observabilidade & Tracing:** Registra tempo de resposta e propaga `X-Correlation-Id`.
                                - **Agregação BFF:** `GET /api/bff/dashboard` retorna **users + apartments + rides + conversations** em um único JSON (chamadas concorrentes).
                                - **Composição BFF:** `GET /api/bff/apartments/{id}` combina apartamento + proprietário.
                                """)
                        .contact(new Contact().name("Campus Connect — Arquitetura de Soluções"))
                        .license(new License().name("Uso Acadêmico")));
    }

    @Bean
    public CommandLineRunner printWelcome() {
        return args -> {
            log.info("============================================================================");
            log.info(" CAMPUS CONNECT API GATEWAY & BFF INICIALIZADO COM SUCESSO!");
            log.info("============================================================================");
            log.info(" Porta do Gateway:        http://localhost:8090");
            log.info(" Swagger UI do Gateway:   http://localhost:8090/swagger-ui.html");
            log.info(" Rotas Ativas (Actuator): http://localhost:8090/actuator/gateway/routes");
            log.info(" Dashboard BFF:           http://localhost:8090/api/bff/dashboard");
            log.info(" Status BFF:              http://localhost:8090/api/bff/status");
            log.info(" Rota Users/Auth:         http://localhost:8090/api/v1/users|auth (-> :8081)");
            log.info(" Rota Apartments:         http://localhost:8090/api/v1/apartments (-> :8080)");
            log.info(" Rota Chat/WS:            http://localhost:8090/api/v1/messages|conversations|/ws (-> :8082)");
            log.info("============================================================================");
        };
    }
}
