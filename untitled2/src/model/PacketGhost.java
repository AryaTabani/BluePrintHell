package model;

import java.awt.geom.Point2D;

public class PacketGhost {
    public final int tick;
    public final Point2D.Double position;
    public final Packet.PacketType packetType;
    public final double health;

    public PacketGhost(int tick, Point2D.Double position, Packet.PacketType type, double health) {
        this.tick = tick;
        this.position = position;
        this.packetType = type;
        this.health = health;
    }
}
