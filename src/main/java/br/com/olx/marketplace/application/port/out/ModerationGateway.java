package br.com.olx.marketplace.application.port.out;

import br.com.olx.marketplace.domain.ad.Ad;

public interface ModerationGateway {

    /**
     * Solicita a decisão de moderação do anúncio.
     *
     * @throws ModerationUnavailableException em qualquer falha técnica (timeout, conexão,
     *                                        erro HTTP, resposta fora do contrato). Nunca é uma reprovação.
     */
    ModerationDecision check(Ad ad);
}
