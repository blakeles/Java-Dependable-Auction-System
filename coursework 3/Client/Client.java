import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class Client {
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        KeyManager KM = new KeyManager();
        KM.generateKeys(0);

        if (args.length < 1) {
            System.out.println("Usage: java Client Register user@email.com");
            return;
        }

        try {
            String name = "FrontEnd";
            Registry registry = LocateRegistry.getRegistry("localhost");
            Auction server = (Auction) registry.lookup(name);

            if (args[0].equalsIgnoreCase("register") || args[0].equalsIgnoreCase("r")) {
                int id = server.register(args[1], KM.getPublic());

                if (id >= 0) {
                    System.out.println("Logged in. User ID: " + id);
                    while (true) {
                        System.out.println("Usage:\n1: Create auction\n2: End auction\n3: See auctions\n4: Make bid");
                        Scanner sscanner = new Scanner(System.in);
                        String s = sscanner.nextLine();

                        if (s.equals("1")) {
                            System.out.println("Usage: Enter item name. (E.g. Watch)");
                            String s1 = sscanner.nextLine();
                            System.out.println("Usage: Enter item description. (E.g. Good condition, tells the time)");
                            String s2 = sscanner.nextLine();
                            System.out.println("Usage: Enter item starting price. (E.g. 50)");
                            int s3;
                            if (sscanner.hasNextInt()) s3 = sscanner.nextInt();
                            else {
                                s3 = 0;
                                System.out.println("Input not valid. Starting price set to 0.");
                            }

                            if (s3 < 0) {
                                s3 = 0;
                                System.out.println("Input not valid. Starting price set to 0.");
                            }

                            AuctionSaleItem item = new AuctionSaleItem();
                            item.name = s1;
                            item.description = s2;
                            item.reservePrice = s3;

                            int check = server.newAuction(id, item, handshake(server, id, KM).token);
                            if (check < 0) System.out.println("Failed to create new auction!");
                        } else if (s.equals("2")) {
                            System.out.println("Usage: Enter item ID. (E.g. 12)");
                            int s1;
                            if (sscanner.hasNextInt()) s1 = sscanner.nextInt();
                            else {
                                s1 = -1;
                                System.out.println("Input not valid.");
                            }
                            AuctionResult result = server.closeAuction(id, s1, handshake(server, id, KM).token);

                            if (result != null) {
                                System.out.println("---------------------AUCTION---------------------");
                                System.out.println("Winner: " + result.winningEmail);
                                System.out.println("Price: " + result.winningPrice);
                                System.out.println("-------------------------------------------------");
                            } else {
                                System.out.println("Error: No permission.");
                            }
                        } else if (s.equals("3")) {
                            AuctionItem[] items = server.listItems(id, handshake(server, id, KM).token);

                            System.out.println("----------------------ITEMS----------------------");
                            if (items.length > 0) {
                                for (int i = 0; i < items.length; i++) {
                                    displayItem(items[i]);
                                    System.out.println("-------------------------------------------------");
                                } 
                            } else System.out.println("No items.");
                            
                        } else if (s.equals("4")) {
                            System.out.println("Usage: Enter item ID. (E.g. 12)");
                            int s1;
                            if (sscanner.hasNextInt()) {
                                s1 = sscanner.nextInt();

                                System.out.println("Usage: Enter bid amount. (E.g. 25)");
                                int s2;
                                if (sscanner.hasNextInt()) s2 = sscanner.nextInt();
                                else {
                                    s2 = -1;
                                    System.out.println("Input not valid. Bid failed!");
                                }

                                if (server.bid(id, s1, s2, handshake(server, id, KM).token)) System.out.println("Bid placed successfully!");
                                else System.out.println("Bid failed!");
                            } else {
                                s1 = -1;
                                System.out.println("Input not valid.");
                            }
                        }
                    }
                } else if (id == -1) {
                    System.out.println("Error: Email already registered.");
                }
                else {
                    System.out.println("Error: Email is not supported.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TokenInfo handshake(Auction server, int userID, KeyManager KM) throws RemoteException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, SignatureException, UnsupportedEncodingException {
        String s = KM.createChars();

        ChallengeInfo challenge = server.challenge(userID, s);
        if (verifySignature(challenge.response, s, KM)) {
            TokenInfo token = server.authenticate(userID, signChallenge(KM, challenge.clientChallenge));
            return token;
        } else return null;
    }

    private static Boolean verifySignature(byte[] signature, String s, KeyManager KM) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(KM.readKey());
            sign.update(s.getBytes("UTF-8"));
            Boolean result = sign.verify(signature);
            
            return result;
        } catch (Exception e) {
            System.out.println("Error with verifying signature.");
            e.printStackTrace();
        }

        return false;
    }

    private static byte[] signChallenge(KeyManager KM, String challenge) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(KM.getPrivate());
        sign.update(challenge.getBytes("UTF-8"));
        return sign.sign();
    }

    private static void displayItem(AuctionItem result) {
            System.out.println("Item ID: " + result.itemID);
            System.out.println("Item Name: " + result.name);
            System.out.println("Description: " + result.description);
            System.out.println("Highest Bid: " + result.highestBid);
    }
}