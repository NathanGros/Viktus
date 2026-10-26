package com.bignat.viktus.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import com.bignat.viktus.model.HistoryEntry;
import com.bignat.viktus.model.Meal;
import com.bignat.viktus.model.SavedEntry;
import com.bignat.viktus.model.Supply;

import java.util.List;

@Dao
public abstract class AppDao {

    @Query("SELECT * FROM supplies ORDER BY position")
    public abstract List<Supply> getSupplies();

    @Query("SELECT * FROM meals ORDER BY position")
    public abstract List<Meal> getMeals();

    @Query("DELETE FROM supplies")
    abstract void clearSupplies();

    @Query("DELETE FROM meals")
    abstract void clearMeals();

    @Query("SELECT * FROM history ORDER BY position")
    public abstract List<HistoryEntry> getHistory();

    @Query("DELETE FROM history")
    abstract void clearHistory();

    @Query("SELECT * FROM saved ORDER BY position")
    public abstract List<SavedEntry> getSaved();

    @Query("DELETE FROM saved")
    abstract void clearSaved();

    @Insert
    abstract void insertSupplies(List<Supply> supplies);

    @Insert
    abstract void insertMeals(List<Meal> meals);

    @Insert
    abstract void insertHistory(List<HistoryEntry> entries);

    @Insert
    abstract void insertSaved(List<SavedEntry> entries);

    // Whole list is rewritten at once; fine for a personal app with a small amount of data
    @Transaction
    public void replaceSupplies(List<Supply> supplies) {
        clearSupplies();
        insertSupplies(supplies);
    }

    @Transaction
    public void replaceMeals(List<Meal> meals) {
        clearMeals();
        insertMeals(meals);
    }

    @Transaction
    public void replaceHistory(List<HistoryEntry> entries) {
        clearHistory();
        insertHistory(entries);
    }

    @Transaction
    public void replaceSaved(List<SavedEntry> entries) {
        clearSaved();
        insertSaved(entries);
    }
}