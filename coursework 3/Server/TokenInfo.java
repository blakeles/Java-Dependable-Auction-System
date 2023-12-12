public class TokenInfo implements java.io.Serializable {
    String token; // a one-time use token issued by server
    long expiryTime; // expiration time as a Unix timestamp
}