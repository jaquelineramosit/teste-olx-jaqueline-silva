package br.com.olx.marketplace.application.port.in;

public interface RetryPendingModerationUseCase {

    /**
     * Reenvia à moderação um lote de anúncios que ficaram pendentes por falha técnica.
     *
     * @return quantidade de anúncios que saíram do estado pendente nesta execução
     */
    int retryPendingModerations();
}
