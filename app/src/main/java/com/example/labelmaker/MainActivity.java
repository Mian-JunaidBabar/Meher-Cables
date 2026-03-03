package com.example.labelmaker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int CREATE_FILE_REQUEST_CODE = 1;
    private static final String PREFS_NAME = "LabelMakerPrefs";

    // A4 dimensions in points (1/72 inch)
    private static final float A4_WIDTH_POINTS = 595f;
    private static final float A4_HEIGHT_POINTS = 842f;

    // Font size presets
    private static final int[] FONT_SIZES = {6, 8, 10, 12, 16, 20, 24, 28};
    private static final String[] FONT_SIZE_LABELS = {"XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL"};

    private DrawerLayout drawerLayout;
    private A4PreviewView a4PreviewView;
    private EditText textInput, rowsInput, columnsInput;
    private MaterialButton textColorButton, backgroundColorButton, exportButton, fontSizeDropdown;
    private MaterialButton fontDecrease, fontIncrease;
    private TextView fontSizeValue, borderWidthValue;
    private SeekBar borderWidthSeekbar;
    private Toolbar toolbar;

    private int textColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private float fontSize = 12f;
    private float borderWidth = 2f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load theme preference before setContentView
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("isDarkMode", false);
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        
        setContentView(R.layout.activity_main);

        initializeViews();
        setupToolbar();
        loadConfiguration();
        setupListeners();
        setupBackPressHandler();
        updatePreview();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        a4PreviewView = findViewById(R.id.a4_preview);
        textInput = findViewById(R.id.text_input);
        rowsInput = findViewById(R.id.rows_input);
        columnsInput = findViewById(R.id.columns_input);
        textColorButton = findViewById(R.id.text_color_button);
        backgroundColorButton = findViewById(R.id.background_color_button);
        exportButton = findViewById(R.id.export_button);
        fontSizeDropdown = findViewById(R.id.font_size_dropdown);
        fontDecrease = findViewById(R.id.font_decrease);
        fontIncrease = findViewById(R.id.font_increase);
        fontSizeValue = findViewById(R.id.font_size_value);
        borderWidthSeekbar = findViewById(R.id.border_width_seekbar);
        borderWidthValue = findViewById(R.id.border_width_value);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupListeners() {
        // Text change listeners for live preview
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        textInput.addTextChangedListener(textWatcher);
        rowsInput.addTextChangedListener(textWatcher);
        columnsInput.addTextChangedListener(textWatcher);

        // Color selection buttons
        textColorButton.setOnClickListener(v -> selectTextColor());
        backgroundColorButton.setOnClickListener(v -> selectBackgroundColor());
        
        // Font size controls
        fontSizeDropdown.setOnClickListener(v -> showFontSizeMenu());
        fontDecrease.setOnClickListener(v -> {
            fontSize = Math.max(4f, fontSize - 1);
            updateFontSizeDisplay();
            updatePreview();
        });
        fontIncrease.setOnClickListener(v -> {
            fontSize = Math.min(72f, fontSize + 1);
            updateFontSizeDisplay();
            updatePreview();
        });
        
        // Border width control
        borderWidthSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Map 0-95 to 0.5-10.0
                borderWidth = 0.5f + (progress / 95f) * 9.5f;
                borderWidthValue.setText(String.format("%.1f", borderWidth));
                a4PreviewView.setBorderWidth(borderWidth);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // Export button
        exportButton.setOnClickListener(v -> exportToPdf());

        // Navigation drawer
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_save_labels) {
                saveConfiguration();
                Toast.makeText(this, "Configuration saved successfully!", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.nav_rate_list) {
                Intent intent = new Intent(this, RateListActivity.class);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
        
        // Setup theme toggle switch
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

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (isEnabled()) {
                        setEnabled(false);
                        MainActivity.super.getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }

    private void selectTextColor() {
        new ColorPickerDialog(this, textColor, color -> {
            textColor = color;
            updateButtonColor(textColorButton, color);
            updatePreview();
        }).show();
    }

    private void selectBackgroundColor() {
        new ColorPickerDialog(this, backgroundColor, color -> {
            backgroundColor = color;
            updateButtonColor(backgroundColorButton, color);
            updatePreview();
        }).show();
    }
    
    private void showFontSizeMenu() {
        PopupMenu popup = new PopupMenu(this, fontSizeDropdown);
        for (int i = 0; i < FONT_SIZE_LABELS.length; i++) {
            popup.getMenu().add(0, i, i, FONT_SIZE_LABELS[i] + " (" + FONT_SIZES[i] + "pt)");
        }
        popup.setOnMenuItemClickListener(item -> {
            int index = item.getItemId();
            fontSize = FONT_SIZES[index];
            fontSizeDropdown.setText(FONT_SIZE_LABELS[index]);
            updateFontSizeDisplay();
            updatePreview();
            return true;
        });
        popup.show();
    }
    
    private void updateFontSizeDisplay() {
        fontSizeValue.setText(String.format("%.0f", fontSize));
    }

    private void updateButtonColor(MaterialButton button, int color) {
        // Update button to show selected color as icon tint
        button.setIconTint(android.content.res.ColorStateList.valueOf(color));
    }

    private void updatePreview() {
        String text = textInput.getText().toString();
        int rows = parseIntOrDefault(rowsInput.getText().toString(), 10);
        int cols = parseIntOrDefault(columnsInput.getText().toString(), 3);

        // Validate and constrain values
        rows = Math.max(1, Math.min(50, rows));
        cols = Math.max(1, Math.min(20, cols));

        a4PreviewView.setLabelData(text, rows, cols, fontSize, textColor, backgroundColor);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    private void saveConfiguration() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString("text", textInput.getText().toString());
        editor.putInt("rows", parseIntOrDefault(rowsInput.getText().toString(), 10));
        editor.putInt("cols", parseIntOrDefault(columnsInput.getText().toString(), 3));
        editor.putFloat("fontSize", fontSize);
        editor.putInt("textColor", textColor);
        editor.putInt("backgroundColor", backgroundColor);
        editor.putFloat("borderWidth", borderWidth);
        editor.apply();
    }

    private void loadConfiguration() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        String text = prefs.getString("text", "");
        int rows = prefs.getInt("rows", 10);
        int cols = prefs.getInt("cols", 3);
        fontSize = prefs.getFloat("fontSize", 12f);
        borderWidth = prefs.getFloat("borderWidth", 2f);
        
        textInput.setText(text);
        rowsInput.setText(String.valueOf(rows));
        columnsInput.setText(String.valueOf(cols));
        
        textColor = prefs.getInt("textColor", Color.BLACK);
        backgroundColor = prefs.getInt("backgroundColor", Color.WHITE);

        // Update UI
        updateButtonColor(textColorButton, textColor);
        updateButtonColor(backgroundColorButton, backgroundColor);
        updateFontSizeDisplay();
        
        // Update border width seekbar
        int progress = (int)(((borderWidth - 0.5f) / 9.5f) * 95);
        borderWidthSeekbar.setProgress(progress);
        borderWidthValue.setText(String.format("%.1f", borderWidth));
        a4PreviewView.setBorderWidth(borderWidth);
    }

    private void exportToPdf() {
        // Validate inputs before export
        if (textInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter label text before exporting", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "cable_labels.pdf");

        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    createPdfDocument(uri);
                }
            }
        }
    }

    private void createPdfDocument(Uri uri) {
        try {
            // Create PDF document
            PdfDocument document = new PdfDocument();
            
            // Create A4 page with exact dimensions (595 x 842 points)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                (int) A4_WIDTH_POINTS, 
                (int) A4_HEIGHT_POINTS, 
                1
            ).create();
            
            PdfDocument.Page page = document.startPage(pageInfo);

            // Draw the label sheet at actual size
            a4PreviewView.drawToCanvas(page.getCanvas(), A4_WIDTH_POINTS, A4_HEIGHT_POINTS);

            document.finishPage(page);

            // Write to file
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                document.writeTo(os);
                document.close();
                
                Toast.makeText(this, "PDF exported successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
