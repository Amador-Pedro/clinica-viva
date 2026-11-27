package br.edu.ceub.clinica.model;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/** Model abstrato: encapsula o estado e define o contrato polimórfico da folha. */
public abstract class Profissional {
    private final UUID id;
    private final String nome;
    private final String crmCoren;
    private final BigDecimal valorHora;
    private final int horasMensais;

    protected Profissional(UUID id, String nome, String crmCoren, BigDecimal valorHora, int horasMensais) {
        this.id = java.util.Objects.requireNonNull(id, "Identificador obrigatório");
        this.nome = Validacao.texto(nome, "Nome");
        this.crmCoren = Validacao.texto(crmCoren, "CRM/COREN").toUpperCase(Locale.ROOT);
        this.valorHora = Validacao.dinheiro(valorHora, "Valor/hora");
        if (horasMensais < 1 || horasMensais > 744) throw new IllegalArgumentException("Horas mensais devem estar entre 1 e 744.");
        this.horasMensais = horasMensais;
    }

    protected BigDecimal pagamentoBase() { return valorHora.multiply(BigDecimal.valueOf(horasMensais)); }
    protected String fichaBase() {
        return "Nome: " + nome + "\nRegistro: " + crmCoren + "\nValor/hora: " + moeda(valorHora)
                + "\nHoras mensais: " + horasMensais;
    }
    public static String moeda(BigDecimal valor) { return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor); }
    public abstract BigDecimal calcularPagamentoMensal();
    public abstract String exibirFicha();
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getCrmCoren() { return crmCoren; }
    public BigDecimal getValorHora() { return valorHora; }
    public int getHorasMensais() { return horasMensais; }
    @Override public String toString() { return nome + " — " + crmCoren; }
}
