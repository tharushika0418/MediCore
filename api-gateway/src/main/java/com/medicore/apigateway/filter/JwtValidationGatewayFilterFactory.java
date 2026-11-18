package com.medicore.apigateway.filter;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Gateway filter that only lets a request through if it carries a valid JWT.
 * <p>
 * Used in application.yml as {@code - JwtValidation} (the class name minus
 * "GatewayFilterFactory"). For each request it calls auth-service's
 * {@code GET /validate} with the request's Authorization header:
 * <ul>
 *   <li>200 from auth-service: forward the request to the target service</li>
 *   <li>missing token: 401 Unauthorized here (target service never called)</li>
 *   <li>invalid token / auth-service unreachable: handled by
 *       {@link com.medicore.apigateway.exception.JwtValidationException} (401 / 503)</li>
 * </ul>
 */
@Component
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private static final String BEARER_PREFIX = "Bearer ";

    private final WebClient webClient;

    public JwtValidationGatewayFilterFactory(@Value("${auth.service.url}") String authServiceUrl) {
        // Fail fast if auth-service is down or slow, instead of Netty's 30 s default.
        // A timeout raises WebClientRequestException -> 503 (JwtValidationException).
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
                .responseTimeout(Duration.ofSeconds(5));

        this.webClient = WebClient.builder()
                .baseUrl(authServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 1. No "Authorization: Bearer <token>" header: reject without calling auth-service
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return reject(exchange, HttpStatus.UNAUTHORIZED);
            }

            // 2. Ask auth-service whether the token is valid (non-blocking call).
            //    A 2xx answer continues to the target service (3). A 401 answer or an
            //    unreachable auth-service raises a WebClient exception, which
            //    JwtValidationException turns into 401 / 503 for the client.
            return webClient.get()
                    .uri("/validate")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .toBodilessEntity()
                    .then(Mono.defer(() -> chain.filter(exchange)));   // 3. valid: forward
        };
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
