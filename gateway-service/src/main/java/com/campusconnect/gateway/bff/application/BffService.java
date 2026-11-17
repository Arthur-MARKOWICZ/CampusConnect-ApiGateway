package com.campusconnect.gateway.bff.application;

import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.ApartmentDTO;
import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.ApartmentDetalhadoBffDTO;
import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.ConversationSummaryDTO;
import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.DashboardBffDTO;
import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.RideDTO;
import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Composição e agregação do Backend For Frontend (BFF).
 *
 * Usa WebClient reativo para chamadas concorrentes aos microsserviços de domínio,
 * propagando o Bearer token quando o cliente o envia.
 */
@Service
public class BffService {

    private static final Logger log = LoggerFactory.getLogger(BffService.class);
    private static final int LIMITE_RECENTES = 5;

    private final WebClient userWebClient;
    private final WebClient apartmentWebClient;
    private final WebClient caronaWebClient;
    private final WebClient chatWebClient;
    private final String userServiceUrl;
    private final String apartmentServiceUrl;

    public BffService(
            WebClient.Builder webClientBuilder,
            @Value("${services.user.url:http://localhost:8081}") String userUrl,
            @Value("${services.apartment.url:http://localhost:8080}") String apartmentUrl,
            @Value("${services.carona.url:http://localhost:8083}") String caronaUrl,
            @Value("${services.chat.url:http://localhost:8082}") String chatUrl) {

        log.info("[BFF SERVICE] Inicializando clientes reativos:");
        log.info("   -> user-service:      {}", userUrl);
        log.info("   -> apartment-service: {}", apartmentUrl);
        log.info("   -> carona-service:    {}", caronaUrl);
        log.info("   -> chat-service:      {}", chatUrl);

        this.userServiceUrl = userUrl;
        this.apartmentServiceUrl = apartmentUrl;
        this.userWebClient = webClientBuilder.baseUrl(userUrl).build();
        this.apartmentWebClient = webClientBuilder.baseUrl(apartmentUrl).build();
        this.caronaWebClient = webClientBuilder.baseUrl(caronaUrl).build();
        this.chatWebClient = webClientBuilder.baseUrl(chatUrl).build();
    }

    public Mono<DashboardBffDTO> obterDashboard(String authorization) {
        log.debug("[BFF] Disparando chamadas concorrentes para users, apartments, rides e conversations...");

        Mono<List<UserDTO>> usersMono = fetchList(
                userWebClient, "/api/v1/users", UserDTO.class, authorization, "user-service");

        Mono<List<ApartmentDTO>> apartmentsMono = fetchList(
                apartmentWebClient, "/api/v1/apartments", ApartmentDTO.class, authorization, "apartment-service");

        Mono<List<RideDTO>> ridesMono = fetchList(
                caronaWebClient, "/api/v1/rides", RideDTO.class, authorization, "carona-service");

        Mono<List<ConversationSummaryDTO>> conversationsMono = fetchList(
                chatWebClient,
                "/api/v1/conversations",
                ConversationSummaryDTO.class,
                authorization,
                "chat-service");

        return Mono.zip(usersMono, apartmentsMono, ridesMono, conversationsMono).map(tuple -> {
            List<UserDTO> users = tuple.getT1();
            List<ApartmentDTO> apartments = tuple.getT2();
            List<RideDTO> rides = tuple.getT3();
            List<ConversationSummaryDTO> conversations = tuple.getT4();

            List<ApartmentDTO> disponiveis = apartments.stream()
                    .filter(ApartmentDTO::available)
                    .toList();

            BigDecimal menorAluguel = disponiveis.stream()
                    .map(ApartmentDTO::rentPrice)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            List<ApartmentDTO> recentes = apartments.stream()
                    .limit(LIMITE_RECENTES)
                    .toList();

            List<RideDTO> caronasAbertas = rides.stream()
                    .filter(RideDTO::aberta)
                    .toList();

            List<RideDTO> caronasAbertasRecentes = caronasAbertas.stream()
                    .limit(LIMITE_RECENTES)
                    .toList();

            Map<String, String> statusMicrosservicos = new LinkedHashMap<>();
            statusMicrosservicos.put("user-service", statusDe(users));
            statusMicrosservicos.put("apartment-service", statusDe(apartments));
            statusMicrosservicos.put("carona-service", statusDe(rides));
            statusMicrosservicos.put("chat-service", statusDe(conversations));

            return new DashboardBffDTO(
                    users.size(),
                    apartments.size(),
                    disponiveis.size(),
                    menorAluguel,
                    recentes,
                    rides.size(),
                    caronasAbertas.size(),
                    caronasAbertasRecentes,
                    conversations.size(),
                    statusMicrosservicos,
                    LocalDateTime.now());
        });
    }

    public Mono<ApartmentDetalhadoBffDTO> obterApartamentoDetalhado(Long id, String authorization) {
        log.debug("[BFF] Consultando apartment-service para o apartamento ID {}", id);

        return apartmentWebClient.get()
                .uri("/api/v1/apartments/{id}", id)
                .headers(headers -> applyAuthorization(headers, authorization))
                .retrieve()
                .bodyToMono(ApartmentDTO.class)
                .flatMap(apartment -> {
                    Long userId = apartment.userId();

                    Mono<Optional<UserDTO>> ownerMono;
                    if (userId != null && userId > 0) {
                        ownerMono = userWebClient.get()
                                .uri("/api/v1/users/{id}", userId)
                                .headers(headers -> applyAuthorization(headers, authorization))
                                .retrieve()
                                .bodyToMono(UserDTO.class)
                                .map(Optional::of)
                                .onErrorReturn(Optional.empty());
                    } else {
                        ownerMono = Mono.just(Optional.empty());
                    }

                    return ownerMono.map(optOwner -> {
                        Map<String, String> fontes = new LinkedHashMap<>();
                        fontes.put("apartamento_origem", apartmentServiceUrl + "/api/v1/apartments/" + id);
                        if (userId != null) {
                            fontes.put("proprietario_origem", userServiceUrl + "/api/v1/users/" + userId);
                        }

                        return new ApartmentDetalhadoBffDTO(
                                apartment,
                                optOwner.orElse(null),
                                fontes,
                                LocalDateTime.now());
                    });
                });
    }

    private <T> Mono<List<T>> fetchList(
            WebClient client,
            String path,
            Class<T> type,
            String authorization,
            String serviceName) {
        return client.get()
                .uri(path)
                .headers(headers -> applyAuthorization(headers, authorization))
                .retrieve()
                .bodyToFlux(type)
                .collectList()
                .onErrorResume(ex -> {
                    log.warn("[BFF FALLBACK] Falha ao consultar {}: {}", serviceName, ex.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    private static String statusDe(List<?> items) {
        return items.isEmpty() ? "OFFLINE/VAZIO" : "ONLINE";
    }

    private static void applyAuthorization(HttpHeaders headers, String authorization) {
        if (authorization != null && !authorization.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }
}
