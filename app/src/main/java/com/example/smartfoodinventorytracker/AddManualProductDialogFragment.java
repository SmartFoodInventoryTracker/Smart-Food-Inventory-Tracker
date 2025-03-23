package com.example.smartfoodinventorytracker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class AddManualProductDialogFragment extends DialogFragment {

    private EditText nameInput, brandInput, expiryInput, quantityInput;
    private ImageView calendarIcon, quantityMinus, quantityPlus;

    public interface ManualProductListener {
        void onProductAdded(Product product);
    }

    private ManualProductListener listener;

    public void setManualProductListener(ManualProductListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_product, null);

        nameInput = view.findViewById(R.id.nameInput);
        brandInput = view.findViewById(R.id.brandInput);
        expiryInput = view.findViewById(R.id.expiryInput);
        quantityInput = view.findViewById(R.id.quantityInput);
        calendarIcon = view.findViewById(R.id.calendarIcon);
        quantityMinus = view.findViewById(R.id.quantityMinus);
        quantityPlus = view.findViewById(R.id.quantityPlus);
        Button btnDone = view.findViewById(R.id.btnDone);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        expiryInput.setOnClickListener(v -> showDatePicker());
        calendarIcon.setOnClickListener(v -> showDatePicker());

        quantityMinus.setOnClickListener(v -> {
            int currentQty = getSafeQuantity();
            if (currentQty > 1) {
                quantityInput.setText(String.valueOf(currentQty - 1));
            }
        });

        quantityPlus.setOnClickListener(v -> {
            int currentQty = getSafeQuantity();
            quantityInput.setText(String.valueOf(currentQty + 1));
        });

        btnDone.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String brand = brandInput.getText().toString().trim();
            String expiry = expiryInput.getText().toString().trim();
            int quantity = getSafeQuantity();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter product name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (brand.isEmpty()) brand = "N/A";
            if (expiry.isEmpty()) expiry = "Not set";

            String barcode = "manual_" + System.currentTimeMillis();
            Product product = new Product(barcode, name, brand);
            product.setQuantity(quantity);
            product.setExpiryDate(expiry);
            product.setDateAdded(getCurrentDate());

            FirebaseDatabase.getInstance()
                    .getReference("inventory_product")
                    .child(product.getBarcode())
                    .setValue(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Product added!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onProductAdded(product);
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to add product", Toast.LENGTH_SHORT).show();
                    });
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    expiryInput.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        // ⛔ Prevent selecting past dates
        datePicker.getDatePicker().setMinDate(calendar.getTimeInMillis());

        datePicker.show();
    }


    private int getSafeQuantity() {
        try {
            return Integer.parseInt(quantityInput.getText().toString().trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }


}