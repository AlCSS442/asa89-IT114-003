//ass89
// creating a the logic that defines a specialized room that manages Hangman logic

/* Goals:
 * 1. Extend from Room class
 * 2. load a word list from a file
 * 3. keep track of:
 *  players
 *  current word   
 *  blanks
 *  strikes
 *  rounds
 */

import java.io.*;
import java.security.cert.PolicyQualifierInfo;
import java.util.*;

import Project.Server.Room;
import Project.Server.ServerThread;
import Project.Server.Player;

public class GameRoom extends Room {

    private static final int MAX_STRIKES = 6; // hangman limit
    private static final int MAX_ROUNDS = 5; // session limit

    // game state varables
    private List<Player> players = new ArrayList<>();
    private List<String> wordList = new ArrayList<>();
    private String currentWord;
    private char[] blanks;
    private int strikes;
    private int currentPlayerIndex;
    private int roundsPlayed;
    private int roundNum;

    private Random random = new Random();

    // GameRoom constructor, Room constructor only takes in one parameter (name)
    public GameRoom(String name) {
        super(name);
        loadWordList("words.txt");
        this.strikes = 0;
        this.roundNum = 0;
        this.strikes = 0;
    }

    // created wordlist, will load the word list now
    // if the wordlist fails to load, will use a default wordlist, not ideal though
private void loadWordList(String filePath){
    loadWordList("C:\\Users\\Dean\\Desktop\\NJIT\\IT114\\asa89-IT114-003\\Project\\Wordlists\\words.txt");
    try(BufferedReader br = new BufferedReader(new FileReader(filePath))){
        while ((word = br.readLine()) != null){
            word = word.trim();
            if (!word.isEmpty()){
                wordList.add(word.toLowerCase());
            }
        }
        System.out.println("[GameRoom] Loaded " + wordList.size() + " words.");
    } catch (IOException e) {
        System.out.println("[GameRoom] Failed to load words.txt.");
        wordList = Arrays.asList("java", "project", "hangman", "payload", "difficult");
    }
    
}

    // starting a new round now
    public void startNewRound() {
        if (roundNum >= MAX_ROUNDS) {
            System.out.println("[GameRoom] Max rounds reached, session will be ended!");
            return;
        }
        currentWord = wordList.get(random.nextInt(wordList.size()));
        blanks = new char[currentWord.length()];
        Arrays.fill(blanks, '_');
        strikes = 0;
        roundNum++;
        currentPlayerIndex = 0;
        System.out.println("[GameRoom] Round " + roundNum + " started. Word: " + currentWord);
        broadcast("New round has started! This word has " + blanks.length + " letters.");
        broadcast("Word: " + getBlanksDisplay());
    }

    // adding and removing players, will bneed to override methods
@Override
public void addClient(ServerThread Client){
    super.addClient(client);
    Player newPlayer = new Player(client.getClientName(), client.getClientId());
    players.add(newPlayer);
    System.out.println("[GameRoom] Player added: " + newPlayer.getName());
}

    @Override
    public void removeClient(ServerThread client) {
        super.removeClient(client);
        players.removeIf(p -> p.getId() == client.getClientId());
        System.out.println("[GameRoom] Player removed: " + client.getClientName());
    }

    // going to create helper to make the game easier to display to the players
    private String getBlanksDisplay() {
        StringBuilder blank = new StringBuilder();
        for (char character : blanks) {
            blank.append(character).append(' ');
        }
        return blank.toString().trim();
    }

}
