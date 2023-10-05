/**
 * Jared Tassin
 * Programming Assignment #1 for CSCI 5311, 3/7/2023
 * Server for Group Chat
 */
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.lang.Math;

public class MyServer {

    static List clients = Collections.synchronizedList(new ArrayList<String>());  //List of usernames
    static ArrayList<MultiThreadSocket> outclients = new ArrayList<MultiThreadSocket>(); //List of connections
    public static void main(String args[]) throws IOException, InterruptedException
    {
        //int port = Integer.parseInt(args[0]); 
        int port = 6969; //for manual testing in netbeans
        System.out.println("Server Stared.");
        while(true){  //make loop of accepting new connections
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Awaiting client");
        Socket s=ss.accept();
        System.out.println("Client joined.");
        MultiThreadSocket t = new MultiThreadSocket(s); //create the client-server connection
        outclients.add(t); //add to list of active connections
        t.start(); //start the thread
        
        Thread.sleep(2000); //wait 2 seconds before reopening for a new connection
        ss.close();
        }
    }    
}
class PokerBetListener extends Thread{
    boolean bet;
    boolean bMyTurn = false;
    PokerPlayer player;
    PokerHandler ph;
    
    public PokerBetListener(PokerPlayer player,PokerHandler ph){
        this.player=player;
        this.bet=player.hasMadeBet();
        this.ph = ph;
    }
    @Override
    public void run(){
        while(true){
        while(!this.bet && bMyTurn){
                this.bet = player.hasMadeBet(); 
        }
        if(bet){
            try{
            
                synchronized(this){this.wait();}
                this.player.bHasMadeBet = false;
                this.bMyTurn = false;
            }catch(InterruptedException e){}
        //synchronized(ph){this.ph.notify();}
        }
        }
    }
}
class BetWaiter extends Thread {
    List<PokerBetListener> l;
    boolean bReady = false;
    public BetWaiter(List<PokerBetListener> bettors){
        l = bettors;
    }
    public BetWaiter(PokerBetListener b){
        l.add(b);
    }
    @Override
    public void run(){
        while(!ready()){
            
        }
        
    }
    public boolean ready(){
        boolean check = false;
        for(PokerBetListener b : l)
            if(b.getState() == Thread.State.WAITING)
                check = true;
            else
                check = false;
        return check;
    }
}
class PokerHandler extends Thread{ //this needs to be singleton so that only one game happens at a time
    private static PokerHandler instance = null;
    static List<PokerBetListener> bettors;
    static List<PokerPlayer> players = new ArrayList<PokerPlayer>(); //players will be dealt in order of connection
    //static List<Integer> pokerdeck = new ArrayList<Integer>(); //deck will be represented as integers and parsed into values and suits
    static Deck pokerdeck;// = new Deck();
    static List<PokerPlayer> activePlayers;
    static PokerTable pokertable;
    static Integer defaultStartingChips = 500;
    static Integer defaultBlinds = 1;
    static Integer smallBlind;
    static Integer bigBlind;
    static Integer currentCall;
    static Integer pot = 0;
    String stage;
    BetWaiter waiter;
    boolean bWaitingForBets = false;
    PokerPlayer dealer;
    PokerBetListener bettor;
    static boolean bIsRunning = false;
    static boolean bEndGame = false;
    //this will be texas hold em poker
    private PokerHandler(){
        //pokerdeck = IntStream.range(1,52).boxed().collect(Collectors.toList());
        //Collections.shuffle(pokerdeck);
        pokerdeck = new Deck();
        pokerdeck.shuffleDeck();
    }
    public static synchronized PokerHandler getInstance(){
        if(instance == null)
            instance = new PokerHandler();
        return instance;
    }
    @Override
    public void run(){
        //this.start();
       while(!this.bEndGame)
       {
           if(bIsRunning){
               if(stage.contentEquals("start")){
               while(bettors.size()<activePlayers.size()){
               for(PokerPlayer player : this.players){
                   PokerBetListener b = new PokerBetListener(player,this);
                   bettors.add(b);
                   b.start();
               }
               }
               for(int i = 0; i < bettors.size(); i++){
                    if(bettors.get(Math.floorMod(i-1,bettors.size())).player == this.dealer)
                        bettors.get(i).bMyTurn = true;
               }
               if(stage.contentEquals("start"))
                    dealPlayers();
                    blinds();
               if(stage.contentEquals("blinds")){
                   if(!bWaitingForBets){
                       waiter = new BetWaiter(bettors);
                       bWaitingForBets = true;
                   }
                   if(bWaitingForBets){
                       for(int i = 0; i < bettors.size(); i++){
                           PokerPlayer p = bettors.get(Math.floorMod(i-1,bettors.size())).player;
                            if(bettors.get(Math.floorMod(i-1,bettors.size())).bMyTurn)
                                MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+p.getName()+"'s turn to bet...");
                       }
                       //if
                   }
                       
                       
                    if(waiter.ready()){
                       stage = "theFlop";
                       bWaitingForBets = false;
                    }
                }
               }
               if(stage.contentEquals("theFlop")){
                    theFlop();
                    for(PokerBetListener b : bettors)
                        synchronized(b){b.notify();}
               }
               //newRound();
           }
       }//System.out.println("poker thread running");}
    }
    public void blinds(){
        this.stage = "blinds";
        //this.activePlayers = this.players;
            //collect blinds
            for(int i = 0; i<this.activePlayers.size(); i++){
                if(this.activePlayers.get(i).isDealer()){
                    PokerPlayer leftPlayer = this.activePlayers.get(Math.floorMod(i-1,this.activePlayers.size()));
                    if(this.activePlayers.size()<3)
                        leftPlayer = this.activePlayers.get((i)%this.activePlayers.size());
                    int smallblind = leftPlayer.takeBlinds(this.smallBlind);
                    this.pot = this.pot + smallblind;
                    PokerPlayer rightPlayer = this.activePlayers.get((i+1)%this.activePlayers.size());
                    int bigblind = rightPlayer.takeBlinds(this.bigBlind);
                    this.pot = this.pot + bigblind;
                }
            }
    }
    public void theFlop(){
        this.currentCall = this.bigBlind;
            //deal
            //dealPlayers();
            for(PokerPlayer player : this.activePlayers){
                player.bHasMadeBet = false;
            }
    }
    
    public void noBets(){
        dealPlayers();
        dealTable(3);
        dealTable(1);
        dealTable(1);
        List<PokerPlayer> winners = new ArrayList();
        List<Card> phand = new ArrayList();
        List<Card> winnershand = new ArrayList();
        HandEvaluator ranker;
        int best=0;
        for(PokerPlayer p : activePlayers){
            phand = new ArrayList();
            MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+p.getName()+"'s hand = "+p.getHand().toString());
            phand = pokertable.table;
            phand.addAll(p.hand);
            ranker = new HandEvaluator(phand);
            if(ranker.ranker()>best){
                winners.add(p);
                winnershand = phand;
                best = ranker.ranker();
            }
        }
        ranker = new HandEvaluator(winnershand);
        best = ranker.ranker();
        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+winners.get(winners.size()-1).getName()+" won the game with a "+ranker.getRank(best));
        //bIsRunning = false;
    }
    public static synchronized void addPlayer(PokerHandler ph,String player){
        if(!bIsRunning)
            addPlayer(ph,player, defaultStartingChips);
    }
    public static synchronized void addPlayer(PokerHandler ph,String player, Integer startingChips){
        if(!bIsRunning){
            ph.players.add(new PokerPlayer(player, startingChips));
            MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player+" joined the game.");
        }
    }
    public static synchronized void removePlayer(PokerHandler ph,String player){
        List<PokerPlayer> temp =  new ArrayList<PokerPlayer>();
        synchronized(ph.players){for(PokerPlayer name : ph.players)
                if(name.getName().contentEquals(player)){
                    temp.add(name);
                    MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+name.getName()+" left the game.");
                }}
        ph.players.removeAll(temp);
        if(ph.players.isEmpty()){
            MultiThreadSocket.sendMessageToAll("[Poker] All players left. Shutting Down.");
            ph.bEndGame = true;
            ph.bIsRunning = false;
        }
    }
    public static synchronized void startGame(PokerHandler ph){
        if(!bIsRunning && !ph.players.isEmpty()){
            ph.bIsRunning = true;
            ph.pokertable = new PokerTable();
            ph.smallBlind = defaultBlinds;
            ph.bigBlind = defaultBlinds*2;
            ph.players.get(0).setButton(true);
            ph.dealer = ph.players.get(0);
            ph.activePlayers = ph.players;
            ph.stage = "start";
            //while(!bEndGame)
            //    ph.newRound();
        }
    }
    public static synchronized void startGameNoBets(PokerHandler ph){
        if(bIsRunning && !ph.players.isEmpty()){
            ph.activePlayers = ph.players;
            ph.shuffleCards();
            ph.pokertable.clearTable();
            for(PokerPlayer p:ph.activePlayers)
                p.fold();
            ph.noBets();
        }
        if(!bIsRunning && !ph.players.isEmpty()){
            ph.bIsRunning = true;
            ph.pokertable = new PokerTable();
            ph.smallBlind = defaultBlinds;
            ph.bigBlind = defaultBlinds*2;
            ph.players.get(0).setButton(true);
            ph.dealer = ph.players.get(0);
            ph.activePlayers = ph.players;
            //while(!bEndGame)
                ph.noBets();
        }
    }
    public static synchronized void startGame(PokerHandler ph,int smallBlind, int bigBlind){
        if(!bIsRunning && !ph.players.isEmpty()){
            ph.bIsRunning = true;
            ph.pokertable = new PokerTable();
            ph.smallBlind = smallBlind;
            ph.bigBlind = bigBlind;
            ph.players.get(0).setButton(true);
            ph.dealer = ph.players.get(0);
            ph.activePlayers = ph.players;
            //while(!bEndGame)
            //    ph.newRound();
            ph.stage = "start";
        }
    }
    public static synchronized void playerMakeBet(PokerHandler ph,String player, Integer bet){
        if(!bIsRunning)
            for(PokerPlayer name : ph.players)
                if(name.getName().contentEquals(player)){
                    name.makeBet(bet);
                }
    }
    public static synchronized void playerMakeCheck(PokerHandler ph, String player){
        if(!bIsRunning)
            for(PokerPlayer name : ph.players)
                if(name.getName().contentEquals(player))
                    if(name.canCheck()){
                        name.makeBet(0);
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+name.getName()+" checked.");
                    }
                    else
                        MultiThreadSocket.sendMessageToOnePokerPlayer("[Poker] You cannot check here.",player);
    }
    public static synchronized Integer getCurrentCall(PokerHandler ph){
        return ph.currentCall;
    }
    public static synchronized void playerFold(PokerHandler ph,String player){
        if(!bIsRunning)
            for(PokerPlayer name : ph.players)
                if(name.getName().contentEquals(player)){
                    name.fold();
                    ph.activePlayers.remove(name);
                    MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+name.getName()+" folded.");
                }
    }
    public static synchronized void getPlayerChips(PokerHandler ph,String player){
        if(!bIsRunning)
            for(PokerPlayer name : ph.players)
                if(name.getName().contentEquals(player)){
                    int chips = name.getChips();
                    MultiThreadSocket.sendMessageToOnePokerPlayer("[Poker] You have "+chips+" chips.",name.getName());
                }
    }
    private synchronized void newRound(){
       if(!bIsRunning){
        this.activePlayers = this.players;
            //collect blinds
            for(int i = 0; i<this.activePlayers.size(); i++){
                if(this.activePlayers.get(i).isDealer()){
                    PokerPlayer leftPlayer = this.activePlayers.get(Math.floorMod(i-1,this.activePlayers.size()));
                    if(this.activePlayers.size()<3)
                        leftPlayer = this.activePlayers.get((i)%this.activePlayers.size());
                    int smallblind = leftPlayer.takeBlinds(this.smallBlind);
                    this.pot = this.pot + smallblind;
                    PokerPlayer rightPlayer = this.activePlayers.get((i+1)%this.activePlayers.size());
                    int bigblind = rightPlayer.takeBlinds(this.bigBlind);
                    this.pot = this.pot + bigblind;
                }
            }
            this.currentCall = this.bigBlind;
            //deal
            dealPlayers();
            for(PokerPlayer player : this.activePlayers){
                player.bHasMadeBet = false;
            }
            //wait for bets and folds, cannot check here
            //boolean bAllBetsIn = false;
            //while(!bAllBetsIn){
                for(PokerPlayer bettingPlayer : this.activePlayers){
                    //boolean bBetMade = false;
                    //boolean bAllin = false;
                    this.bettor = new PokerBetListener(bettingPlayer,this);
                    bettor.start();
                    //for(PokerPlayer player : this.activePlayers){
                    MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+bettingPlayer.getName()+"'s turn to bet...");
                    //}
                    
                
                    try{
                      //  this.bettor = new PokerBetListener(bettingPlayer,this);
                        //bettor.start();
                        //this.wait();
                        bettor.join();
                    }catch(InterruptedException e) {}
                    //while(!bBetMade || !bAllin){
                      //  bBetMade = bettingPlayer.hasMadeBet();
                        //bAllin = bettingPlayer.isAllIn();
                        //System.out.println("stuck here with "+bettingPlayer.getName());
                        //System.out.println(bettingPlayer.getName()+" has made a bet? "+bettingPlayer.hasMadeBet());
                    //}
                    //bAllBetsIn = !bAllBetsIn && player.hasMadeBet();
                    if(bettingPlayer.isAllIn())
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+bettingPlayer.getName()+" is all in! ("+bettingPlayer.getChips()+")");
                    if(bettingPlayer.getBet()>this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+bettingPlayer.getName()+" raised "+bettingPlayer.getBet());
                    if(bettingPlayer.getBet()==this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+bettingPlayer.getName()+" called. ("+this.currentCall+")");
                //}    
                
                    }
            //deal the flop
            dealTable(3);
            //wait for bets and folds
            for(PokerPlayer player : this.activePlayers){
                player.bCanCheck = true;
                player.bHasMadeBet = false;
            }
            for(PokerPlayer player : this.activePlayers){
                    MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+"'s turn to bet...");
                    //while(!player.hasMadeBet() || !player.isAllIn()){
                    try{
                      this.wait();
                    }catch(InterruptedException e) {}
                    //}
                    if(!player.canCheck())
                        for(PokerPlayer allplayer : this.activePlayers){
                            allplayer.bCanCheck = false;
                        }
                    if(player.isAllIn())
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" is all in! ("+player.getChips()+")");
                    //bAllBetsIn = !bAllBetsIn && player.hasMadeBet();
                    if(player.getBet()>this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" raised "+player.getBet());
                    if(player.getBet()==this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" called. ("+this.currentCall+")");
                    //bAllBetsIn = !bAllBetsIn && player.hasMadeBet();
                }
            //deal the turn
            dealTable(1);
            //wait for bets and folds
            for(PokerPlayer player : this.activePlayers){
                player.bHasMadeBet = false;
            }
            for(PokerPlayer player : this.activePlayers){
                    MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+"'s turn to bet...");
                    while(!player.hasMadeBet() || !player.isAllIn()){
                        
                    }
                    if(!player.canCheck())
                        for(PokerPlayer allplayer : this.activePlayers){
                            allplayer.bCanCheck = false;
                        }
                    if(player.isAllIn())
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" is all in! ("+player.getChips()+")");
                    //bAllBetsIn = !bAllBetsIn && player.hasMadeBet();
                    if(player.getBet()>this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" raised "+player.getBet());
                    if(player.getBet()==this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" called. ("+this.currentCall+")");
                    //bAllBetsIn = !bAllBetsIn && player.hasMadeBet();
                }
            //deal the river
            dealTable(1);
            //wait for bets and folds
            for(PokerPlayer player : this.activePlayers){
                player.bHasMadeBet = false;
            }
            for(PokerPlayer player : this.activePlayers){
                    MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+"'s turn to bet...");
                    while(!player.hasMadeBet() || !player.isAllIn()){
                        
                    }
                    if(!player.canCheck())
                        for(PokerPlayer allplayer : this.activePlayers){
                            allplayer.bCanCheck = false;
                        }
                    if(player.isAllIn())
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" is all in! ("+player.getChips()+")");
                    //bAllBetsIn = !bAllBetsIn && player.hasMadeBet();
                    if(player.getBet()>this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" raised "+player.getBet());
                    if(player.getBet()==this.currentCall)
                        MultiThreadSocket.sendMessageToAllPokerPlayers("[Poker] "+player.getName()+" called. ("+this.currentCall+")");
                    //bAllBetsIn = !bAllBetsIn && player.hasMadeBet();
                }
            //determine winner
            //reset hands
            
            shuffleCards();
            //remove broke players
            for(PokerPlayer player : this.players)
                if(player.getChips() <= 0)
                    removePlayer(this,player.getName());
            //rotate dealer if players >1
            //if players == 1, end game
            if(this.activePlayers.size()<2)
                this.bIsRunning = false;
       }
    }
    private void dealPlayers(){
        for(int i = 0; i < this.players.size(); i++){
            //Integer card1 = this.pokerdeck.remove(0);
            Card card1 = this.pokerdeck.drawCard();
            //Integer card2 = this.pokerdeck.remove(0);
            Card card2 = this.pokerdeck.drawCard();
            this.players.get(i).addCard(card1);
            this.players.get(i).addCard(card2);
            MultiThreadSocket.sendMessageToOnePokerPlayer(this.players.get(i).getHand().toString(),this.players.get(i).name);
            if(this.players.get(i).isDealer())
                MultiThreadSocket.sendMessageToOnePokerPlayer("You are the Dealer",this.players.get(i).name);
        }
    }
    private void dealTable(int n){
        for(int i = 0; i < n; i++){
            //Integer card = this.pokerdeck.remove(0);
            //this.pokertable.addCard(card);
            Card c = this.pokerdeck.drawCard();
            this.pokertable.addCard(c);
        }
        MultiThreadSocket.sendMessageToAllPokerPlayers(this.pokertable.getCards().toString());
    }
    private void shuffleCards(){
        //this.pokerdeck = IntStream.range(1,52).boxed().collect(Collectors.toList());
        //Collections.shuffle(pokerdeck);
        pokerdeck.shuffleDeck();
    }
}
class PokerPlayer{
    List<Card> hand = new ArrayList<Card>();
    String name;
    Integer chips;
    Integer currentBet = 0;
    String action;
    boolean bCanCheck = false;
    boolean bIsAllIn = false;
    boolean bHasMadeBet = false;
    boolean bHasButton;
    public PokerPlayer(String name, Integer startingChips){
        this.name = name;
        this.chips = startingChips;
    }
    public String getName(){
        return this.name;
    }
    public void addCard(Card card){
        this.hand.add(card);
    }
    public void removeCard(Card card){
        this.hand.remove(card);
    }
    public List<String> getHand(){
        List temp = new ArrayList();
        String t;
        for(Card c : hand){
            t = Card.rankToString(c.getRank())+" of "+Card.suitToString(c.getSuit());
            temp.add(t);
        }
        return temp;
    }
    public Integer getChips(){
        return this.chips;
    }
    public Integer getBet(){
        return this.currentBet;
    }
    public Integer takeBlinds(int blind){
        if((this.chips - blind) < this.chips){
            this.currentBet = chips;
            this.bHasMadeBet = true;
            this.bIsAllIn = true;
            int n = this.chips;
            this.chips = 0;
            return n;
        }
        else{
            return takeChips(blind);
        }
    }
    public void makeBet(int bet){
        if(bet>0)
            this.bCanCheck = false;
        if((this.chips - bet) > this.chips){
            this.currentBet = this.currentBet + bet;
            this.bHasMadeBet = true;
        }
        else{
            this.currentBet = chips;
            this.bHasMadeBet = true;
            this.bIsAllIn = true;
        }
    }
    public boolean hasMadeBet(){
        return this.bHasMadeBet;
    }
    public void nextBetRound(){
        this.bHasMadeBet = false;
        this.bIsAllIn = false;
    }
    public boolean isAllIn(){
        return bIsAllIn;
    }
    public boolean canCheck(){
        return this.bCanCheck;
    }
    public Integer takeChips(Integer n){
        this.chips = this.chips - n;
        return n;
    }
    public void fold(){
        this.hand.removeAll(hand);
        this.bHasMadeBet = true;
    }
    public boolean isDealer(){
        return this.bHasButton;
    }
    public void setButton(boolean b){
        this.bHasButton = b;
    }
}
class PokerTable{
    List<Card> table = new ArrayList<Card>();
    public PokerTable(){
    
    }
    public void addCard(Card card){
        this.table.add(card);
    }
    public List<String> getCards(){
        List temp = new ArrayList();
        String t;
        for(Card c : table){
            t = Card.rankToString(c.getRank())+" of "+Card.suitToString(c.getSuit());
            temp.add(t);
        }
        return temp;
        //return this.table;
    }
    //public void removeCard(Integer card){
    //    this.table.remove(card);
    //}
    public void clearTable(){
        this.table.removeAll(table);
    }
}
class MultiThreadSocket extends Thread{
    private Socket s;
    String clientusername = null; //Server keeps track of username sent by client, but it needs to be initialized, since there is gap between connection and username being set by client
    DataInputStream infromClient;
    DataOutputStream outtoClient;
    PrintWriter outputpw;
    PokerHandler pokergame;
    static boolean bIsPlayingPoker = false;
    MultiThreadSocket(Socket socket) throws IOException{
        this.s = socket;
        infromClient = new DataInputStream(s.getInputStream());
        outtoClient = new DataOutputStream(s.getOutputStream());
        outputpw = new PrintWriter(outtoClient,true);
    }
    @Override
    public void run(){
        String line = ""; //this is set to empty after some commands below so that whatever is sent to server gets broadcast
            while (!line.equals("Bye")) { //loop until disconnect command is sent
                try {
                    line = infromClient.readUTF();
                    System.out.println(">> "+clientusername+": "+line); //this acts as a log for the server
                } catch (IOException i) {
                    System.out.println("Error " + i.getMessage());
                    //System.out.println("var line = "+line); //debug code
                    break; //the only time this happens is usually because of a connection error, so close the connection. Otherwise, the server ends up repeating the last message to everyone in a loop.
                }
                if(line.startsWith("username = ")){ //set username
                    clientusername = line.substring(11);
                    synchronized (MyServer.clients){ MyServer.clients.add(clientusername); } //make sure that access to shared resource is queued in case of simultaneous access
                    sendMessageToAll("Server: Welcome "+clientusername);
                    line = "";
                }
                if(line.equals("AllClients")){ //get list of active users
                    String listOfClients = "";
                    synchronized (MyServer.clients) {
                        for(int i = 0; i < MyServer.clients.size(); i++){
                            listOfClients = listOfClients + MyServer.clients.get(i) + ", ";
                        }
                    }
                    sendMessageToSelf(listOfClients);
                    line = "";
                }
                if(line.startsWith("/")){ //support for server commands
                    if(line.startsWith("/whisper")){ //send a message to single user
                        List<String> lineargs = Arrays.asList(line.split("\\s+",3));
                        //System.out.println("Size of whisper array is: "+Integer.toString(lineargs.size())); //debug code
                        sendMessageToOne(lineargs.get(2),lineargs.get(1));
                        sendMessageToSelf("[To "+lineargs.get(1)+"] "+lineargs.get(2));
                    }
                    if(line.startsWith("/poker")){
                        pokergame = PokerHandler.getInstance();
                        if(!pokergame.isAlive())
                            pokergame.start();
                        List<String> lineargs = Arrays.asList(line.split("\\s+",3));
                        if(lineargs.get(1).startsWith("join")){
                            //pokergame = PokerHandler.getInstance();
                            this.bIsPlayingPoker = true;
                            if(lineargs.size() == 2)
                                pokergame.addPlayer(pokergame,clientusername);
                            else
                                pokergame.addPlayer(pokergame,clientusername,Integer.valueOf(lineargs.get(2)));
                        }
                        if(lineargs.get(1).startsWith("start")){
                            if(lineargs.size() == 2){
                                //pokergame = PokerHandler.getInstance();
                                pokergame.startGame(pokergame);
                            }
                            if(lineargs.size() == 3){
                                if(lineargs.get(2).startsWith("nobets"))
                                    pokergame.startGameNoBets(pokergame);
                                else{
                                    //pokergame = PokerHandler.getInstance();
                                    pokergame.startGame(pokergame,Integer.valueOf(lineargs.get(2)),Integer.valueOf(lineargs.get(2))*2);
                                }
                            }
                        }
                        if(lineargs.get(1).startsWith("chips")){
                            //pokergame = PokerHandler.getInstance();
                            pokergame.getPlayerChips(pokergame,clientusername);
                        }
                        if(lineargs.get(1).startsWith("bet")){
                            //pokergame = PokerHandler.getInstance();
                            if(lineargs.get(2).startsWith("call")){
                                pokergame.playerMakeBet(pokergame,clientusername, pokergame.getCurrentCall(pokergame));
                                synchronized(pokergame.bettor){pokergame.bettor.notify();}
                            }
                            else if(lineargs.get(2).startsWith("check"))
                                pokergame.playerMakeCheck(pokergame,clientusername);
                            else if(Integer.valueOf(lineargs.get(2))>pokergame.getCurrentCall(pokergame))
                                pokergame.playerMakeBet(pokergame,clientusername, Integer.valueOf(lineargs.get(2)));
                            else
                                sendMessageToSelf("[Poker] Not big enough bet");  
                        }
                        if(lineargs.get(1).startsWith("fold")){
                            pokergame = PokerHandler.getInstance();
                            pokergame.playerFold(pokergame,clientusername);
                        }
                        if(lineargs.get(1).startsWith("leave")){
                            pokergame.removePlayer(pokergame,clientusername);
                        }
                    }
                    line = "";
                }
                if(!line.equals("")){sendMessageToAll(clientusername+": "+line);} //if none of the above commands were called, then it was a message to be broadcast to all.
            } //end of loop, which means client has disconnected.
            System.out.println(">> "+clientusername+" Disconnected."); //log it in the server
            sendMessageToAll("Server: "+clientusername+" Disconnected."); //and let everyone know
            synchronized (MyServer.clients) { MyServer.clients.remove(clientusername); } //remove client from user list
            synchronized (MyServer.outclients) { MyServer.outclients.remove(this); } //and the connection list
            try{
                infromClient.close(); //close the datastreams
                outtoClient.close();
                s.close(); //and the socket
            } catch (IOException i) {
            System.out.println(i);
            }
    }
    public static void sendMessageToAll(String outputString){ //broadcast the message to everyone connected
        synchronized (MyServer.outclients){ //lockdown the list
            for(MultiThreadSocket mts : MyServer.outclients){ //iterate through the client connections
                mts.outputpw.println(">> "+outputString); //and print the message to their datastream
            }
        }
    }
    public static void sendMessageToAllPokerPlayers(String outputString){ //broadcast the message to everyone connected
        synchronized (MyServer.outclients){ //lockdown the list
            for(MultiThreadSocket mts : MyServer.outclients){ //iterate through the client connections
                if(bIsPlayingPoker)
                    mts.outputpw.println(">> "+outputString); //and print the message to their datastream
            }
        }
    }
    private void sendMessageToSelf(String outputString){ //sometimes, we want to only communicate the message to the client that called the command, as with the AllClients command. 
        this.outputpw.println(">> "+outputString);
    }
    private void sendMessageToOne(String outputString, String user){ //send a message to a specific user.
        synchronized (MyServer.outclients){ //lockdown the list
            for(MultiThreadSocket mts : MyServer.outclients){
                if(mts.clientusername.equals(user)){ //find the client whose username matches the desired destination
                    mts.outputpw.println(">> "+"[From "+clientusername+"] "+outputString); //let the receiving client know who the message is from
                }
            }
        }
    }
    public static void sendMessageToOnePokerPlayer(String outputString, String user){ //send a message to a specific user.
        synchronized (MyServer.outclients){ //lockdown the list
            for(MultiThreadSocket mts : MyServer.outclients){
                if(mts.clientusername.equals(user)){ //find the client whose username matches the desired destination
                    mts.outputpw.println(">> "+"[Poker"+"] "+outputString); //let the receiving client know who the message is from
                }
            }
        }
    }
}

