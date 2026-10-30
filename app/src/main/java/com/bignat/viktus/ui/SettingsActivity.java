package com.bignat.viktus.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.bignat.viktus.R;
import com.bignat.viktus.data.AppDao;
import com.bignat.viktus.data.AppDatabase;
import com.bignat.viktus.data.ThemeManager;
import com.bignat.viktus.model.HistoryEntry;
import com.bignat.viktus.model.Meal;
import com.bignat.viktus.model.SavedEntry;
import com.bignat.viktus.ui.dialog.ConfirmDialog;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {

    private ImageView swatchLight, swatchDark, swatchSystem;
    private boolean dataCleared = false;
    private boolean historyCleared = false;
    private String pendingClear = "";   // which button opened the confirm dialog: "all" or "history"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        swatchLight = findViewById(R.id.swatch_light);
        swatchDark = findViewById(R.id.swatch_dark);
        swatchSystem = findViewById(R.id.swatch_system);

        select(ThemeManager.get(this));

        findViewById(R.id.option_light).setOnClickListener(v -> choose(ThemeManager.LIGHT));
        findViewById(R.id.option_dark).setOnClickListener(v -> choose(ThemeManager.DARK));
        findViewById(R.id.option_system).setOnClickListener(v -> choose(ThemeManager.SYSTEM));

        // "" = follow the phone's language
        final String[] tags = {"", "en", "fr"};
        String[] names = {getString(R.string.language_system), "English", "Français"};

        MaterialAutoCompleteTextView dropdown = findViewById(R.id.language_dropdown);
        dropdown.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names));

        // Show the current choice (false = don't filter the list)
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        int selected = 0;
        if (!current.isEmpty()) {
            for (int i = 1; i < tags.length; i++) {
                if (tags[i].equals(current.get(0).getLanguage())) selected = i;
            }
        }
        dropdown.setText(names[selected], false);

        dropdown.setOnItemClickListener((parent, v, position, id) -> {
            // Android saves the choice and recreates the screens in the new language
            AppCompatDelegate.setApplicationLocales(tags[position].isEmpty()
                    ? LocaleListCompat.getEmptyLocaleList()
                    : LocaleListCompat.forLanguageTags(tags[position]));
        });

        findViewById(R.id.btn_clean_images).setOnClickListener(v -> cleanUnusedImages());

        if (savedInstanceState != null) {
            dataCleared = savedInstanceState.getBoolean("data_cleared");
            historyCleared = savedInstanceState.getBoolean("history_cleared");
            pendingClear = savedInstanceState.getString("pending_clear", "");
            updateResult();
        }

        findViewById(R.id.btn_clear_data).setOnClickListener(v -> {
            pendingClear = "all";
            ConfirmDialog.newInstance(
                            getString(R.string.clear_data_title),
                            getString(R.string.clear_data_message),
                            getString(R.string.clear_data_confirm))
                    .show(getSupportFragmentManager(), "confirm_clear");
        });

        findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            pendingClear = "history";
            ConfirmDialog.newInstance(
                            getString(R.string.clear_history_title),
                            getString(R.string.clear_history_message),
                            getString(R.string.clear_history_confirm))
                    .show(getSupportFragmentManager(), "confirm_clear");
        });

        getSupportFragmentManager().setFragmentResultListener(
                ConfirmDialog.RESULT_KEY, this, (key, result) -> {
                    if ("history".equals(pendingClear)) {
                        historyCleared = true;
                        Toast.makeText(this, R.string.toast_history_cleared, Toast.LENGTH_SHORT).show();
                    } else {
                        dataCleared = true;
                        Toast.makeText(this, R.string.toast_data_cleared, Toast.LENGTH_SHORT).show();
                    }
                    updateResult();
                });
    }

    private void choose(int mode) {
        if (mode == ThemeManager.get(this)) return;
        select(mode);
        ThemeManager.set(this, mode);   // the screen recreates itself in the new theme
    }

    private void select(int mode) {
        swatchLight.setSelected(mode == ThemeManager.LIGHT);
        swatchDark.setSelected(mode == ThemeManager.DARK);
        swatchSystem.setSelected(mode == ThemeManager.SYSTEM);
    }

    // Tells MainActivity what to clear when this screen closes
    private void updateResult() {
        if (!dataCleared && !historyCleared) return;
        Intent data = new Intent();
        data.putExtra("clear_all", dataCleared);
        data.putExtra("clear_history", historyCleared);
        setResult(RESULT_OK, data);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("data_cleared", dataCleared);
        outState.putBoolean("history_cleared", historyCleared);
        outState.putString("pending_clear", pendingClear);
    }

    private void cleanUnusedImages() {
        Context app = getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());

        // Database and file work must not run on the main thread
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDao dao = AppDatabase.get(app).dao();

            // File names still referenced somewhere
            Set<String> used = new HashSet<>();
            for (Meal m : dao.getMeals()) addFileName(used, m.imageUri);
            for (HistoryEntry e : dao.getHistory()) addFileName(used, e.imageUri);
            for (SavedEntry e : dao.getSaved()) addFileName(used, e.imageUri);

            int deleted = 0;
            File[] files = new File(app.getFilesDir(), "meal_images").listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!used.contains(f.getName()) && f.delete()) deleted++;
                }
            }

            int count = deleted;
            main.post(() -> Toast.makeText(app,
                    count == 0 ? app.getString(R.string.toast_no_unused_images)
                            : app.getString(R.string.toast_images_cleaned, count),
                    Toast.LENGTH_SHORT).show());
        });
    }

    // "file:///.../meal_images/abc.jpg" -> "abc.jpg"
    private static void addFileName(Set<String> set, String uri) {
        if (uri == null) return;
        String name = Uri.parse(uri).getLastPathSegment();
        if (name != null) set.add(name);
    }
}