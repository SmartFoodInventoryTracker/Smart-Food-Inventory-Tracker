package com.example.smartfoodinventorytracker.shopping_list;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import java.util.List;
import java.util.ArrayList;


public class AddShoppingManualProductDialogFragment extends DialogFragment {

    private EditText nameInput, brandInput, quantityInput, expiryInput;
    private ImageView calendarIcon, quantityMinus, quantityPlus;
    private List<Product> existingProducts = new ArrayList<>();


    public interface ManualShoppingProductListener {
        void onProductAdded(Product product);
    }

    private ManualShoppingProductListener listener;
    private String userId;
    private static final int MAX_QUANTITY = 50;
    // Mode flag: false = Edit mode, true = Shopping mode.
    private boolean isShoppingMode = false;

    public void setManualProductListener(ManualShoppingProductListener listener) {
        this.listener = listener;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
    // Setter for mode.
    public void setShoppingMode(boolean shoppingMode) {
        this.isShoppingMode = shoppingMode;
    }

    public void setExistingProducts(List<Product> existingProducts) {
        this.existingProducts = existingProducts;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate the layout.
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_product, null);

        // Bind views.
        nameInput = view.findViewById(R.id.nameInput);
        brandInput = view.findViewById(R.id.brandInput);
        quantityInput = view.findViewById(R.id.quantityInput);
        expiryInput = view.findViewById(R.id.expiryInput);
        calendarIcon = view.findViewById(R.id.calendarIcon);
        quantityMinus = view.findViewById(R.id.quantityMinus);
        quantityPlus = view.findViewById(R.id.quantityPlus);
        Button btnDone = view.findViewById(R.id.btnDone);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        // Setup plus/minus listeners.
        quantityMinus.setOnClickListener(v -> {
            int currentQty = getSafeQuantity();
            if (currentQty > 1) {
                quantityInput.setText(String.valueOf(currentQty - 1));
            }
        });
        quantityPlus.setOnClickListener(v -> {
            int currentQty = getSafeQuantity();
            if (currentQty < MAX_QUANTITY) {
                quantityInput.setText(String.valueOf(currentQty + 1));
            } else {
                Toast.makeText(getContext(), "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
            }
        });

        // Configure expiry fields based on mode.
        if (isShoppingMode) {
            expiryInput.setVisibility(View.VISIBLE);
            calendarIcon.setVisibility(View.VISIBLE);
            expiryInput.setOnClickListener(v -> showDatePicker());
            calendarIcon.setOnClickListener(v -> showDatePicker());
        } else {
            expiryInput.setVisibility(View.GONE);
            calendarIcon.setVisibility(View.GONE);
        }

        btnDone.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String brand = brandInput.getText().toString().trim();
            int quantity = getSafeQuantity();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter product name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (brand.isEmpty()) brand = "N/A";
            if (quantity > MAX_QUANTITY) {
                Toast.makeText(getContext(), "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
                return;
            }

            String expiry;
            if (isShoppingMode) {
                expiry = expiryInput.getText().toString().trim();
                // In shopping mode, expiry is optional.
                if(expiry.isEmpty()){
                    expiry = "Not set";
                }
            } else {
                expiry = "Not set";
            }

            String barcode = "shopping_" + System.currentTimeMillis();
            Product product = new Product(barcode, name, brand);
            product.setQuantity(quantity);
            product.setExpiryDate(expiry);
            product.setDateAdded(getCurrentDate());

            for (Product existing : existingProducts) {
                if (existing.getName().trim().equalsIgnoreCase(name) &&
                        existing.getBrand().trim().equalsIgnoreCase(brand) &&
                        existing.getExpiryDate().trim().equalsIgnoreCase(expiry)) {
                    Toast.makeText(getContext(), "Product already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (listener != null) {
                listener.onProductAdded(product);
            }
            dismiss();

            dismiss();
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
        // Prevent past dates.
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
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}
