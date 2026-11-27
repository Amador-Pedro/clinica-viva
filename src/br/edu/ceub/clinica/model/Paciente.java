package br.edu.ceub.clinica.model;

import java.util.UUID;

/** Model associado a Profissional por Consulta. Objeto imutável evita alterações sem persistência. */
public final class Paciente {
    private final UUID id;
    private final String nome;
    private final String cpf;
    private final String telefone;
    public Paciente(UUID id, String nome, String cpf, String telefone) {
        this.id = java.util.Objects.requireNonNull(id);
        this.nome = Validacao.texto(nome, "Nome do paciente");
        this.cpf = Validacao.cpf(cpf);
        this.telefone = Validacao.texto(telefone, "Telefone").replaceAll("[() +\\-]", "");
        if (!this.telefone.matches("\\d{10,13}")) throw new IllegalArgumentException("Telefone deve conter de 10 a 13 dígitos, incluindo DDD.");
    }
    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getTelefone() { return telefone; }
    @Override public String toString() { return nome + " — " + cpf; }
}
