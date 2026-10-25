package com.bignat.viktus;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

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

    @Insert
    abstract void insertSupplies(List<Supply> supplies);

    @Insert
    abstract void insertMeals(List<Meal> meals);

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
}