package com.example.smartfoodinventorytracker.shopping_list;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.example.smartfoodinventorytracker.inventory.CategoryUtils;

public class ShoppingProductDetailsDialogFragment extends DialogFragment {

    private Product product;
    private ShoppingProductDialogListener listener;
    private String userId;
    private static final int MAX_QUANTITY = 50;
    // New mode flag: false = Edit mode; true = Shopping mode.
    private boolean isShoppingMode = false;

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

    // Setter for mode
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
            product = (Product) getArguments().getSerializable("product");
        }
        // Inflate the layout defined in dialog_shopping_product_details.xml
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_shopping_product_details, null);

        // Bind views using IDs defined in the XML layout.
        EditText nameInput = view.findViewById(R.id.nameInput);
        EditText brandInput = view.findViewById(R.id.brandInput);
        EditText quantityInput = view.findViewById(R.id.quantityInput);
        // New: bind expiry input
        EditText expiryInput = view.findViewById(R.id.expiryInput);
        ImageView quantityPlus = view.findViewById(R.id.quantityPlus);
        ImageView quantityMinus = view.findViewById(R.id.quantityMinus);
        Button btnDone = view.findViewById(R.id.btnDone);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnDelete = view.findViewById(R.id.btnDelete);

        // Add an input filter to prevent typing "0" as the first character.
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

        // Populate the fields with current product data.
        nameInput.setText(product.getName());
        brandInput.setText(product.getBrand());
        quantityInput.setText(String.valueOf(product.getQuantity()));

        // Conditionally show or hide expiry field:
        if(isShoppingMode) {
            if(expiryInput != null) {
                expiryInput.setVisibility(View.VISIBLE);
                // Pre-fill expiry if available; otherwise leave blank.
                expiryInput.setText(product.getExpiryDate() != null && !product.getExpiryDate().equals("Not set") ? product.getExpiryDate() : "");
            }
        } else {
            if(expiryInput != null) {
                expiryInput.setVisibility(View.GONE);
            }
        }

        // Plus button increases the quantity by 1 (up to MAX_QUANTITY).
        quantityPlus.setOnClickListener(v -> {
            int qty = parseQuantity(quantityInput.getText().toString());
            if (qty >= MAX_QUANTITY) {
                Toast.makeText(getContext(), "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
                return;
            }
            quantityInput.setText(String.valueOf(qty + 1));
        });

        // Minus button decreases the quantity by 1 (if quantity is greater than 1).
        quantityMinus.setOnClickListener(v -> {
            int qty = parseQuantity(quantityInput.getText().toString());
            if (qty > 1) {
                quantityInput.setText(String.valueOf(qty - 1));
            }
        });

        // Done button updates the product details and notifies the listener.
        btnDone.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            String newBrand = brandInput.getText().toString().trim();
            int newQty = parseQuantity(quantityInput.getText().toString().trim());

            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Product name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newQty > MAX_QUANTITY) {
                Toast.makeText(getContext(), "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
                return;
            }

            product.name = newName;
            product.brand = newBrand;
            product.setQuantity(newQty);

            if(isShoppingMode) {
                String newExpiry = expiryInput.getText().toString().trim();
                if(newExpiry.isEmpty()) {
                    Toast.makeText(getContext(), "Please enter expiry date", Toast.LENGTH_SHORT).show();
                    return;
                }
                product.setExpiryDate(newExpiry);
            }
            // In Edit mode, expiry remains unchanged.

            if (listener != null) {
                listener.onProductUpdated(product);
            }
            dismiss();
        });

        // Cancel button dismisses the dialog.
        btnCancel.setOnClickListener(v -> dismiss());

        // Delete button: confirm deletion, then notify listener.
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        if (listener != null) {
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

    // Helper method to safely parse a quantity string to an integer.
    private int parseQuantity(String qtyStr) {
        try {
            return Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