class Card {
    private final int rank;
    private final int suit;

    // Kinds of suits
    public final static int DIAMONDS = 1;
    public final static int CLUBS    = 2;
    public final static int HEARTS   = 3;
    public final static int SPADES   = 4;

    // Kinds of ranks
    public final static int ACE   = 1;
    public final static int DEUCE = 2;
    public final static int THREE = 3;
    public final static int FOUR  = 4;
    public final static int FIVE  = 5;
    public final static int SIX   = 6;
    public final static int SEVEN = 7;
    public final static int EIGHT = 8;
    public final static int NINE  = 9;
    public final static int TEN   = 10;
    public final static int JACK  = 11;
    public final static int QUEEN = 12;
    public final static int KING  = 13;

    public Card(int rank, int suit) {
        assert isValidRank(rank);
        assert isValidSuit(suit);
        this.rank = rank;
        this.suit = suit;
    }

    public int getSuit() {
        return suit;
    }

    public int getRank() {
        return rank;
    }

    public static boolean isValidRank(int rank) {
        return ACE <= rank && rank <= KING;
    }

    public static boolean isValidSuit(int suit) {
        return DIAMONDS <= suit && suit <= SPADES;
    }

    public static String rankToString(int rank) {
        switch (rank) {
        case ACE:
            return "Ace";
        case DEUCE:
            return "Deuce";
        case THREE:
            return "Three";
        case FOUR:
            return "Four";
        case FIVE:
            return "Five";
        case SIX:
            return "Six";
        case SEVEN:
            return "Seven";
        case EIGHT:
            return "Eight";
        case NINE:
            return "Nine";
        case TEN:
            return "Ten";
        case JACK:
            return "Jack";
        case QUEEN:
            return "Queen";
        case KING:
            return "King";
        default:
            return null;
        }    
    }
    
