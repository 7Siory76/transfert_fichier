import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.Properties;
public class ClientTerminal {

    private static String serverAddress;
    private static int serverPort;

    public static void main(String[] args) {
        chargerConfiguration();

        try (Socket socket = new Socket(serverAddress, serverPort);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connecté au serveur : " + serverAddress + ":" + serverPort);

            while (true) {
                System.out.print("Commande (PUT/GET/LIST/REMOVE/EXIT) : ");
                String commande = scanner.nextLine().trim().toUpperCase();

                switch (commande) {
                    case "PUT":
                        envoyerFichier(dos, dis, scanner);
                        break;

                    case "GET":
                        telechargerFichier(dos, dis, scanner);
                        break;

                    case "LIST":
                        listerFichiers(dos, dis);
                        break;

                    case "REMOVE":
                        supprimerFichier(dos, dis, scanner);
                        break;

                    case "EXIT":
                        System.out.println("Déconnexion.");
                        return;

                    default:
                        System.out.println("Commande invalide.");
                }
            }

        } catch (IOException e) {
            System.out.println("Erreur : " + e.getMessage());
        }
    }

    private static void chargerConfiguration() {
        try (FileInputStream fis = new FileInputStream("config.txt")) {
            Properties config = new Properties();
            config.load(fis);

            serverAddress = config.getProperty("serverAddress");
            serverPort = Integer.parseInt(config.getProperty("port"));

        } catch (IOException e) {
            System.out.println("Erreur lors du chargement de la configuration : " + e.getMessage());
            System.exit(1);
        }
    }

    private static void envoyerFichier(DataOutputStream dos, DataInputStream dis, Scanner scanner) throws IOException {
        System.out.print("Chemin du fichier à envoyer : ");
        String cheminFichier = scanner.nextLine().trim();
        File fichier = new File(cheminFichier);

        if (!fichier.exists()) {
            System.out.println("Fichier introuvable.");
            return;
        }

        dos.writeUTF("UPLOAD");
        dos.writeUTF(fichier.getName());
        dos.writeLong(fichier.length());

        try (FileInputStream fis = new FileInputStream(fichier)) {
            byte[] buffer = new byte[4096];
            int lus;
            while ((lus = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, lus);
            }
        }

        System.out.println("Fichier envoyé au serveur.");
    }

    private static void telechargerFichier(DataOutputStream dos, DataInputStream dis, Scanner scanner) throws IOException {
        System.out.print("Nom du fichier à télécharger : ");
        String nomFichier = scanner.nextLine().trim();

        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(nomFichier);

        String reponse = dis.readUTF();
        if ("ERROR".equals(reponse)) {
            System.out.println("Erreur : " + dis.readUTF());
            return;
        }

        System.out.print("Chemin pour sauvegarder le fichier : ");
        String cheminSauvegarde = scanner.nextLine().trim();

        try (FileOutputStream fos = new FileOutputStream(cheminSauvegarde)) {
            byte[] buffer = new byte[4096];
            int lus;
            while ((lus = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, lus);
            }
        }

        System.out.println("Fichier téléchargé avec succès.");
    }

    private static void listerFichiers(DataOutputStream dos, DataInputStream dis) throws IOException {
        dos.writeUTF("LIST");

        int nombreFichiers = dis.readInt();
        if (nombreFichiers == 0) {
            System.out.println("Aucun fichier disponible sur le serveur.");
            return;
        }

        System.out.println("Fichiers disponibles :");
        for (int i = 0; i < nombreFichiers; i++) {
            System.out.println("- " + dis.readUTF());
        }
    }
    /// je veux supprimer ces fichiers que je veux plus utiliser
    /// avec des histoires de maintenant de merde, je ne veux plus donner 
    // de la vie, je ne veux plus une nouvelle vie, je veux juste etre heureux
    /// alors que je ne serai pas heureux sans Itoerantsoa, Fanilo, Ocyy 
    /// ces derniers temps je sens que je ressens de plus en plus quelque chose
    /// pour Oceane, c'est vrai quoi elle est belle, intelligente, douce
    /// mais elle n'est pas trop dans cette histoire de relation amoureuse
    /// pourtant mes sentiments n'arretent pas de s'intensifier de jour en jour
    /// je ne sais pas quoi faire, je n'ai pas envie de me faire friendzoner encore une 
    /// fois c'etait deja tres douloureux avec Laureen et maintenant je ne sais pas quoi faire 
    /// pcq si je fais rien je risque de'avoir mal au coeur mais si je fais quelque chose je risque 
    /// d'avoir aussi mal, a cause de la friendzone

    private static void supprimerFichier(DataOutputStream dos, DataInputStream dis, Scanner scanner) throws IOException {
        System.out.print("Nom du fichier à supprimer : ");
        String nomFichier = scanner.nextLine().trim();

        dos.writeUTF("REMOVE");
        dos.writeUTF(nomFichier);

        String reponse = dis.readUTF();
        if ("OK".equals(reponse)) {
            System.out.println("Fichier supprimé avec succès.");
        } else {
            System.out.println("Erreur lors de la suppression du fichier.");
        }
    }
}
