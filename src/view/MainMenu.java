package view;

import controller.ProgressManager;

import javax.swing.*;
import java.awt.*;

public class MainMenu extends JFrame {
    public MainMenu() {
        setTitle("Game Menu");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(0x2D2D2D));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Font buttonFont = new Font("Arial", Font.BOLD, 16);

        JButton startGameButton = new JButton("Start Game");
        JButton stagesButton = new JButton("Select Stage");
        JButton settingsButton = new JButton("Settings");
        JButton exitButton = new JButton("Exit Game");

        JButton[] buttons = {startGameButton, stagesButton, settingsButton, exitButton};
        for (JButton button : buttons) {
            button.setFont(buttonFont);
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(0x4A4A4A));
            panel.add(button, gbc);
        }

        startGameButton.addActionListener(e -> {
            int unlockedLevel = ProgressManager.loadProgress();
            new NodeEditorUI(unlockedLevel).setVisible(true);
            this.dispose();
        });

        stagesButton.addActionListener(e -> {
            Object[] options = {"Level 1", "Level 2"};
            int choice = JOptionPane.showOptionDialog(this,
                    "Choose a level to start:",
                    "Stage Selection",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            if (choice != JOptionPane.CLOSED_OPTION) {
                int levelNumber = choice + 1;
                new NodeEditorUI(levelNumber).setVisible(true);
                this.dispose();
            }
        });

        settingsButton.addActionListener(e -> new SettingsDialog(this).setVisible(true));
        exitButton.addActionListener(e -> System.exit(0));
        add(panel);
    }
}