package model;

import view.NodeComponent;

import java.awt.*;

public class Port {
    private final NodeComponent parentNode;
    private final Packet.PacketType type;
    private final boolean isInput;
    private final Point relativePosition;

    public Port(NodeComponent parent, Packet.PacketType type, boolean isInput, Point relativePos) {
        this.parentNode = parent;
        this.type = type;
        this.isInput = isInput;
        this.relativePosition = relativePos;
    }

    public Packet.PacketType getType() {
        return type;
    }

    public NodeComponent getParentNode() {
        return parentNode;
    }

    public boolean isInput() {
        return isInput;
    }

    public Point getAbsolutePosition() {
        return new Point(parentNode.getX() + relativePosition.x, parentNode.getY() + relativePosition.y);
    }

    public Point getRelativePosition() {
        return relativePosition;
    }
}
