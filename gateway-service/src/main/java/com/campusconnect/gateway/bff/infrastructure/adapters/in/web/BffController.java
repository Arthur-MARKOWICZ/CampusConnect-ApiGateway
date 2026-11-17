package com.campusconnect.gateway.bff.infrastructure.adapters.in.web;

import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.ApartmentDetalhadoBffDTO;
import com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto.DashboardBffDTO;
import com.campusconnect.gateway.bff.application.BffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller BFF (Backend For Frontend) do API Gateway.
 *
 * Fornece endpoints sob medida para o frontend, eliminando múltiplos round-trips.
 */
@RestController
@RequestMapping("/api/bff")
@CrossOrigin(originPatterns = {
        "http://localhost:3000",
        "http://localhost:3001",
        "http://localhost:3002",
        "http://localhost:3003",
        "http://localhost:3004",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:3001",
        "http://127.0.0.1:3002",
        "http://127.0.0.1:3003",
        "http://127.0.0.1:3004",
        "https://*.azurestaticapps.net",
        "https://*.azurewebsites.net"
})
@Tag(name = "BFF - Backend For Frontend", description = "Endpoints de agregação e composição de dados")
public class BffController {

    private final BffService bffService;

    public BffController(BffService bffService) {
        this.bffService = bffService;
    }

    @GetMapping("/dashboard")
    @Operation(
            summary = "Dashboard consolidado (BFF)",
            description = """
                    Agrega em **um único response** dados de quatro microsserviços, em paralelo:
                    - **user-service** (`/api/v1/users`) — total de usuários
                    - **apartment-service** (`/api/v1/apartments`) — totais, disponíveis, menor aluguel e recentes
                    - **carona-service** (`/api/v1/rides`) — totais, abertas (OPEN) e até 5 recentes abertas
                    - **chat-service** (`/api/v1/conversations`) — total de conversas do usuário autenticado

                    Propaga o Bearer token quando enviado. Se um serviço falhar, aquele domínio volta vazio
                    (fallback) e o restante do dashboard continua disponível.
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard agregado com sucesso",
                    content = @Content(schema = @Schema(implementation = DashboardBffDTO.class)))
    })
    public Mono<ResponseEntity<DashboardBffDTO>> obterDashboard(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return bffService.obterDashboard(authorization)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/apartments/{id}")
    @Operation(
            summary = "Detalhes do apartamento com proprietário (BFF)",
            description = """
                    Busca o anúncio no apartment-service e compõe com os dados do proprietário no user-service
                    em um único response (agregação BFF).
                    """)
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Apartamento e proprietário combinados com sucesso",
                    content = @Content(schema = @Schema(implementation = ApartmentDetalhadoBffDTO.class))),
            @ApiResponse(responseCode = "404", description = "Apartamento não encontrado")
    })
    public Mono<ResponseEntity<ApartmentDetalhadoBffDTO>> obterApartamentoDetalhado(
            @PathVariable("id") Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return bffService.obterApartamentoDetalhado(id, authorization)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/status")
    @Operation(
            summary = "Status da arquitetura Gateway & BFF",
            description = "Informa a topologia atual, portas e componentes do ecossistema de microsserviços.")
    public Mono<ResponseEntity<Map<String, Object>>> statusArquitetura() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("projeto", "Campus Connect — API Gateway & BFF");
        status.put("fase", "Fase 2 - Ponto Único Centralizado");
        status.put("porta_gateway", 8090);
        status.put("rotas_ativas", Map.of(
                "/api/v1/users/**, /api/v1/auth/**", "http://localhost:8081 (user-service)",
                "/api/v1/apartments/**", "http://localhost:8080 (apartment-service)",
                "/api/v1/messages/**, /api/v1/conversations/**, /ws/**", "http://localhost:8082 (chat-service)",
                "/api/v1/rides/**, /api/v1/ride-requests/**", "http://localhost:8083 (carona-service)",
                "/api/bff/**", "Processamento local no Gateway (Composição Reativa)"));
        status.put("bff_agregacao", Map.of(
                "GET /api/bff/dashboard",
                "users + apartments + rides + conversations em um único JSON",
                "GET /api/bff/apartments/{id}",
                "apartment + proprietário (user) em um único JSON"));
        status.put("cors_centralizado", true);
        status.put("origens_permitidas", new String[]{
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:3003",
                "http://localhost:3004"
        });
        status.put("timestamp", LocalDateTime.now());

        return Mono.just(ResponseEntity.ok(status));
    }
}
