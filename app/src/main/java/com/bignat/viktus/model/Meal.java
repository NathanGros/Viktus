package com.bignat.viktus.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "meals")
public class Meal implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int position;
    public String name;
    public String description;
    public String imageUri;          // null = no image
    public List<Supply> supplies;    // ingredients, saved as JSON

    public Meal(String name, String description, String imageUri, List<Supply> supplies) {
        this.name = name;
        this.description = description;
        this.imageUri = imageUri;
        this.supplies = supplies;
    }

    @Ignore
    public Meal(String name) {
        this(name, "", null, new ArrayList<>());
    }

    public Meal copy() {
        List<Supply> c = new ArrayList<>();
        for (Supply s : supplies) c.add(s.copy());
        return new Meal(name, description, imageUri, c);
    }
}