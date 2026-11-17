package br.com.olx.marketplace.infrastructure.adapter.in.web;

import br.com.olx.marketplace.support.PostgresTestcontainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrato HTTP da API, ponta a ponta: PostgreSQL real (Testcontainers) e moderação simulada (WireMock). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestcontainer.class)
class AdApiIT {

    private static final String MODERATION_PATH = "/v1/moderation/check";

    @RegisterExtension
    static WireMockExtension moderation = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void moderationUrl(DynamicPropertyRegistry registry) {
        registry.add("moderation.client.base-url", moderation::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE ads CASCADE");
        moderation.stubFor(WireMock.post(MODERATION_PATH).willReturn(okJson("{\"status\":\"approved\"}")));
    }

    @Test
    void createsApprovedAd() throws Exception {
        ResultActions result = create(validAd("Bicicleta aro 29 Caloi", "esportes", "1500.50"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Bicicleta aro 29 Caloi"))
                .andExpect(jsonPath("$.description").value("Em ótimo estado."))
                .andExpect(jsonPath("$.price").value(1500.50))
                .andExpect(jsonPath("$.category").value("esportes"))
                .andExpect(jsonPath("$.images", hasSize(1)))
                .andExpect(jsonPath("$.status").value("approved"))
                .andExpect(jsonPath("$.rejectionReason").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.moderatedAt").isNotEmpty());

        String id = idOf(result);
        result.andExpect(header().string("Location", endsWith("/api/v1/ads/" + id)));
        mockMvc.perform(get("/api/v1/ads/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));
        moderation.verify(postRequestedFor(urlEqualTo(MODERATION_PATH))
                .withRequestBody(matchingJsonPath("$.category", containing("esportes"))));
    }

    @Test
    void rejectedAdKeepsReasonVisibleInDetailAndIsNotListed() throws Exception {
        moderation.stubFor(WireMock.post(MODERATION_PATH)
                .willReturn(okJson("{\"status\":\"rejected\",\"rejection_reason\":\"contact_info_not_allowed\"}")));

        String id = idOf(create(validAd("Celular chama no whatsapp", "eletronicos", "900"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.rejectionReason").value("contact_info_not_allowed")));

        mockMvc.perform(get("/api/v1/ads/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.rejectionReason").value("contact_info_not_allowed"));
        mockMvc.perform(get("/api/v1/ads"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void moderationOutageKeepsAdPendingInsteadOfRejecting() throws Exception {
        moderation.stubFor(WireMock.post(MODERATION_PATH)
                .willReturn(aResponse().withStatus(500).withBody("{\"error\":\"internal_error\"}")));

        String id = idOf(create(validAd("Sofá retrátil 3 lugares", "moveis", "1200"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending_moderation"))
                .andExpect(jsonPath("$.rejectionReason").value(nullValue())));

        mockMvc.perform(get("/api/v1/ads/{id}", id))
                .andExpect(jsonPath("$.status").value("pending_moderation"));
        moderation.verify(3, postRequestedFor(urlEqualTo(MODERATION_PATH)));
    }

    @Test
    void validatesBusinessRulesWithProblemDetails() throws Exception {
        create(validAd("Curto", "esportes", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());

        create(validAd("Bicicleta aro 29 Caloi", "esportes", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"));

        create(validAd("Bicicleta aro 29 Caloi", "brinquedos", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("category"));

        create("""
                {"title":"Bicicleta aro 29 Caloi","description":"Pouco uso","price":10,"category":"esportes",
                 "images":[]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("images"));

        moderation.verify(0, postRequestedFor(urlEqualTo(MODERATION_PATH)));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ads", Long.class)).isZero();
    }

    @Test
    void reportsMissingFieldsAndMalformedBody() throws Exception {
        create("{\"title\":\"Bicicleta aro 29 Caloi\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors", hasSize(4)));

        create("{not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void listsPublishedAdsWithPaginationAndCategoryFilter() throws Exception {
        create(validAd("Notebook Dell Inspiron", "eletronicos", "3000")).andExpect(status().isCreated());
        create(validAd("Smart TV 50 polegadas", "eletronicos", "2500")).andExpect(status().isCreated());
        create(validAd("Sofá retrátil 3 lugares", "moveis", "1200")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ads").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].title").value("Sofá retrátil 3 lugares"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/ads").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Notebook Dell Inspiron"));

        mockMvc.perform(get("/api/v1/ads").param("category", "eletronicos"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].category").value(everyItem(is("eletronicos"))));
    }

    @Test
    void rejectsInvalidListingParameters() throws Exception {
        mockMvc.perform(get("/api/v1/ads").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/ads").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/ads").param("category", "brinquedos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("category"));
    }

    @Test
    void returnsNotFoundForUnknownAd() throws Exception {
        mockMvc.perform(get("/api/v1/ads/{id}", "6f1c2a3e-0000-4000-8000-000000000000")
                        .header("X-Request-Id", "req-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "req-123"))
                .andExpect(jsonPath("$.code").value("AD_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").value("req-123"));
    }

    @Test
    void rejectsMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/ads/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    private ResultActions create(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/ads").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String idOf(ResultActions result) throws Exception {
        JsonNode body = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private static String validAd(String title, String category, String price) {
        return """
                {"title":"%s","description":"Em ótimo estado.","price":%s,"category":"%s",
                 "images":["https://img.example.com/foto.jpg"]}""".formatted(title, price, category);
    }
}
