package com.bignat.viktus;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    // Same order as the items in bottom_nav_menu.xml
    private static final int[] TAB_IDS = {
            R.id.nav_meals, R.id.nav_supplies, R.id.nav_shopping, R.id.nav_history, R.id.nav_saves
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 pager = findViewById(R.id.pager);
        BottomNavigationView nav = findViewById(R.id.bottom_nav);

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
                nav.getMenu().getItem(position).setChecked(true);
            }
        });
    }
}