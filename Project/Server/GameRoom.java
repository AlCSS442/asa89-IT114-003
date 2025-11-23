package Project.Server;

import java.io.*;
import java.util.*;
import java.nio.file.*;

import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;
import Project.Server.Payload;
import Project.Server.PointsPayload;

public class GameRoom extends BaseGameRoom {

    private TimedEvent roundTimer = null;
    private TimedEvent turnTimer = null;

    private static final int MAX_STRIKES = 6; // Hangman limit
    private static final int MAX_ROUNDS = 5;  // Session limit

    private List<ServerThread> players = new ArrayList<>();
    private List<String> wordList = new ArrayList<>();
    private String currentWord;
    private char[] blanks;
    private int currentTurnIndex = 0;
    private int roundsPlayed = 0;
    private Set<Character> guessedLetters = new HashSet<>();

    // Map to track strikes per player
    private Map<ServerThread, Integer> strikesMap = new HashMap<>();
    private Random random = new Random();

    public GameRoom(String name) {
        super(name);
    }

    @Override
    protected void onClientAdded(ServerThread sp) {
        syncCurrentPhase(sp);
        syncReadyStatus(sp);
        players.add(sp);
        strikesMap.put(sp, 0); // initialize strikes
        relay(null, sp.getClientName() + " joined the game!");
    }

    @Override
    protected void onClientRemoved(ServerThread sp) {
        players.remove(sp);
        strikesMap.remove(sp);
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        }
    }

    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("Session starting...");
        changePhase(Phase.IN_PROGRESS);

        loadWordList("words.txt");
        pickNewWord();
        currentTurnIndex = random.nextInt(players.size());

        relay(null, "Session is starting!");
        onRoundStart();
    }

    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("Round starting...");
        resetRoundTimer();
        startRoundTimer();
        currentTurnIndex = 0;
        roundsPlayed++;

        guessedLetters.clear();
        // reset all strikes
        for (ServerThread sp : players) {
            strikesMap.put(sp, 0);
        }

        relay(null, "Round " + roundsPlayed + " has started! Word: " + getBlanksDisplay());
        onTurnStart();
    }

    @Override
    protected void onTurnStart() {
        if (players.isEmpty()) return;
        startTurnTimer();
        ServerThread current = players.get(currentTurnIndex);
        relay(null, "It's now " + current.getClientName() + "'s turn!");
    }

    @Override
    protected void onTurnEnd() {
        resetTurnTimer();
        ServerThread current = players.get(currentTurnIndex);

        if (isWordSolved() || allPlayersMaxStrikes()) {
            onRoundEnd();
            return;
        }

        currentTurnIndex = (currentTurnIndex + 1) % players.size();
        onTurnStart();
    }

    @Override
    protected void onRoundEnd() {
        resetRoundTimer();
        sendScoreboard();

        if (roundsPlayed >= MAX_ROUNDS) {
            onSessionEnd();
        } else {
            pickNewWord();
            currentTurnIndex = 0;
            onRoundStart();
        }
    }

    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        resetReadyStatus();
        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }

    //Timers
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

    // -------------------- Word Handling --------------------
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
        for (char c : blanks) sb.append(c).append(' ');
        return sb.toString().trim();
    }

    private boolean isWordSolved() {
        for (char c : blanks)
            if (c == '_') return false;
        return true;
    }

    // Commands
    public void processCommand(ServerThread client, String cmd, String arg) {
        ServerThread current = players.get(currentTurnIndex);
        if (!current.equals(client)) {
            relay(null, client.getClientName() + " tried acting out of turn!");
            return;
        }

        switch (cmd.toLowerCase()) {
            case "/guess" -> handleWordGuess(client, arg);
            case "/letter" -> handleLetter(client, arg);
            case "/skip" -> handleSkip(client);
        }
    }

    private void handleWordGuess(ServerThread client, String guess) {
        resetTurnTimer();
        guess = guess.toLowerCase();

        if (guess.equals(currentWord)) {
            int missing = 0;
            for (char b : blanks) if (b == '_') missing++;
            int points = missing * 2;

            client.addPoints(points);
            relay(null, client.getClientName() + " guessed the correct word '" + currentWord + "' and earned " + points + " points!");
            relay(null, "Word solved: " + currentWord);
            sendPlayerPoints(client);
            onRoundEnd();
        } else {
            addStrike(client);
            relay(null, client.getClientName() + " guessed '" + guess + "' incorrectly! Strike " + strikesMap.get(client) + "/" + MAX_STRIKES);
            if (strikesMap.get(client) >= MAX_STRIKES || allPlayersMaxStrikes()) {
                onRoundEnd();
            } else {
                onTurnEnd();
            }
        }
    }

    public void handleLetter(ServerThread client, String letterGuess) {
        resetTurnTimer();
        char c = Character.toLowerCase(letterGuess.charAt(0));

        if (guessedLetters.contains(c)) {
            relay(null, client.getClientName() + " guessed a duplicate letter!");
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
            client.addPoints(hits);
            relay(null, client.getClientName() + " guessed '" + c + "' correctly and earned " + hits + " points!");
            sendPlayerPoints(client);
            relay(null, "Current word: " + getBlanksDisplay());
        } else {
            addStrike(client);
            relay(null, client.getClientName() + " guessed '" + c + "' incorrectly! Strike " + strikesMap.get(client) + "/" + MAX_STRIKES);
        }

        if (isWordSolved() || strikesMap.get(client) >= MAX_STRIKES || allPlayersMaxStrikes()) {
            onRoundEnd();
        } else {
            onTurnEnd();
        }
    }

    public void handleSkip(ServerThread client) {
        resetTurnTimer();
        relay(null, client.getClientName() + " has skipped their turn.");
        onTurnEnd();
    }

    // Strikes Management
    private void addStrike(ServerThread client) {
        strikesMap.put(client, strikesMap.getOrDefault(client, 0) + 1);
    }

    private boolean allPlayersMaxStrikes() {
        for (ServerThread sp : players) {
            if (strikesMap.getOrDefault(sp, 0) < MAX_STRIKES) return false;
        }
        return true;
    }

    // Scoreboard 
    private void sendPlayerPoints(ServerThread client) {
        PointsPayload payload = new PointsPayload(
                "Points: " + client.getClientName() + " has " + client.getPoints() + " points!",
                client.getClientId(),
                client.getPoints());
        relay(null, payload.toString());
    }

    private void sendScoreboard() {
        players.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));

        StringBuilder sb = new StringBuilder("ScoreBoard\n");
        for (ServerThread sp : players) {
            sb.append(sp.getClientName()).append(": ").append(sp.getPoints()).append(" points\n");
        }
        relay(null, sb.toString());
    }
}
