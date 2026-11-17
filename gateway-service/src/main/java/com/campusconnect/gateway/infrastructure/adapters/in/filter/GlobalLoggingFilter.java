package com.campusconnect.gateway.infrastructure.adapters.in.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Filtro global de observabilidade: mede latência, registra origem/destino
 * e injeta headers de auditoria na resposta.
 */
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GlobalLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        String method = request.getMethod().name();
        String path = request.getURI().getRawPath();
        String query = request.getURI().getRawQuery();
        String fullPath = query != null ? path + "?" + query : path;
        String clientAddress = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getHostString()
                : "desconhecido";

        log.info("[GATEWAY IN]  {} {} | Origem: {}", method, fullPath, clientAddress);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();

            URI targetUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            String target = targetUri != null ? targetUri.toString() : "Local/BFF";

            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 200;

            response.getHeaders().add("X-Gateway-Processed-By", "CampusConnect-Spring-Cloud-Gateway");
            response.getHeaders().add("X-Gateway-Execution-Time-Ms", String.valueOf(duration));

            log.info("[GATEWAY OUT] {} {} -> HTTP {} ({} ms) | Alvo: {}",
                    method, path, statusCode, duration, target);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
