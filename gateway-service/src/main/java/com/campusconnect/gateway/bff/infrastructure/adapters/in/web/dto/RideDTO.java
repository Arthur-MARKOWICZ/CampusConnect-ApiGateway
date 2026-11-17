package com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideDTO(
        Long id,
        Long driverUserId,
        String origin,
        String destination,
        LocalDateTime departureAt,
        Integer totalSeats,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        String notes,
        String status
) {
    public boolean aberta() {
        return "OPEN".equalsIgnoreCase(status);
    }
}
