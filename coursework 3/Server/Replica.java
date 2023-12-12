import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;
import java.security.Signature;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Instant;

public class Replica implements IReplica {
    KeyManager KM = new KeyManager();
    AuctionManager AM = new AuctionManager();
    LinkedList<AuctionItem> items = new LinkedList<AuctionItem>();
    LinkedList<User> users = new LinkedList<User>();
    String[] lastCommand = new String[2];

    public Replica() {
        super();

        try {
            grabFromPrimary();
            KM.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChallengeInfo challenge(int userID, String clientChallenge) throws RemoteException {
        try {
            ChallengeInfo chi = new ChallengeInfo();
            Signature sign = Signature.getInstance("SHA256withRSA");

            sign.initSign(KM.getPrivate());
            sign.update(clientChallenge.getBytes("UTF-8"));
            chi.response = sign.sign();

            String s = KM.createChars();
            chi.clientChallenge = s;
            users.get(findUser(userID)).lastChallenge = s;

            return chi;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public TokenInfo authenticate(int userID, byte signature[]) throws RemoteException {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(users.get(findUser(userID)).pubKey);
            sign.update(users.get(findUser(userID)).lastChallenge.getBytes("UTF-8"));
            sign.verify(signature);

            TokenInfo token = new TokenInfo();
            token.token = KM.createChars();
            users.get(userID).lastToken.token = token.token;

            long unixTimestamp = Instant.now().getEpochSecond();
            token.expiryTime = unixTimestamp + 10;
            users.get(userID).lastToken.expiryTime = token.expiryTime;

            return token;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public AuctionItem getSpec(int userID, int itemID, String token) throws RemoteException {
        if (checkToken(userID, token)) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).itemID == itemID) return items.get(i);
            }
        }

        return null;
    }

    public Integer register(String email, PublicKey key) throws RemoteException {
        if (key == null) return -5;

        Pattern pattern = Pattern.compile("@{1}");
        Matcher matcher = pattern.matcher(email);

        if(matcher.find()) {
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).email.equals(email)) return -1;
            }
                
            User user = AM.createUser(users.size(), email, key);
            users.addLast(user);

            System.out.println(email+" registered!");

            try {
                updateReplicas();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return user.userID;
        }

