package br.com.olx.marketplace.infrastructure.adapter.in.web;

import br.com.olx.marketplace.application.port.in.CreateAdUseCase;
import br.com.olx.marketplace.application.port.in.CreateAdUseCase.CreateAdCommand;
import br.com.olx.marketplace.application.port.in.GetAdByIdUseCase;
import br.com.olx.marketplace.application.port.in.ListAdsUseCase;
import br.com.olx.marketplace.application.port.in.ListAdsUseCase.ListAdsQuery;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdId;
import br.com.olx.marketplace.infrastructure.adapter.in.web.dto.AdResponse;
import br.com.olx.marketplace.infrastructure.adapter.in.web.dto.CreateAdRequest;
import br.com.olx.marketplace.infrastructure.adapter.in.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/ads", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Anúncios")
class AdController {

    private final CreateAdUseCase createAdUseCase;
    private final ListAdsUseCase listAdsUseCase;
    private final GetAdByIdUseCase getAdByIdUseCase;

    AdController(CreateAdUseCase createAdUseCase, ListAdsUseCase listAdsUseCase, GetAdByIdUseCase getAdByIdUseCase) {
        this.createAdUseCase = createAdUseCase;
        this.listAdsUseCase = listAdsUseCase;
        this.getAdByIdUseCase = getAdByIdUseCase;
    }

    @PostMapping
    @Operation(summary = "Cria um anúncio e o submete à moderação",
            description = "Retorna 201 com o status resultante: approved, rejected (com rejectionReason) ou "
                    + "pending_moderation, quando a moderação está indisponível e será reprocessada.")
    @ApiResponse(responseCode = "201", description = "Anúncio criado")
    @ApiResponse(responseCode = "400", description = "Payload inválido ou regra de negócio violada",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    ResponseEntity<AdResponse> create(@Valid @RequestBody CreateAdRequest request) {
        Ad ad = createAdUseCase.create(new CreateAdCommand(request.title(), request.description(), request.price(),
                request.category(), request.images()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(ad.id().value()).toUri();
        return ResponseEntity.created(location).body(AdResponse.from(ad));
    }

    @GetMapping
    @Operation(summary = "Lista anúncios publicados (aprovados)",
            description = "Ordenados do mais recente para o mais antigo. Página começa em 0.")
    @ApiResponse(responseCode = "200", description = "Página de anúncios")
    @ApiResponse(responseCode = "400", description = "Categoria ou paginação inválida",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    PageResponse<AdResponse> list(
            @Parameter(description = "Filtra por categoria", example = "eletronicos",
                    schema = @Schema(allowableValues = {"eletronicos", "moveis", "veiculos", "imoveis", "esportes",
                            "colecionaveis"}))
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(listAdsUseCase.listPublished(new ListAdsQuery(Optional.ofNullable(category), page,
                size)), AdResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um anúncio em qualquer status, com o motivo da reprovação quando houver")
    @ApiResponse(responseCode = "200", description = "Anúncio encontrado")
    @ApiResponse(responseCode = "404", description = "Anúncio inexistente",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    AdResponse getById(@PathVariable UUID id) {
        return AdResponse.from(getAdByIdUseCase.getById(new AdId(id)));
    }
}
