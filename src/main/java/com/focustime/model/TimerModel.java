// 🔥 FIXED & COMPLETE TimerModel.java
package com.focustime.model;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;

public class TimerModel {

    private final IntegerProperty totalSeconds = new SimpleIntegerProperty(0);
    private final IntegerProperty remainingSeconds = new SimpleIntegerProperty(0);
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    private final BooleanProperty isPaused = new SimpleBooleanProperty(false);
    private final StringProperty timeDisplay = new SimpleStringProperty("00:00");

    private Task<Void> timerTask;
    private Thread timerThread;
    private Runnable onTimerFinishedCallback;

    public TimerModel() {
        remainingSeconds.addListener((obs, oldVal, newVal) -> updateTimeDisplay());
    }

    public void setDuration(int minutes) {
        setDurationSeconds(minutes * 60);
    }

    public void setDurationSeconds(int seconds) {
        if (seconds < 0) seconds = 0;
        totalSeconds.set(seconds);
        remainingSeconds.set(seconds);
        updateTimeDisplay();
    }

    public void start() {
        if (isRunning.get()) return;

        isRunning.set(true);
        isPaused.set(false);

        timerTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                while (remainingSeconds.get() > 0 && !isCancelled()) {
                    Thread.sleep(1000);
                    if (!isPaused.get()) {
                        Platform.runLater(() -> {
                            if (remainingSeconds.get() > 0) {
                                remainingSeconds.set(remainingSeconds.get() - 1);
                            }
                        });
                    }
                }

                if (!isCancelled() && remainingSeconds.get() == 0) {
                    Platform.runLater(() -> {
                        isRunning.set(false);
                        if (onTimerFinishedCallback != null) {
                            onTimerFinishedCallback.run();
                        }
                    });
                }
                return null;
            }
        };

        timerThread = new Thread(timerTask);
        timerThread.setDaemon(true);
        timerThread.start();
    }

    public void pause() {
        if (isRunning.get()) {
            isPaused.set(true);
        }
    }

    public void resume() {
        if (isRunning.get()) {
            isPaused.set(false);
        }
    }

    public void stop() {
        if (timerTask != null && timerTask.isRunning()) {
            timerTask.cancel();
        }
        isRunning.set(false);
        isPaused.set(false);
        remainingSeconds.set(totalSeconds.get());
    }

    public void reset() {
        stop();
        updateTimeDisplay();
    }

    private void updateTimeDisplay() {
        int seconds = remainingSeconds.get();
        int minutes = seconds / 60;
        int secs = seconds % 60;
        timeDisplay.set(String.format("%02d:%02d", minutes, secs));
    }

    public void setOnTimerFinished(Runnable callback) {
        this.onTimerFinishedCallback = callback;
    }

    public IntegerProperty totalSecondsProperty() { return totalSeconds; }
    public IntegerProperty remainingSecondsProperty() { return remainingSeconds; }
    public BooleanProperty isRunningProperty() { return isRunning; }
    public BooleanProperty isPausedProperty() { return isPaused; }
    public StringProperty timeDisplayProperty() { return timeDisplay; }

    public int getTotalSeconds() { return totalSeconds.get(); }
    public int getRemainingSeconds() { return remainingSeconds.get(); }
    public boolean isRunning() { return isRunning.get(); }
    public boolean isPaused() { return isPaused.get(); }
    public String getTimeDisplay() { return timeDisplay.get(); }
}