        return -2;
    }

    public Integer newAuction(int userID, AuctionSaleItem item, String token) throws RemoteException {
        if (checkToken(userID, token)) {
            int itemID = AM.auctions.size();
            items.addLast(AM.createItem(itemID, item.name, item.description, item.reservePrice));
            AM.addEvent(userID, itemID);

            System.out.println(userID + "added " + itemID + "!");

            try {
                updateReplicas();
            } catch (Exception e) {
                e.printStackTrace();
            }
    
            return itemID;
        } else return null;
    }

    public AuctionItem[] listItems(int userID, String token) throws RemoteException {
        if (checkToken(userID, token)) {
            AuctionItem[] itemList = new AuctionItem[items.size()];
            for (int i = 0; i < items.size(); i++) itemList[i] = items.get(i);

            return itemList;
        } else return null;
    }

    public AuctionResult closeAuction(int userID, int itemID, String token) throws RemoteException {
        if (checkToken(userID, token)) {
            int check = AM.eventCheck(userID, itemID, 1);
            int item = findItem(itemID);

            if (item != -1 && check != -1) {
                AuctionResult result = new AuctionResult();
                result.winningEmail = users.get(userID).email;
                result.winningPrice = items.get(item).highestBid;
                items.remove(item);

                System.out.println(userID + " closed auction for item " + itemID + "!");

                try {
                    updateReplicas();
                } catch (Exception e) {
                    e.printStackTrace();
                }              

                return result;
            } else return null;
        } else return null;
    }

    public boolean bid(int userID, int itemID, int price, String token) throws RemoteException {
        if (checkToken(userID, token)) {
            int item = findItem(itemID);

            if (item < 0) return false;

            if (price > items.get(item).highestBid) {
                items.get(item).highestBid = price;
                if (AM.updateEvent(AM.eventCheck(userID, itemID, 0), userID)) {
                    System.out.println(userID + " bidded " + price + " on item " + itemID + "!");

                    try {
                        updateReplicas();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return true;
                } else return false;
            } else return false;
        } else return false;
    }

    public int getPrimaryReplicaID() throws RemoteException {
        try {
            Auction FE = waitForPrimary();
            return FE.getPrimaryReplicaID();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int findItem(int ID) {
        for (int i = 0; i < items.size(); i++) if (items.get(i).itemID == ID) return i;

        return -1;
    }

    private int findUser(int ID) {
        for (int i = 0; i < users.size(); i++) if (users.get(i).userID == ID) return i;

        return -1;
    }

    private boolean checkToken(int ID, String token) {
        TokenInfo t = users.get(findUser(ID)).lastToken;

        if (t.token.equals(token) && t.expiryTime >= Instant.now().getEpochSecond()) {
            t.expiryTime = 0;
            return true;
        }
        else return false;
    }

    public void receiveAM(AuctionManager aum) throws RemoteException {
        this.AM = aum;
    }

    public void receiveItems(LinkedList<AuctionItem> inputItems) throws RemoteException {
        this.items = inputItems;
    }

    public void receiveUsers(LinkedList<User> inputUsers) throws RemoteException {
        this.users = inputUsers;
    }

    public KeyManager grabKM() throws RemoteException {
        return KM;
    }

    public AuctionManager grabAM() throws RemoteException {
        return AM;
    }

    public LinkedList<AuctionItem> grabItems() throws RemoteException {
        return items;
    }

    public LinkedList<User> grabUsers() throws RemoteException {
        return users;
    }

    private void grabFromPrimary() throws RemoteException, NotBoundException, InterruptedException {
        Auction FE = waitForPrimary();
        Registry registry = LocateRegistry.getRegistry();
        String[] regList  = registry.list();
        Boolean check = false;

        for (int i = 0; i < regList.length; i++) {
            String name = regList[i];
            if (name.startsWith("Replica")) {
                check = true;
                break;
            }
        }

        if (check) {
            String primaryReplica = "Replica"+FE.getPrimaryReplicaID();
            IReplica primary = (IReplica) registry.lookup(primaryReplica);

            this.KM = primary.grabKM();
            this.AM = primary.grabAM();
            this.items = primary.grabItems();
            this.users = primary.grabUsers();
        }
    }

    private void updateReplicas() throws RemoteException, NotBoundException, InterruptedException {
        Auction FE = waitForPrimary();
        String primaryReplica = "Replica"+FE.getPrimaryReplicaID();
        Registry registry = LocateRegistry.getRegistry();
        String[] regList  = registry.list();

        for (int i = 0; i < regList.length; i++) {
            String name = regList[i];
            if (name.startsWith("Replica") && !name.equals(primaryReplica)) {
                IReplica a = (IReplica) registry.lookup(registry.list()[i]);
                try {
                    a.register("", null);
                } catch (Exception e) {
                    continue;
                }
                a.receiveAM(AM);
                a.receiveItems(items);
                a.receiveUsers(users);
            }
        } 
    }

    // If any Replica can find the Front End, then a primary Replica must exist
    private Auction waitForPrimary() throws RemoteException, NotBoundException, InterruptedException {
        Registry registry = LocateRegistry.getRegistry();

        while (true) {
            String[] regList  = registry.list();
            
            for (int i = 0; i < regList.length; i++) {
                String name = regList[i];
                if (name.equals("FrontEnd")) {
                    Auction FE = (Auction) registry.lookup(name);
                    return FE;
                }
            }

            Thread.sleep(1000);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Replica #");
            return;
        }

        try {
            Replica s = new Replica();
            String name = "Replica"+args[0];
            IReplica stub = (IReplica) UnicastRemoteObject.exportObject(s, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
            System.out.println(name+" ready");
        } catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }
}