package br.com.olx.marketplace.infrastructure.adapter.out.moderation;

import br.com.olx.marketplace.application.port.out.ModerationDecision;
import br.com.olx.marketplace.application.port.out.ModerationGateway;
import br.com.olx.marketplace.application.port.out.ModerationUnavailableException;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.RejectionReason;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Cliente HTTP do serviço de moderação ({@code POST /v1/moderation/check}).
 * Converte o contrato externo em {@link ModerationDecision} e trata toda falha técnica como
 * {@link ModerationUnavailableException}, com retentativa curta apenas para erros transitórios.
 */
public class HttpModerationGateway implements ModerationGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpModerationGateway.class);
    private static final String CHECK_PATH = "/v1/moderation/check";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ModerationClientProperties properties;

    HttpModerationGateway(RestClient restClient, ObjectMapper objectMapper, ModerationClientProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * Monta o cliente HTTP com os timeouts configurados (JDK HttpClient).
     * HTTP/1.1 explícito: por padrão o JDK tenta upgrade para HTTP/2 em texto puro (h2c), que o serviço de
     * moderação não suporta — servidores que rejeitam o upgrade derrubam o stream.
     */
    public static HttpModerationGateway create(RestClient.Builder builder, ObjectMapper objectMapper,
                                               ModerationClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        RestClient restClient = builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
        return new HttpModerationGateway(restClient, objectMapper, properties);
    }

    @Override
    public ModerationDecision check(Ad ad) {
        ModerationRequest request = new ModerationRequest(
                ad.title().value(), ad.description(), ad.price().amount(), ad.category().code());

        for (int attempt = 1; ; attempt++) {
            long start = System.nanoTime();
            Outcome outcome = call(request);
            long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

            if (outcome instanceof Outcome.Success success) {
                log.info("Moderation ad={} attempt={} status=200 decision={} durationMs={}",
                        ad.id(), attempt, success.decision().getClass().getSimpleName(), elapsedMs);
                return success.decision();
            }
            Outcome.Failure failure = (Outcome.Failure) outcome;
            log.warn("Moderation ad={} attempt={} failure=\"{}\" retryable={} durationMs={}",
                    ad.id(), attempt, failure.description(), failure.retryable(), elapsedMs);

            if (!failure.retryable() || attempt >= properties.maxAttempts()) {
                throw new ModerationUnavailableException(
                        "moderation failed after %d attempt(s): %s".formatted(attempt, failure.description()));
            }
            pause(failure.retryAfter() != null ? failure.retryAfter() : backoffFor(attempt));
        }
    }

    private Outcome call(ModerationRequest request) {
        try {
            return restClient.post()
                    .uri(CHECK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, res) -> interpret(res.getStatusCode(), res.getHeaders(),
                            res.getBody().readAllBytes()));
        } catch (ResourceAccessException e) {
            // timeout de conexão/leitura, conexão recusada ou interrompida
            return Outcome.Failure.retryable("I/O error: " + e.getMostSpecificCause().getMessage());
        }
    }

    private Outcome interpret(HttpStatusCode status, HttpHeaders headers, byte[] body) {
        if (status.value() == 200) {
            return parseDecision(body);
        }
        if (status.value() == 429) {
            return new Outcome.Failure("HTTP 429 too many requests", true, retryAfter(headers));
        }
        if (status.is5xxServerError()) {
            return Outcome.Failure.retryable("HTTP " + status.value());
        }
        // 400 e demais: a requisição não muda numa nova tentativa
        return Outcome.Failure.permanent("HTTP " + status.value() + " " + abbreviate(body));
    }

    private Outcome parseDecision(byte[] body) {
        JsonNode json;
        try {
            json = objectMapper.readTree(body);
        } catch (IOException e) {
            return Outcome.Failure.permanent("invalid response: malformed JSON");
        }
        if (json == null || !json.isObject()) {
            return Outcome.Failure.permanent("invalid response: body is not a JSON object");
        }
        String status = json.path("status").asText("");
        return switch (status) {
            case "approved" -> new Outcome.Success(new ModerationDecision.Approved());
            case "rejected" -> rejected(json);
            default -> Outcome.Failure.permanent("invalid response: unknown status '" + status + "'");
        };
    }

    /**
     * O mock devolve {@code rejection_reason}; o {@code API.md} documenta {@code reason}.
     * Priorizamos o campo que o serviço realmente envia e mantemos o documentado como alternativa,
     * caso o serviço seja alinhado à documentação (DECISIONS.md, ambiguidade A1).
     */
    private Outcome rejected(JsonNode json) {
        String reason = firstNonBlank(json.path("rejection_reason").asText(null), json.path("reason").asText(null));
        if (reason == null || reason.length() > RejectionReason.MAX_LENGTH) {
            return Outcome.Failure.permanent("invalid response: rejection without a valid reason");
        }
        return new Outcome.Success(new ModerationDecision.Rejected(new RejectionReason(reason)));
    }

    private Duration retryAfter(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
        try {
            Duration requested = Duration.ofSeconds(Long.parseLong(value == null ? "" : value.strip()));
            return requested.compareTo(properties.maxRetryAfter()) > 0 ? properties.maxRetryAfter() : requested;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Duration backoffFor(int attempt) {
        return properties.backoff().multipliedBy(1L << (attempt - 1));
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModerationUnavailableException("interrupted while waiting to retry moderation", e);
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private static String abbreviate(byte[] body) {
        String text = new String(body);
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    /** Corpo enviado ao moderador, exatamente com os campos do contrato. */
    record ModerationRequest(String title, String description, BigDecimal price, String category) {
    }

    private sealed interface Outcome {

        record Success(ModerationDecision decision) implements Outcome {
        }

        record Failure(String description, boolean retryable, Duration retryAfter) implements Outcome {
            static Failure retryable(String description) {
                return new Failure(description, true, null);
            }

            static Failure permanent(String description) {
                return new Failure(description, false, null);
            }
        }
    }
}
