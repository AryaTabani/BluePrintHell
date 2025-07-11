package controller;


public class SourceStats {
    public int safePackets = 0;
    public int packetLoss = 0;

    SourceStats(SourceStats other) {
        this.safePackets = other.safePackets;
        this.packetLoss = other.packetLoss;
    }

    public SourceStats() {
    }

    void incrementSafe() {
        safePackets++;
    }

    void incrementLoss() {
        packetLoss++;
    }
}
