CREATE TABLE ads (
    id               UUID          PRIMARY KEY,
    title            VARCHAR(100)  NOT NULL,
    description      VARCHAR(5000) NOT NULL,
    price            NUMERIC(12, 2) NOT NULL,
    category         VARCHAR(30)   NOT NULL,
    status           VARCHAR(30)   NOT NULL,
    rejection_reason VARCHAR(100),
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    moderated_at     TIMESTAMPTZ,
    CONSTRAINT ck_ads_price_positive CHECK (price > 0),
    CONSTRAINT ck_ads_category CHECK (category IN
        ('ELETRONICOS', 'MOVEIS', 'VEICULOS', 'IMOVEIS', 'ESPORTES', 'COLECIONAVEIS')),
    CONSTRAINT ck_ads_status CHECK (status IN ('PENDING_MODERATION', 'APPROVED', 'REJECTED')),
    -- motivo existe se, e somente se, o anúncio foi reprovado
    CONSTRAINT ck_ads_rejection_reason CHECK ((status = 'REJECTED') = (rejection_reason IS NOT NULL))
);

CREATE TABLE ad_images (
    ad_id    UUID          NOT NULL REFERENCES ads (id) ON DELETE CASCADE,
    position INTEGER       NOT NULL,
    url      VARCHAR(2048) NOT NULL,
    PRIMARY KEY (ad_id, position)
);

-- Listagem pública (somente aprovados), com e sem filtro de categoria, na ordem da API.
CREATE INDEX ix_ads_published ON ads (created_at DESC, id DESC) WHERE status = 'APPROVED';
CREATE INDEX ix_ads_published_category ON ads (category, created_at DESC, id DESC) WHERE status = 'APPROVED';

-- Busca de pendentes pelo job de reprocessamento da moderação.
CREATE INDEX ix_ads_pending ON ads (created_at) WHERE status = 'PENDING_MODERATION';
