package com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto;

import java.time.LocalDateTime;

public record UserDTO(Long id, String name, String email, LocalDateTime createAt) {}
