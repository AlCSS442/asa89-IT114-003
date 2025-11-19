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

package Project.Server;

import java.io.*;
import java.util.*;
import java.nio.file.*;

import Project.Common.LoggerUtil;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;

import Project.Common.Phase;
import Project.Common.TimedEvent;

import Project.Server.Room;
import Project.Server.ServerThread;
import Project.Server.Player;
import Project.Server.PointsPayload;
import Project.Server.BaseGameRoom;
import Project.Server.Payload;

public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;

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

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerThread sp) {
        syncCurrentPhase(sp);
        syncReadyStatus(sp);

        Player player = new Player(sp.getClientName(), String.valueOf(sp.getClientId()));
        players.add(player);

        relay(null, player.getClientName() + " joined the game!");
        sendScoreboard();
    }

    @Override
    protected void onClientRemoved(ServerThread sp) {
        players.removeIf(p -> p.getClientId().equals(String.valueOf(sp.getClientId())));
        relay(null, sp.getClientName() + " left the room!");

        if (currentTurnIndex >= players.size()) {
            currentTurnIndex = 0;
        }

        sendScoreboard();

        if (clientsInRoom.isEmpty()) {
            resetTimers();
            onSessionEnd();
        }
    }

    // -------------------- Lifecycle --------------------

    @Override
    protected void onSessionStart() {
        if (!allPlayersReady()) {
            relay(null, "Cannot start session: not all players are ready!");
            return;
        }

        LoggerUtil.INSTANCE.info("Session starting...");
        changePhase(Phase.IN_PROGRESS);

        loadWordList("words.txt");
        pickNewWord();

        // Randomize first turn
        currentTurnIndex = random.nextInt(players.size());

        relay(null, "Session is starting!");
        onRoundStart();
    }

    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("Round starting...");
        resetRoundTimer();
        startRoundTimer();

        roundsPlayed++;

        // Reset strikes and guessed letters
        guessedLetters.clear();
        for (Player p : players) {
            p.setStrikes(0);
        }

        relay(null, "Round " + roundsPlayed + " has started! Word: " + getBlanksDisplay());
        onTurnStart();
    }

    @Override
    protected void onTurnStart() {
        if (players.isEmpty())
            return;

        startTurnTimer();

        Player current = players.get(currentTurnIndex);
        relay(null, "It's now " + current.getClientName() + "'s turn!");
    }

    @Override
    protected void onTurnEnd() {
        resetTurnTimer();

        Player current = players.get(currentTurnIndex);

        // Move to next turn
        currentTurnIndex++;
        if (currentTurnIndex >= players.size()) {
            // End of round
            onRoundEnd();
        } else {
            onTurnStart();
        }
    }

    @Override
    protected void onRoundEnd() {
        resetRoundTimer();
        sendScoreboard();

        if (roundsPlayed >= MAX_ROUNDS) {
            onSessionEnd();
        } else {
            pickNewWord();
            currentTurnIndex = 0; // round-robin
            onRoundStart();
        }
    }

    @Override
    protected void onSessionEnd() {
        resetTimers();
        relay(null, "Session over! Final Scores:");
        sendScoreboard();
        relay(null, "Game Over!");

        // Reset all player data
        for (Player p : players) {
            p.setStrikes(0);
            p.setPoints(0);
        }

        changePhase(Phase.READY);
    }

    // -------------------- Timers --------------------

    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, this::onRoundEnd);
        roundTimer.setTickCallback(time -> System.out.println("Round Time: " + time));
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    private void startTurnTimer() {
        turnTimer = new TimedEvent(30, this::onTurnEnd);
        turnTimer.setTickCallback(time -> System.out.println("Turn Time: " + time));
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }
    }

    private void resetTimers() {
        resetTurnTimer();
        resetRoundTimer();
    }

    // -------------------- Word & Letters --------------------

    private void loadWordList(String filePath) {
        try {
            wordList = Files.readAllLines(Paths.get(filePath));
        } catch (IOException e) {
            wordList = Arrays.asList("hangman", "java", "multiplayer", "testing");
        }
    }

    private void pickNewWord() {
        currentWord = wordList.get(random.nextInt(wordList.size())).toLowerCase();
        blanks = new char[currentWord.length()];
        Arrays.fill(blanks, '_');
        guessedLetters.clear();

        relay(null, "New word: " + getBlanksDisplay());
    }

    private String getBlanksDisplay() {
        StringBuilder sb = new StringBuilder();
        for (char c : blanks)
            sb.append(c).append(' ');
        return sb.toString().trim();
    }

    private boolean isWordSolved() {
        for (char c : blanks)
            if (c == '_')
                return false;
        return true;
    }

    // -------------------- Commands --------------------

    public void processCommand(Player player, String cmd, String arg) {
        if (players.get(currentTurnIndex) != player) {
            relay(null, player.getClientName() + " tried acting out of turn!");
            return;
        }

        switch (cmd) {
            case "/guess" -> handleWordGuess(player, arg);
            case "/letter" -> handleLetter(player, arg.charAt(0));
            case "/skip" -> handleSkip(player);
        }
    }

    private void handleWordGuess(Player player, String guess) {
        if (guess.equalsIgnoreCase(currentWord)) {
            int missing = 0;
            for (char b : blanks)
                if (b == '_')
                    missing++;

            int points = missing * 2; // Milestone: bonus points for solving
            player.addPoints(points);

            relay(null, player.getClientName() + " guessed the correct word '" + currentWord + "' and earned " + points
                    + " points!");
            syncPoints(player);

            onRoundEnd();
        } else {
            player.addStrike();
            relay(null, player.getClientName() + " guessed '" + guess + "' incorrectly!");
            onTurnEnd();
        }
    }

    private void handleLetter(Player player, char c) {
        c = Character.toLowerCase(c);
        if (guessedLetters.contains(c)) {
            relay(null, player.getClientName() + " guessed a duplicate letter!");
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

        int points = hits;
        if (hits > 0) {            
            player.addPoints(points);
            relay(null, player.getClientName() + " guessed '" + c + "' and earned " + points + " points!");
            syncPoints(player);

            if (isWordSolved())
                onRoundEnd();
            else
                onTurnEnd();
        } else {
            player.addStrike();
            relay(null, player.getClientName() + " guessed '" + c + "' and there were " + hits + " '" + c + "'s which got " + points + " points!");
            if (player.getStrikes() >= MAX_STRIKES)
                onRoundEnd();
            else
                onTurnEnd();
        }
    }

    private void handleSkip(Player player) {
        relay(null, player.getClientName() + " skipped their turn.");
        onTurnEnd();
    }

    // -------------------- Scoreboard & Payload --------------------

    private void sendScoreboard() {
        players.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));

        StringBuilder sb = new StringBuilder("ScoreBoard\n");
        for (Player p : players) {
            sb.append(p.getClientName()).append(": ").append(p.getPoints()).append(" points\n");
        }
        relay(null, sb.toString());
    }

    private void syncPoints(Player player) {
        PointsPayload payload = new PointsPayload(
                "Points: " + player.getClientName() + " has " + player.getPoints() + " points!",
                Long.parseLong(player.getClientId()), player.getPoints());
        relay(null, payload.toString());
        System.out.println(payload); // debug output
    }

    // -------------------- Helpers --------------------

    private boolean allPlayersReady() {
        for (Player p : players) {
            if (!p.isReady())
                return false;
        }
        return true;
    }
}

