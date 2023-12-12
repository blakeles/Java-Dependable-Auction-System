import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

public class KeyManager implements Serializable {
    PrivateKey privateKey;
    PublicKey publicKey;
    int connections = 0;

    public void generateKeys(int type) throws NoSuchAlgorithmException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keys = keyGen.genKeyPair();
    
        this.privateKey = keys.getPrivate();
        this.publicKey = keys.getPublic();
    
        if (type > 0) {
            String encodedKey = Base64.getEncoder().encodeToString(keys.getPublic().getEncoded());
            FileWriter fWriter = new FileWriter("../keys/serverKey.pub");
            fWriter.write(encodedKey);
            fWriter.close();
        }
    }

    public PublicKey readKey() {
        String encodedKey = "";

        try {
            File file = new File("../keys/serverKey.pub");
            Scanner fReader = new Scanner(file);
            encodedKey = fReader.nextLine();
            fReader.close();
        } catch (Exception e) {
            System.out.println("Error with reading key.");
            e.printStackTrace();
        }

        try {
            X509EncodedKeySpec key = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey));
            KeyFactory keyFac = KeyFactory.getInstance("RSA");
            return keyFac.generatePublic(key);
        } catch(Exception e) {
            System.out.println("Error with regenerating key.");
            e.printStackTrace();
        }

        return null;
    }

    public String createChars() {
        Random rand = new Random();
        int length = rand.nextInt(10) + 6;
        char[] text = new char[length];

        for (int i = 0; i < length; i++)
        {
            text[i] = "ABCDEFGHIJKLMOPQRSTUVWXYZ1234567890".charAt(rand.nextInt("ABCDEFGHIJKLMOPQRSTUVWXYZ1234567890".length()));
        }

        return new String(text);
    }

    public void connect() {
        connections++;

        if (connections == 1)
            try {
                generateKeys(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public PrivateKey getPrivate() {
        return privateKey;
    }

    public PublicKey getPublic() {
        return publicKey;
    }
}