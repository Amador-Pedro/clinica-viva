package br.edu.ceub.clinica;

import br.edu.ceub.clinica.controller.ClinicaController;
import br.edu.ceub.clinica.repository.RepositorioCsv;
import br.edu.ceub.clinica.service.ClinicaService;
import br.edu.ceub.clinica.view.ClinicaView;
import javax.swing.*;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.channels.*;
import java.nio.file.*;
import java.io.IOException;

/** Composição MVC e ciclo de vida. Nenhum menu de terminal; erros de abertura usam diálogo.
 * O lock impede duas instâncias de sobrescreverem o mesmo arquivo. CSV inválido nunca é reiniciado.
 */
public final class Main {
    private Main() { }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FileChannel canal = null;
            FileLock trava = null;
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                for (Object chave : UIManager.getDefaults().keySet().toArray()) {
                    if (UIManager.get(chave) instanceof javax.swing.plaf.FontUIResource)
                        UIManager.put(chave, new javax.swing.plaf.FontUIResource("Segoe UI", Font.PLAIN, 14));
                }
                UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 14));
                Path diretorio = Path.of(System.getProperty("clinica.dados", "data")).toAbsolutePath();
                Files.createDirectories(diretorio);
                canal = FileChannel.open(diretorio.resolve("clinica.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                trava = canal.tryLock();
                if (trava == null) throw new IOException("A clínica já está aberta nesta pasta de dados. Feche a outra janela primeiro.");
                ClinicaService service = new ClinicaService(new RepositorioCsv(diretorio.resolve("clinica.csv")));
                ClinicaView view = new ClinicaView(service, new ClinicaController(service));
                final FileChannel canalFinal = canal;
                final FileLock travaFinal = trava;
                view.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosed(WindowEvent e) {
                        try { travaFinal.release(); canalFinal.close(); }
                        catch (IOException erro) { /* O processo também libera a trava ao encerrar. */ }
                    }
                });
                view.setVisible(true);
            } catch (Exception e) {
                try { if (trava != null) trava.release(); if (canal != null) canal.close(); }
                catch (IOException ignorada) { /* Preserva a mensagem original de abertura. */ }
                JOptionPane.showMessageDialog(null, "Não foi possível abrir o sistema.\n" + e.getMessage()
                        + "\n\nSe o CSV estiver inválido, restaure uma cópia de segurança. Os dados existentes não foram sobrescritos.", "Falha na abertura", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
