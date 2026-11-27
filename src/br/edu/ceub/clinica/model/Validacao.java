package br.edu.ceub.clinica.model;

import java.math.BigDecimal;

/** Model: invariantes compartilhadas. Não conhece Swing, eventos ou arquivos. */
public final class Validacao {
    private Validacao() { }

    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " é obrigatório.");
        String texto = valor.strip();
        if (texto.length() > 160 || texto.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(campo + " deve ter até 160 caracteres, sem quebras de linha.");
        return texto;
    }

    public static BigDecimal dinheiro(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0 || valor.compareTo(new BigDecimal("10000000")) > 0
                || valor.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException(campo + " deve ser de 0 a 10.000.000,00, com até duas casas decimais.");
        return valor.setScale(2);
    }

    public static String cpf(String valor) {
        String cpf = texto(valor, "CPF").replaceAll("[.\\- ]", "");
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1)
            throw new IllegalArgumentException("Informe um CPF válido, com 11 dígitos.");
        for (int tamanho = 9; tamanho <= 10; tamanho++) {
            int soma = 0;
            for (int i = 0; i < tamanho; i++) soma += (cpf.charAt(i) - '0') * (tamanho + 1 - i);
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != cpf.charAt(tamanho) - '0') throw new IllegalArgumentException("Os dígitos verificadores do CPF são inválidos.");
        }
        return cpf;
    }
}
