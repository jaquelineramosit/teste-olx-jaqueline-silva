package br.com.olx.marketplace.infrastructure.adapter.out.moderation;

import br.com.olx.marketplace.application.port.out.ModerationDecision;
import br.com.olx.marketplace.application.port.out.ModerationUnavailableException;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.Category;
import br.com.olx.marketplace.domain.ad.RejectionReason;
import br.com.olx.marketplace.support.AdFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Contrato do {@code POST /v1/moderation/check} simulado com WireMock, inclusive as falhas do mock real. */
class HttpModerationGatewayTest {

    private static final String PATH = "/v1/moderation/check";
    /**
     * Folgado de propósito: a primeira chamada da classe (JVM e WireMock "frios") pode passar de 300 ms,
     * o que virava uma retentativa inesperada e tornava as contagens de requisições intermitentes.
     */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(2);
    /** Usado só no teste de timeout, contra um servidor que atrasa a resposta além dele. */
    private static final Duration SHORT_READ_TIMEOUT = Duration.ofMillis(300);

    /**
     * Um servidor novo por teste (campo de instância, não estático). Com um servidor compartilhado, requisições
     * que terminam depois do fim de um teste (timeout, conexão derrubada) entravam na contagem do teste seguinte.
     */
    @RegisterExtension
    WireMockExtension moderation = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private final Ad ad = AdFixtures.pendingAd(Category.ESPORTES);

    @Test
    void sendsExactlyTheContractFieldsAndInterpretsApproval() {
        moderation.stubFor(post(PATH)
                .withRequestBody(equalToJson("""
                        {"title":"Bicicleta aro 29 Caloi","description":"Pouco uso, revisada.",
                         "price":1500.00,"category":"esportes"}
                        """))
                .willReturn(okJson("{\"status\":\"approved\"}")));

        assertThat(gateway().check(ad)).isInstanceOf(ModerationDecision.Approved.class);
    }

    @Test
    void prefersFieldReturnedByTheServiceWhenBothArePresent() {
        moderation.stubFor(post(PATH).willReturn(okJson(
                "{\"status\":\"rejected\",\"rejection_reason\":\"prohibited_item\",\"reason\":\"price_out_of_range\"}")));

        assertThat(gateway().check(ad))
                .isEqualTo(new ModerationDecision.Rejected(new RejectionReason("prohibited_item")));
    }

    @Test
    void readsRejectionReasonFromDocumentedFieldAsFallback() {
        moderation.stubFor(post(PATH).willReturn(okJson("{\"status\":\"rejected\",\"reason\":\"prohibited_item\"}")));

        assertThat(gateway().check(ad))
                .isEqualTo(new ModerationDecision.Rejected(new RejectionReason("prohibited_item")));
    }

    @Test
    void readsRejectionReasonFromFieldReturnedByTheMockImplementation() {
        moderation.stubFor(post(PATH)
                .willReturn(okJson("{\"status\":\"rejected\",\"rejection_reason\":\"contact_info_not_allowed\"}")));

        ModerationDecision decision = gateway().check(ad);

        assertThat(decision).isInstanceOf(ModerationDecision.Rejected.class);
        assertThat(((ModerationDecision.Rejected) decision).reason().code()).isEqualTo("contact_info_not_allowed");
    }

