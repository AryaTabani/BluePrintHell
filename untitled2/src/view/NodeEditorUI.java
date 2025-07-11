package view;

import controller.NodeCanvasPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class NodeEditorUI extends JFrame {
    private final NodeCanvasPanel canvas;

    public NodeEditorUI(int levelNumber) {
        setTitle("Network Architect - Level " + levelNumber);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1024, 768));
        setLocationRelativeTo(null);
        JLayeredPane layeredPane = new JLayeredPane();
        HudPanel hud = new HudPanel();
        JButton rewindButton = new JButton("Rewind Time");
        JButton previewButton = new JButton("Preview");



        JButton exitPreviewButton = new JButton("Exit Preview");
        JSlider timeSlider = new JSlider();

        canvas = new NodeCanvasPanel(hud, this, rewindButton, levelNumber,
                previewButton, exitPreviewButton, timeSlider);
        canvas.setBounds(0, 0, 1024, 768);

        hud.setBounds(getWidth() - 320, 20, 250, 120);
        JPanel topBar = createTopBar(hud, rewindButton, previewButton);
        JPanel bottomBar = createBottomBar(exitPreviewButton, timeSlider);

        layeredPane.add(canvas, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(hud, JLayeredPane.PALETTE_LAYER);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(topBar, BorderLayout.NORTH);
        contentPane.add(layeredPane, BorderLayout.CENTER);
        contentPane.add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createTopBar(HudPanel hud, JButton rewindButton, JButton previewButton) {
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setBackground(new Color(0x3C3C3C));
        topBar.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        westPanel.setOpaque(false);
        JLabel day4Label = new JLabel("DAY4");
        day4Label.setOpaque(true);
        day4Label.setBackground(new Color(0x2D2D2D));
        day4Label.setForeground(Color.WHITE);
        day4Label.setBorder(new EmptyBorder(5, 15, 5, 15));
        westPanel.add(day4Label);
        JButton shopButton = new JButton("Shop");
        shopButton.setForeground(Color.WHITE);
        shopButton.setBackground(new Color(0x5A5A5A));
        shopButton.addActionListener(e -> {
            canvas.getGameLoop().stop();
            new ShopDialog(this, canvas).setVisible(true);
            if (!canvas.isGameOver()) {
                canvas.getGameLoop().start();
            }
        });
        westPanel.add(shopButton);
        rewindButton.setForeground(Color.WHITE);
        rewindButton.setBackground(new Color(0x007ACC));
        westPanel.add(rewindButton);
        previewButton.setForeground(Color.WHITE);
        previewButton.setBackground(new Color(0x28a745));
        westPanel.add(previewButton);
        topBar.add(westPanel, BorderLayout.WEST);

        JPanel timeControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        timeControlPanel.setOpaque(false);

        topBar.add(timeControlPanel, BorderLayout.CENTER);

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        eastPanel.setOpaque(false);
        JButton infoButton = new JButton(" i ");
        infoButton.setForeground(Color.WHITE);
        infoButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        infoButton.addActionListener(e -> hud.setVisible(!hud.isVisible()));
        eastPanel.add(infoButton);
        JButton menuButton = new JButton("Main Menu");
        menuButton.setForeground(Color.WHITE);
        menuButton.addActionListener(e -> {
            this.dispose();
            new MainMenu().setVisible(true);
        });
        eastPanel.add(menuButton);
        topBar.add(eastPanel, BorderLayout.EAST);

        return topBar;
    }

    private JPanel createBottomBar(JButton exitButton, JSlider slider) {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        slider.setValue(0);
        bottomPanel.add(slider, BorderLayout.CENTER);
        bottomPanel.add(exitButton, BorderLayout.EAST);
        bottomPanel.setVisible(false);
        return bottomPanel;
    }
}