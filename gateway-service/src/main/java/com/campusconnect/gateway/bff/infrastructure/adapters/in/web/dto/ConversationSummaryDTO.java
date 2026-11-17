package com.campusconnect.gateway.bff.infrastructure.adapters.in.web.dto;

public record ConversationSummaryDTO(
        String conversationId,
        Long otherUserId
) {}