/*
 * // created wordlist, will load the word list now
 * // if the wordlist fails to load, will use a default wordlist, not ideal
 * though
 * private void loadWordList(String filePath) {
 * try {
 * List<String> list = Files.readAllLines(Paths.get(filePath));
 * for (String w : list) {
 * w = w.trim();
 * if (!w.isEmpty()) {
 * wordList.add(w.toLowerCase());
 * }
 * }
 * System.out.println("[GameRoom] Loaded " + wordList.size() + " words.");
 * } catch (Exception e) {
 * System.out.
 * println("[GameRoom] Failed to load words.txt, using default list instead.");
 * wordList = Arrays.asList("hangman", "java", "testing", "multiplayer");
 * }
 * }
 * 
 * 
 * 
 * 
 * public void onSessionStart() {
 * System.out.println("[GameRoom] Session is now starting...");
 * 
 * wordList.clear();
 * loadWordList("words.txt");
 * 
 * roundsPlayed = 0;
 * currentTurnIndex = random.nextInt(players.size());
 * 
 * relay(null, "Buckle up, the session is starting!");
 * onRoundStart();
 * }
 * 
 * // starting a new round now
 * public void onRoundStart() {
 * if (roundsPlayed >= MAX_ROUNDS) {
 * onSessionEnd();
 * return;
 * }
 * 
 * roundsPlayed++;
 * 
 * currentWord = wordList.get(random.nextInt(wordList.size()));
 * blanks = new char[currentWord.length()];
 * Arrays.fill(blanks, '_');
 * guessedLetters.clear();
 * 
 * for (Player player : players) {
 * player.setStrikes(0);
 * }
 * relay(null, "Round " + roundsPlayed +
 * " has started! New word has been selected!");
 * relay(null, "Word: " + getBlanksDisplay());
 * 
 * onTurnStart();
 * }
 * 
 * 
 * public void onTurnStart() {
 * if (players.isEmpty()) {
 * return;
 * }
 * Player current = players.get(currentTurnIndex);
 * relay(null, "It's now " + current.getClientName() + "'s turn to play!");
 * }
 * 
 * // processing the commands now
 * public void processCommand(Player player, String cmd, String arg) {
 * if (players.get(currentTurnIndex) != player) {
 * relay(null, player.getClientName() + " tried acting out of turn!");
 * return;
 * }
 * switch (cmd) {
 * case "/letter":
 * handleLetter(player, arg.charAt(0));
 * break;
 * case "/guess":
 * handleWordGuess(player, arg);
 * break;
 * case "/skip":
 * handleSkip(player);
 * break;
 * }
 * }
 * 
 * private void handleLetter(Player player, char c) {
 * c = Character.toLowerCase(c);
 * if (guessedLetters.contains(c)) {
 * relay(null, player.getClientName() + " guessed a duplicate letter!");
 * onTurnEnd();
 * return;
 * }
 * guessedLetters.add(c);
 * int hits = 0;
 * for (int i = 0; i < currentWord.length(); i++) {
 * if (currentWord.charAt(i) == c && blanks[i] == '_') {
 * blanks[i] = c;
 * hits++;
 * }
 * }
 * if (hits > 0) {
 * int points = hits;
 * player.addPoints(points);
 * relay(null, player.getClientName() + " found " + hits + " '" + c + "'"
 * + " 'and earned " + points + " points!");
 * relay(null, "Word: " + getBlanksDisplay());
 * 
 * if (isWordSolved()) {
 * onRoundEnd();
 * } else {
 * onTurnEnd();
 * }
 * } else {
 * player.addStrike();
 * relay(null, player.getClientName() + " guessed '" + c +
 * "' which is not in the word!");
 * relay(null, "Player: " + player.getClientId() + " got " + player.getStrikes()
 * + " strikes!");
 * 
 * if (player.getStrikes() >= MAX_STRIKES) {
 * onRoundEnd();
 * } else {
 * onTurnEnd();
 * }
 * }
 * }
 * 
 * private void handleWordGuess( Player player, String guess){
 * if (guess.equalsIgnoreCase(currentWord)){
 * 
 * int missing = 0;
 * for (char b : blanks){
 * if (b == '_'){
 * missing++;
 * }
 * }
 * 
 * int points = missing * 2;
 * player.addPoints(points);
 * 
 * relay (null, player.getClientName() + " guessed the word '" + currentWord +
 * "' and earned " + points + " points!");
 * onRoundEnd();
 * } else{
 * player.addStrike();
 * 
 * relay (null, player.getClientName() + " guessed the word '" + guess +
 * "' which is incorrect!");
 * 
 * onTurnEnd();
 * }
 * }
 * 
 * private void handleSkip(Player player) {
 * relay(null, player.getClientName() + " has chosen to skip their turn.");
 * onTurnEnd();
 * }
 * 
 * 
 * public void onTurnEnd() {
 * currentTurnIndex++;
 * 
 * if (currentTurnIndex >= players.size()) {
 * onRoundEnd();
 * return;
 * }
 * onTurnStart();
 * }
 * 
 * 
 * public void onRoundEnd() {
 * relay(null, "Round has ended!");
 * sendScoreboard();
 * 
 * if (roundsPlayed >= MAX_ROUNDS) {
 * onSessionEnd();
 * return;
 * }
 * onRoundStart();
 * }
 * 
 * 
 * public void onSessionEnd() {
 * relay(null, "The Session is over, here is the Final Scores:");
 * sendScoreboard();
 * 
 * for (Player player : players) {
 * player.setStrikes(0);
 * player.setPoints(0);
 * }
 * 
 * relay(null, "All player data has been reset.");
 * }
 * 
 * // need a scoreboard method
 * private void sendScoreboard() {
 * players.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
 * 
 * StringBuilder string = new StringBuilder("ScoreBoard\n");
 * for (Player player : players) {
 * string.append(player.getClientName()).append(":").append(player.getPoints()).
 * append(" points\n");
 * }
 * relay(null, string.toString());
 * }
 * 
 * // adding and removing players
 * public void onClientAdded(ServerThread client) {
 * addClient(client);
 * 
 * Player player = new Player(client.getClientName(), (int)
 * client.getClientId());
 * players.add(player);
 * 
 * relay(null, player.getClientName() + " joined the game!");
 * sendScoreboard();
 * 
 * }
 * 
 * 
 * public void onClientRemoved(ServerThread client) {
 * removeClient(client);
 * 
 * players.removeIf(player ->
 * player.getClientId().equals(client.getClientId()));
 * 
 * relay(null, client.getClientName() + " left the room!");
 * 
 * if (currentTurnIndex >= players.size()) {
 * currentTurnIndex = 0;
 * }
 * sendScoreboard();
 * }
 * 
 * // going to create helper to make the game easier to display to the players
 * private String getBlanksDisplay() {
 * StringBuilder blank = new StringBuilder();
 * for (char character : blanks) {
 * blank.append(character).append(" ");
 * }
 * return blank.toString().trim();
 * }
 * 
 * // another help--> check if the word is solved
 * private boolean isWordSolved() {
 * for (char c : blanks) {
 * if (c == '_') {
 * return false;
 * }
 * }
 * return true;
 * }
 */
