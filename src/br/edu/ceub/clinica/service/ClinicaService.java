package br.edu.ceub.clinica.service;

import br.edu.ceub.clinica.model.*;
import br.edu.ceub.clinica.repository.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/** Service: regras entre entidades, conflitos de agenda e transação salvar-antes-de-publicar.
 * Não usa Swing. Se o disco falhar, o snapshot anterior permanece intacto em memória.
 */
public final class ClinicaService {
    private final RepositorioClinica repositorio;
    private EstadoClinica estado;
    public ClinicaService(RepositorioClinica repositorio) throws IOException {
        this.repositorio = repositorio;
        EstadoClinica carregado = repositorio.carregar();
        try { validar(carregado); }
        catch (IllegalArgumentException e) { throw new IOException("Os dados salvos são inconsistentes: " + e.getMessage(), e); }
        estado = carregado;
    }
    public EstadoClinica getEstado() { return estado; }
    public Profissional profissional(UUID id) {
        return estado.profissionais().stream().filter(p -> p.getId().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Profissional não encontrado."));
    }
    public Paciente paciente(UUID id) {
        return estado.pacientes().stream().filter(p -> p.getId().equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado."));
    }
    public void salvarProfissional(Profissional profissional) throws IOException {
        List<Profissional> lista = new ArrayList<>(estado.profissionais());
        lista.removeIf(p -> p.getId().equals(profissional.getId()));
        lista.add(profissional);
        confirmar(new EstadoClinica(lista, estado.pacientes(), estado.consultas()));
    }
    public void salvarPaciente(Paciente paciente) throws IOException {
        List<Paciente> lista = new ArrayList<>(estado.pacientes());
        lista.removeIf(p -> p.getId().equals(paciente.getId()));
        lista.add(paciente);
        confirmar(new EstadoClinica(estado.profissionais(), lista, estado.consultas()));
    }
    public void salvarConsulta(Consulta consulta) throws IOException {
        List<Consulta> lista = new ArrayList<>(estado.consultas());
        lista.removeIf(c -> c.getId().equals(consulta.getId()));
        lista.add(consulta);
        confirmar(new EstadoClinica(estado.profissionais(), estado.pacientes(), lista));
    }
    public void removerProfissional(UUID id) throws IOException {
        profissional(id);
        if (estado.consultas().stream().anyMatch(c -> c.getProfissionalId().equals(id)))
            throw new IllegalArgumentException("Este profissional possui consultas. Exclua os agendamentos vinculados primeiro.");
        confirmar(new EstadoClinica(estado.profissionais().stream().filter(p -> !p.getId().equals(id)).toList(), estado.pacientes(), estado.consultas()));
    }
    public void removerPaciente(UUID id) throws IOException {
        paciente(id);
        if (estado.consultas().stream().anyMatch(c -> c.getPacienteId().equals(id)))
            throw new IllegalArgumentException("Este paciente possui consultas. Exclua os agendamentos vinculados primeiro.");
        confirmar(new EstadoClinica(estado.profissionais(), estado.pacientes().stream().filter(p -> !p.getId().equals(id)).toList(), estado.consultas()));
    }
    public void removerConsulta(UUID id) throws IOException {
        if (estado.consultas().stream().noneMatch(c -> c.getId().equals(id))) throw new IllegalArgumentException("Consulta não encontrada.");
        confirmar(new EstadoClinica(estado.profissionais(), estado.pacientes(), estado.consultas().stream().filter(c -> !c.getId().equals(id)).toList()));
    }
    public BigDecimal calcularFolhaMensal() {
        // Polimorfismo sobre coleção da superclasse: cada objeto executa sua sobrescrita.
        return estado.profissionais().stream().map(Profissional::calcularPagamentoMensal).reduce(new BigDecimal("0.00"), BigDecimal::add);
    }
    private void confirmar(EstadoClinica candidato) throws IOException {
        validar(candidato);
        repositorio.salvar(candidato);
        estado = candidato;
    }
    private static void validar(EstadoClinica e) {
        Set<UUID> profissionais = new HashSet<>(), pacientes = new HashSet<>(), consultas = new HashSet<>();
        Set<String> registros = new HashSet<>(), cpfs = new HashSet<>();
        for (Profissional p : e.profissionais()) {
            if (!profissionais.add(p.getId()) || !registros.add(p.getClass().getSimpleName() + ":" + p.getCrmCoren()))
                throw new IllegalArgumentException("Já existe profissional com esse registro ou identificador.");
        }
        for (Paciente p : e.pacientes()) {
            if (!pacientes.add(p.getId()) || !cpfs.add(p.getCpf())) throw new IllegalArgumentException("Já existe paciente com esse CPF ou identificador.");
        }
        List<Consulta> anteriores = new ArrayList<>();
        for (Consulta c : e.consultas()) {
            if (!consultas.add(c.getId())) throw new IllegalArgumentException("Identificador de consulta repetido.");
            if (!profissionais.contains(c.getProfissionalId()) || !pacientes.contains(c.getPacienteId()))
                throw new IllegalArgumentException("Selecione um paciente e um profissional cadastrados.");
            if (anteriores.stream().anyMatch(c::conflitaCom))
                throw new IllegalArgumentException("Horário indisponível: o paciente ou profissional já possui uma consulta nesse intervalo.");
            anteriores.add(c);
        }
    }
}
