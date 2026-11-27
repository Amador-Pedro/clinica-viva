package br.edu.ceub.clinica.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Model: sobrescrita dos métodos abstratos; o adicional fixo incide somente no turno noturno. */
public final class Enfermeiro extends Profissional {
    public enum Turno { DIURNO, NOTURNO }
    private final Turno turno;
    private final BigDecimal adicionalNoturno;
    public Enfermeiro(UUID id, String nome, String registro, BigDecimal hora, int horas, Turno turno, BigDecimal adicional) {
        super(id, nome, registro, hora, horas);
        this.turno = java.util.Objects.requireNonNull(turno, "Turno obrigatório");
        this.adicionalNoturno = Validacao.dinheiro(adicional, "Adicional noturno");
        if (turno == Turno.DIURNO && adicional.signum() != 0)
            throw new IllegalArgumentException("No turno diurno, informe adicional noturno igual a zero.");
    }
    @Override public BigDecimal calcularPagamentoMensal() { return pagamentoBase().add(adicionalNoturno); }
    @Override public String exibirFicha() {
        return "ENFERMEIRO\n\n" + fichaBase() + "\nTurno: " + turno + "\nAdicional noturno mensal: "
                + moeda(adicionalNoturno) + "\n\nPagamento mensal: " + moeda(calcularPagamentoMensal());
    }
    public Turno getTurno() { return turno; }
    public BigDecimal getAdicionalNoturno() { return adicionalNoturno; }
}
