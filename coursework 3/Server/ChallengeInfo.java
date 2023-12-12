public class ChallengeInfo implements java.io.Serializable
{
    byte [] response; // server’s response (signature) to client’s challenge
    String clientChallenge; // server’s challenge to the client
}