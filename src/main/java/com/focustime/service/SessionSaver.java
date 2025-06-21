package com.focustime.service;

public interface SessionSaver {
    boolean save(int userId, String category, int duration, String note);
}
