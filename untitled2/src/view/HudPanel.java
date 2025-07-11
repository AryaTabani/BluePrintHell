package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HudPanel extends JPanel {
    private final JLabel packetLossLabel;
    private final JLabel coinsLabel;
    private final JLabel wireLengthLabel;
    private final JLabel safePacketsLabel;

    public HudPanel() {
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        Font hudFont = new Font("Monospaced", Font.BOLD, 14);
        Color hudColor = new Color(0, 255, 100);
        wireLengthLabel = new JLabel("Remaining Wire: 0m");
        wireLengthLabel.setFont(hudFont);
        wireLengthLabel.setForeground(hudColor);
        add(wireLengthLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        packetLossLabel = new JLabel("model.Packet Loss: 0");
        packetLossLabel.setFont(hudFont);
        packetLossLabel.setForeground(hudColor);
        add(packetLossLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        coinsLabel = new JLabel("Coins: 0");
        coinsLabel.setFont(hudFont);
        coinsLabel.setForeground(hudColor);
        add(coinsLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        safePacketsLabel = new JLabel("Safe Packets: 0");
        safePacketsLabel.setFont(hudFont);
        safePacketsLabel.setForeground(hudColor);
        add(safePacketsLabel);
    }

    public void updateWireLength(double remaining) {
        wireLengthLabel.setText(String.format("Remaining Wire: %.0fm", remaining));
    }

    public void updatePacketLoss(int count) {
        packetLossLabel.setText("model.Packet Loss: " + count);
    }

    public void updateCoins(int count) {
        coinsLabel.setText("Coins: " + count);
    }

    public void updateSafePackets(int count) {
        safePacketsLabel.setText("Safe Packets: " + count);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
