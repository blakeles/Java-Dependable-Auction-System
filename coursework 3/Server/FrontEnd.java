import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;

public class FrontEnd implements Auction {
    String primaryReplica = null;

    public Integer register(String email, PublicKey pubKey) throws RemoteException {
        try {
            return findReplica().register(email, pubKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public ChallengeInfo challenge(int userID, String clientChallenge) throws RemoteException {
        try {
            return findReplica().challenge(userID, clientChallenge);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public TokenInfo authenticate(int userID, byte signature[]) throws RemoteException {
        try {
            return findReplica().authenticate(userID, signature);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public AuctionItem getSpec(int userID, int itemID, String token) throws RemoteException {
        try {
            return findReplica().getSpec(userID, itemID, token);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Integer newAuction(int userID, AuctionSaleItem item, String token) throws RemoteException {
        try {
            return findReplica().newAuction(userID, item, token);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public AuctionItem[] listItems(int userID, String token) throws RemoteException {
        try {
            return findReplica().listItems(userID, token);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public AuctionResult closeAuction(int userID, int itemID, String token) throws RemoteException {
        try {
            return findReplica().closeAuction(userID, itemID, token);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean bid(int userID, int itemID, int price, String token) throws RemoteException {
        try {
            return findReplica().bid(userID, itemID, price, token);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    public int getPrimaryReplicaID() throws RemoteException {
        try {
            findReplica();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.parseInt(primaryReplica.replace("Replica", ""));
    }

    private IReplica findReplica() throws AccessException, RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry();
        if (primaryReplica != null && ping(registry, primaryReplica)) return (IReplica) registry.lookup(primaryReplica);

        String[] regList = registry.list();
        for (int i = 0; i < regList.length; i++) {
            String name = regList[i];
            if (name.startsWith("Replica") && ping(registry, name)) return (IReplica) registry.lookup(primaryReplica);
        }

        return null;
    }

    private Boolean ping(Registry registry, String s) throws RemoteException, NotBoundException {
        System.out.println("Checking "+s);
        try {
            IReplica a = (IReplica) registry.lookup(s);
            if (a.register("", null) == -5) {
                primaryReplica = s;
                System.out.println("Primary replica set as "+s);
                return true;
            } else return false;
        } catch (Exception e) {
            return false;
        }    
    }

    public static void main(String args[]) {
        try {
            FrontEnd s = new FrontEnd();    
            String name = "FrontEnd";
            Auction stub = (Auction) UnicastRemoteObject.exportObject(s, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
            System.out.println(name+" ready");
        } catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }
}