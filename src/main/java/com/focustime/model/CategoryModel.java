package com.focustime.model;

import com.focustime.util.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class CategoryModel {

    private final int userId;

    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        "Kuliah", "Tugas", "Belajar Mandiri"
    );

    private final ObservableList<String> categories;
    private final StringProperty selectedCategory;

    public CategoryModel(int userId) {
        this.userId = userId;
        this.categories = FXCollections.observableArrayList();
        this.selectedCategory = new SimpleStringProperty();

        loadCategoriesFromDB();
    }

    private void loadCategoriesFromDB() {
        String sql = "SELECT name FROM user_categories WHERE user_id = ? ORDER BY id";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            categories.clear();
            while (rs.next()) {
                categories.add(rs.getString("name"));
            }

            if (!categories.isEmpty()) {
                selectedCategory.set(categories.get(0));
            } else {
                categories.addAll(DEFAULT_CATEGORIES);
                selectedCategory.set(DEFAULT_CATEGORIES.get(0));
                for (String cat : DEFAULT_CATEGORIES) {
                    saveCategoryToDB(cat);
                }
            }

        } catch (SQLException e) {
            System.err.println("Gagal load kategori: " + e.getMessage());
        }
    }

    private void saveCategoryToDB(String name) {
        String sql = "INSERT INTO user_categories (name, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setInt(2, userId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Gagal simpan kategori: " + e.getMessage());
        }
    }

    public boolean addCategory(String category) {
        if (category == null || category.trim().isEmpty()) return false;

        String trimmed = category.trim();

        for (String existing : categories) {
            if (existing.equalsIgnoreCase(trimmed)) return false;
        }

        categories.add(trimmed);
        saveCategoryToDB(trimmed);
        return true;
    }

    public boolean removeCategory(String category) {
        if (categories.size() <= 1) return false;
        if (category.equals(selectedCategory.get())) return false;

        boolean removed = categories.remove(category);

        if (removed) {
            deleteCategoryFromDB(category);
        }

        return removed;
    }

    private void deleteCategoryFromDB(String name) {
        String sql = "DELETE FROM user_categories WHERE user_id = ? AND name = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setString(2, name);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Gagal hapus kategori: " + e.getMessage());
        }
    }

    public ObservableList<String> getCategories() {
        return categories;
    }

    public void setSelectedCategory(String category) {
        if (categories.contains(category)) {
            selectedCategory.set(category);
        }
    }

    public String getSelectedCategory() {
        return selectedCategory.get();
    }

    public StringProperty selectedCategoryProperty() {
        return selectedCategory;
    }

    public boolean hasCategory(String category) {
        return categories.contains(category);
    }
}
