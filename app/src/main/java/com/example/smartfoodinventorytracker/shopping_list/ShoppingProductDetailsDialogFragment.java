package com.example.smartfoodinventorytracker.shopping_list;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.example.smartfoodinventorytracker.inventory.CategoryUtils;
import com.example.smartfoodinventorytracker.inventory.DateInfo;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ShoppingProductDetailsDialogFragment extends DialogFragment {

    private Product product;
    private ShoppingProductDialogListener listener;
    private String userId;
    private static final int MAX_QUANTITY = 50;
    // Mode flag: false = Edit mode; true = Shopping mode.
    private boolean isShoppingMode = false;
    List<Product> existingProducts = new ArrayList<>();
    private String listKey;

    public void setListKey(String listKey) {
        this.listKey = listKey;
    }


    public interface ShoppingProductDialogListener {
        void onProductUpdated(Product updatedProduct);
        void onProductDeleted(String barcode);
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setShoppingProductDialogListener(ShoppingProductDialogListener listener) {
        this.listener = listener;
    }

    // Setter for mode.
    public void setShoppingMode(boolean shoppingMode) {
        this.isShoppingMode = shoppingMode;
    }

    public static ShoppingProductDetailsDialogFragment newInstance(Product product) {
        ShoppingProductDetailsDialogFragment fragment = new ShoppingProductDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("product", product);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        if(getArguments() != null) {
            Product original = (Product) getArguments().getSerializable("product");
            product = new Product(original.getBarcode(), original.getName(), original.getBrand());
            product.setQuantity(original.getQuantity());
            product.setExpiryDate(original.getExpiryDate());
            product.setDateAdded(original.getDateAdded());

        }
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_shopping_product_details, null);

        // Bind views.
        EditText nameInput = view.findViewById(R.id.nameInput);
        EditText brandInput = view.findViewById(R.id.brandInput);
        EditText quantityInput = view.findViewById(R.id.quantityInput);
        // Bind expiry field.
        EditText expiryInput = view.findViewById(R.id.expiryInput);
        ImageView calendarIcon = view.findViewById(R.id.calendarIcon);
        ImageView quantityPlus = view.findViewById(R.id.quantityPlus);
        ImageView quantityMinus = view.findViewById(R.id.quantityMinus);
        Button btnDone = view.findViewById(R.id.btnDone);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnDelete = view.findViewById(R.id.btnDelete);

        // Prevent typing "0" as first character.
        quantityInput.setFilters(new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        if (dest.length() == 0 && source.toString().equals("0")) {
                            return "";
                        }
                        return null;
                    }
                }
        });

        // Populate fields.
        nameInput.setText(product.getName());
        brandInput.setText(product.getBrand());
        quantityInput.setText(String.valueOf(product.getQuantity()));

        // Setup expiry field based on mode.
        if (isShoppingMode) {
            if(expiryInput != null) {
                expiryInput.setVisibility(View.VISIBLE);
                if(product.getExpiryDate() != null && !product.getExpiryDate().equals("Not set")) {
                    expiryInput.setText(product.getExpiryDate());
                } else {
                    expiryInput.setText("");
                }
                // Set click listener to show DatePicker.
                expiryInput.setOnClickListener(v -> showDatePicker(expiryInput));
                // Optionally, set the same listener on the calendar icon.
                if(calendarIcon != null) {
                    calendarIcon.setVisibility(View.VISIBLE);
                    calendarIcon.setOnClickListener(v -> showDatePicker(expiryInput));
                }
            }
        } else {
            if(expiryInput != null) {
                expiryInput.setVisibility(View.GONE);
                View expiryLabel = view.findViewById(R.id.expiryLabel);
                View calendarIconView = view.findViewById(R.id.calendarIcon);
                if(expiryLabel != null) expiryLabel.setVisibility(View.GONE);
                if(calendarIconView != null) calendarIconView.setVisibility(View.GONE);
            }
        }

        // Plus button increases quantity.
        quantityPlus.setOnClickListener(v -> {
            int qty = parseQuantity(quantityInput.getText().toString());
            if(qty >= MAX_QUANTITY) {
                Toast.makeText(getContext(), "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
                return;
            }
            quantityInput.setText(String.valueOf(qty + 1));
        });

        // Minus button decreases quantity.
        quantityMinus.setOnClickListener(v -> {
            int qty = parseQuantity(quantityInput.getText().toString());
            if(qty > 1) {
                quantityInput.setText(String.valueOf(qty - 1));
            }
        });

        // Done button updates product details.
        btnDone.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newBrand = brandInput.getText().toString().trim();
            int newQty = parseQuantity(quantityInput.getText().toString().trim());

            if(newName.isEmpty()) {
                Toast.makeText(getContext(), "Product name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if(newQty > MAX_QUANTITY) {
                Toast.makeText(getContext(), "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
                return;
            }

            // Duplicate check (ignore current product's barcode)
            for (Product other : existingProducts) {
                if (!other.getBarcode().equals(product.getBarcode()) &&
                        other.getName().trim().equalsIgnoreCase(newName) &&
                        other.getBrand().trim().equalsIgnoreCase(newBrand) &&
                        other.getExpiryDate().trim().equalsIgnoreCase(
                                isShoppingMode ? expiryInput.getText().toString().trim() : product.getExpiryDate())
                ) {
                    Toast.makeText(getContext(), "A similar product already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Proceed to update
            product.name = newName;
            product.brand = newBrand;
            product.setQuantity(newQty);


            if(isShoppingMode) {
                String newExpiry = expiryInput.getText().toString().trim();
                // Now expiry is optional: if empty, default to "Not set"
                if(newExpiry.isEmpty()) {
                    newExpiry = "Not set";
                } else if(!DateInfo.isValidDateFormat(newExpiry)) {
                    Toast.makeText(getContext(), "Please enter a valid date (dd/MM/yyyy)", Toast.LENGTH_SHORT).show();
                    return;
                }
                product.setExpiryDate(newExpiry);
            }
            // In Edit mode, expiry remains unchanged.

            // Save to Firebase (Edit Mode only)
            if (!isShoppingMode && listKey != null) {
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userId)
                        .child("shopping-list")
                        .child(listKey)
                        .child("items")
                        .child(product.getBarcode())
                        .setValue(product)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Product updated", Toast.LENGTH_SHORT).show();
                            if (listener != null) listener.onProductUpdated(product);
                            dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update product", Toast.LENGTH_SHORT).show();
                        });
            } else {
                if (listener != null) listener.onProductUpdated(product);
                dismiss();
            }

        });

        // Cancel button dismisses dialog.
        btnCancel.setOnClickListener(v -> dismiss());

        // Delete button: confirm deletion.
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if(listener != null) {
                            listener.onProductDeleted(product.getBarcode());
                        }
                        dismiss();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    public void setExistingProducts(List<Product> products) {
        this.existingProducts = products;
    }

    // Helper method to safely parse quantity.
    private int parseQuantity(String qtyStr) {
        try {
            return Integer.parseInt(qtyStr);
        } catch(NumberFormatException e) {
            return 1;
        }
    }

    private void showDatePicker(EditText expiryInput) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    expiryInput.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        // Prevent selecting past dates.
        datePicker.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePicker.show();
    }
}
