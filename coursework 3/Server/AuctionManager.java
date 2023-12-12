import java.io.Serializable;
import java.security.PublicKey;
import java.util.LinkedList;

public class AuctionManager implements Serializable {    
    LinkedList<Event> auctions = new LinkedList<Event>();

    public AuctionItem createItem(int ID, String name, String description, int price) {
        AuctionItem item = new AuctionItem();

        item.itemID = ID;
        item.name = name;
        item.description = description;
        item.highestBid = price;

        return item;
    }

    public User createUser(int ID, String email, PublicKey key) {
        User user = new User();

        user.userID = ID;
        user.email = email;
        user.pubKey = key;

        TokenInfo token = new TokenInfo();
        token.expiryTime = 0;
        token.token = "";
        user.lastToken = token;
        user.lastChallenge = "";

        return user;
    }

    public void addEvent(int user, int item) {
        Event event = new Event();
        event.userID = user;
        event.itemID = item;
        event.winner = user;

        auctions.addLast(event);
    }

    public int eventCheck(int user, int item, int check) {
        if (check == 0) for (int i = 0; i < auctions.size(); i++) if (auctions.get(i).userID != user && auctions.get(i).itemID == item) return i;
        
        if (check > 0) for (int i = 0; i < auctions.size(); i++) if (auctions.get(i).userID == user && auctions.get(i).itemID == item) return i;

        return -1;
    }

    public Boolean updateEvent(int x, int w) {
        if (x != -1) {
            auctions.get(x).winner = w;
            return true;
        }

        return false;
    }

    public LinkedList<Event> getAuctionList() {
        return auctions;
    }
}
