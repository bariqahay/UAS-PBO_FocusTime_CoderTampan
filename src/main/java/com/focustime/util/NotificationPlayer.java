package com.focustime.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.awt.*;
import java.net.URL;

public class NotificationPlayer {
    private MediaPlayer player;

    public NotificationPlayer(String audioPath) {
        try {
            URL soundURL = getClass().getResource(audioPath);
            if (soundURL != null) {
                Media media = new Media(soundURL.toString());
                player = new MediaPlayer(media);
                player.setVolume(0.7);
            }
        } catch (Exception e) {
            System.out.println("Gagal load audio: " + e.getMessage());
        }
    }

    public void play() {
        try {
            if (player != null) {
                player.stop();
                player.play();
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
