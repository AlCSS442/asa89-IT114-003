package Project.Client.Views;

import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JPanel;

import Project.Client.Client;
import Project.Common.Phase;
import java.awt.Dimension;

public class PlayView extends JPanel {
    private final JPanel buttonPanel = new JPanel();

    public PlayView(String name) {
        this.setName(name);

        /*
         * // example user interaction
         * JButton doSomething = new JButton("Do Something");
         * doSomething.addActionListener(_ -> {
         * try {
         * Client.INSTANCE.sendDoTurn("example");
         * } catch (IOException e) {
         * e.printStackTrace();
         * }
         * });
         * buttonPanel.add(doSomething);
         * this.add(buttonPanel);
         * }
         * public void changePhase(Phase phase){
         * if (phase == Phase.READY) {
         * buttonPanel.setVisible(false);
         * } else if (phase == Phase.IN_PROGRESS) {
         * buttonPanel.setVisible(true);
         * }
         * }
         */

        // Replaced the Do Something button with letter buttons
        for (char c = 'A'; c <= 'Z'; c++) {
            String letter = String.valueOf(c);

            JButton letterButton = new JButton(letter);

            letterButton.addActionListener(_ -> {
                try {
                    Client.INSTANCE.sendLetter(letter.charAt(0)); 
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            buttonPanel.add(letterButton);
        }
            
            
        buttonPanel.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
        this.add(buttonPanel);
    }

    public void changePhase(Phase phase) {
        if (phase == Phase.READY) {
            buttonPanel.setVisible(false);
        } else if (phase == Phase.IN_PROGRESS) {
            buttonPanel.setVisible(true);
        }
    }
}