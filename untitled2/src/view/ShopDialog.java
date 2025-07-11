package view;

import controller.NodeCanvasPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class ShopDialog extends JDialog {
    private final NodeCanvasPanel canvas;

    public ShopDialog(JFrame parent, NodeCanvasPanel canvas) {
        super(parent, "Shop", true);
        this.canvas = canvas;
        setTitle("Shop");
        setSize(400, 300);
        setLocationRelativeTo(parent);
        JPanel panel = new JPanel(new GridLayout(4, 1, 10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.add(new JLabel("Spend Coins to Activate Abilities:", SwingConstants.CENTER));
        JButton atarButton = new JButton("Atar (Cost: 3) - Disable Impact for 10s");
        atarButton.addActionListener(e -> purchaseAbility("Atar", 3));
        panel.add(atarButton);
        JButton airyamanButton = new JButton("Airyaman (Cost: 4) - Disable Collisions for 5s");
        airyamanButton.addActionListener(e -> purchaseAbility("Airyaman", 4));
        panel.add(airyamanButton);
        JButton anahitaButton = new JButton("Anahita (Cost: 5) - Destroy all Noises");
        anahitaButton.addActionListener(e -> purchaseAbility("Anahita", 5));
        panel.add(anahitaButton);
        add(panel);
    }

    private void purchaseAbility(String abilityName, int cost) {
        if (canvas.getCoins() >= cost) {
            canvas.spendCoins(cost);
            switch (abilityName) {
                case "Atar":
                    canvas.activateAtar();
                    break;
                case "Airyaman":
                    canvas.activateAiryaman();
                    break;
                case "Anahita":
                    canvas.activateAnahita();
                    break;
            }
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Not enough coins!", "Purchase Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
