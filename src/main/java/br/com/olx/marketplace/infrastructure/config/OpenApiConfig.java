package br.com.olx.marketplace.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/** Metadados da especificação OpenAPI publicada em /v3/api-docs e /swagger-ui.html. */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Marketplace Ads API",
        version = "1.0.0",
        description = """
                Inserção e listagem de anúncios com moderação automática.
                Todo anúncio nasce `pending_moderation` e passa pelo serviço de moderação:
                aprovados aparecem na listagem; reprovados ficam `rejected`, com `rejectionReason`.
                Falha técnica da moderação mantém o anúncio pendente e ele é reprocessado.
                Erros seguem Problem Details (RFC 9457), com `code`, `errors` e `correlationId`."""))
class OpenApiConfig {
}
