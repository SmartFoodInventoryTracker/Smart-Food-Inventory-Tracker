package com.example.smartfoodinventorytracker.shopping_list;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddShoppingManualProductDialogFragment extends DialogFragment {

    private EditText nameInput, brandInput, quantityInput, expiryInput;
    // The expiry-related views will be hidden unless in shopping mode.

    public interface ManualShoppingProductListener {
        void onProductAdded(Product product);
    }

    private ManualShoppingProductListener listener;
    private String userId;
    private static final int MAX_QUANTITY = 50;
    // New mode flag: false = Edit mode, true = Shopping mode.
    private boolean isShoppingMode = false;

    public void setManualProductListener(ManualShoppingProductListener listener) {
        this.listener = listener;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Setter for mode
    public void setShoppingMode(boolean shoppingMode) {
        this.isShoppingMode = shoppingMode;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate the layout for the manual product dialog.
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_manual_product, null);

        // Bind views
        nameInput = view.findViewById(R.id.nameInput);
        brandInput = view.findViewById(R.id.brandInput);
        quantityInput = view.findViewById(R.id.quantityInput);
        expiryInput = view.findViewById(R.id.expiryInput);

        // Conditionally show/hide expiry-related fields:
        View expiryLabel = view.findViewById(R.id.expiryLabel);
        View calendarIcon = view.findViewById(R.id.calendarIcon);
        if (isShoppingMode) {
            if(expiryLabel != null) {
                expiryLabel.setVisibility(View.VISIBLE);
            }
            if(expiryInput != null) {
                expiryInput.setVisibility(View.VISIBLE);
            }
            if(calendarIcon != null) {
                calendarIcon.setVisibility(View.VISIBLE);
                // Optionally, add a click listener to show a DatePicker.
            }
        } else {
            if(expiryLabel != null) {
                expiryLabel.setVisibility(View.GONE);
            }
            if(expiryInput != null) {
                expiryInput.setVisibility(View.GONE);
            }
            if(calendarIcon != null) {
                calendarIcon.setVisibility(View.GONE);
            }
        }

        Button btnDone = view.findViewById(R.id.btnDone);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnDone.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String brand = brandInput.getText().toString().trim();
            int quantity = getSafeQuantity();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Please enter product name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (brand.isEmpty()) {
                brand = "N/A";
            }
            if (quantity > MAX_QUANTITY) {
                Toast.makeText(getContext(), "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
                return;
            }

            String expiry;
            if(isShoppingMode) {
                expiry = expiryInput.getText().toString().trim();
                if(expiry.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter expiry date", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                expiry = "Not set";
            }

            // Generate a unique barcode for shopping products.
            String barcode = "shopping_" + System.currentTimeMillis();
            Product product = new Product(barcode, name, brand);
            product.setQuantity(quantity);
            product.setExpiryDate(expiry);
            product.setDateAdded(getCurrentDate());

            if (listener != null) {
                listener.onProductAdded(product);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    private int getSafeQuantity() {
        try {
            int qty = Integer.parseInt(quantityInput.getText().toString().trim());
            return qty > 0 ? qty : 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
}
