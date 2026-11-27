package br.edu.ceub.clinica.view;

import br.edu.ceub.clinica.controller.ClinicaController;
import br.edu.ceub.clinica.model.*;
import br.edu.ceub.clinica.service.ClinicaService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** View Swing: apresenta informações e recolhe entradas. Listeners chamam o Controller;
 * cálculos ficam no Model/Service e File I/O no Repository. Alertas tratam falhas sem encerrar a tela.
 */
public final class ClinicaView extends JFrame {
    private static final Color FUNDO = new Color(244, 247, 250);
    private static final Color VERDE = new Color(15, 101, 104);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private final ClinicaService service;
    private final ClinicaController controller;
    private final DefaultTableModel profissionais = modelo("Nome", "Categoria", "CRM / COREN", "Valor/hora", "Horas/mês", "Pagamento mensal");
    private final DefaultTableModel pacientes = modelo("Nome", "CPF", "Telefone");
    private final DefaultTableModel consultas = modelo("Data", "Hora", "Paciente", "Profissional", "Minutos", "Status");
    private final JTable tabelaProfissionais = tabela(profissionais);
    private final JTable tabelaPacientes = tabela(pacientes);
    private final JTable tabelaConsultas = tabela(consultas);
    private final JLabel totalProfissionais = indicador(), totalPacientes = indicador(), totalConsultas = indicador(), folha = indicador();
    private final JLabel aviso = new JLabel("Pronto. As alterações são salvas automaticamente.");
    private final JTabbedPane abas = new JTabbedPane();
    private List<Profissional> listaProfissionais;
    private List<Paciente> listaPacientes;
    private List<Consulta> listaConsultas;

