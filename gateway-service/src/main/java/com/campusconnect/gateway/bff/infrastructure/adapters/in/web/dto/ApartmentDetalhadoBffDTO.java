package com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ApartmentDetalhadoBffDTO(
        ApartmentDTO apartamento,
        UserDTO proprietario,
        Map<String, String> fontesMicrosservicos,
        LocalDateTime timestampProcessamento
) {}
