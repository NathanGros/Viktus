package com.bignat.viktus;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bignat.viktus.data.AppViewModel;
import com.bignat.viktus.ui.SettingsActivity;
import com.bignat.viktus.ui.fragment.HistoryFragment;
import com.bignat.viktus.ui.fragment.MealsFragment;
import com.bignat.viktus.ui.fragment.SavesFragment;
import com.bignat.viktus.ui.fragment.ShoppingFragment;
import com.bignat.viktus.ui.fragment.SuppliesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // Same order as the items in bottom_nav_menu.xml
    private static final int[] TAB_IDS = {
            R.id.nav_meals, R.id.nav_supplies, R.id.nav_shopping, R.id.nav_history, R.id.nav_saves
    };
    private static final int[] TITLES = {
            R.string.title_meals, R.string.title_supplies, R.string.title_shopping,
            R.string.title_history, R.string.title_saves
    };

    private final ActivityResultLauncher<Intent> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK) return;

                Intent data = result.getData();
                AppViewModel vm = new ViewModelProvider(this).get(AppViewModel.class);

                if (data != null && data.getBooleanExtra("clear_all", false)) {
                    vm.clearAll();                       // already includes the history
                } else if (data != null && data.getBooleanExtra("clear_history", false)) {
                    vm.clearHistory();
                }
                vm.loaded.setValue(true);                // every page redraws
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewPager2 pager = findViewById(R.id.pager);
        BottomNavigationView nav = findViewById(R.id.bottom_nav);

        Menu menu = nav.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            View item = nav.findViewById(menu.getItem(i).getItemId());
            if (item != null) {
                TooltipCompat.setTooltipText(item, null);
                item.setOnLongClickListener(v -> true);   // swallow the long press
            }
        }

        TextView title = findViewById(R.id.page_title);
        title.setText(TITLES[pager.getCurrentItem()]);

        findViewById(R.id.btn_settings).setOnClickListener(v ->
                settingsLauncher.launch(new Intent(this, SettingsActivity.class)));

        pager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:  return new MealsFragment();
                    case 1:  return new SuppliesFragment();
                    case 2:  return new ShoppingFragment();
                    case 3:  return new HistoryFragment();
                    default: return new SavesFragment();
                }
            }

            @Override
            public int getItemCount() {
                return TAB_IDS.length;
            }
        });

        // Keep all 4 pages alive so their lists survive switching tabs
        pager.setOffscreenPageLimit(TAB_IDS.length - 1);

        // Tap a tab -> go to that page
        nav.setOnItemSelectedListener(item -> {
            for (int i = 0; i < TAB_IDS.length; i++) {
                if (TAB_IDS[i] == item.getItemId()) {
                    pager.setCurrentItem(i);
                    return true;
                }
            }
            return false;
        });

        // Swipe -> highlight the matching tab
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                nav.setSelectedItemId(TAB_IDS[position]);
                title.setText(TITLES[position]);
            }
        });
    }
}