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
import java.util.*;
import java.nio.file.*;

import Project.Server.Room;
import Project.Server.ServerThread;
import Project.Server.Player;
import Project.Server.Payloads.*;

public class GameRoom extends Room {

    private static final int MAX_STRIKES = 6; // hangman limit
    private static final int MAX_ROUNDS = 5; // session limit

    // game state varables
    private List<Player> players = new ArrayList<>();
    private List<String> wordList = new ArrayList<>();
    private String currentWord;
    private char[] blanks;
    private int currentTurnIndex = 0;
    private int roundsPlayed = 0;
    private Set<Character> guessedLetters = new HashSet<>(); // the array of letters the players already guessed

    private Random random = new Random();

    // GameRoom constructor, Room constructor only takes in one parameter (name)
    public GameRoom(String name) {
        super(name);
    }

    // created wordlist, will load the word list now
    // if the wordlist fails to load, will use a default wordlist, not ideal though
    private void loadWordList(String filePath) {
        try {
            List<String> list = Files.readAllLines(Paths.get(filePath));
            for (String w : list) {
                w = w.trim();
                if (!w.isEmpty()) {
                    wordList.add(w.toLowerCase());
                }
            }
            System.out.println("[GameRoom] Loaded " + wordList.size() + " words.");
        } catch (Exception e) {
            System.out.println("[GameRoom] Failed to load words.txt, using default list instead.");
            wordList = Arrays.asList("hangman", "java", "testing", "multiplayer");
        }
    }

    @Override
    public void onSessionStart() {
        System.out.println("[GameRoom] Session is now starting...");

        wordList.clear();
        loadWordList("words.txt");

        roundsPlayed = 0;
        currentTurnIndex = random.nextInt(players.size());

        broadcast(new MessagePayload("server", "A new session has started, buckle up!"));
        onRoundStart();
    }

    // starting a new round now
    public void onRoundStart() {
        if (roundsPlayed >= MAX_ROUNDS) {
            onSessionEnd();
            return;
        }

        roundsPlayed++;

        currentWord = wordList.get(random.nextInt(wordList.size()));
        blanks = new char[currentWord.length()];
        Arrays.fill(blanks, '_');
        guessedLetters.clear();

        for (Player player : players) {
            player.setStrikes(0);
        }

        broadcast(new MessagePayload("server", "Round " + roundsPlayed + " has started! New word has been selected!"));
        broadcast(new MessagePayload("server", "Word: " + getBlanksDisplay()));

        onTurnStart();
    }

    @Override
    public void onTurnStart() {
        if (players.isEmpty()) {
            return;
        }
        Player current = players.get(currentTurnIndex);
        broadcast(new MessagePayload("server", "It's now " + current.getClientName() + "'s turn to play!"));
    }

    // processing the commands now
    public void processCommand(Player player, String cmd, String arg) {
        if (players.get(currentTurnIndex) != player) {
            broadcast(new MessagePayload("server", player.getClientName() + " tried acting out of turn!"));
            return;
        }
        switch (cmd) {
            case "/letter":
                handleLetter(player, arg.charAt(0));
                break;
            case "/guess":
                handleWordGuess(player, arg);
                break;
            case "/skip":
                handleSkip(player);
                break;
        }
    }

