package controller;

import model.*;
import view.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class NodeCanvasPanel extends JPanel {
    private static final int GAME_OVER_LOSS_COUNT = 50;
    private static final double MAX_WIRE_LENGTH = 3000.0;
    private static final long PACKET_SPAWN_INTERVAL_MS = 1000;
    private static final int MAX_SPAWNED_PACKETS = 30;
    // private final Random random = new Random(12345L);
    // private  Random random2 = new Random(12345L);
    private static final long RANDOM_SEED = 12345L;
    private static final int MAX_LEVEL = 2;
    private final HudPanel hud;
    private final JFrame parentFrame;
    private final Timer gameLoop;
    private final JButton rewindButton;
    private final List<PacketGhost> ghostTimeline = new ArrayList<>();
    private final JSlider timeSlider;
    private final JButton previewButton;
    private final JButton exitPreviewButton;
    private final List<HudState> hudTimeline = new ArrayList<>();
    private Random random = new Random(RANDOM_SEED);
    private int levelNumber;
    private List<NodeComponent> nodes;
    private List<Connection> connections;
    private List<Packet> packets;
    private double currentWireLength = 0;
    private int packetLossCount = 0;
    private int safePacketCount = 0;
    private int coins = 0;
    private boolean isGameOver = false;
    private boolean isCollisionDisabled = false;
    private long collisionDisableEndTime = 0;
    private UserAction currentAction = UserAction.NONE;
    private Point wireStartPoint, wireEndPoint, dragOffset;
    private NodeComponent startNode, draggedNode;
    private Port startPort;
    private GameMode currentMode = GameMode.BUILDING;
    private int previewTick = 0;
    private boolean isGlobalDamageDisabled = false;
    private long globalDamageDisableEndTime = 0;
    private int packetsSpawnedCount = 0;

    public NodeCanvasPanel(HudPanel hud, JFrame parentFrame, JButton rewindButton, int levelNumber,
                           JButton previewButton, JButton exitPreviewButton, JSlider timeSlider) {
        this.hud = hud;
        this.parentFrame = parentFrame;
        this.rewindButton = rewindButton;
        this.levelNumber = levelNumber;
        this.previewButton = previewButton;
        this.exitPreviewButton = exitPreviewButton;
        this.timeSlider = timeSlider;
        this.rewindButton.addActionListener(e -> rewindSimulation());
        this.previewButton.addActionListener(e -> enterPreviewMode());
        this.exitPreviewButton.addActionListener(e -> exitPreviewMode());
        this.timeSlider.addChangeListener(e -> {
            if (currentMode == GameMode.PREVIEW) {
                this.previewTick = timeSlider.getValue();
                if (previewTick < hudTimeline.size()) {
                    HudState state = hudTimeline.get(previewTick);
                    hud.updateSafePackets(state.safePacketCount);
                    hud.updatePacketLoss(state.packetLossCount);
                }
                repaint();
            }
        });
        setBackground(new Color(0x252526));
        setLayout(null);
        addMouseAndKeyListeners();
        gameLoop = new Timer(16, e -> updateGame());
        gameLoop.start();
        resetLevel();
    }

    private void enterPreviewMode() {
        this.random = new Random(RANDOM_SEED);
        currentMode = GameMode.SIMULATING;
        gameLoop.stop();
        timeSlider.getParent().setVisible(true);
        previewButton.setEnabled(false);
        rewindButton.setEnabled(false);
        timeSlider.setEnabled(false);
        exitPreviewButton.setEnabled(false);
        //this.random2 = random;
        new SimulationWorker().execute();
        repaint();
    }

    private void handleWinCondition() {
        isGameOver = true;
        gameLoop.stop();
        rewindButton.setEnabled(false);

        int nextLevel = levelNumber + 1;
        boolean hasNextLevel = nextLevel <= MAX_LEVEL;

        String message = "Level " + levelNumber + " Complete!";
        JOptionPane.showMessageDialog(parentFrame, message, "Success!", JOptionPane.INFORMATION_MESSAGE);

        if (hasNextLevel) {
            ProgressManager.saveProgress(nextLevel);

            this.levelNumber = nextLevel;
            resetLevel();
        } else {
            JOptionPane.showMessageDialog(parentFrame, "Congratulations! You have beaten all available levels.", "Game Complete!", JOptionPane.INFORMATION_MESSAGE);


            parentFrame.dispose();
            new MainMenu().setVisible(true);
        }
    }

    private void exitPreviewMode() {
        currentMode = GameMode.BUILDING;
        timeSlider.getParent().setVisible(false);
        previewButton.setEnabled(true);
        rewindButton.setEnabled(true);
        ghostTimeline.clear();
        hudTimeline.clear();

        hud.updateSafePackets(this.safePacketCount);
        hud.updatePacketLoss(this.packetLossCount);
        hud.updateCoins(this.coins);

        if (!isGameOver) {
            gameLoop.start();
        }
        repaint();
    }

    public Timer getGameLoop() {
        return gameLoop;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public int getCoins() {
        return coins;
    }

    public void spendCoins(int amount) {
        this.coins -= amount;
        hud.updateCoins(this.coins);
    }

    public void activateAtar() {
        System.out.println("ABILITY: Atar activated (Global damage disabled for 10s).");
        isGlobalDamageDisabled = true;
        globalDamageDisableEndTime = System.currentTimeMillis() + 10000;
    }

    public void activateAiryaman() {
        System.out.println("ABILITY: Airyaman activated (Collisions disabled & health restored).");
        isCollisionDisabled = true;
        collisionDisableEndTime = System.currentTimeMillis() + 5000;

        for (Packet packet : packets) {
            packet.resetHealth();
        }
        for (NodeComponent node : nodes) {
            if (node.getRole() == NodeComponent.Role.STANDARD) {
                for (Packet storedPacket : node.getStoredPackets()) {
                    storedPacket.resetHealth();
                }
            }
        }
    }

    public void activateAnahita() {


        for (Packet packet : packets) {
            packet.resetHealth();
        }

        for (NodeComponent node : nodes) {
            if (node.getRole() == NodeComponent.Role.STANDARD) {
                for (Packet storedPacket : node.getStoredPackets()) {
                    storedPacket.resetHealth();
                }
            }
        }
    }

    public final void resetLevel() {
        if (nodes == null) nodes = new ArrayList<>();
        if (connections == null) connections = new ArrayList<>();
        if (packets == null) packets = new ArrayList<>();
        nodes.clear();
        connections.clear();
        packets.clear();
        removeAll();
        createInitialNodes(this.levelNumber);
        currentWireLength = 0;
        packetLossCount = 0;
        safePacketCount = 0;
        coins = 50;
        packetsSpawnedCount = 0;
        isGameOver = false;
        hud.updateWireLength(MAX_WIRE_LENGTH);
        hud.updatePacketLoss(packetLossCount);
        hud.updateCoins(coins);
        hud.updateSafePackets(safePacketCount);
        if (gameLoop != null && !gameLoop.isRunning()) gameLoop.start();
        repaint();
    }

    private void rewindSimulation() {
        packets.clear();
        packetLossCount = 0;
        safePacketCount = 0;
        coins = 50;
        packetsSpawnedCount = 0;
        hud.updatePacketLoss(packetLossCount);
        for (NodeComponent node : nodes) {
            node.clearStorage();
        }
        hud.updateCoins(coins);
        hud.updateSafePackets(safePacketCount);
        if (gameLoop != null && !gameLoop.isRunning()) gameLoop.start();
    }

    private boolean isConnectionOccupied(Connection connection) {
        for (Packet packet : packets) {
            if (packet.getConnection() == connection) {
                return true;
            }
        }
        return false;
    }


    private void createInitialNodes(int levelNumber) {
        add(new JLabel("Network Systems") {{
            setForeground(Color.LIGHT_GRAY);
            setBounds(20, 15, 150, 20);
        }});

        generateNodesForLevel(levelNumber);

        for (NodeComponent node : nodes) {
            add(node);
        }
    }


    private void generateNodesForLevel(int levelNumber) {
        add(new JLabel("Network Systems") {{
            setForeground(Color.LIGHT_GRAY);
            setBounds(20, 15, 150, 20);
        }});

        if (this.levelNumber == 1) {

            Point[] sourcePositions = {
                    new Point(50, 150)
            };


            for (Point pos : sourcePositions) {
                nodes.add(new NodeComponent(pos.x, pos.y, NodeComponent.Role.SOURCE, null,
                        List.of(Packet.PacketType.TYPE_1_DIAMOND, Packet.PacketType.TYPE_2_TRIANGLE)));

                nodes.add(new NodeComponent(pos.x - 20, pos.y, NodeComponent.Role.DESTINATION, null, List.of()));
            }

            nodes.add(new NodeComponent(300, 150, NodeComponent.Role.STANDARD,
                    NodeComponent.FlowDirection.LEFT_TO_RIGHT,
                    List.of(Packet.PacketType.TYPE_1_DIAMOND, Packet.PacketType.TYPE_2_TRIANGLE)));
            nodes.add(new NodeComponent(550, 400, NodeComponent.Role.STANDARD,
                    NodeComponent.FlowDirection.RIGHT_TO_LEFT,
                    List.of(Packet.PacketType.TYPE_1_DIAMOND, Packet.PacketType.TYPE_2_TRIANGLE)));

        } else if (this.levelNumber == 2) {

            Point[] sourcePositions = {
                    new Point(50, 50)
            };


            for (Point pos : sourcePositions) {
                nodes.add(new NodeComponent(pos.x, pos.y, NodeComponent.Role.SOURCE, null,
                        List.of(Packet.PacketType.TYPE_1_DIAMOND, Packet.PacketType.TYPE_2_TRIANGLE)));

                nodes.add(new NodeComponent(pos.x - 20, pos.y, NodeComponent.Role.DESTINATION, null, List.of()));
            }

            // 3. Add your Standard nodes
            nodes.add(new NodeComponent(250, 350, NodeComponent.Role.STANDARD,
                    NodeComponent.FlowDirection.LEFT_TO_RIGHT,
                    List.of(Packet.PacketType.TYPE_1_DIAMOND, Packet.PacketType.TYPE_2_TRIANGLE)));
            nodes.add(new NodeComponent(450, 200, NodeComponent.Role.STANDARD,
                    NodeComponent.FlowDirection.RIGHT_TO_LEFT,
                    List.of(Packet.PacketType.TYPE_1_DIAMOND, Packet.PacketType.TYPE_2_TRIANGLE)));
            nodes.add(new NodeComponent(650, 500, NodeComponent.Role.STANDARD,
                    NodeComponent.FlowDirection.LEFT_TO_RIGHT,
                    List.of(Packet.PacketType.TYPE_1_DIAMOND, Packet.PacketType.TYPE_2_TRIANGLE)));
        }

        // Add all created nodes to the panel
        for (NodeComponent node : nodes) {
            add(node);
        }
    }

    private void handleGameOver(String message) {
        isGameOver = true;
        gameLoop.stop();
        rewindButton.setEnabled(false);
        Object[] options = {"Restart Level"};
        JOptionPane.showOptionDialog(parentFrame,
                message,
                "Game Over",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        resetLevel();
    }

    private void updateGame() {
        if (isGameOver) return;

        rewindButton.setEnabled(packetLossCount < GAME_OVER_LOSS_COUNT);
        if (packetLossCount >= GAME_OVER_LOSS_COUNT) {
            handleGameOver("Packet Loss reached " + packetLossCount + "!");
            return;
        }

        if (isCollisionDisabled && System.currentTimeMillis() > collisionDisableEndTime) {
            isCollisionDisabled = false;
        }
        if (isGlobalDamageDisabled && System.currentTimeMillis() > globalDamageDisableEndTime) {
            isGlobalDamageDisabled = false;
        }


        boolean allPeripheralsConnected = true;
        for (NodeComponent node : nodes) {
            if (node.getRole() == NodeComponent.Role.SOURCE || node.getRole() == NodeComponent.Role.DESTINATION) {
                boolean isThisPeripheralConnected = true;
                for (Port port : node.getOutputPorts()) {
                    if (connections.stream().noneMatch(c -> c.getStartPort() == port)) {
                        isThisPeripheralConnected = false;
                        break;
                    }
                }
                if (isThisPeripheralConnected) {
                    for (Port port : node.getInputPorts()) {
                        if (connections.stream().noneMatch(c -> c.getEndPort() == port)) {
                            isThisPeripheralConnected = false;
                            break;
                        }
                    }
                }
                if (!isThisPeripheralConnected) {
                    allPeripheralsConnected = false;
                    break;
                }
            }
        }

        for (NodeComponent node : nodes) {
            if (node.getRole() == NodeComponent.Role.STANDARD) {
                boolean isThisNodeFullyConnected = true;
                for (Port port : node.getOutputPorts()) {
                    if (connections.stream().noneMatch(c -> c.getStartPort() == port)) {
                        isThisNodeFullyConnected = false;
                        break;
                    }
                }
                if (isThisNodeFullyConnected) {
                    for (Port port : node.getInputPorts()) {
                        if (connections.stream().noneMatch(c -> c.getEndPort() == port)) {
                            isThisNodeFullyConnected = false;
                            break;
                        }
                    }
                }
                node.setFullyConnected(isThisNodeFullyConnected);
            } else if (node.getRole() == NodeComponent.Role.SOURCE) {
                node.setFullyConnected(allPeripheralsConnected);
            } else {
                node.setFullyConnected(false);
            }
        }


        List<Packet> newPackets = new ArrayList<>();


        if (allPeripheralsConnected && packetsSpawnedCount < MAX_SPAWNED_PACKETS) {
            long currentTime = System.currentTimeMillis();
            for (NodeComponent node : nodes) {
                if (node.getRole() == NodeComponent.Role.SOURCE) {
                    if (currentTime - node.getLastPacketSpawnTime() > PACKET_SPAWN_INTERVAL_MS) {
                        Packet.PacketType shapeToSpawn = random.nextBoolean() ? Packet.PacketType.TYPE_1_DIAMOND : Packet.PacketType.TYPE_2_TRIANGLE;
                        List<Connection> validConns = connections.stream()
                                .filter(c -> c.getStartNode() == node && c.getStartPort().getType() == shapeToSpawn)
                                .collect(Collectors.toList());

                        if (!validConns.isEmpty()) {
                            Connection randomConn = validConns.get(random.nextInt(validConns.size()));
                            if (!isConnectionOccupied(randomConn)) {
                                newPackets.add(new Packet(randomConn, shapeToSpawn, 0));
                                node.setLastPacketSpawnTime(currentTime);
                                packetsSpawnedCount++;
                            }
                        }
                    }
                }
            }
        }

        Iterator<Packet> packetIterator = packets.iterator();
        while (packetIterator.hasNext()) {
            Packet p = packetIterator.next();
            p.update();
            if (p.isFinished()) {
                packetIterator.remove();
                NodeComponent endNode = p.getConnection().getEndNode();
                if (endNode.getRole() == NodeComponent.Role.DESTINATION) {

                    safePacketCount++;
                    hud.updateSafePackets(safePacketCount);
                    coins += p.getReward();
                    hud.updateCoins(coins);
                    SoundManager.getInstance().playSound(SoundManager.SoundType.PACKET_DELIVERED);

                } else if (endNode.getRole() == NodeComponent.Role.STANDARD) {
                    if (endNode.hasSpace()) {
                        endNode.addToStorage(p);
                    } else {
                        packetLossCount++;
                        hud.updatePacketLoss(packetLossCount);
                    }
                }
            }
        }

        for (NodeComponent node : nodes) {
            if (node.getRole() == NodeComponent.Role.STANDARD && !node.getStoredPackets().isEmpty()) {
                for (Packet storedPacket : new ArrayList<>(node.getStoredPackets())) {
                    List<Connection> outgoing = connections.stream()
                            .filter(c -> c.getStartNode() == node && c.getStartPort().getType() == storedPacket.getPacketType())
                            .collect(Collectors.toList());
                    Connection availableConn = outgoing.stream().filter(c -> !isConnectionOccupied(c)).findFirst().orElse(null);
                    if (availableConn != null) {
                        newPackets.add(new Packet(availableConn, storedPacket.getPacketType(), 0, storedPacket.getHealth()));
                        node.releasePacket(storedPacket);
                        break;
                    }
                }
            }
        }

        packets.addAll(newPackets);

        if (!isCollisionDisabled) {
            boolean collisionOccurred = false;
            for (int i = 0; i < packets.size(); i++) {
                for (int j = i + 1; j < packets.size(); j++) {
                    Packet p1 = packets.get(i);
                    Packet p2 = packets.get(j);
                    if (!p1.isPhased() && !p2.isPhased() && p1.getPosition().distance(p2.getPosition()) < Packet.RADIUS * 2) {
                        p1.phase();
                        p2.phase();
                        collisionOccurred = true;
                    }
                }
            }
            if (collisionOccurred) {
                SoundManager.getInstance().playSound(SoundManager.SoundType.PACKET_COLLISION);
                if (!isGlobalDamageDisabled) {
                    for (Packet p : packets) {
                        p.takeDamage(0.2);
                    }
                    for (NodeComponent node : nodes) {
                        for (Packet storedPacket : node.getStoredPackets()) {
                            storedPacket.takeDamage(0.2);
                        }
                    }
                }
            }
            List<Packet> destroyedPackets = new ArrayList<>();
            packets.stream().filter(Packet::isDestroyed).forEach(destroyedPackets::add);
            for (NodeComponent node : nodes) {
                node.getStoredPackets().stream().filter(Packet::isDestroyed).forEach(destroyedPackets::add);
            }
            if (!destroyedPackets.isEmpty()) {
                packets.removeAll(destroyedPackets);
                for (NodeComponent node : nodes) {
                    node.getStoredPackets().removeAll(destroyedPackets);
                }
                packetLossCount += destroyedPackets.size();
                hud.updatePacketLoss(packetLossCount);
            }
        }

        boolean allPacketsInStorageAreGone = nodes.stream()
                .filter(n -> n.getRole() == NodeComponent.Role.STANDARD)
                .allMatch(n -> n.getStoredPackets().isEmpty());
        if (packetsSpawnedCount >= MAX_SPAWNED_PACKETS && packets.isEmpty() && allPacketsInStorageAreGone) {
            if (packetLossCount < MAX_SPAWNED_PACKETS / 2.0) {
                handleWinCondition();
            } else {
                handleGameOver("Mission Failed: Packet loss was too high.");
            }
            return;
        }

        repaint();
    }

    private void handleRightClick(MouseEvent e) {
        Connection clickedConnection = findConnectionAt(e.getPoint());
        if (clickedConnection != null) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem deleteItem = new JMenuItem("Delete Wire");
            deleteItem.addActionListener(actionEvent -> {
                currentWireLength -= clickedConnection.getLength();
                hud.updateWireLength(MAX_WIRE_LENGTH - currentWireLength);

                connections.remove(clickedConnection);
                repaint();
            });
            popupMenu.add(deleteItem);
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private Connection findConnectionAt(Point p) {
        for (Connection conn : connections) {
            Point2D.Double p1 = conn.getStartPoint();
            Point2D.Double p2 = conn.getEndPoint();
            double distance = Line2D.ptSegDist(p1.x, p1.y, p2.x, p2.y, p.x, p.y);
            if (distance < 5) {
                return conn;
            }
        }
        return null;
    }

    private void addMouseAndKeyListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e);
                    return;
                }

                for (int i = nodes.size() - 1; i >= 0; i--) {
                    NodeComponent node = nodes.get(i);
                    if (node.getBounds().contains(e.getPoint())) {
                        Point localPoint = SwingUtilities.convertPoint(NodeCanvasPanel.this, e.getPoint(), node);
                        Port port = node.getPortAt(localPoint);

                        if (port != null && !port.isInput()) {
                            currentAction = UserAction.WIRING;
                            startPort = port;
                            wireStartPoint = port.getAbsolutePosition();
                            wireEndPoint = e.getPoint();
                        }
                        return;
                    }
                }
                currentAction = UserAction.NONE;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (currentAction == UserAction.WIRING) {
                    wireEndPoint = e.getPoint();
                    repaint();
                }
            }


            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e);
                    return;
                }

                if (currentAction == UserAction.WIRING) {
                    Port endPort = null;
                    for (NodeComponent node : nodes) {
                        if (node.getBounds().contains(e.getPoint())) {
                            Point localPoint = SwingUtilities.convertPoint(NodeCanvasPanel.this, e.getPoint(), node);
                            Port port = node.getPortAt(localPoint);
                            if (port != null && port.isInput() && port.getParentNode() != startPort.getParentNode()) {
                                endPort = port;
                                break;
                            }
                        }
                    }

                    if (endPort != null) {
                        final Port finalEndPort = endPort;

                        boolean startPortInUse = connections.stream().anyMatch(c -> c.getStartPort() == startPort);
                        boolean endPortInUse = connections.stream().anyMatch(c -> c.getEndPort() == finalEndPort);

                        if (startPortInUse) {
                            JOptionPane.showMessageDialog(parentFrame, "This output port is already connected.", "model.Connection Error", JOptionPane.WARNING_MESSAGE);
                        } else if (endPortInUse) {
                            JOptionPane.showMessageDialog(parentFrame, "This input port is already connected.", "model.Connection Error", JOptionPane.WARNING_MESSAGE);
                        } else {
                            double wireNeeded = startPort.getAbsolutePosition().distance(endPort.getAbsolutePosition());
                            if (currentWireLength + wireNeeded <= MAX_WIRE_LENGTH) {
                                Connection newConn = new Connection(startPort, endPort, Color.WHITE);
                                connections.add(newConn);
                                currentWireLength += newConn.getLength();
                                hud.updateWireLength(MAX_WIRE_LENGTH - currentWireLength);
                                SoundManager.getInstance().playSound(SoundManager.SoundType.CONNECTION_SUCCESS);
                            } else {
                                JOptionPane.showMessageDialog(parentFrame, "Not enough wire length remaining!", "Resource Limit", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    }
                }
                currentAction = UserAction.NONE;
                repaint();
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrid(g2d);
        for (Connection conn : connections) {
            conn.draw(g2d);
        }

        if (currentMode == GameMode.BUILDING) {
            for (Packet packet : packets) {
                packet.draw(g2d);
            }
        } else if (currentMode == GameMode.SIMULATING) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2d.drawString("Simulating...", getWidth() / 2 - 100, getHeight() / 2);
        } else {
            for (PacketGhost ghost : ghostTimeline) {
                if (ghost.tick == previewTick) {
                    Color baseColor = (ghost.packetType == Packet.PacketType.TYPE_1_DIAMOND ? Color.GREEN : Color.YELLOW);
                    g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 150));

                    if (ghost.packetType == Packet.PacketType.TYPE_1_DIAMOND) {
                        Polygon diamond = new Polygon();
                        diamond.addPoint((int) ghost.position.x, (int) ghost.position.y - Packet.RADIUS);
                        diamond.addPoint((int) ghost.position.x + Packet.RADIUS, (int) ghost.position.y);
                        diamond.addPoint((int) ghost.position.x, (int) ghost.position.y + Packet.RADIUS);
                        diamond.addPoint((int) ghost.position.x - Packet.RADIUS, (int) ghost.position.y);
                        g2d.fill(diamond);
                    } else {
                        Polygon triangle = new Polygon();
                        triangle.addPoint((int) ghost.position.x, (int) ghost.position.y - Packet.RADIUS);
                        triangle.addPoint((int) ghost.position.x - Packet.RADIUS, (int) ghost.position.y + Packet.RADIUS);
                        triangle.addPoint((int) ghost.position.x + Packet.RADIUS, (int) ghost.position.y + Packet.RADIUS);
                        g2d.fill(triangle);
                    }

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2d.drawString(String.format("%.1f", ghost.health), (int) ghost.position.x - 5, (int) ghost.position.y + 4);
                }
            }
        }

        if (currentAction == UserAction.WIRING) {
            g2d.setColor(Color.ORANGE);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawLine(wireStartPoint.x, wireStartPoint.y, wireEndPoint.x, wireEndPoint.y);
        }

        g2d.dispose();
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 50));
        for (int x = 0; x < getWidth(); x += 20) g2d.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 20) g2d.drawLine(0, y, getWidth(), y);
    }

    private enum GameMode {BUILDING, SIMULATING, PREVIEW}

    private enum UserAction {NONE, WIRING, DRAGGING}

    private static class HudState {
        final int tick;
        final int safePacketCount;
        final int packetLossCount;

        HudState(int tick, int safe, int loss) {
            this.tick = tick;
            this.safePacketCount = safe;
            this.packetLossCount = loss;
        }
    }

    private static class NodeState {
        private static final int STORAGE_CAPACITY = 5;
        private final List<Packet> storedPackets = new ArrayList<>();

        public boolean hasSpace() {
            return storedPackets.size() < STORAGE_CAPACITY;
        }

        public void addToStorage(Packet p) {
            storedPackets.add(p);
        }

        public void releasePacket(Packet p) {
            storedPackets.remove(p);
        }

        public List<Packet> getStoredPackets() {
            return storedPackets;
        }
    }

    private class SimulationWorker extends SwingWorker<Void, Void> {
        @Override
        protected Void doInBackground() throws Exception {
            Random random2 = new Random(RANDOM_SEED);
            ghostTimeline.clear();
            hudTimeline.clear();

            List<Packet> simPackets = new ArrayList<>();
            List<NodeState> simNodeStates = new ArrayList<>();
            nodes.forEach(n -> simNodeStates.add(new NodeState()));
            long[] lastSpawnTimes = new long[nodes.size()];
            int simSafeCount = 0;
            int simLossCount = 0;
            int simPacketsSpawned = 0;
            int tick = 0;
            while (tick < 10000) {
                boolean simIsFinished = simPacketsSpawned >= MAX_SPAWNED_PACKETS &&
                        simPackets.isEmpty() &&
                        simNodeStates.stream().allMatch(s -> s.getStoredPackets().isEmpty());
                if (simIsFinished) {
                    break;
                }
                List<Packet> newSimPackets = new ArrayList<>();
                long currentSimTime = tick * 16L;

                if (simPacketsSpawned < MAX_SPAWNED_PACKETS) {
                    for (int i = 0; i < nodes.size(); i++) {
                        NodeComponent node = nodes.get(i);
                        if (node.getRole() == NodeComponent.Role.SOURCE && (currentSimTime - lastSpawnTimes[i]) > PACKET_SPAWN_INTERVAL_MS) {
                            Packet.PacketType shapeToSpawn = random2.nextBoolean() ? Packet.PacketType.TYPE_1_DIAMOND : Packet.PacketType.TYPE_2_TRIANGLE;
                            List<Connection> validConns = connections.stream().filter(c -> c.getStartNode() == node && c.getStartPort().getType() == shapeToSpawn).collect(Collectors.toList());
                            if (!validConns.isEmpty()) {
                                Connection randomConn = validConns.get(random2.nextInt(validConns.size()));
                                if (simPackets.stream().noneMatch(p -> p.getConnection() == randomConn)) {
                                    newSimPackets.add(new Packet(randomConn, shapeToSpawn, 0));
                                    lastSpawnTimes[i] = currentSimTime;
                                    simPacketsSpawned++;
                                }
                            }
                        }
                    }
                }

                Iterator<Packet> simIterator = simPackets.iterator();
                while (simIterator.hasNext()) {
                    Packet p = simIterator.next();
                    p.update();
                    if (p.isFinished()) {
                        simIterator.remove();
                        NodeComponent endNode = p.getConnection().getEndNode();
                        int nodeIndex = nodes.indexOf(endNode);
                        if (nodeIndex != -1) {
                            NodeState endNodeState = simNodeStates.get(nodeIndex);
                            if (endNode.getRole() == NodeComponent.Role.DESTINATION) {
                                simSafeCount++;
                            } else if (endNode.getRole() == NodeComponent.Role.STANDARD) {
                                if (endNodeState.hasSpace()) endNodeState.addToStorage(p);
                                else simLossCount++;
                            } else {
                                simLossCount++;
                            }
                        } else {
                            simLossCount++;
                        }
                    }
                }

                for (int i = 0; i < nodes.size(); i++) {
                    NodeComponent node = nodes.get(i);
                    NodeState nodeState = simNodeStates.get(i);
                    if (node.getRole() == NodeComponent.Role.STANDARD && !nodeState.getStoredPackets().isEmpty()) {
                        for (Packet storedPacket : new ArrayList<>(nodeState.getStoredPackets())) {
                            List<Connection> outgoing = connections.stream().filter(c -> c.getStartNode() == node && c.getStartPort().getType() == storedPacket.getPacketType()).collect(Collectors.toList());
                            Connection availableConn = outgoing.stream().filter(c -> simPackets.stream().noneMatch(p -> p.getConnection() == c)).findFirst().orElse(null);
                            if (availableConn != null) {
                                newSimPackets.add(new Packet(availableConn, storedPacket.getPacketType(), 0, storedPacket.getHealth()));
                                nodeState.releasePacket(storedPacket);
                                break;
                            }
                        }
                    }
                }

                simPackets.addAll(newSimPackets);

                boolean collisionOccurred = false;
                for (int i = 0; i < simPackets.size(); i++) {
                    for (int j = i + 1; j < simPackets.size(); j++) {
                        Packet p1 = simPackets.get(i);
                        Packet p2 = simPackets.get(j);
                        if (!p1.isPhased() && !p2.isPhased() && p1.getPosition().distance(p2.getPosition()) < Packet.RADIUS * 2) {
                            p1.phase();
                            p2.phase();
                            collisionOccurred = true;
                            if (collisionOccurred) {
                                p1.takeDamage(1);
                                p2.takeDamage(1);
                            }
                        }
                    }
                }
                if (collisionOccurred) {
                    for (Packet p : simPackets) {
                        p.takeDamage(0.2);
                    }
                    for (NodeState nodeState : simNodeStates) {
                        for (Packet storedPacket : nodeState.getStoredPackets()) {
                            storedPacket.takeDamage(0.2);
                        }
                    }
                }
                List<Packet> destroyedSimPackets = new ArrayList<>();
                simPackets.stream().filter(Packet::isDestroyed).forEach(destroyedSimPackets::add);
                for (NodeState nodeState : simNodeStates) {
                    nodeState.getStoredPackets().stream().filter(Packet::isDestroyed).forEach(destroyedSimPackets::add);
                }
                if (!destroyedSimPackets.isEmpty()) {
                    simPackets.removeAll(destroyedSimPackets);
                    for (NodeState nodeState : simNodeStates) {
                        nodeState.getStoredPackets().removeAll(destroyedSimPackets);
                    }
                    simLossCount += destroyedSimPackets.size();
                }

                for (Packet p : simPackets) {
                    ghostTimeline.add(new PacketGhost(tick, p.getPosition(), p.getPacketType(), p.getHealth()));
                }
                hudTimeline.add(new HudState(tick, simSafeCount, simLossCount));

                tick++;
            }
            return null;
        }

        @Override
        protected void done() {
            currentMode = GameMode.PREVIEW;
            int finalTick = hudTimeline.size();
            timeSlider.setMaximum(finalTick > 0 ? finalTick - 1 : 0);
            timeSlider.setValue(0);
            previewTick = 0;
            timeSlider.setEnabled(true);
            exitPreviewButton.setEnabled(true);
            repaint();
        }
    }


}
