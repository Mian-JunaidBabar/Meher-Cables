package com.example.labelmaker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final String PREFS_NAME = "LabelMakerPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", false);
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set toolbar title color to the theme's colorOnBackground (dark in light theme, light in dark theme)
        try {
            android.util.TypedValue typedValue = new android.util.TypedValue();
            // resolve app theme attribute colorOnBackground
            boolean resolved = getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnBackground, typedValue, true);
            int titleColor = resolved ? typedValue.data : androidx.core.content.ContextCompat.getColor(this, android.R.color.white);
            toolbar.setTitleTextColor(titleColor);
        } catch (Exception ignored) {
            // fallback
            toolbar.setTitleTextColor(0xFFFFFFFF);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        setupThemeToggle();
        setupBackPressHandler();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LabelMakerFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_label_maker);
            setTitle("Meher Cables");
        }
    }
    
    private void setupThemeToggle() {
        MenuItem themeItem = navigationView.getMenu().findItem(R.id.nav_theme_toggle);
        View actionView = themeItem.getActionView();
        if (actionView != null) {
            SwitchMaterial themeSwitch = actionView.findViewById(R.id.theme_switch);
            if (themeSwitch != null) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                themeSwitch.setChecked(prefs.getBoolean("isDarkMode", false));

                themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("isDarkMode", isChecked);
                    editor.apply();

                    AppCompatDelegate.setDefaultNightMode(
                        isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                    );
                });
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        String title = "";

        int itemId = item.getItemId();
        if (itemId == R.id.nav_label_maker) {
            selectedFragment = new LabelMakerFragment();
            title = "Label Maker";
        } else if (itemId == R.id.nav_rate_list) {
            selectedFragment = new RateListFragment();
            title = "Meher Cables";
        } else if (itemId == R.id.nav_theme_toggle) {
            // Handled by switch listener
            return false;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            setTitle(title);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Disable the callback and trigger default behavior
                    if (isEnabled()) {
                        setEnabled(false);
                        MainActivity.super.getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }
}