    private void handleLetter(Player player, char c) {
        c = Character.toLowerCase(c);
        if (guessedLetters.contains(c)) {
            broadcast(new MessagePayload("server", player.getClientName() + " guessed a duplicate letter!"));
            onTurnEnd();
            return;
        }
        guessedLetters.add(c);
        int hits = 0;
        for (int i = 0; i < currentWord.length(); i++) {
            if (currentWord.charAt(i) == c && blanks[i] == '_') {
                blanks[i] = c;
                hits++;
            }
        }
        if (hits > 0) {
            int points = hits;
            player.addPoints(points);

            broadcast(new PointsPayload(player.getClientId(), player.getPoints()));
            broadcast(new MessagePayload("server", player.getClientName() + " found " + hits + " '" + c + "'"
                    + " 'and earned " + points + " points!"));
            broadcast(new MessagePayload("server", "Word: " + getBlanksDisplay()));

            if (isWordSolved()) {
                onRoundEnd();
            } else {
                onTurnEnd();
            }
        } else {
            player.addStrike();
            broadcast(new StrikePayload(player.getClientId(), player.getStrikes()));
            broadcast(new MessagePayload("server",
                    player.getClientName() + " guessed '" + c + "' which is not in the word!"));

            if (player.getStrikes() >= MAX_STRIKES) {
                onRoundEnd();
            } else {
                onTurnEnd();
            }
        }
    }

    private void handleWordGuess( Player player, String guess){
        if (guess.equalsIgnoreCase(currentWord)){

            int missing = 0;
            for (char b : blanks){
                if (b == '_'){
                    missing++;
                }
            
            int points = missing * 2;
            player.addPoints(points);
            
            broadcast(new PointsPayload(player.getClientId(), player.getPoints()));
            broadcast(new MessagePayload("server", player.getClientName() + " guessed the word '" + currentWord + "' and earned " + points + " points!"));
            onRoundEnd();
            } else{
                player.addStrike();
                broadcast(new StrikePayload(player.getClientId(), player.getStrikes()));
                broadcast(new MessagePayload("server", player.getClientName() + " guessed the word '" + guess + "' which is incorrect!"));
            
                onTurnEnd();
            }
        }

    private void handleSkip(Player player) {
        broadcast(new MessagePayload("server", player.getClientName() + " has chosen to skip their turn."));
        onTurnEnd();
    }

    @Override
    public void onTurnEnd() {
        currentTurnIndex++;

        if (currentTurnIndex >= players.size()) {
            onRoundEnd();
            return;
        }
        onTurnStart();
    }

    @Override
    public void onRoundEnd() {
        broadcast(new MessagePayload("server", "Round has ended!"));
        sendScoreboard();

        if (roundsPlayed >= MAX_ROUNDS) {
            onSessionEnd();
            return;
        }
        onRoundStart();
    }

    @Override
    public void onSessionEnd() {
        broadcast(new MessagePayload("server", "The Session is over, here is the Final Scores:"));
        sendScoreboard();

        for (Player player : players) {
            player.setStrikes(0);
            player.setPoints(0);
        }

        broadcast(new MessagePayload("server", "All player data has been reset."));
    }

    // need a scoreboard method
    private void sendScoreboard() {
        players.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));

        StringBuilder string = new StringBuilder("ScoreBoard\n");
        for (Player player : players) {
            string.append(player.getClientName()).append(":").append(player.getPoints()).append(" points\n");
        }
        broadcast(new MessagePayload("server", string.toString()));
    }

    // adding and removing players, will bneed to override methods
    @Override
    public void onClientAdded(ServerThread client) {
        super.onClientAdded(client);

        Player player = new Player(client.getClientName(), client.getClientId());
        players.add(player);

        broadcast(new MessagePayload("server", player.getClientName() + " joined the game!"));
        sendScoreboard();

    }

    @Override
    public void onClientRemoved(ServerThread client) {
        super.onClientRemoved(client);

        players.removeIf(player -> player.getClientId().equals(client.getClientId()));

        broadcast(new MessagePayload("server", client.getClientName() + " left the room!"));

        if (currentTurnIndex >= players.size()) {
            currentTurnIndex = 0;
        }
        sendScoreboard();
    }

    // going to create helper to make the game easier to display to the players
    private String getBlanksDisplay() {
        StringBuilder blank = new StringBuilder();
        for (char character : blanks) {
            blank.append(character).append(" ");
        }
        return blank.toString().trim();
    }

    // another help--> check if the word is solved
    private boolean isWordSolved() {
        for (char c : blanks) {
            if (c == '_') {
                return false;
            }
        }
        return true;
    }

}

