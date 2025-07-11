package view;

import model.SoundManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

class SettingsDialog extends JDialog {
    public SettingsDialog(JFrame parent) {
        super(parent, "Settings", true);
        setSize(350, 200);
        setLocationRelativeTo(parent);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel titleLabel = new JLabel("Sound Volume", SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);
        JSlider volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 75);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                SoundManager.getInstance().setVolume(source.getValue());
            }
        });
        panel.add(volumeSlider, BorderLayout.CENTER);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton, BorderLayout.SOUTH);
        add(panel);
    }
}