    public static String suitToString(int suit) {
        switch (suit) {
        case DIAMONDS:
            return "Diamonds";
        case CLUBS:
            return "Clubs";
        case HEARTS:
            return "Hearts";
        case SPADES:
            return "Spades";
        default:
            return null;
        }    
    }

    public static void main(String[] args) {
    	
        assert rankToString(ACE) == "Ace";
        assert rankToString(DEUCE) == "Deuce";
        assert rankToString(THREE) == "Three";
        assert rankToString(FOUR) == "Four";
        assert rankToString(FIVE) == "Five";
        assert rankToString(SIX) == "Six";
        assert rankToString(SEVEN) == "Seven";
        assert rankToString(EIGHT) == "Eight";
        assert rankToString(NINE) == "Nine";
        assert rankToString(TEN) == "Ten";
        assert rankToString(JACK) == "Jack";
        assert rankToString(QUEEN) == "Queen";
        assert rankToString(KING) == "King";

        assert suitToString(DIAMONDS) == "Diamonds";
        assert suitToString(CLUBS) == "Clubs";
        assert suitToString(HEARTS) == "Hearts";
        assert suitToString(SPADES) == "Spades";

    }
}

class Deck{
    List<Card> deck = new ArrayList<Card>();
    List<Card> removedCards = new ArrayList<Card>();
    public Deck(){
        for(int i = 1; i<5;i++)
            for(int j = 1; j<14; j++)
                this.deck.add(new Card(j,i));
    }
    public Card drawCard(){
        if(!deck.isEmpty()){
            Card c = deck.remove(0);
            removedCards.add(c);
            return c;
        }
        else{
            shuffleDeck();
            return deck.remove(0);
        }
    }
    public void shuffleDeck(){
        deck.addAll(removedCards);
        Collections.shuffle(deck);
    }
}
class HandEvaluator{
    private List<Card> cards = new ArrayList();
    private List<Card> hand;
    
    
    public static final int ROYAL_FLUSH = 99999;
    public static final int STRAIGHT_FLUSH = 10000;
    public static final int FOUR_OF_A_KIND = 5000;
    public static final int FULL_HOUSE = 2500;
    public static final int FLUSH = 1000;
    public static final int STRAIGHT = 500;
    public static final int THREE_OF_A_KIND = 250;
    public static final int TWO_PAIR = 100;
    public static final int ONE_PAIR = 50;
    public static final int HIGH_CARD = 0;
    