    public ClinicaView(ClinicaService service, ClinicaController controller) {
        super("Clínica Viva | Gestão de clínica — TP2");
        this.service = service;
        this.controller = controller;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(950, 640));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        JPanel raiz = new JPanel(new BorderLayout(0, 18));
        raiz.setBackground(FUNDO);
        raiz.setBorder(new EmptyBorder(22, 26, 18, 26));
        raiz.add(cabecalho(), BorderLayout.NORTH);
        abas.setFont(new Font("Segoe UI", Font.BOLD, 14));
        abas.addTab("Visão geral", painelInicial());
        abas.addTab("Profissionais", painelTabela(tabelaProfissionais, "Equipe e remuneração", "Cadastre médicos e enfermeiros e consulte a ficha de pagamento.",
                botao("Novo profissional", () -> editarProfissional(null)),
                botao("Editar", () -> editarProfissional(profissionalSelecionado())),
                botao("Ver ficha", this::exibirFicha), botao("Excluir", this::excluirProfissional)));
        abas.addTab("Pacientes", painelTabela(tabelaPacientes, "Cadastro de pacientes", "Mantenha os dados de identificação e contato dos pacientes.",
                botao("Novo paciente", () -> editarPaciente(null)),
                botao("Editar", () -> editarPaciente(pacienteSelecionado())), botao("Excluir", this::excluirPaciente)));
        abas.addTab("Agenda", painelTabela(tabelaConsultas, "Agenda de atendimentos", "Consulte horários, reagende e atualize o status dos atendimentos.",
                botao("Agendar consulta", () -> editarConsulta(null)),
                botao("Editar / status", () -> editarConsulta(consultaSelecionada())), botao("Excluir", this::excluirConsulta)));
        raiz.add(abas, BorderLayout.CENTER);
        aviso.setForeground(new Color(65, 85, 100));
        aviso.setBorder(new EmptyBorder(4, 4, 0, 0));
        raiz.add(aviso, BorderLayout.SOUTH);
        setContentPane(raiz);
        atualizar();
    }

    private JPanel cabecalho() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel titulo = new JLabel("Clínica Viva");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 30));
        titulo.setForeground(VERDE);
        p.add(titulo, BorderLayout.NORTH);
        p.add(new JLabel("GESTÃO DE CLÍNICA  /  Pessoas, agenda e pagamentos"), BorderLayout.SOUTH);
        p.add(botao("Ajuda", () -> JOptionPane.showMessageDialog(this,
                "COMO USAR\n1. Cadastre profissionais e pacientes.\n2. Agende uma consulta na aba Agenda.\n"
                + "3. Use Editar / status para reagendar, concluir ou cancelar.\n4. As alterações são salvas a cada confirmação.\n\n"
                + "PAGAMENTO MENSAL\nMédico: valor/hora × horas mensais + taxa mensal de cirurgia.\n"
                + "Enfermeiro: valor/hora × horas mensais + adicional noturno mensal.\n"
                + "Turno diurno exige adicional zero. São regras didáticas deste projeto.\n\n"
                + "Use valores como 150,50, sem separador de milhar.\nUma consulta cancelada libera o horário.\n"
                + "Para excluir uma pessoa, exclua primeiro suas consultas vinculadas.", "Ajuda", JOptionPane.INFORMATION_MESSAGE)), BorderLayout.EAST);
        return p;
    }
    private JPanel painelInicial() {
        JPanel p = new JPanel(new BorderLayout(16, 24));
        p.setBackground(FUNDO);
        p.setBorder(new EmptyBorder(24, 12, 20, 12));
        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(card("PROFISSIONAIS", totalProfissionais));
        cards.add(card("PACIENTES", totalPacientes));
        cards.add(card("CONSULTAS AGENDADAS", totalConsultas));
        cards.add(card("FOLHA MENSAL ESTIMADA", folha));
        p.add(cards, BorderLayout.NORTH);
        JPanel centro = new JPanel();
        centro.setBackground(Color.WHITE);
        centro.setBorder(new EmptyBorder(30, 30, 30, 30));
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("Sua clínica, organizada.");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 25));
        titulo.setForeground(VERDE);
        centro.add(titulo);
        centro.add(Box.createVerticalStrut(18));
        centro.add(new JLabel("Comece pelos cadastros de profissionais e pacientes. Depois, organize os atendimentos na agenda."));
        centro.add(Box.createVerticalStrut(10));
        centro.add(new JLabel("Consulte a ficha de cada profissional para visualizar os componentes de seu pagamento."));
        centro.add(Box.createVerticalStrut(28));
        centro.add(botao("Ir para a agenda", () -> abas.setSelectedIndex(3)));
        centro.add(Box.createVerticalStrut(20));
        centro.add(new JLabel("Os cadastros permanecem disponíveis quando você fecha e reabre o sistema."));
        p.add(centro, BorderLayout.CENTER);
        return p;
    }
    private JPanel painelTabela(JTable tabela, String titulo, String subtitulo, JButton... botoes) {
        JPanel p = new JPanel(new BorderLayout(12, 15));
        p.setBackground(FUNDO);
        p.setBorder(new EmptyBorder(20, 12, 12, 12));
        JPanel topo = new JPanel(new GridLayout(0, 1, 0, 10));
        topo.setOpaque(false);
        JLabel t = new JLabel(titulo); t.setFont(new Font("Segoe UI", Font.BOLD, 21));
        topo.add(t); topo.add(new JLabel(subtitulo));
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); acoes.setOpaque(false);
        for (JButton b : botoes) acoes.add(b);
        topo.add(acoes);
        JTextField busca = new JTextField();
        busca.getAccessibleContext().setAccessibleName("Buscar em " + titulo);
        busca.setToolTipText("Digite para filtrar qualquer coluna da tabela");
        JPanel pesquisa = new JPanel(new BorderLayout(10, 0)); pesquisa.setOpaque(false);
        pesquisa.add(new JLabel("Buscar"), BorderLayout.WEST); pesquisa.add(busca, BorderLayout.CENTER);
        topo.add(pesquisa);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel)tabela.getModel());
        tabela.setRowSorter(sorter);
        // Listener visual de busca: filtra somente a apresentação, sem modificar o domínio.
        busca.getDocument().addDocumentListener(new DocumentListener() {
            private void filtrar() { sorter.setRowFilter(RowFilter.regexFilter("(?iu)" + Pattern.quote(busca.getText()))); }
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
        });
        p.add(topo, BorderLayout.NORTH); p.add(new JScrollPane(tabela), BorderLayout.CENTER);
        return p;
    }
    private void atualizar() {
        listaProfissionais = service.getEstado().profissionais().stream().sorted(java.util.Comparator.comparing(Profissional::getNome)).toList();
        listaPacientes = service.getEstado().pacientes().stream().sorted(java.util.Comparator.comparing(Paciente::getNome)).toList();
        listaConsultas = service.getEstado().consultas().stream().sorted(java.util.Comparator.comparing(Consulta::getInicio)).toList();
        profissionais.setRowCount(0); pacientes.setRowCount(0); consultas.setRowCount(0);
        for (Profissional p : listaProfissionais) profissionais.addRow(new Object[]{p.getNome(), p instanceof Medico ? "Médico" : "Enfermeiro", p.getCrmCoren(), Profissional.moeda(p.getValorHora()), p.getHorasMensais(), Profissional.moeda(p.calcularPagamentoMensal())});
        for (Paciente p : listaPacientes) pacientes.addRow(new Object[]{p.getNome(), p.getCpf(), p.getTelefone()});
        for (Consulta c : listaConsultas) consultas.addRow(new Object[]{c.getInicio().format(DATA), c.getInicio().toLocalTime().toString(), service.paciente(c.getPacienteId()).getNome(), service.profissional(c.getProfissionalId()).getNome(), c.getDuracaoMinutos(), c.getStatus()});
        totalProfissionais.setText(String.valueOf(listaProfissionais.size()));
        totalPacientes.setText(String.valueOf(listaPacientes.size()));
        totalConsultas.setText(String.valueOf(listaConsultas.stream().filter(c -> c.getStatus() == Consulta.Status.AGENDADA).count()));
        folha.setText(Profissional.moeda(service.calcularFolhaMensal()));
    }

    private void editarProfissional(Profissional original) {
        JComboBox<String> tipo = new JComboBox<>(new String[]{"Médico", "Enfermeiro"});
        tipo.setSelectedIndex(original instanceof Enfermeiro ? 1 : 0);
        tipo.setEnabled(original == null);
        JTextField nome = campo(original == null ? "" : original.getNome());
        JTextField registro = campo(original == null ? "" : original.getCrmCoren());
        JTextField hora = campo(original == null ? "" : original.getValorHora().toPlainString());
        JTextField horas = campo(original == null ? "160" : String.valueOf(original.getHorasMensais()));
        JTextField especialidade = campo(original instanceof Medico m ? m.getEspecialidade() : "");
        JComboBox<Enfermeiro.Turno> turno = new JComboBox<>(Enfermeiro.Turno.values());
        if (original instanceof Enfermeiro e) turno.setSelectedItem(e.getTurno());
        JTextField extra = campo(original instanceof Medico m ? m.getTaxaCirurgia().toPlainString() : original instanceof Enfermeiro e ? e.getAdicionalNoturno().toPlainString() : "0,00");
        JLabel rotuloExtra = new JLabel();
        JPanel formulario = formulario("Categoria", tipo, "Nome", nome, "CRM / COREN (inclua UF)", registro,
                "Valor/hora (R$)", hora, "Horas mensais", horas, "Especialidade (médico)", especialidade,
                "Turno (enfermeiro)", turno, rotuloExtra, extra);
        Runnable ajustar = () -> {
            boolean medico = tipo.getSelectedIndex() == 0;
            especialidade.setEnabled(medico); turno.setEnabled(!medico);
            rotuloExtra.setText(medico ? "Taxa mensal de cirurgia (R$)" : "Adicional noturno mensal (R$)");
        };
        tipo.addActionListener(e -> ajustar.run()); ajustar.run();
        salvarDialogo(original == null ? "Novo profissional" : "Editar profissional", formulario, () ->
                controller.salvarProfissional(original == null ? null : original.getId(), tipo.getSelectedIndex() == 0,
                        nome.getText(), registro.getText(), hora.getText(), horas.getText(), especialidade.getText(), (Enfermeiro.Turno)turno.getSelectedItem(), extra.getText()));
    }
    private void editarPaciente(Paciente original) {
        JTextField nome = campo(original == null ? "" : original.getNome());
        JTextField cpf = campo(original == null ? "" : original.getCpf());
        JTextField telefone = campo(original == null ? "" : original.getTelefone());
        salvarDialogo(original == null ? "Novo paciente" : "Editar paciente", formulario("Nome", nome, "CPF", cpf, "Telefone com DDD", telefone),
                () -> controller.salvarPaciente(original == null ? null : original.getId(), nome.getText(), cpf.getText(), telefone.getText()));
    }
    private void editarConsulta(Consulta original) {
        if (listaPacientes.isEmpty() || listaProfissionais.isEmpty()) throw new IllegalArgumentException("Cadastre ao menos um paciente e um profissional antes de agendar.");
        JComboBox<Paciente> paciente = new JComboBox<>(listaPacientes.toArray(Paciente[]::new));
        JComboBox<Profissional> profissional = new JComboBox<>(listaProfissionais.toArray(Profissional[]::new));
        JComboBox<Consulta.Status> status = new JComboBox<>(Consulta.Status.values());
        if (original != null) {
            paciente.setSelectedItem(service.paciente(original.getPacienteId()));
            profissional.setSelectedItem(service.profissional(original.getProfissionalId()));
            status.setSelectedItem(original.getStatus());
        }
        JTextField data = campo(original == null ? LocalDate.now().format(DATA) : original.getInicio().format(DATA));
        JTextField hora = campo(original == null ? "09:00" : original.getInicio().toLocalTime().toString());
        JTextField duracao = campo(original == null ? "30" : String.valueOf(original.getDuracaoMinutos()));
        salvarDialogo(original == null ? "Agendar consulta" : "Editar consulta", formulario("Paciente", paciente, "Profissional", profissional,
                "Data (dd/mm/aaaa)", data, "Hora (hh:mm)", hora, "Duração em minutos", duracao, "Status", status),
                () -> controller.salvarConsulta(original == null ? null : original.getId(), (Paciente)paciente.getSelectedItem(),
                        (Profissional)profissional.getSelectedItem(), data.getText(), hora.getText(), duracao.getText(), (Consulta.Status)status.getSelectedItem()));
    }
    private void exibirFicha() {
        Profissional p = profissionalSelecionado();
        JTextArea texto = new JTextArea(p.exibirFicha(), 13, 42);
        texto.setEditable(false); texto.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        texto.setCaretPosition(0); texto.setBorder(new EmptyBorder(14, 14, 14, 14));
        JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Ficha profissional", JOptionPane.INFORMATION_MESSAGE);
    }
    private void excluirProfissional() throws IOException {
        Profissional p = profissionalSelecionado();
        if (confirmarExclusao(p.getNome())) { controller.removerProfissional(p.getId()); sucesso(); }
    }
    private void excluirPaciente() throws IOException {
        Paciente p = pacienteSelecionado();
        if (confirmarExclusao(p.getNome())) { controller.removerPaciente(p.getId()); sucesso(); }
    }
    private void excluirConsulta() throws IOException {
        Consulta c = consultaSelecionada();
        if (confirmarExclusao("consulta de " + service.paciente(c.getPacienteId()).getNome())) { controller.removerConsulta(c.getId()); sucesso(); }
    }
    private boolean confirmarExclusao(String nome) {
        return JOptionPane.showConfirmDialog(this, "Deseja excluir " + nome + "?", "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }
    private Profissional profissionalSelecionado() { return listaProfissionais.get(selecionado(tabelaProfissionais)); }
    private Paciente pacienteSelecionado() { return listaPacientes.get(selecionado(tabelaPacientes)); }
    private Consulta consultaSelecionada() { return listaConsultas.get(selecionado(tabelaConsultas)); }
    private static int selecionado(JTable tabela) {
        int linha = tabela.getSelectedRow();
        if (linha < 0) throw new IllegalArgumentException("Selecione uma linha da tabela primeiro.");
        return tabela.convertRowIndexToModel(linha);
    }
    private void salvarDialogo(String titulo, JPanel formulario, Acao acao) {
        JDialog dialogo = new JDialog(this, titulo, true);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel conteudo = new JPanel(new BorderLayout(12, 18));
        conteudo.setBorder(new EmptyBorder(20, 22, 18, 22));
        conteudo.add(formulario, BorderLayout.CENTER);
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rodape.add(botao("Cancelar", dialogo::dispose));
        JButton salvar = botao("Salvar", () -> {
            // Listener apenas delega: Controller converte, Service valida e Repository grava.
            acao.executar(); sucesso(); dialogo.dispose();
        });
        rodape.add(salvar); conteudo.add(rodape, BorderLayout.SOUTH);
        dialogo.setContentPane(conteudo); dialogo.getRootPane().setDefaultButton(salvar);
        dialogo.pack(); dialogo.setResizable(false); dialogo.setLocationRelativeTo(this); dialogo.setVisible(true);
    }
    private void sucesso() { atualizar(); aviso.setText("Alteração salva com sucesso."); }
    @FunctionalInterface private interface Acao { void executar() throws IOException; }
    private JButton botao(String titulo, Acao acao) {
        JButton b = new JButton(titulo); b.setFocusPainted(false); b.setMargin(new Insets(9, 14, 9, 14));
        b.addActionListener(e -> {
            try { acao.executar(); }
            catch (IllegalArgumentException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Revise os dados", JOptionPane.WARNING_MESSAGE); }
            catch (IOException ex) { JOptionPane.showMessageDialog(this, "Não foi possível salvar no disco. A alteração não foi aplicada.\nVerifique a permissão da pasta e o espaço disponível.\n\nDetalhe: " + ex.getMessage(), "Falha ao salvar", JOptionPane.ERROR_MESSAGE); }
        });
        return b;
    }
    private static JPanel formulario(Object... campos) {
        JPanel p = new JPanel(new GridBagLayout());
        for (int i = 0; i < campos.length; i += 2) {
            JLabel label = campos[i] instanceof JLabel l ? l : new JLabel(campos[i].toString());
            JComponent controle = (JComponent)campos[i + 1]; label.setLabelFor(controle);
            GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = i / 2; c.anchor = GridBagConstraints.WEST; c.insets = new Insets(7, 0, 7, 18);
            p.add(label, c); c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(7, 0, 7, 0);
            p.add(controle, c);
        }
        return p;
    }
    private static JTextField campo(String texto) { return new JTextField(texto, 26); }
    private static DefaultTableModel modelo(String... colunas) {
        return new DefaultTableModel(colunas, 0) { @Override public boolean isCellEditable(int linha, int coluna) { return false; } };
    }
    private static JTable tabela(DefaultTableModel modelo) {
        JTable t = new JTable(modelo); t.setRowHeight(34); t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1)); t.setGridColor(new Color(230, 236, 241));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12)); t.getTableHeader().setReorderingAllowed(false);
        return t;
    }
    private static JLabel indicador() {
        JLabel l = new JLabel("0"); l.setFont(new Font("Segoe UI", Font.BOLD, 23)); l.setForeground(VERDE); return l;
    }
    private static JPanel card(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout(0, 14)); p.setBackground(Color.WHITE); p.setBorder(new EmptyBorder(20, 16, 20, 16));
        JLabel label = new JLabel(titulo); label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        p.add(label, BorderLayout.NORTH); p.add(valor, BorderLayout.CENTER); return p;
    }
}
