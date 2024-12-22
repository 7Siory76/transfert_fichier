import java.io.*;
import java.net.*;
import java.util.Properties;

public class Serveur {
    private static String uploadsDir;
    private static String part1Dir;
    private static String part2Dir;
    private static String part3Dir;
    private static int port;

    public static void main(String[] args) {
        chargerConfiguration();

        // Créer les dossiers nécessaires si inexistants
        new File(part1Dir).mkdirs();
        new File(part2Dir).mkdirs();
        new File(part3Dir).mkdirs();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Serveur démarré sur le port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté.");
                new Thread(() -> traiterClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("Erreur lors du démarrage du serveur : " + e.getMessage());
        }
    }

    private static void chargerConfiguration() {
        try (FileInputStream fis = new FileInputStream("config.txt")) {
            Properties config = new Properties();
            config.load(fis);

            port = Integer.parseInt(config.getProperty("port"));
            uploadsDir = config.getProperty("uploadsDir");
            part1Dir = config.getProperty("part1Dir");
            part2Dir = config.getProperty("part2Dir");
            part3Dir = config.getProperty("part3Dir");

        } catch (IOException e) {
            System.out.println("Erreur lors du chargement de la configuration : " + e.getMessage());
            System.exit(1);
        }
    }

    private static void traiterClient(Socket clientSocket) {
    try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
         DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

        String commande = dis.readUTF();

        if (commande.equals("UPLOAD")) {
            recevoirEtDecouperFichier(dis);
        } else if (commande.equals("DOWNLOAD")) {
            String nomFichier = dis.readUTF();
            fusionnerEtEnvoyerFichier(nomFichier, dos);
        } else if (commande.equals("LIST")) {
            listerFichiers(dos);
        } else if (commande.equals("REMOVE")) {
            String nomFichier = dis.readUTF();
            supprimerFichier(nomFichier, dos);
        } else {
            dos.writeUTF("COMMANDE_INVALIDE");
        }

    } catch (IOException e) {
        System.out.println("Erreur lors du traitement du client : " + e.getMessage());
    }
}


private static void supprimerFichier(String nomFichier, DataOutputStream dos) {
    // Construire les chemins des parties du fichier
    File part1 = new File(part1Dir + "/" + nomFichier + "_part1");
    File part2 = new File(part2Dir + "/" + nomFichier + "_part2");
    File part3 = new File(part3Dir + "/" + nomFichier + "_part3");

    // Vérifier si les parties existent et les supprimer
    boolean supprime = true;
    if (part1.exists()) {
        supprime &= part1.delete();
    }
    if (part2.exists()) {
        supprime &= part2.delete();
    }
    if (part3.exists()) {
        supprime &= part3.delete();
    }

    // Répondre au client en fonction du résultat
    try {
        if (supprime) {
            dos.writeUTF("OK"); // Tout a été supprimé
            System.out.println("Fichier et ses parties supprimés : " + nomFichier);
        } else {
            dos.writeUTF("ERROR");
            System.out.println("Erreur lors de la suppression du fichier : " + nomFichier);
        }
    } catch (IOException e) {
        System.out.println("Erreur lors de la suppression du fichier : " + e.getMessage());
    }
}

    private static void recevoirEtDecouperFichier(DataInputStream dis) throws IOException {
        String nomFichier = dis.readUTF();
        long tailleFichier = dis.readLong();
        long taillePartie = tailleFichier / 3;

        System.out.println("Réception du fichier : " + nomFichier);
        System.out.println("Taille du fichier : " + tailleFichier + " octets, taille de chaque partie : " + taillePartie + " octets");

        // S'assurer que les répertoires existent
        new File(part1Dir).mkdirs();
        new File(part2Dir).mkdirs();
        new File(part3Dir).mkdirs();

        try (FileOutputStream fos1 = new FileOutputStream(part1Dir + "/" + nomFichier + "_part1");
             FileOutputStream fos2 = new FileOutputStream(part2Dir + "/" + nomFichier + "_part2");
             FileOutputStream fos3 = new FileOutputStream(part3Dir + "/" + nomFichier + "_part3")) {

            byte[] buffer = new byte[4096];
            long totalLus = 0;
            int lus;

            while ((lus = dis.read(buffer)) > 0) {
                totalLus += lus;

                // Répartir les données dans les bonnes parties
                if (totalLus <= taillePartie) {
                    fos1.write(buffer, 0, lus);
                } else if (totalLus <= taillePartie * 2) {
                    fos2.write(buffer, 0, lus);
                } else {
                    fos3.write(buffer, 0, lus);
                }

                if (totalLus >= tailleFichier) break;
            }
        }

        System.out.println("Fichier reçu et découpé en 3 parties.");
    }

    private static void fusionnerEtEnvoyerFichier(String nomFichier, DataOutputStream dos) throws IOException {
        File part1 = new File(part1Dir + "/" + nomFichier + "_part1");
        File part2 = new File(part2Dir + "/" + nomFichier + "_part2");
        File part3 = new File(part3Dir + "/" + nomFichier + "_part3");

        if (!part1.exists() || !part2.exists() || !part3.exists()) {
            dos.writeUTF("ERROR");
            dos.writeUTF("Une ou plusieurs parties du fichier sont manquantes.");
            return;
        }

        dos.writeUTF("OK");

        try (FileInputStream fis1 = new FileInputStream(part1);
             FileInputStream fis2 = new FileInputStream(part2);
             FileInputStream fis3 = new FileInputStream(part3)) {

            byte[] buffer = new byte[4096];
            int lus;

            while ((lus = fis1.read(buffer)) > 0) {
                dos.write(buffer, 0, lus);
            }

            while ((lus = fis2.read(buffer)) > 0) {
                dos.write(buffer, 0, lus);
            }

            while ((lus = fis3.read(buffer)) > 0) {
                dos.write(buffer, 0, lus);
            }
        }

        System.out.println("Fichier fusionné et envoyé au client.");
    }

    private static void listerFichiers(DataOutputStream dos) throws IOException {
        File dossierPart1 = new File(part1Dir);
        File dossierPart2 = new File(part2Dir);
        File dossierPart3 = new File(part3Dir);

        String[] fichiersPart1 = dossierPart1.list();
        if (fichiersPart1 == null) {
            dos.writeInt(0); // Aucun fichier
            return;
        }

        dos.writeInt(fichiersPart1.length); // Nombre de fichiers potentiels

        for (String nomFichier : fichiersPart1) {
            String fichierSansExtension = nomFichier.replace("_part1", "");
            File part2 = new File(dossierPart2, fichierSansExtension + "_part2");
            File part3 = new File(dossierPart3, fichierSansExtension + "_part3");

            if (part2.exists() && part3.exists()) {
                dos.writeUTF(fichierSansExtension); // Envoyer le nom des fichiers complets
            }
        }
    }
}
