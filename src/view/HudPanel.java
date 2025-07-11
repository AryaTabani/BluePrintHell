package view;


import controller.SourceStats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HudPanel extends JPanel {
    private final Font hudFont = new Font("Monospaced", Font.BOLD, 10);
    private final Color hudColor = new Color(0, 255, 100);
    private final JLabel coinsLabel;
    private final JLabel wireLengthLabel;

    private final Map<NodeComponent, JLabel> sourceStatLabels = new HashMap<>();

    public HudPanel() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        wireLengthLabel = new JLabel();
        wireLengthLabel.setFont(hudFont);
        wireLengthLabel.setForeground(hudColor);
        add(wireLengthLabel);

        coinsLabel = new JLabel();
        coinsLabel.setFont(hudFont);
        coinsLabel.setForeground(hudColor);
        add(coinsLabel);

    }


    public void initialize(Set<NodeComponent> sources) {
        sourceStatLabels.values().forEach(this::remove);
        sourceStatLabels.clear();

        int sourceId = 1;
        for (NodeComponent source : sources) {
            JLabel statLabel = new JLabel();
            statLabel.setFont(hudFont);
            statLabel.setForeground(hudColor);
            add(statLabel);
            sourceStatLabels.put(source, statLabel);

            updateSourceStats(source, new SourceStats(), sourceId++);
        }
        revalidate();
        repaint();
    }

    public void updateSourceStats(NodeComponent source, SourceStats stats, int sourceId) {
        JLabel label = sourceStatLabels.get(source);
        if (label != null) {
            label.setText(String.format("Source %d: [Safe: %d | Lost: %d]",
                    sourceId, stats.safePackets, stats.packetLoss));
        }
    }

    public void updateWireLength(double remaining) {
        wireLengthLabel.setText(String.format("Remaining Wire: %.0fm", remaining));
    }

    public void updateCoins(int count) {
        coinsLabel.setText("Coins: " + count);
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}