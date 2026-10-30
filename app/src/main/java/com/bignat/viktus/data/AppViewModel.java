package com.bignat.viktus.data;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.bignat.viktus.logic.StockConsumer;
import com.bignat.viktus.model.HistoryEntry;
import com.bignat.viktus.model.Meal;
import com.bignat.viktus.model.SavedEntry;
import com.bignat.viktus.model.Supply;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppViewModel extends AndroidViewModel {
    public static final int HISTORY_LIMIT = 20;

    // The lists your adapters work on directly
    public final List<Supply> supplies = new ArrayList<>();
    public final List<Meal> meals = new ArrayList<>();
    public final List<Meal> history = new ArrayList<>();
    public final List<Meal> saved = new ArrayList<>();
    public final MutableLiveData<Boolean> loaded = new MutableLiveData<>(false);

    private final AppDao dao;
    // One thread = writes happen in the order they were requested
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    public AppViewModel(@NonNull Application app) {
        super(app);
        dao = AppDatabase.get(app).dao();

        io.execute(() -> {
            List<Supply> s = dao.getSupplies();
            List<Meal> m = dao.getMeals();
            List<HistoryEntry> h = dao.getHistory();
            List<SavedEntry> sv = dao.getSaved();
            new Handler(Looper.getMainLooper()).post(() -> {
                supplies.addAll(s);
                meals.addAll(m);
                for (HistoryEntry e : h) history.add(e.toMeal());
                for (SavedEntry e : sv) saved.add(e.toMeal());
                loaded.setValue(true);
            });
        });
    }

    public void saveSupplies() {
        // Snapshot on the main thread so the background write can't race with edits
        List<Supply> snapshot = new ArrayList<>();
        for (int i = 0; i < supplies.size(); i++) {
            Supply c = supplies.get(i).copy();
            c.position = i;
            snapshot.add(c);
        }
        io.execute(() -> dao.replaceSupplies(snapshot));
    }

    public void saveMeals() {
        List<Meal> snapshot = new ArrayList<>();
        for (int i = 0; i < meals.size(); i++) {
            Meal c = meals.get(i).copy();
            c.position = i;
            snapshot.add(c);
        }
        io.execute(() -> dao.replaceMeals(snapshot));
    }

    // Newest goes on top; anything past the limit (the oldest) is dropped
    public void addToHistory(Meal meal) {
        history.add(0, meal.copy());
        while (history.size() > HISTORY_LIMIT) {
            history.remove(history.size() - 1);
        }
        saveHistory();
    }

    private void saveHistory() {
        List<HistoryEntry> snapshot = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            snapshot.add(HistoryEntry.from(history.get(i).copy(), i));
        }
        io.execute(() -> dao.replaceHistory(snapshot));
    }

    // Returns false if a saved meal with the same name already exists
    public boolean addToSaved(Meal meal) {
        for (Meal m : saved) {
            if (m.name.trim().equalsIgnoreCase(meal.name.trim())) return false;
        }
        saved.add(meal.copy());
        saveSaved();
        return true;
    }

    public void saveSaved() {
        List<SavedEntry> snapshot = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            snapshot.add(SavedEntry.from(saved.get(i).copy(), i));
        }
        io.execute(() -> dao.replaceSaved(snapshot));
    }

    // Empties the lists, the database and the saved photos
    public void clearAll() {
        supplies.clear();
        meals.clear();
        history.clear();
        saved.clear();

        io.execute(() -> {
            dao.replaceSupplies(new ArrayList<>());
            dao.replaceMeals(new ArrayList<>());
            dao.replaceHistory(new ArrayList<>());
            dao.replaceSaved(new ArrayList<>());

            File[] images = new File(getApplication().getFilesDir(), "meal_images").listFiles();
            if (images != null) {
                for (File f : images) f.delete();
            }
        });
    }

    public void clearHistory() {
        history.clear();
        saveHistory();   // writes an empty list, which deletes the rows
    }

    // Called when a meal is eaten: takes its ingredients out of the stock
    public void consumeIngredients(Meal meal) {
        StockConsumer.consume(supplies, meal.supplies);
        saveSupplies();
    }
}