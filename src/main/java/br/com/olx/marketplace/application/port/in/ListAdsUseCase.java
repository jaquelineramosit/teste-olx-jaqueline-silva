package br.com.olx.marketplace.application.port.in;

import br.com.olx.marketplace.domain.ad.Ad;

import java.util.Optional;

public interface ListAdsUseCase {

    /** Lista apenas anúncios publicados (aprovados), do mais recente para o mais antigo. */
    PageResult<Ad> listPublished(ListAdsQuery query);

    record ListAdsQuery(Optional<String> category, int page, int size) {

        public ListAdsQuery {
            if (page < 0) {
                throw new IllegalArgumentException("page must be >= 0");
            }
            if (size < 1) {
                throw new IllegalArgumentException("size must be >= 1");
            }
        }
    }
}
