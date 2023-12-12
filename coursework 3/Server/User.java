import java.io.Serializable;
import java.security.PublicKey;

public class User implements Serializable {
    int userID;
    String email;
    PublicKey pubKey;
    TokenInfo lastToken;
    String lastChallenge;
}
