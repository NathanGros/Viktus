package com.bignat.viktus;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "supplies")
public class Supply implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int position;          // order in the list
    public String name;
    public double quantity;
    public Unit unit;

    // UI state only (status icon / checkbox), not saved
    @Ignore
    public boolean checked;

    // Used by Room
    public Supply(String name, double quantity, Unit unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    @Ignore
    public Supply(String name, double quantity, Unit unit, boolean checked) {
        this(name, quantity, unit);
        this.checked = checked;
    }

    public Supply copy() {
        return new Supply(name, quantity, unit, checked);
    }
}