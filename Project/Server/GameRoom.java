package Project.Server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.TimedEvent;
import Project.Common.TimerType;
import Project.Exceptions.MissingCurrentPlayerException;
import Project.Exceptions.NotPlayersTurnException;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;

public class GameRoom extends BaseGameRoom {

    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;

    // professor-required turn fields
    private List<ServerThread> turnOrder = new ArrayList<>();
    private long currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
    private int round = 0;

    // --- Hangman game-specific fields ---
    private static final int MAX_STRIKES = 6; // Hangman limit per player
    private static final int MAX_ROUNDS = 5; // Session limit (you can adjust)
    private List<String> wordList = new ArrayList<>();
    private String currentWord;
    private char[] blanks;
    private Set<Character> guessedLetters = new HashSet<>();
    private int strikes = 0;
    private Random random = new Random();
    // -------------------------------------

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerThread sp) {
        // sync GameRoom state to new client

        syncCurrentPhase(sp);
        // sync only what's necessary for the specific phase
        // if you blindly sync everything, you'll get visual artifacts/discrepancies
        syncReadyStatus(sp);
        if (currentPhase != Phase.READY) {
            syncTurnStatus(sp); // turn/ready use the same visual process so ensure turn status is only called
                                // outside of ready phase
            syncPlayerPoints(sp);
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        long removedClient = sp.getClientId();
        turnOrder.removeIf(player -> player.getClientId() == sp.getClientId());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        } else if (removedClient == currentTurnClientId) {
            // if the player removed was the current, advance to next player
            onTurnStart();
        }
    }

    // timer handlers
    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, () -> onRoundEnd());
        roundTimer.setTickCallback((time) -> {
            System.out.println("Round Time: " + time);
            sendCurrentTime(TimerType.ROUND, time);
        });
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
            sendCurrentTime(TimerType.ROUND, -1);
        }
    }

    private void startTurnTimer() {
        turnTimer = new TimedEvent(30, () -> onTurnEnd());
        turnTimer.setTickCallback((time) -> {
            System.out.println("Turn Time: " + time);
            sendCurrentTime(TimerType.TURN, time);
        });
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
            sendCurrentTime(TimerType.TURN, -1);
        }
    }
    // end timer handlers

    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        changePhase(Phase.IN_PROGRESS);
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
        setTurnOrder();
        round = 0;
        // prepare words (load once per session)
        loadWordList("words.txt");
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        resetRoundTimer();
        resetTurnStatus();
        round++;

        pickNewWord();
         
        guessedLetters.clear();
        strikes = 0;


        // relay(null, String.format("Round %d has started", round));
        sendGameEvent(String.format("Round %d has started", round));
        // startRoundTimer(); Round timers aren't needed for turns
        // if you do decide to use it, ensure it's reasonable and based on the number of
        // players
        LoggerUtil.INSTANCE.info("onRoundStart() end");
        onTurnStart();
    }

    /** {@inheritDoc} */

    /*
     * @Override
     * protected void onTurnStart() {
     * LoggerUtil.INSTANCE.info("onTurnStart() start");
     * resetTurnTimer();
     * try {
     * // getNextPlayer advances the currentTurnClientId (if needed) and returns the
     * // player whose turn it is
     * ServerThread currentPlayer = getNextPlayer();
     * relay(null, String.format("It's %s's turn", currentPlayer.getDisplayName()));
     * } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {
     * 
     * e.printStackTrace();
     * }
     * startTurnTimer();
     * LoggerUtil.INSTANCE.info("onTurnStart() end");
     * }
     */
    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();

        try {
            ServerThread currentPlayer = getNextPlayer();

            if (currentPlayer == null) {
                // all players eliminated, end round
                onRoundEnd();
                return;
            }

            // relay(null, String.format("It's %s's turn", currentPlayer.getDisplayName()));
            sendGameEvent(String.format("It's %s's turn", currentPlayer.getDisplayName()));
        } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {

            e.printStackTrace();
        }

        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring
        try {
            // optionally can use checkAllTookTurn();
            if (isLastPlayer()) {
                // if the current player is the last player in the turn order, end the round
                onRoundEnd();
            } else {
                onTurnStart();
            }
        } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {

            e.printStackTrace();
        }
        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("Hangman onRoundEnd() start");

        // Only end the round if word solved OR all players struck out
        if (isWordSolved() || allPlayersMaxStrikes()) {

            sendGameEvent("The word was: " + currentWord);

           

            // Showing the scoreboard
            sendScoreboard();

             // Reset word + guessed letters + blanks
            strikes = 0;

            if (round >= MAX_ROUNDS) {
                onSessionEnd();
            } else {
                onRoundStart();
            }

        } else {
            // If round SHOULD NOT END, keep going
            // Move to next player's turn
            LoggerUtil.INSTANCE.info("Word not finished — continuing round");
            try {
                onTurnStart();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        turnOrder.clear();
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
        resetReadyStatus();
        resetTurnStatus();
        changePhase(Phase.READY);
        LoggerUtil.INSTANCE.info("onSessionEnd() end");
    }
    // end lifecycle methods

    // send/sync data to ServerThread(s)
    private void syncPlayerPoints(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendPlayerPoints(serverUser.getClientId(),
                        serverUser.getPoints());
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    private void sendPlayerPoints(ServerThread sp) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPlayerPoints(sp.getClientId(), sp.getPoints());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void sendResetTurnStatus() {
        clientsInRoom.values().forEach(spInRoom -> {
            boolean failedToSend = !spInRoom.sendResetTurnStatus();
            if (failedToSend) {
                removeClient(spInRoom);
            }
        });
    }

    private void sendTurnStatus(ServerThread client, boolean tookTurn) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(client.getClientId(), client.didTakeTurn());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void syncTurnStatus(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendTurnStatus(serverUser.getClientId(),
                        serverUser.didTakeTurn(), true);
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    // end send data to ServerThread(s)

    // misc methods
    private void resetTurnStatus() {
        clientsInRoom.values().forEach(sp -> {
            sp.setTookTurn(false);
        });
        sendResetTurnStatus();
    }

    /**
     * Sets `turnOrder` to a shuffled list of players who are ready.
     */
    private void setTurnOrder() {
        turnOrder.clear();
        turnOrder = clientsInRoom.values().stream().filter(ServerThread::isReady).collect(Collectors.toList());
        Collections.shuffle(turnOrder);
    }

    /**
     * Gets the current player based on the `currentTurnClientId`.
     *
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private ServerThread getCurrentPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // quick early exit
        if (currentTurnClientId == Constants.DEFAULT_CLIENT_ID) {
            throw new MissingCurrentPlayerException("Current Player not set");
        }
        return turnOrder.stream()
                .filter(sp -> sp.getClientId() == currentTurnClientId)
                .findFirst()
                // this shouldn't occur but is included as a "just in case"
                .orElseThrow(() -> new PlayerNotFoundException("Current player not found in turn order"));
    }

    /**
     * Gets the next player in the turn order.
     * If the current player is the last in the turn order, it wraps around
     * (round-robin).
     *
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    /*
     * !!!
     * private ServerThread getNextPlayer() throws MissingCurrentPlayerException,
     * PlayerNotFoundException {
     * int index = 0;
     * if (currentTurnClientId != Constants.DEFAULT_CLIENT_ID) {
     * index = turnOrder.indexOf(getCurrentPlayer()) + 1;
     * if (index >= turnOrder.size()) {
     * index = 0;
     * }
     * }
     * ServerThread nextPlayer = turnOrder.get(index);
     * currentTurnClientId = nextPlayer.getClientId();
     * return nextPlayer;
     * }
     */
    private ServerThread getNextPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        if (turnOrder.isEmpty()) {
            throw new PlayerNotFoundException("No players in turn order");
        }

        int index = 0;
        if (currentTurnClientId != Constants.DEFAULT_CLIENT_ID) {
            index = turnOrder.indexOf(getCurrentPlayer()) + 1;
            if (index >= turnOrder.size()) {
                index = 0;
            }
        }
        ServerThread nextPlayer = turnOrder.get(index);
        currentTurnClientId = nextPlayer.getClientId();
        return nextPlayer;
    }

    /**
     * Checks if the current player is the last player in the turn order.
     *
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private boolean isLastPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // check if the current player is the last player in the turn order
        return turnOrder.indexOf(getCurrentPlayer()) == (turnOrder.size() - 1);
    }

    private void checkAllTookTurn() {
        int numReady = clientsInRoom.values().stream()
                .filter(sp -> sp.isReady())
                .toList().size();
        int numTookTurn = clientsInRoom.values().stream()
                // ensure to verify the isReady part since it's against the original list
                .filter(sp -> sp.isReady() && sp.didTakeTurn())
                .toList().size();
        if (numReady == numTookTurn) {
            // relay(null,
            // String.format("All players have taken their turn (%d/%d) ending the round",
            // numTookTurn, numReady));
            sendGameEvent(
                    String.format("All players have taken their turn (%d/%d) ending the round", numTookTurn, numReady));
            onRoundEnd();
        }
    }

    // start check methods
    private void checkCurrentPlayer(long clientId) throws NotPlayersTurnException {
        if (currentTurnClientId != clientId) {
            throw new NotPlayersTurnException("You are not the current player");
        }
    }

    // end check methods

    // receive data from ServerThread (GameRoom specific)

    /**
     * Handles the turn action from the client.
     * 
     * @param currentUser
     * @param exampleText (arbitrary text from the client, can be used for
     *                    additional actions or information)
     */
    protected void handleTurnAction(ServerThread currentUser, String exampleText) {
        // check if the client is in the room
        try {
            checkPlayerInRoom(currentUser);
            checkCurrentPhase(currentUser, Phase.IN_PROGRESS);
            checkCurrentPlayer(currentUser.getClientId());
            checkIsReady(currentUser);
            if (currentUser.didTakeTurn()) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have already taken your turn this round");
                return;
            }
            // example points
            int points = new Random().nextInt(4) == 3 ? 1 : 0;
            sendGameEvent(String.format("%s %s", currentUser.getDisplayName(),
                    points > 0 ? "gained a point" : "didn't gain a point"));
            if (points > 0) {
                currentUser.changePoints(points);
                sendPlayerPoints(currentUser);
            }
            currentUser.setTookTurn(true);
            // TODO handle example text possibly or other turn related intention from client
            sendTurnStatus(currentUser, currentUser.didTakeTurn());
            // finished processing the turn
            onTurnEnd();
        } catch (NotPlayersTurnException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "It's not your turn");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (NotReadyException e) {
            // The check method already informs the currentUser
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PlayerNotFoundException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PhaseMismatchException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You can only take a turn during the IN_PROGRESS phase");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        }
    }

    // end receive data from ServerThread (GameRoom specific)

    // -------------------- Hangman: Word & game handling --------------------

    private void loadWordList(String filePath) {
        try {
            wordList = Files.readAllLines(
                    Paths.get("C:\\Users\\Dean\\Desktop\\NJIT\\IT114\\asa89-IT114-003\\Project\\words.txt"));
            // remove empty lines and trim
            wordList = wordList.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        } catch (IOException e) {
            // fallback small list if file missing
            wordList = Arrays.asList("hangman");
        }
    }

    private void pickNewWord() {
        if (wordList.isEmpty()) {
            currentWord = "hangman";
        } else {
            currentWord = wordList.get(random.nextInt(wordList.size())).toLowerCase();
        }
        blanks = new char[currentWord.length()];
        Arrays.fill(blanks, '_');
        guessedLetters.clear();
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

    // Strikes Management
    private void addStrike() {
        strikes++;
    }

    private boolean allPlayersMaxStrikes() {
        return strikes >= MAX_STRIKES;
    }

    /*
    // Scoreboard
    private void sendPlayerPoints(ServerThread client) {
        // I have a PointsPayload or similar, adapt this. For now send simple
        // relay.
        relay(null, "Points: " + client.getClientName() + " has " + client.getPoints() + " points!");
    }
        */

    private void sendScoreboard() {
        // produce sorted scoreboard
        List<ServerThread> sorted = clientsInRoom.values().stream()
                .sorted((a, b) -> Integer.compare(b.getPoints(), a.getPoints()))
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder("ScoreBoard\n");
        for (ServerThread sp : sorted) {
            sb.append(sp.getClientName()).append(": ").append(sp.getPoints()).append(" points\n");
        }
        sendGameEvent(sb.toString());
    }

    // -------------------- Command processing (Hangman) --------------------

    /**
     * Accepts the command and ensures it's the current player's turn according to
     * the
     * professor's turn system. After processing, marks the player as having taken
     * their turn,
     * notifies other clients, and advances the turn lifecycle.
     *
     * Commands:
     * - /guess <word>
     * - /letter <char>
     * - /skip
     */
    public void handleLetter(ServerThread client, String letterStr) {

        if (letterStr == null || letterStr.isEmpty()) {
            sendGameEvent("No letter provided.");
            return;
        }

        char letter = Character.toLowerCase(letterStr.charAt(0));

        // ignore letters already guessed
        if (guessedLetters.contains(letter)) {
           sendGameEvent(client.getClientName() + " already guessed '" + letter + "'");
            onTurnEnd();
            return;
        }

        guessedLetters.add(letter);

        boolean correct = false;
        for (int i = 0; i < currentWord.length(); i++) {
            if (currentWord.charAt(i) == letter) {
                blanks[i] = letter;
                correct = true;
            }
        }

        if (correct) {
            int points = 1; // you can adjust points per letter
            client.addPoints(points);
            sendGameEvent(client.getClientName() + " guessed letter '" + letter + "' correctly and earned " + points
                    + " points!");
        } else {
            // wrong guess
            addStrike();
            sendGameEvent(
                    client.getClientName() + " guessed incorrectly! Global strikes: " + strikes + "/" + MAX_STRIKES);

        }

        sendGameEvent("Current word: " + getBlanksDisplay());
        sendPlayerPoints(client);

        // mark turn complete
        client.setTookTurn(true);
        sendTurnStatus(client, true);

        // check if word solved or all players max strikes
        if (isWordSolved() || allPlayersMaxStrikes()) {
            onRoundEnd();
        } else {
            onTurnEnd();
        }
    }

    public void handleSkip(ServerThread client) {
        sendGameEvent(client.getClientName() + " chose to skip their turn.");

        client.setTookTurn(true);
        sendTurnStatus(client, true);

        onTurnEnd();
    }

    public void handleWordGuess(ServerThread client, String guess) {

        if (guess == null) {
            sendGameEvent("No guess provided.");
            return;
        }
        guess = guess.toLowerCase().trim();

        // CORRECT FULL WORD GUESS
        if (guess.equals(currentWord)) {
            int missing = 0;
            for (char b : blanks)
                if (b == '_')
                    missing++;

            client.addPoints(missing);

            sendGameEvent(client.getClientName() +
                    " guessed the correct word '" + currentWord +
                    "' and earned " + missing + " points!");

            sendGameEvent("Word solved: " + currentWord);
            sendPlayerPoints(client);

            // marking it as solved
            for (int i = 0; i < currentWord.length(); i++) {
                blanks[i] = currentWord.charAt(i);
            }

            // mark turn taken for professor's turn system
            client.setTookTurn(true);
            sendTurnStatus(client, true);

            strikes = 0;
            onRoundEnd();
            return;
        }

        // wrong guess
        addStrike();
        sendGameEvent(client.getClientName() + " guessed incorrectly! Global strikes: " + strikes + "/" + MAX_STRIKES);

        sendPlayerPoints(client);

        // Mark turn complete
        client.setTookTurn(true);
        sendTurnStatus(client, true);

        // If global strikes exceeded, end round
        if (strikes >= MAX_STRIKES) {
            sendGameEvent("The word was: " + currentWord);
            strikes = 0;
            onRoundEnd();
            return;
        }

        // End turn, NOT the round
        onTurnEnd();
    }
}