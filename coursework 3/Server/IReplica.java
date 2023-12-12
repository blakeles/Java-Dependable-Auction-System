import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.PublicKey;
import java.util.LinkedList;

public interface IReplica extends Remote {
    public Integer register(String email, PublicKey pubKey)throws RemoteException;
    public ChallengeInfo challenge(int userID, String clientChallenge) throws RemoteException;
    public TokenInfo authenticate(int userID, byte signature[]) throws RemoteException;
    public AuctionItem getSpec(int userID, int itemID, String token) throws RemoteException;
    public Integer newAuction(int userID, AuctionSaleItem item, String token) throws RemoteException;
    public AuctionItem[] listItems(int userID, String token) throws RemoteException;
    public AuctionResult closeAuction(int userID, int itemID, String token) throws RemoteException;
    public boolean bid(int userID, int itemID, int price, String token) throws RemoteException;
    public int getPrimaryReplicaID() throws RemoteException;
    public void receiveAM(AuctionManager aum) throws RemoteException;
    public void receiveItems(LinkedList<AuctionItem> inputItems) throws RemoteException;
    public void receiveUsers(LinkedList<User> inputUsers) throws RemoteException;
    public KeyManager grabKM() throws RemoteException;
    public AuctionManager grabAM() throws RemoteException;
    public LinkedList<AuctionItem> grabItems() throws RemoteException;
    public LinkedList<User> grabUsers() throws RemoteException;
}
