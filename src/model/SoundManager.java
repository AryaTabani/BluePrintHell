package model;

public class SoundManager {
    private static final SoundManager instance = new SoundManager();
    private int volume = 75;

    private SoundManager() {
    }

    public static SoundManager getInstance() {
        return instance;
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    public void playSound(SoundType type) {
        if (volume > 0) {
            System.out.println("🎵 Sound Event: " + type + " (at " + volume + "% volume)");
        }
    }

    public enum SoundType {CONNECTION_SUCCESS, PACKET_COLLISION, PACKET_DELIVERED}
}
