package com.example.labelmaker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.io.OutputStream;

public class LabelMakerFragment extends Fragment {

    private static final int CREATE_FILE_REQUEST_CODE = 1;
    private static final String PREFS_NAME = "LabelMakerPrefs";

    // A4 dimensions in points (1/72 inch)
    private static final float A4_WIDTH_POINTS = 595f;
    private static final float A4_HEIGHT_POINTS = 842f;

    private A4PreviewView a4PreviewView;
    private EditText textInput, rowsInput, columnsInput;
    private MaterialButton exportButton, textColorButton, backgroundColorButton;
    private MaterialButton gradientStartButton, gradientEndButton;
    private MaterialButtonToggleGroup backgroundTypeToggle;
    private LinearLayout solidColorContainer, gradientEditorContainer;
    private TextView fontSizeValue;
    private Slider fontSizeSlider;

    private int textColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private int gradientStartColor = Color.parseColor("#006666"); // Primary Teal
    private int gradientEndColor = Color.parseColor("#0f2323"); // Dark Background
    private boolean isGradient = false;
    private float fontSize = 12f;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_label_maker, container, false);

        initializeViews(view);
        loadConfiguration();
        setupListeners();
        updatePreview();

        return view;
    }

    private void initializeViews(View view) {
        a4PreviewView = view.findViewById(R.id.a4_preview);
        textInput = view.findViewById(R.id.text_input);
        rowsInput = view.findViewById(R.id.rows_input);
        columnsInput = view.findViewById(R.id.columns_input);
        
        exportButton = view.findViewById(R.id.export_button);
        fontSizeSlider = view.findViewById(R.id.font_size_slider);
        fontSizeValue = view.findViewById(R.id.font_size_value);
        
        textColorButton = view.findViewById(R.id.text_color_button);
        backgroundColorButton = view.findViewById(R.id.background_color_button);
        gradientStartButton = view.findViewById(R.id.gradient_start_color_button);
        gradientEndButton = view.findViewById(R.id.gradient_end_color_button);
        
        backgroundTypeToggle = view.findViewById(R.id.background_type_toggle);
        solidColorContainer = view.findViewById(R.id.solid_color_container);
        gradientEditorContainer = view.findViewById(R.id.gradient_editor_container);
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

        // Font size control
        fontSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            fontSize = value;
            updateFontSizeDisplay();
            updatePreview();
        });
        
        // Background Type Toggle
        backgroundTypeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_solid_color) {
                    isGradient = false;
                    solidColorContainer.setVisibility(View.VISIBLE);
                    gradientEditorContainer.setVisibility(View.GONE);
                } else if (checkedId == R.id.btn_gradient) {
                    isGradient = true;
                    solidColorContainer.setVisibility(View.GONE);
                    gradientEditorContainer.setVisibility(View.VISIBLE);
                }
                updatePreview();
            }
        });

        // Color buttons (Placeholders for actual color pickers)
        textColorButton.setOnClickListener(v -> {
            // Toggle for demonstration
            textColor = (textColor == Color.BLACK) ? Color.WHITE : Color.BLACK;
            updateButtonColor(textColorButton, textColor);
            updatePreview();
        });

        backgroundColorButton.setOnClickListener(v -> {
            // Toggle for demonstration
            backgroundColor = (backgroundColor == Color.WHITE) ? Color.parseColor("#1e293b") : Color.WHITE;
            updateButtonColor(backgroundColorButton, backgroundColor);
            updatePreview();
        });

        gradientStartButton.setOnClickListener(v -> {
            // Toggle for demonstration
            gradientStartColor = (gradientStartColor == Color.parseColor("#006666")) ? Color.parseColor("#2ea4a4") : Color.parseColor("#006666");
            updateButtonColor(gradientStartButton, gradientStartColor);
            updatePreview();
        });

        gradientEndButton.setOnClickListener(v -> {
            // Toggle for demonstration
            gradientEndColor = (gradientEndColor == Color.parseColor("#0f2323")) ? Color.parseColor("#1e293b") : Color.parseColor("#0f2323");
            updateButtonColor(gradientEndButton, gradientEndColor);
            updatePreview();
        });

        // Export button
        exportButton.setOnClickListener(v -> exportToPdf());
    }
    
    private void updateButtonColor(MaterialButton button, int color) {
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }
    
    private void updateFontSizeDisplay() {
        fontSizeValue.setText(String.format("%.0fpx", fontSize));
    }

    private void updatePreview() {
        String text = textInput.getText().toString();
        int rows = parseIntOrDefault(rowsInput.getText().toString(), 10);
        int cols = parseIntOrDefault(columnsInput.getText().toString(), 3);

        // Validate and constrain values
        rows = Math.max(1, Math.min(50, rows));
        cols = Math.max(1, Math.min(20, cols));

        if (isGradient) {
            a4PreviewView.setLabelData(text, rows, cols, fontSize, textColor, gradientStartColor, gradientEndColor);
        } else {
            a4PreviewView.setLabelData(text, rows, cols, fontSize, textColor, backgroundColor);
        }
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void saveConfiguration() {
        SharedPreferences.Editor editor = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString("text", textInput.getText().toString());
        editor.putInt("rows", parseIntOrDefault(rowsInput.getText().toString(), 10));
        editor.putInt("cols", parseIntOrDefault(columnsInput.getText().toString(), 3));
        editor.putFloat("fontSize", fontSize);
        editor.putInt("textColor", textColor);
        editor.putInt("backgroundColor", backgroundColor);
        editor.putInt("gradientStartColor", gradientStartColor);
        editor.putInt("gradientEndColor", gradientEndColor);
        editor.putBoolean("isGradient", isGradient);
        editor.apply();
        
        Toast.makeText(requireContext(), "Configuration saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private void loadConfiguration() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        String text = prefs.getString("text", "15mm Cable");
        int rows = prefs.getInt("rows", 10);
        int cols = prefs.getInt("cols", 3);
        fontSize = prefs.getFloat("fontSize", 12f);
        
        textInput.setText(text);
        rowsInput.setText(String.valueOf(rows));
        columnsInput.setText(String.valueOf(cols));
        
        textColor = prefs.getInt("textColor", Color.BLACK);
        backgroundColor = prefs.getInt("backgroundColor", Color.WHITE);
        gradientStartColor = prefs.getInt("gradientStartColor", Color.parseColor("#006666"));
        gradientEndColor = prefs.getInt("gradientEndColor", Color.parseColor("#0f2323"));
        isGradient = prefs.getBoolean("isGradient", false);

        fontSizeSlider.setValue(fontSize);
        updateFontSizeDisplay();
        
        updateButtonColor(textColorButton, textColor);
        updateButtonColor(backgroundColorButton, backgroundColor);
        updateButtonColor(gradientStartButton, gradientStartColor);
        updateButtonColor(gradientEndButton, gradientEndColor);

        if (isGradient) {
            backgroundTypeToggle.check(R.id.btn_gradient);
            solidColorContainer.setVisibility(View.GONE);
            gradientEditorContainer.setVisibility(View.VISIBLE);
        } else {
            backgroundTypeToggle.check(R.id.btn_solid_color);
            solidColorContainer.setVisibility(View.VISIBLE);
            gradientEditorContainer.setVisibility(View.GONE);
        }
    }

    private void exportToPdf() {
        // Validate inputs before export
        if (textInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter label text before exporting", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "cable_labels.pdf");

        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == getActivity().RESULT_OK) {
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
            try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                document.writeTo(os);
                document.close();
                
                Toast.makeText(requireContext(), "PDF exported successfully!", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
