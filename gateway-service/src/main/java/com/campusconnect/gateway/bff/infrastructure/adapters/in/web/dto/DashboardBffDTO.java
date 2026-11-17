package com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardBffDTO(
        int totalUsuarios,
        int totalApartamentos,
        int totalApartamentosDisponiveis,
        BigDecimal menorAluguelDisponivel,
        List<ApartmentDTO> apartamentosRecentes,
        int totalCaronas,
        int totalCaronasAbertas,
        List<RideDTO> caronasAbertasRecentes,
        int totalConversas,
        Map<String, String> statusMicrosservicos,
        LocalDateTime timestampGeracao
) {}
