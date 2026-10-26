package com.bignat.viktus.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "history")
public class HistoryEntry {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int position;             // 0 = most recently eaten
    public String name;
    public String description;
    public String imageUri;
    public List<Supply> supplies;    // saved as JSON by Converters

    public HistoryEntry(String name, String description, String imageUri, List<Supply> supplies) {
        this.name = name;
        this.description = description;
        this.imageUri = imageUri;
        this.supplies = supplies;
    }

    public static HistoryEntry from(Meal meal, int position) {
        HistoryEntry e = new HistoryEntry(meal.name, meal.description, meal.imageUri, meal.supplies);
        e.position = position;
        return e;
    }

    public Meal toMeal() {
        return new Meal(name, description, imageUri, supplies);
    }
}