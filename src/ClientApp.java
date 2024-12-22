import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Properties;

public class ClientApp {

    private static String SERVER_ADDRESS;
    private static int SERVER_PORT;

    public static void main(String[] args) {
        // Charger la configuration depuis le fichier config.txt
        Properties config = ConfigLoader.loadConfig("config.txt");
        SERVER_ADDRESS = config.getProperty("serverAddress", "localhost"); // Valeur par défaut
        SERVER_PORT = Integer.parseInt(config.getProperty("port", "12345")); // Valeur par défaut

        SwingUtilities.invokeLater(ClientApp::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Gestion des fichiers");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);  // Augmenter la taille de la fenêtre
        frame.setLocationRelativeTo(null);  // Centrer la fenêtre
        frame.setResizable(false);  // Empêcher le redimensionnement de la fenêtre

        frame.setLayout(new FlowLayout());

        JButton uploadButton = new JButton("Uploader fichier");
        JButton listButton = new JButton("Lister les fichiers");

        uploadButton.addActionListener(e -> openFileChooser());
        listButton.addActionListener(e -> showFileList());

        frame.add(uploadButton);
        frame.add(listButton);

        frame.setVisible(true);
    }

    private static void openFileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            uploadFile(selectedFile.getAbsolutePath());
        }
    }

    private static void uploadFile(String filePath) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(filePath)) {

            dos.writeUTF("UPLOAD");
            dos.writeUTF(new File(filePath).getName());

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }

            dos.flush();
            JOptionPane.showMessageDialog(null, "Fichier uploadé avec succès !");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Erreur lors de l'upload : " + ex.getMessage());
        }
    }

    private static void showFileList() {
        JFrame listFrame = new JFrame("Liste des fichiers");
        listFrame.setSize(600, 400);

        String[] columnNames = {"Nom du fichier", "Télécharger", "Supprimer"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0; // Rendre les colonnes 1 et 2 cliquables
            }
        };

        // Définir les rendus de cellules pour les boutons
        table.getColumn("Télécharger").setCellRenderer(new ButtonRenderer());
        table.getColumn("Supprimer").setCellRenderer(new ButtonRenderer());

        // Définir les éditeurs de cellules pour les boutons (pour rendre les boutons cliquables)
        table.getColumn("Télécharger").setCellEditor(new ButtonEditor(new JCheckBox()));
        table.getColumn("Supprimer").setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(table);
        listFrame.add(scrollPane);

        // Charger les fichiers du serveur
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("LIST");
            int fileCount = dis.readInt();

            for (int i = 0; i < fileCount; i++) {
                String fileName = dis.readUTF();
                JButton downloadButton = new JButton("Télécharger");
                JButton deleteButton = new JButton("Supprimer");

                downloadButton.addActionListener(e -> downloadFile(fileName));
                deleteButton.addActionListener(e -> deleteFile(fileName, model));

                model.addRow(new Object[]{fileName, downloadButton, deleteButton});
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Erreur lors du chargement de la liste : " + ex.getMessage());
        }

        listFrame.setVisible(true);
    }

    private static void downloadFile(String fileName) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {

            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(fileName);

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(fileName));
            int returnValue = fileChooser.showSaveDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = dis.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                JOptionPane.showMessageDialog(null, "Fichier téléchargé avec succès !");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Erreur lors du téléchargement : " + ex.getMessage());
        }
    }

    private static void deleteFile(String fileName, DefaultTableModel model) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            dos.writeUTF("REMOVE");
            dos.writeUTF(fileName);

            JOptionPane.showMessageDialog(null, "Fichier supprimé avec succès !");
            for (int i = 0; i < model.getRowCount(); i++) {
                if (model.getValueAt(i, 0).equals(fileName)) {
                    model.removeRow(i);
                    break;
                }
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Erreur lors de la suppression : " + ex.getMessage());
        }
    }

    static class ConfigLoader {
        public static Properties loadConfig(String filePath) {
            Properties config = new Properties();
            try (FileInputStream fis = new FileInputStream(filePath)) {
                config.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return config;
        }
    }

    // Classe pour le rendu des boutons dans la table
    static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof JButton) {
                return (JButton) value;
            }
            return this;
        }
    }

    // Classe pour l'éditeur des boutons dans la table
    static class ButtonEditor extends DefaultCellEditor {
        private JButton button;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);

            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof JButton) {
                button = (JButton) value;
            }
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return button;
        }
    }
}
