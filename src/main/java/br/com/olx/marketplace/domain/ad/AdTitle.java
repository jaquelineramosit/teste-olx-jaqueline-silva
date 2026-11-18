package br.com.olx.marketplace.domain.ad;

/**
 * Título do anúncio: entre 10 e 100 caracteres, contados após remover espaços das pontas
 * e em code points (um emoji conta como um caractere).
 */
public record AdTitle(String value) {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 100;

    public AdTitle {
        if (value == null) {
            throw new DomainValidationException("title", "title is required");
        }
        value = value.strip();
        int length = value.codePointCount(0, value.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new DomainValidationException("title",
                    "title must have between %d and %d characters".formatted(MIN_LENGTH, MAX_LENGTH));
        }
    }
}
