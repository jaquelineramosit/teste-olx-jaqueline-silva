package br.com.olx.marketplace.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * Valida apenas a forma do payload (campos presentes). As regras de negócio — tamanho do título,
 * preço positivo, ao menos uma imagem — ficam no domínio, para existir uma única fonte de verdade.
 */
public record CreateAdRequest(
        @Schema(example = "Bicicleta aro 29 Caloi", description = "Entre 10 e 100 caracteres")
        @NotNull String title,
        @Schema(example = "Pouco uso, revisada, com nota fiscal.")
        @NotNull String description,
        @Schema(example = "1500.00", description = "Maior que zero, até 2 casas decimais")
        @NotNull BigDecimal price,
        @Schema(example = "esportes",
                allowableValues = {"eletronicos", "moveis", "veiculos", "imoveis", "esportes", "colecionaveis"})
        @NotNull String category,
        @Schema(example = "[\"https://img.example.com/bike.jpg\"]", description = "De 1 a 10 URLs http(s)")
        @NotNull List<String> images) {
}
