package br.com.olx.marketplace.infrastructure.adapter.out.moderation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

/**
 * @param baseUrl        URL base do serviço de moderação
 * @param connectTimeout tempo máximo para abrir a conexão
 * @param readTimeout    tempo máximo de espera pela resposta (o mock leva até 2 s)
 * @param maxAttempts    tentativas por moderação, contando a primeira
 * @param backoff        espera base entre tentativas (dobra a cada nova tentativa)
 * @param maxRetryAfter  teto para o header Retry-After do 429
 */
@ConfigurationProperties(prefix = "moderation.client")
public record ModerationClientProperties(URI baseUrl, Duration connectTimeout, Duration readTimeout,
                                         int maxAttempts, Duration backoff, Duration maxRetryAfter) {
}
