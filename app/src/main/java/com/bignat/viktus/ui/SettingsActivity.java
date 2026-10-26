package com.bignat.viktus.ui;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bignat.viktus.R;
import com.bignat.viktus.data.ThemeManager;

public class SettingsActivity extends AppCompatActivity {

    private ImageView swatchLight, swatchDark, swatchSystem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        swatchLight = findViewById(R.id.swatch_light);
        swatchDark = findViewById(R.id.swatch_dark);
        swatchSystem = findViewById(R.id.swatch_system);

        select(ThemeManager.get(this));

        findViewById(R.id.option_light).setOnClickListener(v -> choose(ThemeManager.LIGHT));
        findViewById(R.id.option_dark).setOnClickListener(v -> choose(ThemeManager.DARK));
        findViewById(R.id.option_system).setOnClickListener(v -> choose(ThemeManager.SYSTEM));
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
}