package controller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;


public class ProgressManager {
    private static final String SAVE_FILE = "save.properties";
    private static final String LEVEL_KEY = "unlocked_level";

    public static int loadProgress() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(SAVE_FILE)) {
            props.load(fis);
            return Integer.parseInt(props.getProperty(LEVEL_KEY, "1"));
        } catch (IOException | NumberFormatException e) {
            return 1;
        }
    }
    public static void saveProgress(int levelNumber) {
        Properties props = new Properties();
        props.setProperty(LEVEL_KEY, String.valueOf(levelNumber));
        try (FileOutputStream fos = new FileOutputStream(SAVE_FILE)) {
            props.store(fos, "Game Progress");
            System.out.println("Progress saved: Unlocked Level " + levelNumber);
        } catch (IOException e) {
            System.err.println("Error saving progress: " + e.getMessage());
        }
    }
}