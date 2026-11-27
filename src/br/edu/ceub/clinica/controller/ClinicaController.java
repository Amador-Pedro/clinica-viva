package br.edu.ceub.clinica.controller;

import br.edu.ceub.clinica.model.*;
import br.edu.ceub.clinica.service.ClinicaService;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.*;
import java.util.UUID;

/** Controller: converte os textos vindos da View e delega regras e persistência ao Service.
 * O listener chama estes métodos; nenhuma fórmula de pagamento pertence ao evento visual.
 */
public final class ClinicaController {
    private final ClinicaService service;
    public ClinicaController(ClinicaService service) { this.service = service; }
    public void salvarProfissional(UUID id, boolean medico, String nome, String registro, String hora,
            String horas, String especialidade, Enfermeiro.Turno turno, String extra) throws IOException {
        UUID chave = id == null ? UUID.randomUUID() : id;
        Profissional p = medico
                ? new Medico(chave, nome, registro, dinheiro(hora), inteiro(horas, "Horas mensais"), especialidade, dinheiro(extra))
                : new Enfermeiro(chave, nome, registro, dinheiro(hora), inteiro(horas, "Horas mensais"), turno, dinheiro(extra));
        service.salvarProfissional(p);
    }
    public void salvarPaciente(UUID id, String nome, String cpf, String telefone) throws IOException {
        service.salvarPaciente(new Paciente(id == null ? UUID.randomUUID() : id, nome, cpf, telefone));
    }
    public void salvarConsulta(UUID id, Paciente paciente, Profissional profissional, String data,
            String hora, String duracao, Consulta.Status status) throws IOException {
        if (paciente == null || profissional == null) throw new IllegalArgumentException("Cadastre e selecione um paciente e um profissional.");
        try {
            LocalDateTime inicio = LocalDateTime.parse(data.strip() + " " + hora.strip(),
                    DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT));
            service.salvarConsulta(new Consulta(id == null ? UUID.randomUUID() : id, paciente.getId(), profissional.getId(), inicio, inteiro(duracao, "Duração"), status));
        } catch (DateTimeParseException e) { throw new IllegalArgumentException("Informe uma data existente (dd/mm/aaaa) e um horário de 00:00 a 23:59."); }
    }
    public void removerProfissional(UUID id) throws IOException { service.removerProfissional(id); }
    public void removerPaciente(UUID id) throws IOException { service.removerPaciente(id); }
    public void removerConsulta(UUID id) throws IOException { service.removerConsulta(id); }
    public static BigDecimal dinheiro(String texto) {
        // Aceita vírgula brasileira OU ponto decimal, sem separador de milhar ambíguo.
        if (texto == null || !texto.strip().matches("\\d{1,8}([,.]\\d{1,2})?"))
            throw new IllegalArgumentException("Informe um valor numérico, como 150,50, sem separador de milhar.");
        return new BigDecimal(texto.strip().replace(',', '.'));
    }
    private static int inteiro(String texto, String campo) {
        try { return Integer.parseInt(texto.strip()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException(campo + " deve ser um número inteiro."); }
    }
}
