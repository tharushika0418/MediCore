package com.medicore.apigateway.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Turns errors from the JWT validation call (gateway -> auth-service /validate)
 * into HTTP responses for the client.
 * <p>
 * The JwtValidation filter lets WebClient errors propagate; this handler maps them:
 * <ul>
 *   <li>auth-service answered 401 (invalid/expired/tampered token): 401 Unauthorized</li>
 *   <li>auth-service could not be reached: 503 Service Unavailable</li>
 * </ul>
 */
@RestControllerAdvice
public class JwtValidationException {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationException.class);

    @ExceptionHandler(WebClientResponseException.Unauthorized.class)
    public Mono<Void> handleUnauthorizedException(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @ExceptionHandler(WebClientRequestException.class)
    public Mono<Void> handleAuthServiceUnavailable(WebClientRequestException ex,
                                                   ServerWebExchange exchange) {
        log.error("Auth service unreachable: {}", ex.getMessage());
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }
}
