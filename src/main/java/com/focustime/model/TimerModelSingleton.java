package com.focustime.model;

public class TimerModelSingleton {
    private static final TimerModel INSTANCE = new TimerModel();

    private TimerModelSingleton() {
        // prevent instantiation
    }

    public static TimerModel getInstance() {
        return INSTANCE;
    }
}
