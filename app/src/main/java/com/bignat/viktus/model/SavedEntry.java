package com.bignat.viktus.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "saved")
public class SavedEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int position;             // order in the list
    public String name;
    public String description;
    public String imageUri;
    public List<Supply> supplies;    // saved as JSON by Converters

    public SavedEntry(String name, String description, String imageUri, List<Supply> supplies) {
        this.name = name;
        this.description = description;
        this.imageUri = imageUri;
        this.supplies = supplies;
    }

    public static SavedEntry from(Meal meal, int position) {
        SavedEntry e = new SavedEntry(meal.name, meal.description, meal.imageUri, meal.supplies);
        e.position = position;
        return e;
    }

    public Meal toMeal() {
        return new Meal(name, description, imageUri, supplies);
    }
}