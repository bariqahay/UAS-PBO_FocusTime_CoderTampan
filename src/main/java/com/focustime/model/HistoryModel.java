package com.focustime.model;

import java.time.LocalDateTime;

public class HistoryModel {
    private final String category;
    private final int durationMinutes;
    private final LocalDateTime timestamp;
    private final String note;

    public HistoryModel(String category, int durationMinutes, LocalDateTime timestamp, String note) {
        this.category = category;
        this.durationMinutes = durationMinutes;
        this.timestamp = timestamp;
        this.note = note;
    }

    public String getCategory() { return category; }
    public int getDurationMinutes() { return durationMinutes; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getNote() { return note; }
}
