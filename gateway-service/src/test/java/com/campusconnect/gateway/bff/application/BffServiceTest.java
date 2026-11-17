package com.campusconnect.gateway.bff.application;

import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.DashboardBffDTO;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BffServiceTest {

    private MockWebServer userServer;
    private MockWebServer apartmentServer;
    private MockWebServer caronaServer;
    private MockWebServer chatServer;
    private BffService bffService;

    @BeforeEach
    void setUp() throws IOException {
        userServer = new MockWebServer();
        apartmentServer = new MockWebServer();
        caronaServer = new MockWebServer();
        chatServer = new MockWebServer();
        userServer.start();
        apartmentServer.start();
        caronaServer.start();
        chatServer.start();

        bffService = new BffService(
                WebClient.builder(),
                userServer.url("/").toString().replaceAll("/$", ""),
                apartmentServer.url("/").toString().replaceAll("/$", ""),
                caronaServer.url("/").toString().replaceAll("/$", ""),
                chatServer.url("/").toString().replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() {
        shutdownQuietly(userServer);
        shutdownQuietly(apartmentServer);
        shutdownQuietly(caronaServer);
        shutdownQuietly(chatServer);
    }

    private static void shutdownQuietly(MockWebServer server) {
        try {
            server.shutdown();
        } catch (IOException ignored) {
            // ambiente restrito pode falhar ao sinalizar threads do ServerSocket
        }
    }

    @Test
    void deveAgregarUsersApartmentsCaronasEConversasNoDashboard() {
        enqueueJson(userServer, """
                [{"id":1,"name":"Ana","email":"ana@campus.com","createAt":"2026-01-01T10:00:00"}]
                """);
        enqueueJson(apartmentServer, """
                [{"id":10,"name":"Kitnet","description":"Perto do campus","address":"Rua A",
                  "number":"100","complement":null,"neighborhood":"Centro","city":"Curitiba",
                  "state":"PR","cep":"80000-000","bedrooms":1,"bathrooms":1,
                  "rentPrice":1200.00,"condominiumPrice":200.00,"available":true,"userId":1}]
                """);
        enqueueJson(caronaServer, """
                [
                  {"id":20,"driverUserId":1,"origin":"Campus","destination":"Centro",
                   "departureAt":"2026-11-05T08:00:00","totalSeats":4,"availableSeats":2,
                   "pricePerSeat":10.00,"notes":null,"status":"OPEN"},
                  {"id":21,"driverUserId":2,"origin":"Centro","destination":"Campus",
                   "departureAt":"2026-11-05T18:00:00","totalSeats":3,"availableSeats":0,
                   "pricePerSeat":12.00,"notes":null,"status":"FULL"}
                ]
                """);
        enqueueJson(chatServer, """
                [{"conversationId":"1:2","otherUserId":2,"lastMessage":{"content":"Oi","senderId":1,"createdAt":"2026-11-04T12:00:00Z"}}]
                """);

        StepVerifier.create(bffService.obterDashboard("Bearer token-teste"))
                .assertNext(dashboard -> {
                    assertThat(dashboard.totalUsuarios()).isEqualTo(1);
                    assertThat(dashboard.totalApartamentos()).isEqualTo(1);
                    assertThat(dashboard.totalApartamentosDisponiveis()).isEqualTo(1);
                    assertThat(dashboard.menorAluguelDisponivel()).isEqualByComparingTo(new BigDecimal("1200.00"));
                    assertThat(dashboard.apartamentosRecentes()).hasSize(1);
                    assertThat(dashboard.totalCaronas()).isEqualTo(2);
                    assertThat(dashboard.totalCaronasAbertas()).isEqualTo(1);
                    assertThat(dashboard.caronasAbertasRecentes()).hasSize(1);
                    assertThat(dashboard.caronasAbertasRecentes().get(0).id()).isEqualTo(20L);
                    assertThat(dashboard.totalConversas()).isEqualTo(1);
                    assertThat(dashboard.statusMicrosservicos())
                            .containsEntry("user-service", "ONLINE")
                            .containsEntry("apartment-service", "ONLINE")
                            .containsEntry("carona-service", "ONLINE")
                            .containsEntry("chat-service", "ONLINE");
                })
                .verifyComplete();
    }

    @Test
    void deveRetornarListasVaziasQuandoMicrosservicoFalha() {
        enqueueJson(userServer, """
                [{"id":1,"name":"Ana","email":"ana@campus.com","createAt":"2026-01-01T10:00:00"}]
                """);
        apartmentServer.enqueue(new MockResponse().setResponseCode(500).setBody("erro"));
        caronaServer.enqueue(new MockResponse().setResponseCode(503).setBody("indisponivel"));
        chatServer.enqueue(new MockResponse().setResponseCode(401).setBody("nao autenticado"));

        StepVerifier.create(bffService.obterDashboard("Bearer token-teste"))
                .assertNext(dashboard -> {
                    assertThat(dashboard.totalUsuarios()).isEqualTo(1);
                    assertThat(dashboard.totalApartamentos()).isZero();
                    assertThat(dashboard.totalCaronas()).isZero();
                    assertThat(dashboard.totalCaronasAbertas()).isZero();
                    assertThat(dashboard.caronasAbertasRecentes()).isEmpty();
                    assertThat(dashboard.totalConversas()).isZero();
                    assertThat(dashboard.statusMicrosservicos())
                            .containsEntry("apartment-service", "OFFLINE/VAZIO")
                            .containsEntry("carona-service", "OFFLINE/VAZIO")
                            .containsEntry("chat-service", "OFFLINE/VAZIO");
                })
                .verifyComplete();
    }

    @Test
    void deveLimitarCaronasAbertasRecentesACinco() {
        enqueueJson(userServer, "[]");
        enqueueJson(apartmentServer, "[]");
        enqueueJson(caronaServer, """
                [
                  {"id":1,"driverUserId":1,"origin":"A","destination":"B","departureAt":"2026-11-05T08:00:00",
                   "totalSeats":4,"availableSeats":2,"pricePerSeat":10.00,"notes":null,"status":"OPEN"},
                  {"id":2,"driverUserId":1,"origin":"A","destination":"B","departureAt":"2026-11-05T09:00:00",
                   "totalSeats":4,"availableSeats":2,"pricePerSeat":10.00,"notes":null,"status":"OPEN"},
                  {"id":3,"driverUserId":1,"origin":"A","destination":"B","departureAt":"2026-11-05T10:00:00",
                   "totalSeats":4,"availableSeats":2,"pricePerSeat":10.00,"notes":null,"status":"OPEN"},
                  {"id":4,"driverUserId":1,"origin":"A","destination":"B","departureAt":"2026-11-05T11:00:00",
                   "totalSeats":4,"availableSeats":2,"pricePerSeat":10.00,"notes":null,"status":"OPEN"},
                  {"id":5,"driverUserId":1,"origin":"A","destination":"B","departureAt":"2026-11-05T12:00:00",
                   "totalSeats":4,"availableSeats":2,"pricePerSeat":10.00,"notes":null,"status":"OPEN"},
                  {"id":6,"driverUserId":1,"origin":"A","destination":"B","departureAt":"2026-11-05T13:00:00",
                   "totalSeats":4,"availableSeats":2,"pricePerSeat":10.00,"notes":null,"status":"OPEN"}
                ]
                """);
        enqueueJson(chatServer, "[]");

        StepVerifier.create(bffService.obterDashboard(null))
                .assertNext(dashboard -> {
                    assertThat(dashboard.totalCaronasAbertas()).isEqualTo(6);
                    assertThat(dashboard.caronasAbertasRecentes()).hasSize(5);
                })
                .verifyComplete();
    }

    @Test
    void devePropagarAuthorizationNasQuatroChamadas() throws InterruptedException {
        enqueueJson(userServer, "[]");
        enqueueJson(apartmentServer, "[]");
        enqueueJson(caronaServer, "[]");
        enqueueJson(chatServer, "[]");

        StepVerifier.create(bffService.obterDashboard("Bearer abc.xyz"))
                .assertNext(DashboardBffDTO::totalUsuarios)
                .verifyComplete();

        assertAuthorization(userServer.takeRequest(2, TimeUnit.SECONDS));
        assertAuthorization(apartmentServer.takeRequest(2, TimeUnit.SECONDS));
        assertAuthorization(caronaServer.takeRequest(2, TimeUnit.SECONDS));
        assertAuthorization(chatServer.takeRequest(2, TimeUnit.SECONDS));
    }

    private static void enqueueJson(MockWebServer server, String body) {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body));
    }

    private static void assertAuthorization(RecordedRequest request) {
        assertThat(request).isNotNull();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer abc.xyz");
    }
}
