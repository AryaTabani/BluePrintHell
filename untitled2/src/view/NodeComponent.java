package view;

import model.Packet;
import model.Port;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class NodeComponent extends JPanel {
    public static final int WIDTH = 120, HEIGHT = 80;
    private static final int STORAGE_CAPACITY = 5;
    private final List<Packet> storedPackets = new ArrayList<>();
    private final List<Port> inputPorts = new ArrayList<>();
    private final List<Port> outputPorts = new ArrayList<>();
    private final Role role;
    private boolean isFullyConnected = false;
    private long lastPacketSpawnTime = 0;


    public enum FlowDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }
    private final FlowDirection direction;

    public NodeComponent(int x, int y, Role role, FlowDirection direction, List<Packet.PacketType> outputTypes) {
        this.role = role;

        this.direction = (role == Role.STANDARD) ? direction : FlowDirection.LEFT_TO_RIGHT;

        setBounds(x, y, WIDTH, HEIGHT);
        setLayout(null);
        setOpaque(false);
        createPorts(outputTypes);
    }

    public long getLastPacketSpawnTime() { return lastPacketSpawnTime; }
    public void setLastPacketSpawnTime(long time) { this.lastPacketSpawnTime = time; }
    public void setFullyConnected(boolean isFullyConnected) { this.isFullyConnected = isFullyConnected; }
    public boolean hasSpace() { return storedPackets.size() < STORAGE_CAPACITY; }
    public void addToStorage(Packet packet) { if (hasSpace()) storedPackets.add(packet); }
    public void releasePacket(Packet packet) { storedPackets.remove(packet); }
    public List<Packet> getStoredPackets() { return storedPackets; }
    public void clearStorage() { storedPackets.clear(); }
    public List<Port> getInputPorts() { return inputPorts; }
    public List<Port> getOutputPorts() { return outputPorts; }


    private void createPorts(List<Packet.PacketType> outputTypes) {
        inputPorts.clear();
        outputPorts.clear();

        switch (role) {
            case STANDARD:
                if (direction == FlowDirection.LEFT_TO_RIGHT) {
                    addInputs(new Point(0, HEIGHT / 2));
                    addOutputs(outputTypes, new Point(WIDTH, 0));
                } else {
                    addOutputs(outputTypes, new Point(0, 0));
                    addInputs(new Point(WIDTH, HEIGHT / 2));
                }
                break;
            case SOURCE:
                addOutputs(outputTypes, new Point(WIDTH, 0));
                break;
            case DESTINATION:
                addInputs(new Point(0, HEIGHT / 2));
                break;
        }
    }


    private void addOutputs(List<Packet.PacketType> outputTypes, Point basePosition) {
        int totalOutputs = outputTypes.size();
        for (int i = 0; i < totalOutputs; i++) {
            int yPos = (HEIGHT / (totalOutputs + 1)) * (i + 1);
            outputPorts.add(new Port(this, outputTypes.get(i), false, new Point(basePosition.x, yPos)));
        }
    }


    private void addInputs(Point basePosition) {
        inputPorts.add(new Port(this, Packet.PacketType.TYPE_1_DIAMOND, true, new Point(basePosition.x, HEIGHT / 2 - 15)));
        inputPorts.add(new Port(this, Packet.PacketType.TYPE_2_TRIANGLE, true, new Point(basePosition.x, HEIGHT / 2 + 15)));
    }


    public Port getPortAt(Point p) {
        int portHitboxSize = 12;
        for (Port port : outputPorts) {
            Rectangle bounds = new Rectangle(port.getRelativePosition().x - portHitboxSize / 2, port.getRelativePosition().y - portHitboxSize / 2, portHitboxSize, portHitboxSize);
            if (bounds.contains(p)) return port;
        }
        for (Port port : inputPorts) {
            Rectangle bounds = new Rectangle(port.getRelativePosition().x - portHitboxSize / 2, port.getRelativePosition().y - portHitboxSize / 2, portHitboxSize, portHitboxSize);
            if (bounds.contains(p)) return port;
        }
        return null;
    }

    public Role getRole() { return role; }
    public Point getCenterPoint() { return new Point(getX() + WIDTH / 2, getY() + HEIGHT / 2); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(0x3C3C3C));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        if (role == Role.SOURCE || role == Role.STANDARD) {
            if (isFullyConnected) {
                g2d.setColor(Color.GREEN);
            } else {
                g2d.setColor(Color.RED);
            }
            g2d.fillOval(5, 5, 10, 10);
        }

        if (role == Role.SOURCE) {
            g2d.setColor(Color.ORANGE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2d.drawString("SOURCE", 35, 20);
        } else if (role == Role.DESTINATION) {
            g2d.setColor(Color.ORANGE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2d.drawString("DESTINATION", 25, 20);
        } else if (role == Role.STANDARD) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 12));
            String storageText = String.format("[%d/%d]", storedPackets.size(), STORAGE_CAPACITY);
            g2d.drawString(storageText, 35, 20);
        }

        for (Port port : outputPorts) drawPort(g2d, port);
        for (Port port : inputPorts) drawPort(g2d, port);
    }


    private void drawPort(Graphics2D g2d, Port port) {
        int radius = 6;
        Point p = port.getRelativePosition();
        if (port.getType() == Packet.PacketType.TYPE_1_DIAMOND) {
            g2d.setColor(Color.GREEN);
            Polygon diamond = new Polygon();
            diamond.addPoint(p.x, p.y - radius);
            diamond.addPoint(p.x + radius, p.y);
            diamond.addPoint(p.x, p.y + radius);
            diamond.addPoint(p.x - radius, p.y);
            g2d.fill(diamond);
        } else {
            g2d.setColor(Color.YELLOW);
            Polygon triangle = new Polygon();

            int side = (p.x == 0) ? 1 : -1;
            int direction = port.isInput() ? side : -side;
            triangle.addPoint(p.x + (direction * radius), p.y);
            triangle.addPoint(p.x - (direction * radius / 2), p.y - radius);
            triangle.addPoint(p.x - (direction * radius / 2), p.y + radius);
            g2d.fill(triangle);
        }
    }

    public enum Role {STANDARD, SOURCE, DESTINATION}
}