    public HandEvaluator(List<Card> hand){
        if(hand != null)
            for(Card c : hand)
                cards.add(c);
    }
    public int main(){
        return ranker();
    }
    public void sortCards(){
        Collections.sort(cards,(c1,c2) -> {return c1.getRank()-c1.getRank();});
        //List<Card> temp = new ArrayList();
        //for(int i = 0; i<cards.size(); i++){
        //}
    }
    public int ranker(){
        if(isRoyalFlush())
          return HandEvaluator.ROYAL_FLUSH;
        if(isFlush() && isStraight())
            return HandEvaluator.STRAIGHT_FLUSH;
        if(isFourKind())
            return HandEvaluator.FOUR_OF_A_KIND;
        if(isFullHouse())
            return HandEvaluator.FULL_HOUSE;
        if(isFlush())
            return HandEvaluator.FLUSH;
        if(isStraight())
            return HandEvaluator.STRAIGHT;
        if(isThreeKind())
            return HandEvaluator.THREE_OF_A_KIND;
        if(isTwoPair())
            return HandEvaluator.TWO_PAIR;
        if(isTwoKind())
            return HandEvaluator.ONE_PAIR;
        return HandEvaluator.HIGH_CARD + highCard();
    }
    public String getRank(int rank){
        if(rank>=HandEvaluator.ROYAL_FLUSH)
            return "Royal Flush";
        if(rank>=HandEvaluator.STRAIGHT_FLUSH)
            return "Straight Flush";
        if(rank>=HandEvaluator.FOUR_OF_A_KIND)
            return "Four of a Kind";
        if(rank>=HandEvaluator.FLUSH)
            return "Flush";
        if(rank>=HandEvaluator.STRAIGHT)
            return "Straight";
        if(rank>=HandEvaluator.THREE_OF_A_KIND)
            return "Three of a Kind";
        if(rank>=HandEvaluator.TWO_PAIR)
            return "Two Pair";
        if(rank>=HandEvaluator.ONE_PAIR)
            return "One Pair";
        if(rank>=HandEvaluator.HIGH_CARD)
            return "High Card";
        return "Nothing";
    }
    public int highCard(){
        List<Integer> temp = new ArrayList();
        sortCards();
        for(Card c : cards)
            temp.add(c.getRank());
        if(temp.get(0)==1)
            return 14;
        return temp.get(temp.size()-1);
    }
    public boolean isRoyalFlush(){
        List<Integer> temp = new ArrayList();
        if(isFlush())
            if(isStraight()){
                for(Card c : cards)
                    temp.add(c.getRank());
                if(temp.containsAll(List.of(Card.ACE,Card.TEN,Card.JACK,Card.QUEEN,Card.KING)))
                    return true;
            }
        return false;
    }
    public boolean isFullHouse(){
        boolean twokind=false;
        boolean threekind=false;
        if(isTwoKind())
            twokind = true;
        if(isThreeKind())
            threekind = true;
        return twokind && threekind;
    }
    public boolean isFourKind(){
        for(Card c : cards){
            int count = nPair(c.getRank());
            if(count==4)
                return true;
        }
            return false;
    }
    public boolean isThreeKind(){
        for(Card c : cards){
            int count = nPair(c.getRank());
            if(count==3)
                return true;
        }
            return false;
    }
    public boolean isTwoPair(){
        List<Integer> pairs = new ArrayList();
        for(Card c : cards){
            int count = nPair(c.getRank());
            if(count==2){
                if(!pairs.contains(c.getRank()))
                    pairs.add(c.getRank());
            }
        }
        if(pairs.size()>=2)
            return true;
        return false;
    }
    public boolean isTwoKind(){
        for(Card c : cards){
            int count = nPair(c.getRank());
            if(count==2)
                return true;
        }
            return false;
    }
    public int nPair(int card){
        List<Integer> temp = new ArrayList();
        for(Card c : cards){
            temp.add(c.getRank());
        }
        int count = Collections.frequency(temp, card);
        
        return count;
    }
    //public int rankNPair(int card){
    //    
    //}
    public boolean isStraight(){
        sortCards();
        int count=0;
        for(int i = 1; i<cards.size(); i++){
            if(count>=5)
                return true;
            if((cards.get(i).getRank()-cards.get(i-1).getRank() == 1) || (cards.get(i).getRank()-cards.get(0).getRank() == 12)) //incremental, or King and Ace, which has a diference of 12, and Ace would always be first if present, and would never be next to a King
                count++;
            else
                count=0;
        }
        return false;
    }
    public boolean isFlush(){
        int diamonds=0;
        int clubs=0;
        int hearts=0;
        int spades=0;
        for(Card c : cards){
            if(c.getSuit()==1)
                diamonds++;
            if(c.getSuit()==2)
                clubs++;
            if(c.getSuit()==3)
                hearts++;
            if(c.getSuit()==4)
                spades++;
        }
        if(diamonds>=5)
            return true;
        if(clubs>=5)
            return true;
        if(hearts>=5)
            return true;
        if(spades>=5)
            return true;
        return false;               
    }
}