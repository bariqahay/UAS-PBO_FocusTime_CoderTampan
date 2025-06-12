package com.focustime.model;

import javafx.beans.property.*;
import javafx.concurrent.Task;
import javafx.application.Platform;

public class TimerModel {
    private IntegerProperty totalSeconds;
    private IntegerProperty remainingSeconds;
    private BooleanProperty isRunning;
    private BooleanProperty isPaused;
    private StringProperty timeDisplay;
    
    private Task<Void> timerTask;
    private Thread timerThread;
    
    public TimerModel() {
        this.totalSeconds = new SimpleIntegerProperty(0);
        this.remainingSeconds = new SimpleIntegerProperty(0);
        this.isRunning = new SimpleBooleanProperty(false);
        this.isPaused = new SimpleBooleanProperty(false);
        this.timeDisplay = new SimpleStringProperty("00:00");
        
        // Update time display when remaining seconds change
        remainingSeconds.addListener((obs, oldVal, newVal) -> {
            updateTimeDisplay();
        });
    }
    
    public void setDuration(int minutes) {
        int seconds = minutes * 60;
        this.totalSeconds.set(seconds);
        this.remainingSeconds.set(seconds);
        updateTimeDisplay();
    }
    
    public void start() {
        if (isPaused.get()) {
            resume();
            return;
        }
        
        if (isRunning.get()) return;
        
        isRunning.set(true);
        isPaused.set(false);
        
        timerTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (remainingSeconds.get() > 0 && !isCancelled()) {
                    Thread.sleep(1000);
                    if (!isPaused.get()) {
                        Platform.runLater(() -> {
                            int current = remainingSeconds.get();
                            if (current > 0) {
                                remainingSeconds.set(current - 1);
                            }
                        });
                    }
                }
                
                if (remainingSeconds.get() == 0) {
                    Platform.runLater(() -> {
                        isRunning.set(false);
                        onTimerFinished();
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
        isPaused.set(true);
    }
    
    public void resume() {
        isPaused.set(false);
    }
    
    public void stop() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        isRunning.set(false);
        isPaused.set(false);
        remainingSeconds.set(totalSeconds.get());
    }
    
    public void reset() {
        stop();
        remainingSeconds.set(totalSeconds.get());
        updateTimeDisplay();
    }
    
    private void updateTimeDisplay() {
        int seconds = remainingSeconds.get();
        int minutes = seconds / 60;
        int secs = seconds % 60;
        timeDisplay.set(String.format("%02d:%02d", minutes, secs));
    }
    
    private void onTimerFinished() {
        // This will be called when timer reaches 0
        // Controller can listen to this via property binding
        System.out.println("Timer finished!"); // For now
    }
    
    // Getters for properties
    public IntegerProperty totalSecondsProperty() { return totalSeconds; }
    public IntegerProperty remainingSecondsProperty() { return remainingSeconds; }
    public BooleanProperty isRunningProperty() { return isRunning; }
    public BooleanProperty isPausedProperty() { return isPaused; }
    public StringProperty timeDisplayProperty() { return timeDisplay; }
    
    // Convenience getters
    public int getTotalSeconds() { return totalSeconds.get(); }
    public int getRemainingSeconds() { return remainingSeconds.get(); }
    public boolean isRunning() { return isRunning.get(); }
    public boolean isPaused() { return isPaused.get(); }
    public String getTimeDisplay() { return timeDisplay.get(); }
}