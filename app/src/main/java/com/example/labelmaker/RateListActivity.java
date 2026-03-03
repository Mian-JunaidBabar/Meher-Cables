package com.example.labelmaker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.OutputStream;

public class RateListActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CREATE_PDF = 101;
    private static final int REQUEST_CODE_CREATE_PNG = 102;

    // UI Components
    private TextInputEditText headerProduct, headerOldRate, headerNewRate;
    private TextInputEditText inputProductName, inputOldRate, inputNewRate;
    private TextView headerProductDisplay, headerOldRateDisplay, headerNewRateDisplay;
    private TextView emptyMessage;
    private RecyclerView rateListRecycler;
    private LinearLayout rateListContainer;
    private MaterialButton btnAddItem, btnExportPdf, btnExportPng;
    private MaterialToolbar toolbar;

    // Data
    private RateListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_list);

        initializeViews();
        setupRecyclerView();
        setupListeners();
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Header inputs
        headerProduct = findViewById(R.id.header_product);
        headerOldRate = findViewById(R.id.header_old_rate);
        headerNewRate = findViewById(R.id.header_new_rate);

        // Item inputs
        inputProductName = findViewById(R.id.input_product_name);
        inputOldRate = findViewById(R.id.input_old_rate);
        inputNewRate = findViewById(R.id.input_new_rate);

        // Display headers
        headerProductDisplay = findViewById(R.id.header_product_display);
        headerOldRateDisplay = findViewById(R.id.header_old_rate_display);
        headerNewRateDisplay = findViewById(R.id.header_new_rate_display);

        // List components
        emptyMessage = findViewById(R.id.empty_message);
        rateListRecycler = findViewById(R.id.rate_list_recycler);
        rateListContainer = findViewById(R.id.rate_list_container);

        // Buttons
        btnAddItem = findViewById(R.id.btn_add_item);
        btnExportPdf = findViewById(R.id.btn_export_pdf);
        btnExportPng = findViewById(R.id.btn_export_png);
    }

    private void setupRecyclerView() {
        adapter = new RateListAdapter();
        rateListRecycler.setLayoutManager(new LinearLayoutManager(this));
        rateListRecycler.setAdapter(adapter);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        // Update display headers when user types
        headerProduct.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                headerProductDisplay.setText(s.toString());
            }
        });

        headerOldRate.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                headerOldRateDisplay.setText(s.toString());
            }
        });

        headerNewRate.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                headerNewRateDisplay.setText(s.toString());
            }
        });

        // Add item button
        btnAddItem.setOnClickListener(v -> addItem());

        // Export buttons
        btnExportPdf.setOnClickListener(v -> exportPdf());
        btnExportPng.setOnClickListener(v -> exportPng());
    }

    private void addItem() {
        String productName = inputProductName.getText().toString().trim();
        String oldRate = inputOldRate.getText().toString().trim();
        String newRate = inputNewRate.getText().toString().trim();

        if (TextUtils.isEmpty(productName)) {
            inputProductName.setError("Product name required");
            inputProductName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(oldRate)) {
            inputOldRate.setError("Old rate required");
            inputOldRate.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(newRate)) {
            inputNewRate.setError("New rate required");
            inputNewRate.requestFocus();
            return;
        }

        RateItem item = new RateItem(productName, oldRate, newRate);
        adapter.addItem(item);

        // Clear inputs
        inputProductName.setText("");
        inputOldRate.setText("");
        inputNewRate.setText("");
        inputProductName.requestFocus();

        // Hide empty message
        if (adapter.getItemCount() > 0) {
            emptyMessage.setVisibility(View.GONE);
        }

        Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show();
    }

    private void exportPdf() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(this, "Add items to the list first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "rate_list_" + System.currentTimeMillis() + ".pdf");
        startActivityForResult(intent, REQUEST_CODE_CREATE_PDF);
    }

    private void exportPng() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(this, "Add items to the list first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, "rate_list_" + System.currentTimeMillis() + ".png");
        startActivityForResult(intent, REQUEST_CODE_CREATE_PNG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQUEST_CODE_CREATE_PDF) {
            savePdf(uri);
        } else if (requestCode == REQUEST_CODE_CREATE_PNG) {
            savePng(uri);
        }
    }

    private void savePdf(Uri uri) {
        try {
            // A4 size in points (1/72 inch)
            int pageWidth = 595;  // 8.27 inches * 72
            int pageHeight = 842; // 11.69 inches * 72

            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Canvas canvas = page.getCanvas();

            // Draw white background
            canvas.drawColor(Color.WHITE);

            // Draw the rate list
            drawRateListToCanvas(canvas, pageWidth, pageHeight);

            pdfDocument.finishPage(page);

            // Write to file
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            pdfDocument.writeTo(outputStream);
            outputStream.close();
            pdfDocument.close();

            Toast.makeText(this, "PDF saved successfully", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void savePng(Uri uri) {
        try {
            // Create bitmap from the rate list container
            Bitmap bitmap = createBitmapFromView(rateListContainer);

            // Write to file
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.close();

            Toast.makeText(this, "PNG saved successfully", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving PNG: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap createBitmapFromView(View view) {
        // Measure and layout the view
        view.measure(
                View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        // Create bitmap and canvas
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw white background
        canvas.drawColor(Color.WHITE);

        // Draw view to canvas
        view.draw(canvas);

        return bitmap;
    }

    private void drawRateListToCanvas(Canvas canvas, int pageWidth, int pageHeight) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int margin = 40;
        int currentY = margin;

        // Draw title
        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        String title = "Rate List";
        canvas.drawText(title, margin, currentY, paint);
        currentY += 40;

        // Draw header background
        paint.setColor(Color.parseColor("#E8DEF8")); // Primary container color
        int headerHeight = 40;
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + headerHeight, paint);

        // Draw headers
        paint.setColor(Color.BLACK);
        paint.setTextSize(16);
        paint.setFakeBoldText(true);

        String headerProd = headerProductDisplay.getText().toString();
        String headerOld = headerOldRateDisplay.getText().toString();
        String headerNew = headerNewRateDisplay.getText().toString();

        float col1Width = (pageWidth - 2 * margin) * 0.5f;
        float col2Width = (pageWidth - 2 * margin) * 0.25f;
        float col3Width = (pageWidth - 2 * margin) * 0.25f;

        canvas.drawText(headerProd, margin + 10, currentY + 25, paint);
        canvas.drawText(headerOld, margin + col1Width + 10, currentY + 25, paint);
        canvas.drawText(headerNew, margin + col1Width + col2Width + 10, currentY + 25, paint);

        currentY += headerHeight;

        // Draw separator line
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(2);
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
        currentY += 10;

        // Draw items
        paint.setFakeBoldText(false);
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);

        for (int i = 0; i < adapter.getItemCount(); i++) {
            RateItem item = adapter.getItems().get(i);

            // Check if we need a new page
            if (currentY > pageHeight - 60) {
                break; // For simplicity, we'll just stop at page end
            }

            canvas.drawText(item.getProductName(), margin + 10, currentY + 20, paint);
            canvas.drawText(item.getOldRate(), margin + col1Width + 10, currentY + 20, paint);
            canvas.drawText(item.getNewRate(), margin + col1Width + col2Width + 10, currentY + 20, paint);

            currentY += 30;

            // Draw divider
            paint.setColor(Color.LTGRAY);
            paint.setStrokeWidth(1);
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
            paint.setColor(Color.BLACK);
            currentY += 5;
        }
    }

    // Simple TextWatcher helper class
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
}
