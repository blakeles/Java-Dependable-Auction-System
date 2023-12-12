cd Server/
javac *.java
cp ./Auction.class ./AuctionItem.class ./AuctionResult.class ./AuctionSaleItem.class ./ChallengeInfo.class ./TokenInfo.class ./KeyManager.class ../Client
cd ..
cd Client/
javac Client.java