    @Test
    void retriesTransientServerErrorAndSucceeds() {
        moderation.stubFor(post(PATH).inScenario("flaky").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(json(500, "{\"error\":\"internal_error\"}")).willSetStateTo("recovered"));
        moderation.stubFor(post(PATH).inScenario("flaky").whenScenarioStateIs("recovered")
                .willReturn(okJson("{\"status\":\"approved\"}")));

        assertThat(gateway().check(ad)).isInstanceOf(ModerationDecision.Approved.class);
        moderation.verify(2, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void failsAsUnavailableAfterMaxAttemptsOfServerError() {
        moderation.stubFor(post(PATH).willReturn(json(500, "{\"error\":\"internal_error\"}")));

        assertThatThrownBy(() -> gateway().check(ad))
                .isInstanceOf(ModerationUnavailableException.class)
                .hasMessageContaining("3 attempt(s)")
                .hasMessageContaining("HTTP 500");
        moderation.verify(3, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void honoursRateLimitAndRetries() {
        moderation.stubFor(post(PATH).inScenario("rate").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(json(429, "{\"error\":\"too_many_requests\"}").withHeader("Retry-After", "1"))
                .willSetStateTo("allowed"));
        moderation.stubFor(post(PATH).inScenario("rate").whenScenarioStateIs("allowed")
                .willReturn(okJson("{\"status\":\"approved\"}")));

        assertThat(gateway().check(ad)).isInstanceOf(ModerationDecision.Approved.class);
        moderation.verify(2, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void doesNotRetryClientError() {
        moderation.stubFor(post(PATH)
                .willReturn(json(400, "{\"error\":\"validation_error\",\"details\":[\"title is required\"]}")));

        assertThatThrownBy(() -> gateway().check(ad))
                .isInstanceOf(ModerationUnavailableException.class)
                .hasMessageContaining("HTTP 400");
        moderation.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void treatsTimeoutAsUnavailable() {
        WireMockServer slowModeration = new WireMockServer(wireMockConfig().dynamicPort());
        slowModeration.start();
        try {
            slowModeration.stubFor(post(PATH).willReturn(okJson("{\"status\":\"approved\"}")
                    .withFixedDelay((int) SHORT_READ_TIMEOUT.multipliedBy(3).toMillis())));

            assertThatThrownBy(() -> gateway(URI.create(slowModeration.baseUrl()), SHORT_READ_TIMEOUT).check(ad))
                    .isInstanceOf(ModerationUnavailableException.class)
                    .hasMessageContaining("3 attempt(s)")
                    .hasMessageContaining("I/O error");
        } finally {
            slowModeration.stop();
        }
    }

    @Test
    void treatsBrokenConnectionAsUnavailable() {
        moderation.stubFor(post(PATH).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> gateway().check(ad)).isInstanceOf(ModerationUnavailableException.class);
    }

    @Test
    void treatsRefusedConnectionAsUnavailable() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        assertThatThrownBy(() -> gateway(URI.create("http://localhost:" + closedPort)).check(ad))
                .isInstanceOf(ModerationUnavailableException.class)
                .hasMessageContaining("I/O error");
    }

    @Test
    void malformedJsonIsInvalidResponseNotRejection() {
        moderation.stubFor(post(PATH).willReturn(aResponse().withStatus(200).withBody("<html>oops</html>")));

        assertThatThrownBy(() -> gateway().check(ad))
                .isInstanceOf(ModerationUnavailableException.class)
                .hasMessageContaining("invalid response");
        moderation.verify(1, postRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void unknownStatusIsInvalidResponse() {
        moderation.stubFor(post(PATH).willReturn(okJson("{\"status\":\"maybe\"}")));

        assertThatThrownBy(() -> gateway().check(ad))
                .isInstanceOf(ModerationUnavailableException.class)
                .hasMessageContaining("unknown status");
    }

    @Test
    void rejectionWithoutReasonIsInvalidResponse() {
        moderation.stubFor(post(PATH).willReturn(okJson("{\"status\":\"rejected\"}")));

        assertThatThrownBy(() -> gateway().check(ad))
                .isInstanceOf(ModerationUnavailableException.class)
                .hasMessageContaining("rejection without a valid reason");
    }

    private HttpModerationGateway gateway() {
        return gateway(URI.create(moderation.baseUrl()));
    }

    private static HttpModerationGateway gateway(URI baseUrl) {
        return gateway(baseUrl, READ_TIMEOUT);
    }

    private static HttpModerationGateway gateway(URI baseUrl, Duration readTimeout) {
        ModerationClientProperties properties = new ModerationClientProperties(baseUrl, Duration.ofSeconds(1),
                readTimeout, 3, Duration.ofMillis(5), Duration.ofMillis(20));
        return HttpModerationGateway.create(RestClient.builder(), new ObjectMapper(), properties);
    }

    private static ResponseDefinitionBuilder json(int status, String body) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/json").withBody(body);
    }
}
