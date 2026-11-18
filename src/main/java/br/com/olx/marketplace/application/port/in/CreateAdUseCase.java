package br.com.olx.marketplace.application.port.in;

import br.com.olx.marketplace.domain.ad.Ad;

import java.math.BigDecimal;
import java.util.List;

public interface CreateAdUseCase {

    /**
     * Cria o anúncio e o submete à moderação. Retorna o anúncio no status resultante:
     * aprovado, reprovado ou ainda pendente (quando a moderação está indisponível).
     */
    Ad create(CreateAdCommand command);

    record CreateAdCommand(String title, String description, BigDecimal price, String category,
                           List<String> imageUrls) {
    }
}
