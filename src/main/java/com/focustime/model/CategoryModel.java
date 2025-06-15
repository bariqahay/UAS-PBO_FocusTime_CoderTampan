package com.focustime.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CategoryModel {
    // Default preset categories
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
        "Kuliah", "Tugas", "Belajar Mandiri"
    );
    
    private ObservableList<String> categories;
    private StringProperty selectedCategory;
    
    public CategoryModel() {
        // Initialize with default categories
        this.categories = FXCollections.observableArrayList();
        this.categories.addAll(DEFAULT_CATEGORIES);
        
        // Set default selected category
        this.selectedCategory = new SimpleStringProperty(DEFAULT_CATEGORIES.get(0));
    }
    
    /**
     * Add new category if it doesn't exist
     */
    public boolean addCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCategory = category.trim();
        
        // Check if category already exists (case insensitive)
        for (String existing : categories) {
            if (existing.equalsIgnoreCase(trimmedCategory)) {
                return false; // Already exists
            }
        }
        
        categories.add(trimmedCategory);
        return true;
    }
    
    /**
     * Remove category (can't remove if it's the only one or currently selected)
     */
    public boolean removeCategory(String category) {
        if (categories.size() <= 1) {
            return false; // Can't remove if it's the only category
        }
        
        if (category.equals(selectedCategory.get())) {
            return false; // Can't remove currently selected category
        }
        
        return categories.remove(category);
    }
    
    /**
     * Get all categories as observable list
     */
    public ObservableList<String> getCategories() {
        return categories;
    }
    
    /**
     * Set selected category
     */
    public void setSelectedCategory(String category) {
        if (categories.contains(category)) {
            selectedCategory.set(category);
        }
    }
    
    /**
     * Get current selected category
     */
    public String getSelectedCategory() {
        return selectedCategory.get();
    }
    
    /**
     * Get selected category property for binding
     */
    public StringProperty selectedCategoryProperty() {
        return selectedCategory;
    }
    
    /**
     * Reset to default categories
     */
    public void resetToDefaults() {
        categories.clear();
        categories.addAll(DEFAULT_CATEGORIES);
        selectedCategory.set(DEFAULT_CATEGORIES.get(0));
    }
    
    /**
     * Check if category exists
     */
    public boolean hasCategory(String category) {
        return categories.contains(category);
    }
    
    /**
     * Get categories as regular list (for saving to file/db later)
     */
    public List<String> getCategoriesAsList() {
        return new ArrayList<>(categories);
    }
    
    /**
     * Load categories from list (from file/db later)
     */
    public void loadCategories(List<String> categoriesList) {
        categories.clear();
        if (categoriesList != null && !categoriesList.isEmpty()) {
            categories.addAll(categoriesList);
            selectedCategory.set(categoriesList.get(0));
        } else {
            // Fallback to defaults
            resetToDefaults();
        }
    }
}
