package br.edu.ceub.clinica;

import br.edu.ceub.clinica.controller.ClinicaController;
import br.edu.ceub.clinica.model.*;
import br.edu.ceub.clinica.repository.*;
import br.edu.ceub.clinica.service.ClinicaService;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** Testes de integração sem bibliotecas externas: domínio, controller, arquivo real e falha de I/O.
 * Usam diretório temporário isolado; nunca alteram data/clinica.csv do usuário.
 */
public final class ClinicaTest {
    private static int verificacoes;
    private static BigDecimal valor(String s) { return new BigDecimal(s); }
    private static void verificar(boolean condicao, String mensagem) {
        verificacoes++;
        if (!condicao) throw new AssertionError(mensagem);
    }
    @FunctionalInterface private interface Operacao { void executar() throws Exception; }
    private static void falha(Class<? extends Throwable> tipo, Operacao operacao) throws Exception {
        try { operacao.executar(); }
        catch (Exception e) { verificar(tipo.isInstance(e), "Exceção inesperada: " + e); return; }
        throw new AssertionError("Era esperada uma exceção " + tipo.getName());
    }
    private static Consulta consulta(Paciente p, Profissional m, int hora, int minuto, Consulta.Status status) {
        return new Consulta(UUID.randomUUID(), p.getId(), m.getId(), LocalDateTime.of(2026, 11, 30, hora, minuto), 30, status);
    }
    public static void main(String[] args) throws Exception {
        Path pasta = Files.createTempDirectory("clinica-teste-");
        try {
            Path csv = pasta.resolve("clinica.csv");
            RepositorioCsv repositorio = new RepositorioCsv(csv);
            ClinicaService s = new ClinicaService(repositorio);
            verificar(s.getEstado().pacientes().isEmpty(), "Primeira execução deve iniciar vazia");
            Medico m = new Medico(UUID.randomUUID(), "Dra. Teste; \"A\"", "CRM-DF 100", valor("100.50"), 160, "Clínica médica", valor("500.00"));
            Enfermeiro e = new Enfermeiro(UUID.randomUUID(), "Enfermeiro Teste", "COREN-DF 200", valor("50"), 160, Enfermeiro.Turno.NOTURNO, valor("400"));
            Enfermeiro diurno = new Enfermeiro(UUID.randomUUID(), "Diurno", "COREN-DF 300", valor("50"), 160, Enfermeiro.Turno.DIURNO, valor("0"));
            verificar(m.calcularPagamentoMensal().compareTo(valor("16580")) == 0, "Cálculo do médico");
            verificar(e.calcularPagamentoMensal().compareTo(valor("8400")) == 0, "Cálculo noturno");
            verificar(diurno.calcularPagamentoMensal().compareTo(valor("8000")) == 0, "Cálculo diurno");
            s.salvarProfissional(m); s.salvarProfissional(e);
            verificar(s.calcularFolhaMensal().compareTo(valor("24980")) == 0, "Folha polimórfica");
            verificar(m.exibirFicha().contains("Clínica médica") && e.exibirFicha().contains("NOTURNO"), "Fichas especializadas");
            Paciente p = new Paciente(UUID.randomUUID(), "Paciente de teste", "529.982.247-25", "(61) 99999-0000");
            Paciente p2 = new Paciente(UUID.randomUUID(), "Outro paciente", "11144477735", "61999990001");
            s.salvarPaciente(p); s.salvarPaciente(p2);
            falha(IllegalArgumentException.class, () -> s.salvarPaciente(new Paciente(UUID.randomUUID(), "Duplicado", p.getCpf(), p.getTelefone())));
            falha(IllegalArgumentException.class, () -> s.salvarProfissional(new Medico(UUID.randomUUID(), "Duplicado", "crm-df 100", valor("1"), 1, "Teste", valor("0"))));
            falha(IllegalArgumentException.class, () -> new Paciente(UUID.randomUUID(), "Nome", "00000000000", "61999990000"));
            falha(IllegalArgumentException.class, () -> new Paciente(UUID.randomUUID(), "Nome", "52998224724", "61999990000"));
            falha(IllegalArgumentException.class, () -> new Paciente(UUID.randomUUID(), "Nome", p.getCpf(), "abc"));
            falha(IllegalArgumentException.class, () -> new Enfermeiro(UUID.randomUUID(), "Nome", "COREN 1", valor("1"), 1, Enfermeiro.Turno.DIURNO, valor("10")));
            falha(IllegalArgumentException.class, () -> new Medico(UUID.randomUUID(), "Nome", "CRM 1", valor("-1"), 1, "Teste", valor("0")));
            falha(IllegalArgumentException.class, () -> new Medico(UUID.randomUUID(), "Nome", "CRM 1", valor("1"), 0, "Teste", valor("0")));
            verificar(ClinicaController.dinheiro("150,50").equals(valor("150.50")), "Conversão brasileira");
            falha(IllegalArgumentException.class, () -> ClinicaController.dinheiro("abc"));
            falha(IllegalArgumentException.class, () -> ClinicaController.dinheiro("1.000,00"));
            falha(IllegalArgumentException.class, () -> ClinicaController.dinheiro("NaN"));
            ClinicaController controller = new ClinicaController(s);
            falha(IllegalArgumentException.class, () -> controller.salvarConsulta(null, p, m, "31/02/2026", "10:00", "30", Consulta.Status.AGENDADA));
            falha(IllegalArgumentException.class, () -> controller.salvarProfissional(null, true, "Nome", "CRM 5", "1", "abc", "Teste", null, "0"));
            Consulta primeira = consulta(p, m, 9, 0, Consulta.Status.AGENDADA);
            s.salvarConsulta(primeira);
            falha(IllegalArgumentException.class, () -> s.salvarConsulta(consulta(p2, m, 9, 15, Consulta.Status.AGENDADA)));
            falha(IllegalArgumentException.class, () -> s.salvarConsulta(consulta(p, e, 9, 15, Consulta.Status.AGENDADA)));
            s.salvarConsulta(consulta(p, m, 9, 30, Consulta.Status.AGENDADA));
            verificar(s.getEstado().consultas().size() == 2, "Intervalos adjacentes permitidos");
            falha(IllegalArgumentException.class, () -> s.removerPaciente(p.getId()));
            falha(IllegalArgumentException.class, () -> s.removerProfissional(m.getId()));
            ClinicaService reaberto = new ClinicaService(repositorio);
            verificar(reaberto.getEstado().profissionais().size() == 2 && reaberto.getEstado().consultas().size() == 2, "Reabertura preserva coleções");
            verificar(reaberto.profissional(m.getId()).getNome().equals(m.getNome()), "CSV preserva acentos, aspas e ponto e vírgula");
            verificar(reaberto.profissional(e.getId()) instanceof Enfermeiro, "CSV restaura subtipos");
            verificar(reaberto.getEstado().consultas().get(0).getPacienteId().equals(p.getId()), "CSV preserva associação");
            s.salvarConsulta(new Consulta(primeira.getId(), p.getId(), m.getId(), primeira.getInicio(), 30, Consulta.Status.CANCELADA));
            s.salvarConsulta(consulta(p2, m, 9, 0, Consulta.Status.AGENDADA));
            verificar(s.getEstado().consultas().size() == 3, "Cancelamento libera horário sem apagar histórico");
            s.salvarPaciente(new Paciente(p.getId(), "Nome editado", p.getCpf(), p.getTelefone()));
            verificar(new ClinicaService(repositorio).paciente(p.getId()).getNome().equals("Nome editado"), "Edição persistida");
            for (Consulta c : List.copyOf(s.getEstado().consultas())) s.removerConsulta(c.getId());
            s.removerPaciente(p.getId()); s.removerProfissional(m.getId());
            verificar(new ClinicaService(repositorio).getEstado().consultas().isEmpty(), "Exclusões persistidas");
            falha(UnsupportedOperationException.class, () -> s.getEstado().pacientes().clear());
            RepositorioClinica quebrado = new RepositorioClinica() {
                public EstadoClinica carregar() { return EstadoClinica.vazio(); }
                public void salvar(EstadoClinica estado) throws IOException { throw new IOException("Falha de disco simulada"); }
            };
            ClinicaService seguro = new ClinicaService(quebrado);
            falha(IOException.class, () -> seguro.salvarPaciente(p));
            verificar(seguro.getEstado().pacientes().isEmpty(), "Falha de disco não altera memória");
            String conteudo = Files.readString(csv);
            Path bloqueio = pasta.resolve("destino-diretorio"); Files.createDirectory(bloqueio);
            falha(IOException.class, () -> new RepositorioCsv(bloqueio).salvar(s.getEstado()));
            verificar(Files.readString(csv).equals(conteudo), "Outro arquivo permanece íntegro após falha real");
            repositorio.salvar(new EstadoClinica(List.of(m), List.of(), List.of(primeira)));
            falha(IOException.class, () -> new ClinicaService(repositorio));
            Files.writeString(csv, "CLINICA_TP2;1\n\"TIPO_INVALIDO\"\n");
            falha(IOException.class, repositorio::carregar);
            Files.writeString(csv, "arquivo danificado");
            falha(IOException.class, repositorio::carregar);
            verificar(Files.readString(csv).equals("arquivo danificado"), "Leitura inválida não sobrescreve dados");
            System.out.println("OK: " + verificacoes + " verificações concluídas.");
        } finally {
            try (var caminhos = Files.walk(pasta)) {
                for (Path caminho : caminhos.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(caminho);
            }
        }
    }
}
