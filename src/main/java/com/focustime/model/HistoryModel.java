package com.focustime.model;

import java.time.LocalDateTime;

public class HistoryModel {
    private final String category;
    private final int durationMinutes;
    private final LocalDateTime createdAt;
    private final String note;

    public HistoryModel(String category, int durationMinutes, LocalDateTime createdAt, String note) {
        this.category = category;
        this.durationMinutes = durationMinutes;
        this.createdAt = createdAt;
        this.note = note;
    }

    public String getCategory() {
        return category;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getNote() {
        return note;
    }
}
