package br.com.olx.marketplace.application.port.out;

import br.com.olx.marketplace.application.port.in.PageResult;
import br.com.olx.marketplace.domain.ad.Ad;
import br.com.olx.marketplace.domain.ad.AdId;
import br.com.olx.marketplace.domain.ad.Category;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AdRepository {

    void insert(Ad ad);

    /**
     * Grava o resultado da moderação somente se o anúncio ainda estiver pendente no banco.
     * Torna a gravação segura contra moderações concorrentes (requisição + job de reprocessamento).
     *
     * @return {@code false} se o anúncio já havia sido moderado por outro fluxo
     */
    boolean saveModerationOutcome(Ad ad);

    Optional<Ad> findById(AdId id);

    PageResult<Ad> findPublished(Optional<Category> category, int page, int size);

    List<Ad> findPendingCreatedBefore(Instant threshold, int limit);
}
