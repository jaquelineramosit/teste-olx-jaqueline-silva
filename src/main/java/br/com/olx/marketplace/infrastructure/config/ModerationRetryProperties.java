package br.com.olx.marketplace.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param enabled   liga/desliga o job de reprocessamento
 * @param interval  intervalo entre execuções
 * @param minAge    idade mínima do anúncio pendente para ser reprocessado
 * @param batchSize anúncios por execução
 */
@ConfigurationProperties(prefix = "moderation.retry")
public record ModerationRetryProperties(boolean enabled, Duration interval, Duration minAge, int batchSize) {
}
