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

    private EditText nameInput, brandInput, quantityInput;
    // The expiry-related views will be hidden.
    // Ensure your XML assigns an ID (e.g., expiryLabel) to the expiry TextView.

    public interface ManualShoppingProductListener {
        void onProductAdded(Product product);
    }

    private ManualShoppingProductListener listener;
    private String userId;
    private static final int MAX_QUANTITY = 50;

    public void setManualProductListener(ManualShoppingProductListener listener) {
        this.listener = listener;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

        // Hide expiry-related fields:
        // Hide the expiry label (ensure your XML TextView has android:id="@+id/expiryLabel")
        View expiryLabel = view.findViewById(R.id.expiryLabel);
        if(expiryLabel != null) {
            expiryLabel.setVisibility(View.GONE);
        }
        // Hide the expiry input
        View expiryInput = view.findViewById(R.id.expiryInput);
        if(expiryInput != null) {
            expiryInput.setVisibility(View.GONE);
        }
        // Hide the calendar icon, if present
        View calendarIcon = view.findViewById(R.id.calendarIcon);
        if(calendarIcon != null) {
            calendarIcon.setVisibility(View.GONE);
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

            // We don't collect expiry here, so set it to "Not set"
            String expiry = "Not set";

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
