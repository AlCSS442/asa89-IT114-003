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
        // sync GameRoom state to new client
        syncCurrentPhase(sp);
        syncReadyStatus(sp);

        Player newPlayer = new Player(sp.getClientName(), Long.toString(sp.getClientId()));
        players.add(newPlayer);

        relay(null, newPlayer.getClientName() + " joined the game!");
    }

    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }

    // -------------------- Lifecycle --------------------

    @Override
    protected void onSessionStart() {

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

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        resetReadyStatus();
        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
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
        Player current = players.get(currentTurnIndex);
        if (!current.getClientId().equals(player.getClientId())) {
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
            resetTurnTimer();
            onTurnEnd();
        }
    }

    private void handleLetter(Player player, char c) {
        System.out.println("Handling letter guess: " + c);
        c = Character.toLowerCase(c);
        if (guessedLetters.contains(c)) {
            relay(null, player.getClientName() + " guessed a duplicate letter!");
            resetTurnTimer();
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
            relay(null, player.getClientName() + " guessed '" + c + "' and there were " + hits + " '" + c
                    + "'s which got " + points + " points!");
            if (player.getStrikes() >= MAX_STRIKES)
                onRoundEnd();
            else
                onTurnEnd();
        }
    }

    private void handleSkip(Player player) {
        relay(null, player.getClientName() + " skipped their turn.");
        resetTurnTimer();
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

}
