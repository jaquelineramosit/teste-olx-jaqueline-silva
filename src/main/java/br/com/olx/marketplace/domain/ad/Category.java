package br.com.olx.marketplace.domain.ad;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Categorias aceitas pelo marketplace (as mesmas do contrato de moderação). */
public enum Category {
    ELETRONICOS("eletronicos"),
    MOVEIS("moveis"),
    VEICULOS("veiculos"),
    IMOVEIS("imoveis"),
    ESPORTES("esportes"),
    COLECIONAVEIS("colecionaveis");

    private final String code;

    Category(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static Category fromCode(String code) {
        return Arrays.stream(values())
                .filter(category -> category.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new DomainValidationException("category",
                        "category must be one of: " + allowedCodes()));
    }

    private static String allowedCodes() {
        return Arrays.stream(values()).map(Category::code).collect(Collectors.joining(", "));
    }
}
