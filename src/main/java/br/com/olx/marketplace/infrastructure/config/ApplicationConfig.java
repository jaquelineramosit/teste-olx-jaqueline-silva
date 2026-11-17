package br.com.olx.marketplace.infrastructure.config;

import br.com.olx.marketplace.application.port.in.CreateAdUseCase;
import br.com.olx.marketplace.application.port.in.GetAdByIdUseCase;
import br.com.olx.marketplace.application.port.in.ListAdsUseCase;
import br.com.olx.marketplace.application.port.in.RetryPendingModerationUseCase;
import br.com.olx.marketplace.application.port.out.AdRepository;
import br.com.olx.marketplace.application.port.out.ModerationGateway;
import br.com.olx.marketplace.application.service.AdModerator;
import br.com.olx.marketplace.application.service.CreateAdService;
import br.com.olx.marketplace.application.service.GetAdByIdService;
import br.com.olx.marketplace.application.service.ListAdsService;
import br.com.olx.marketplace.application.service.PendingModerationRetryService;
import br.com.olx.marketplace.infrastructure.adapter.out.moderation.HttpModerationGateway;
import br.com.olx.marketplace.infrastructure.adapter.out.moderation.ModerationClientProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

/**
 * Composição da aplicação: a camada de aplicação não conhece o Spring, então os casos de uso
 * são instanciados aqui e ligados às implementações das portas.
 */
@Configuration
@EnableScheduling
class ApplicationConfig {

    /** Precisão de microssegundos, a mesma do timestamptz do PostgreSQL. */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }

    @Bean
    ModerationGateway moderationGateway(RestClient.Builder builder, ObjectMapper objectMapper,
                                        ModerationClientProperties properties) {
        return HttpModerationGateway.create(builder, objectMapper, properties);
    }

    @Bean
    AdModerator adModerator(ModerationGateway moderationGateway, AdRepository adRepository, Clock clock) {
        return new AdModerator(moderationGateway, adRepository, clock);
    }

    @Bean
    CreateAdUseCase createAdUseCase(AdRepository adRepository, AdModerator adModerator, Clock clock) {
        return new CreateAdService(adRepository, adModerator, clock);
    }

    @Bean
    ListAdsUseCase listAdsUseCase(AdRepository adRepository) {
        return new ListAdsService(adRepository);
    }

    @Bean
    GetAdByIdUseCase getAdByIdUseCase(AdRepository adRepository) {
        return new GetAdByIdService(adRepository);
    }

    @Bean
    RetryPendingModerationUseCase retryPendingModerationUseCase(AdRepository adRepository, AdModerator adModerator,
                                                                Clock clock, ModerationRetryProperties properties) {
        return new PendingModerationRetryService(adRepository, adModerator, clock, properties.minAge(),
                properties.batchSize());
    }
}
