package com.example.labelmaker;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RateListFragment extends Fragment {

    // UI Components
    private TextInputEditText headerProduct, headerOldRate, headerNewRate;
    private TextView headerProductDisplay, headerOldRateDisplay, headerNewRateDisplay;
    private TextView emptyMessage;
    private RecyclerView rateListRecycler;
    private LinearLayout rateListContainer;
    private MaterialButton btnExportPdf, btnExportPng;
    private ExtendedFloatingActionButton fabAddItem;

    // Data
    private RateListAdapter adapter;
    
    // Background execution
    private ExecutorService executorService;
    private Handler mainThreadHandler;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> createPdfLauncher;
    private ActivityResultLauncher<Intent> createPngLauncher;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rate_list, container, false);
        
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());
        
        registerResultLaunchers();
        initializeViews(view);
        setupRecyclerView();
        setupListeners();
        return view;
    }

    private void registerResultLaunchers() {
        createPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        savePdfBackground(uri);
                    }
                }
            }
        );
        
        createPngLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        savePngBackground(uri);
                    }
                }
            }
        );
    }

    private void initializeViews(View view) {
        headerProduct = view.findViewById(R.id.header_product);
        headerOldRate = view.findViewById(R.id.header_old_rate);
        headerNewRate = view.findViewById(R.id.header_new_rate);

        headerProductDisplay = view.findViewById(R.id.header_product_display);
        headerOldRateDisplay = view.findViewById(R.id.header_old_rate_display);
        headerNewRateDisplay = view.findViewById(R.id.header_new_rate_display);

        emptyMessage = view.findViewById(R.id.empty_message);
        rateListRecycler = view.findViewById(R.id.rate_list_recycler);
        rateListContainer = view.findViewById(R.id.rate_list_container);

        btnExportPdf = view.findViewById(R.id.btn_export_pdf);
        btnExportPng = view.findViewById(R.id.btn_export_png);
        fabAddItem = view.findViewById(R.id.fab_add_item);
    }

    private void setupRecyclerView() {
        adapter = new RateListAdapter();
        rateListRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        rateListRecycler.setAdapter(adapter);
    }

    private void setupListeners() {
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

        fabAddItem.setOnClickListener(v -> showAddItemDialog());
        btnExportPdf.setOnClickListener(v -> exportPdf());
        btnExportPng.setOnClickListener(v -> exportPng());
    }

    private void showAddItemDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_add_rate_item, null);
        
        TextInputEditText inputProductName = bottomSheetView.findViewById(R.id.input_product_name);
        TextInputEditText inputOldRate = bottomSheetView.findViewById(R.id.input_old_rate);
        TextInputEditText inputNewRate = bottomSheetView.findViewById(R.id.input_new_rate);
        MaterialButton btnSave = bottomSheetView.findViewById(R.id.btn_save_item);
        
        btnSave.setOnClickListener(v -> {
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

            if (adapter.getItemCount() > 0) {
                emptyMessage.setVisibility(View.GONE);
            }

            Toast.makeText(requireContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialog.setContentView(bottomSheetView);
        dialog.show();
    }

    private void exportPdf() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(requireContext(), "Add items to the list first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "rate_list_" + System.currentTimeMillis() + ".pdf");
        createPdfLauncher.launch(intent);
    }

    private void exportPng() {
        if (adapter.getItemCount() == 0) {
            Toast.makeText(requireContext(), "Add items to the list first", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, "rate_list_" + System.currentTimeMillis() + ".png");
        createPngLauncher.launch(intent);
    }
    
    private void savePdfBackground(Uri uri) {
         Toast.makeText(requireContext(), "Exporting PDF in background...", Toast.LENGTH_SHORT).show();
         
         executorService.execute(() -> {
            try {
                // A4 size in points (1/72 inch)
                int pageWidth = 595;  // 8.27 inches * 72
                int pageHeight = 842; // 11.69 inches * 72

                PdfDocument pdfDocument = new PdfDocument();
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                
                Canvas canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);
                
                drawRateListToCanvas(canvas, pageWidth, pageHeight);
                pdfDocument.finishPage(page);
                
                OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                pdfDocument.writeTo(outputStream);
                outputStream.close();
                pdfDocument.close();
                
                mainThreadHandler.post(() -> {
                     Toast.makeText(requireContext(), "PDF saved successfully", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                 e.printStackTrace();
                 mainThreadHandler.post(() -> {
                     Toast.makeText(requireContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
                 });
            }
         });
    }

    private void savePngBackground(Uri uri) {
        Toast.makeText(requireContext(), "Exporting PNG in background...", Toast.LENGTH_SHORT).show();
        
        executorService.execute(() -> {
             try {
                // Create bitmap from the rate list container in main thread context but drawn in background
                Bitmap bitmap = Bitmap.createBitmap(rateListContainer.getWidth(), rateListContainer.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);
                
                mainThreadHandler.post(() -> {
                    rateListContainer.draw(canvas);
                    
                    // Proceed to save in background after drawing
                    executorService.execute(() -> {
                         try {
                             OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
                             bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                             outputStream.close();
                             
                             mainThreadHandler.post(() -> {
                                 Toast.makeText(requireContext(), "PNG saved successfully", Toast.LENGTH_LONG).show();
                             });
                         } catch (Exception e) {
                             e.printStackTrace();
                             mainThreadHandler.post(() -> {
                                 Toast.makeText(requireContext(), "Error saving PNG: " + e.getMessage(), Toast.LENGTH_LONG).show();
                             });
                         }
                    });
                });
             } catch (Exception e) {
                 e.printStackTrace();
                 mainThreadHandler.post(() -> {
                     Toast.makeText(requireContext(), "Error rendering PNG: " + e.getMessage(), Toast.LENGTH_LONG).show();
                 });
             }
        });
    }

    private void drawRateListToCanvas(Canvas canvas, int pageWidth, int pageHeight) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int margin = 40;
        int currentY = margin;

        paint.setTextSize(24);
        paint.setColor(Color.BLACK);
        paint.setFakeBoldText(true);
        String title = "Rate List";
        canvas.drawText(title, margin, currentY, paint);
        currentY += 40;

        paint.setColor(Color.parseColor("#E8DEF8"));
        int headerHeight = 40;
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + headerHeight, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(16);
        paint.setFakeBoldText(true);

        String headerProd = headerProductDisplay.getText().toString();
        String headerOld = headerOldRateDisplay.getText().toString();
        String headerNew = headerNewRateDisplay.getText().toString();

        float col1Width = (pageWidth - 2 * margin) * 0.5f;
        float col2Width = (pageWidth - 2 * margin) * 0.25f;

        canvas.drawText(headerProd, margin + 10, currentY + 25, paint);
        canvas.drawText(headerOld, margin + col1Width + 10, currentY + 25, paint);
        canvas.drawText(headerNew, margin + col1Width + col2Width + 10, currentY + 25, paint);

        currentY += headerHeight;

        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(2);
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
        currentY += 10;

        paint.setFakeBoldText(false);
        paint.setTextSize(14);
        paint.setColor(Color.BLACK);

        for (int i = 0; i < adapter.getItemCount(); i++) {
            RateItem item = adapter.getItems().get(i);

            if (currentY > pageHeight - 60) {
                break;
            }

            canvas.drawText(item.getProductName(), margin + 10, currentY + 20, paint);
            canvas.drawText(item.getOldRate(), margin + col1Width + 10, currentY + 20, paint);
            canvas.drawText(item.getNewRate(), margin + col1Width + col2Width + 10, currentY + 20, paint);

            currentY += 30;

            paint.setColor(Color.LTGRAY);
            paint.setStrokeWidth(1);
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint);
            paint.setColor(Color.BLACK);
            currentY += 5;
        }
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
}
