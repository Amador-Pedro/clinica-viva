package br.edu.ceub.clinica.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Model: herança com super() e especialização do pagamento. Taxa é o total mensal de cirurgias. */
public final class Medico extends Profissional {
    private final String especialidade;
    private final BigDecimal taxaCirurgia;
    public Medico(UUID id, String nome, String registro, BigDecimal hora, int horas, String especialidade, BigDecimal taxa) {
        super(id, nome, registro, hora, horas);
        this.especialidade = Validacao.texto(especialidade, "Especialidade");
        this.taxaCirurgia = Validacao.dinheiro(taxa, "Taxa de cirurgia mensal");
    }
    @Override public BigDecimal calcularPagamentoMensal() { return pagamentoBase().add(taxaCirurgia); }
    @Override public String exibirFicha() {
        return "MÉDICO\n\n" + fichaBase() + "\nEspecialidade: " + especialidade + "\nTaxa de cirurgia mensal: "
                + moeda(taxaCirurgia) + "\n\nPagamento mensal: " + moeda(calcularPagamentoMensal());
    }
    public String getEspecialidade() { return especialidade; }
    public BigDecimal getTaxaCirurgia() { return taxaCirurgia; }
}
