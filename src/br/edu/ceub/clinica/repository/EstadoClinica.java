package br.edu.ceub.clinica.repository;

import br.edu.ceub.clinica.model.*;
import java.util.List;

/** Snapshot imutável: transporta as coleções entre serviço e disco, sem dependência da GUI. */
public record EstadoClinica(List<Profissional> profissionais, List<Paciente> pacientes, List<Consulta> consultas) {
    public EstadoClinica {
        profissionais = List.copyOf(profissionais);
        pacientes = List.copyOf(pacientes);
        consultas = List.copyOf(consultas);
    }
    public static EstadoClinica vazio() { return new EstadoClinica(List.of(), List.of(), List.of()); }
}
