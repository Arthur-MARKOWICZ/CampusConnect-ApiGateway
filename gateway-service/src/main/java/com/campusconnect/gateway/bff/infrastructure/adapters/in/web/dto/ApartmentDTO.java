package com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto;

import java.math.BigDecimal;

public record ApartmentDTO(
        Long id,
        String name,
        String description,
        String address,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String cep,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal rentPrice,
        BigDecimal condominiumPrice,
        boolean available,
        Long userId
) {}
