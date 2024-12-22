import java.io.*;
import java.net.*;
import java.util.Properties;

public class Client {
    public static void main(String[] args) {
        // Charger la configuration depuis config.txt
        Properties config = chargerConfiguration("config.txt");
        if (config == null) return;

        // Lire les valeurs du fichier de configuration
        String adresseServeur = config.getProperty("serverAddress");
        String portStr = config.getProperty("port");

        if (adresseServeur == null || portStr == null) {
            System.out.println("Erreur : Le fichier config.txt doit contenir les clés 'serverAddress' et 'port'.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.out.println("Erreur : Le port spécifié dans config.txt n'est pas un nombre valide.");
            return;
        }

        try (Socket socket = new Socket(adresseServeur, port);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            // Exemple d'utilisation des fonctions
            // listerFichiers(dis, dos); // Lister les fichiers disponibles
            supprimerFichier("alaivo", dos, dis); // Supprimer un fichier
            // envoyerFichier("/home/ioty/alaivo", dos); // Envoyer un fichier
            // downloadFile("alaivo", dis, dos); // Télécharger un fichier

        } catch (IOException e) {
            System.out.println("Erreur de connexion au serveur : " + e.getMessage());
        }
    }

    private static void listerFichiers(DataInputStream dis, DataOutputStream dos) {
        try {
            // Envoyer la commande LIST au serveur
            dos.writeUTF("LIST");

            // Lire la réponse du serveur
            int nombreFichiers = dis.readInt();
            if (nombreFichiers == -1) {
                System.out.println("Erreur du serveur : " + dis.readUTF());
                return;
            }

            if (nombreFichiers == 0) {
                System.out.println("Aucun fichier disponible sur le serveur.");
                return;
            }

            System.out.println("Fichiers disponibles sur le serveur :");
            for (int i = 0; i < nombreFichiers; i++) {
                String nomFichier = dis.readUTF();
                System.out.println("- " + nomFichier);
            }
        } catch (IOException e) {
            System.out.println("Erreur lors de la liste des fichiers : " + e.getMessage());
        }
    }

    private static void envoyerFichier(String cheminFichier, DataOutputStream dos) {
        File fichier = new File(cheminFichier);

        if (!fichier.exists()) {
            System.out.println("Le fichier n'existe pas : " + cheminFichier);
            return;
        }

        try (FileInputStream fis = new FileInputStream(fichier)) {
            dos.writeUTF("UPLOAD");
            dos.writeUTF(fichier.getName());
            dos.writeLong(fichier.length());

            byte[] tampon = new byte[4096];
            int lu;

            while ((lu = fis.read(tampon)) > 0) {
                dos.write(tampon, 0, lu);
            }

            System.out.println("Fichier envoyé : " + fichier.getName());
        } catch (IOException e) {
            System.out.println("Erreur lors de l'envoi du fichier : " + e.getMessage());
        }
    }

    private static void downloadFile(String fileName, DataInputStream dis, DataOutputStream dos) {
        try {
            // Envoyer la requête au serveur pour télécharger le fichier
            dos.writeUTF("DOWNLOAD");
            dos.writeUTF(fileName);

            // Lire le statut du serveur
            String status = dis.readUTF();
            if (status.equals("ERROR")) {
                System.out.println("Erreur du serveur : " + dis.readUTF());
                return;
            }

            // Préparer le chemin de destination dans le dossier 'download'
            File dossierDownload = new File("../download");
            if (!dossierDownload.exists()) {
                dossierDownload.mkdir(); // Créer le dossier si inexistant
            }
            File fichierDestination = new File(dossierDownload, fileName);

            // Télécharger le fichier et l'écrire dans le dossier 'download'
            try (FileOutputStream fos = new FileOutputStream(fichierDestination)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = dis.read(buffer)) > 0) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Fichier téléchargé et enregistré sous : " + fichierDestination.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Erreur lors du téléchargement du fichier : " + e.getMessage());
        }
    }

    // private static void supprimerFichier(String nomFichier, DataOutputStream dos, DataInputStream dis) {
    //     try {
    //         // Envoyer la commande REMOVE au serveur
    //         dos.writeUTF("REMOVE");
    //         dos.writeUTF(nomFichier);

    //         // Lire la réponse du serveur
    //         String response = dis.readUTF(); // Utiliser dis pour lire la réponse
    //         if (response.equals("OK")) {
    //             System.out.println("Fichier " + nomFichier + " et ses parties ont été supprimés.");
    //         } else {
    //             System.out.println("Erreur lors de la suppression du fichier " + nomFichier + ": " + response);
    //         }
    //     } catch (IOException e) {
    //         System.out.println("Erreur lors de la suppression du fichier : " + e.getMessage());
    //     }
    // }
private static void supprimerFichier(String nomFichier, DataOutputStream dos, DataInputStream dis) {
    try {
        // Envoyer la commande REMOVE au serveur
        dos.writeUTF("REMOVE");
        dos.writeUTF(nomFichier);

        // Ajouter un log pour vérifier la commande envoyée
        System.out.println("Commande envoyée au serveur : REMOVE " + nomFichier);

        // Lire la réponse du serveur
        String response = dis.readUTF(); // Utiliser dis pour lire la réponse
        if (response.equals("OK")) {
            System.out.println("Fichier " + nomFichier + " et ses parties ont été supprimés.");
        } else {
            System.out.println("Erreur lors de la suppression du fichier " + nomFichier + ": " + response);
        }
    } catch (IOException e) {
        System.out.println("Erreur lors de la suppression du fichier : " + e.getMessage());
    }
}

    private static Properties chargerConfiguration(String cheminFichier) {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(cheminFichier)) {
            props.load(fis);
            return props;
        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture du fichier de configuration : " + e.getMessage());
        }

        return null;
    }
}
