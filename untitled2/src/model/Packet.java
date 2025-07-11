package model;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

public class Packet {
    public static final int RADIUS = 8;
    private final Connection connection;
    private final PacketType type;
    private final double speed;
    private double progress = 0.0;
    private Point2D.Double position;
    private GeneralPath shape;
    private double health;
    private boolean isPhased = false;
    private long phasedEndTime = 0;

    public Packet(Connection connection, PacketType type, long congestion) {
        this(connection, type, congestion, (type == PacketType.TYPE_1_DIAMOND) ? 3 : 2);
    }

    public Packet(Connection connection, PacketType type, long congestion, double startingHealth) {
        this.connection = connection;
        this.type = type;
        this.position = connection.getStartPoint();
        this.health = startingHealth;
        this.speed = (type == PacketType.TYPE_1_DIAMOND) ? 0.006 / (1 + congestion) : 0.01;
        createShape();
    }

    public void resetHealth() {
        this.health = (this.type == PacketType.TYPE_1_DIAMOND) ? 3 : 2;
    }

    public void phase() {
        this.isPhased = true;
        this.phasedEndTime = System.currentTimeMillis() + 1500;
    }

    public boolean isPhased() {
        return isPhased;
    }

    public void takeDamage(double amount) {
        this.health -= amount;
    }

    public boolean isDestroyed() {
        return this.health <= 0;
    }

    public double getHealth() {
        return this.health;
    }

    private void createShape() {
        shape = new GeneralPath();
        switch (type) {
            case TYPE_1_DIAMOND:
                shape.moveTo(0, -RADIUS);
                shape.lineTo(RADIUS, 0);
                shape.lineTo(0, RADIUS);
                shape.lineTo(-RADIUS, 0);
                shape.closePath();
                break;
            case TYPE_2_TRIANGLE:
                shape.moveTo(0, -RADIUS);
                shape.lineTo(RADIUS, RADIUS);
                shape.lineTo(-RADIUS, RADIUS);
                shape.closePath();
                break;
        }
    }

    public void update() {
        if (progress < 1.0) {
            progress += speed;
            this.position = connection.getPointAt(progress);
        }
        if (isPhased && System.currentTimeMillis() > phasedEndTime) {
            isPhased = false;
        }
    }

    public void draw(Graphics2D g2d) {
        Graphics2D g2 = (Graphics2D) g2d.create();
        g2.translate(position.x, position.y);

        Color baseColor = (type == PacketType.TYPE_1_DIAMOND ? Color.GREEN : Color.YELLOW);

        if (isPhased) {
            g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 128)); // 50% transparent
        } else {
            g2.setColor(baseColor);
        }

        g2.fill(shape);
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString(String.format("%.1f", health), -5, 4);
        g2.dispose();
    }

    public int getReward() {
        return type == PacketType.TYPE_1_DIAMOND ? 1 : 2;
    }

    public Connection getConnection() {
        return connection;
    }

    public PacketType getPacketType() {
        return type;
    }

    public Point2D.Double getPosition() {
        return position;
    }

    public boolean isFinished() {
        return progress >= 1.0;
    }

    public enum PacketType {TYPE_1_DIAMOND, TYPE_2_TRIANGLE}
}
