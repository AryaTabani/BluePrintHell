package model;

import view.NodeComponent;

import java.awt.*;
import java.awt.geom.Point2D;

public class Connection {
    private final Port startPort;
    private final Port endPort;
    private final Color color;

    public Connection(Port start, Port end, Color c) {
        this.startPort = start;
        this.endPort = end;
        this.color = c;
    }

    public Port getStartPort() {
        return startPort;
    }

    public Port getEndPort() {
        return endPort;
    }

    public NodeComponent getStartNode() {
        return startPort.getParentNode();
    }

    public NodeComponent getEndNode() {
        return endPort.getParentNode();
    }

    public Point2D.Double getStartPoint() {
        return new Point2D.Double(startPort.getAbsolutePosition().x, startPort.getAbsolutePosition().y);
    }

    public Point2D.Double getEndPoint() {
        return new Point2D.Double(endPort.getAbsolutePosition().x, endPort.getAbsolutePosition().y);
    }

    public double getLength() {
        return getStartPoint().distance(getEndPoint());
    }

    public Point2D.Double getPointAt(double progress) {
        Point2D.Double start = getStartPoint();
        Point2D.Double end = getEndPoint();
        return new Point2D.Double(start.x + (end.x - start.x) * progress, start.y + (end.y - start.y) * progress);
    }

    public void draw(Graphics2D g2d) {
        Point2D.Double p1 = getStartPoint();
        Point2D.Double p2 = getEndPoint();
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
    }
}
