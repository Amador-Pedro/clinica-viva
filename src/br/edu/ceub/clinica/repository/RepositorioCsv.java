package br.edu.ceub.clinica.repository;

import br.edu.ceub.clinica.model.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** Repository: único lugar que lê/grava o estado em disco; CSV UTF-8 com escape de aspas.
 * Um snapshot único mantém os vínculos consistentes; publicação por renomeação atômica.
 */
public final class RepositorioCsv implements RepositorioClinica {
    private static final String CABECALHO = "CLINICA_TP2;1";
    private final Path arquivo;
    public RepositorioCsv(Path arquivo) { this.arquivo = arquivo.toAbsolutePath(); }

    @Override public EstadoClinica carregar() throws IOException {
        if (Files.notExists(arquivo)) return EstadoClinica.vazio();
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        if (linhas.isEmpty() || !linhas.get(0).equals(CABECALHO)) throw new IOException("Cabeçalho do arquivo CSV inválido.");
        List<Profissional> profissionais = new ArrayList<>();
        List<Paciente> pacientes = new ArrayList<>();
        List<Consulta> consultas = new ArrayList<>();
        for (int i = 1; i < linhas.size(); i++) {
            try {
                List<String> c = separar(linhas.get(i));
                switch (c.get(0)) {
                    case "MEDICO" -> {
                        tamanho(c, 8);
                        profissionais.add(new Medico(uuid(c, 1), c.get(2), c.get(3), decimal(c, 4), Integer.parseInt(c.get(5)), c.get(6), decimal(c, 7)));
                    }
                    case "ENFERMEIRO" -> {
                        tamanho(c, 8);
                        profissionais.add(new Enfermeiro(uuid(c, 1), c.get(2), c.get(3), decimal(c, 4), Integer.parseInt(c.get(5)), Enfermeiro.Turno.valueOf(c.get(6)), decimal(c, 7)));
                    }
                    case "PACIENTE" -> {
                        tamanho(c, 5);
                        pacientes.add(new Paciente(uuid(c, 1), c.get(2), c.get(3), c.get(4)));
                    }
                    case "CONSULTA" -> {
                        tamanho(c, 7);
                        consultas.add(new Consulta(uuid(c, 1), uuid(c, 2), uuid(c, 3), LocalDateTime.parse(c.get(4)), Integer.parseInt(c.get(5)), Consulta.Status.valueOf(c.get(6))));
                    }
                    default -> throw new IllegalArgumentException("Tipo de registro desconhecido.");
                }
            } catch (RuntimeException e) {
                throw new IOException("CSV inválido na linha " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
        return new EstadoClinica(profissionais, pacientes, consultas);
    }

    @Override public void salvar(EstadoClinica estado) throws IOException {
        List<String> linhas = new ArrayList<>();
        linhas.add(CABECALHO);
        for (Profissional p : estado.profissionais()) {
            if (p instanceof Medico m) linhas.add(linha("MEDICO", m.getId(), m.getNome(), m.getCrmCoren(), m.getValorHora(), m.getHorasMensais(), m.getEspecialidade(), m.getTaxaCirurgia()));
            else if (p instanceof Enfermeiro e) linhas.add(linha("ENFERMEIRO", e.getId(), e.getNome(), e.getCrmCoren(), e.getValorHora(), e.getHorasMensais(), e.getTurno(), e.getAdicionalNoturno()));
            else throw new IOException("Tipo de profissional sem serialização definida.");
        }
        for (Paciente p : estado.pacientes()) linhas.add(linha("PACIENTE", p.getId(), p.getNome(), p.getCpf(), p.getTelefone()));
        for (Consulta c : estado.consultas()) linhas.add(linha("CONSULTA", c.getId(), c.getPacienteId(), c.getProfissionalId(), c.getInicio(), c.getDuracaoMinutos(), c.getStatus()));
        Files.createDirectories(arquivo.getParent());
        Path temporario = Files.createTempFile(arquivo.getParent(), "clinica-", ".tmp");
        try {
            Files.write(temporario, linhas, StandardCharsets.UTF_8);
            // Se o sistema de arquivos não suporta operação atômica, falha sem sobrescrever o original.
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporario); }
    }

    private static UUID uuid(List<String> c, int i) { return UUID.fromString(c.get(i)); }
    private static BigDecimal decimal(List<String> c, int i) { return new BigDecimal(c.get(i)); }
    private static void tamanho(List<String> c, int n) {
        if (c.size() != n) throw new IllegalArgumentException("Quantidade de colunas incorreta.");
    }
    private static String linha(Object... valores) {
        return Arrays.stream(valores).map(Object::toString).map(s -> "\"" + s.replace("\"", "\"\"") + "\"").collect(java.util.stream.Collectors.joining(";"));
    }
    private static List<String> separar(String linha) {
        List<String> campos = new ArrayList<>();
        int i = 0;
        while (i < linha.length()) {
            if (linha.charAt(i++) != '"') throw new IllegalArgumentException("Campo sem aspas.");
            StringBuilder campo = new StringBuilder();
            boolean fechado = false;
            while (i < linha.length()) {
                char c = linha.charAt(i++);
                if (c != '"') campo.append(c);
                else if (i < linha.length() && linha.charAt(i) == '"') { campo.append('"'); i++; }
                else { fechado = true; break; }
            }
            if (!fechado) throw new IllegalArgumentException("Aspas não fechadas.");
            campos.add(campo.toString());
            if (i < linha.length() && (linha.charAt(i++) != ';' || i == linha.length()))
                throw new IllegalArgumentException("Separador inválido.");
        }
        if (campos.isEmpty()) throw new IllegalArgumentException("Linha vazia.");
        return campos;
    }
}
