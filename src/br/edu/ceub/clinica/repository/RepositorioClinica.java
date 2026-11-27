package br.edu.ceub.clinica.repository;

import java.io.IOException;

/** Contrato da camada de persistência: serviço depende da abstração, não de detalhes de CSV. */
public interface RepositorioClinica {
    EstadoClinica carregar() throws IOException;
    void salvar(EstadoClinica estado) throws IOException;
}
