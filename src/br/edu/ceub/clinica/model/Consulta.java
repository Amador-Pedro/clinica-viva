package br.edu.ceub.clinica.model;

import java.time.LocalDateTime;
import java.util.UUID;

/** Model: associação explícita entre um paciente e um profissional, identificados por UUID. */
public final class Consulta {
    public enum Status { AGENDADA, CONCLUIDA, CANCELADA }
    private final UUID id;
    private final UUID pacienteId;
    private final UUID profissionalId;
    private final LocalDateTime inicio;
    private final int duracaoMinutos;
    private final Status status;
    public Consulta(UUID id, UUID paciente, UUID profissional, LocalDateTime inicio, int duracao, Status status) {
        this.id = java.util.Objects.requireNonNull(id);
        this.pacienteId = java.util.Objects.requireNonNull(paciente);
        this.profissionalId = java.util.Objects.requireNonNull(profissional);
        this.inicio = java.util.Objects.requireNonNull(inicio);
        this.status = java.util.Objects.requireNonNull(status);
        if (inicio.getYear() < 1900 || inicio.getYear() > 9999 || inicio.getSecond() != 0 || inicio.getNano() != 0)
            throw new IllegalArgumentException("Informe uma data válida, com precisão de minutos.");
        if (duracao < 10 || duracao > 240) throw new IllegalArgumentException("A duração deve ser de 10 a 240 minutos.");
        this.duracaoMinutos = duracao;
    }
    public boolean conflitaCom(Consulta outra) {
        return status != Status.CANCELADA && outra.status != Status.CANCELADA
                && (pacienteId.equals(outra.pacienteId) || profissionalId.equals(outra.profissionalId))
                && inicio.isBefore(outra.inicio.plusMinutes(outra.duracaoMinutos))
                && outra.inicio.isBefore(inicio.plusMinutes(duracaoMinutos));
    }
    public UUID getId() { return id; }
    public UUID getPacienteId() { return pacienteId; }
    public UUID getProfissionalId() { return profissionalId; }
    public LocalDateTime getInicio() { return inicio; }
    public int getDuracaoMinutos() { return duracaoMinutos; }
    public Status getStatus() { return status; }
}
