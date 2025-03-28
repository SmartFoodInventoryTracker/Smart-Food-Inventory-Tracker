package com.example.smartfoodinventorytracker.shopping_list;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.example.smartfoodinventorytracker.inventory.CategoryUtils;
import com.google.firebase.database.FirebaseDatabase;

public class ShoppingProductDetailsDialogFragment extends DialogFragment {

    private Product product;
    private ShoppingProductDialogListener listener;
    private String userId;

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
        if (getArguments() != null) {
            product = (Product) getArguments().getSerializable("product");
        }
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_shopping_product_details, null);

        // UI references
        TextView nameText = view.findViewById(R.id.nameText);
        TextView brandText = view.findViewById(R.id.brandText);
        EditText quantityText = view.findViewById(R.id.quantityText);
        TextView quantityBadge = view.findViewById(R.id.quantityBadge);
        ImageView productImage = view.findViewById(R.id.productImageDialog);
        Button saveBtn = view.findViewById(R.id.saveButton);
        Button deleteBtn = view.findViewById(R.id.deleteButton);
        Button cancelBtn = view.findViewById(R.id.cancelButton);

        // Populate read-only fields
        nameText.setText(product.getName());
        brandText.setText(product.getBrand());
        nameText.setEnabled(false);
        brandText.setEnabled(false);

        // Populate quantity field
        quantityText.setText(String.valueOf(product.getQuantity()));
        quantityBadge.setText("Qty: " + product.getQuantity());

        // Set product image using CategoryUtils
        int iconResId = CategoryUtils.getCategoryIcon(product.getName());
        productImage.setImageResource(iconResId);

        // Save: update only the quantity
        saveBtn.setOnClickListener(v -> {
            try {
                int newQty = Integer.parseInt(quantityText.getText().toString().trim());
                product.setQuantity(newQty);
                // Optionally update Firebase here, or notify the listener for update.
                if (listener != null) {
                    listener.onProductUpdated(product);
                }
                dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
            }
        });

        // Delete: confirm deletion then notify listener
        deleteBtn.setOnClickListener(v -> {
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

        cancelBtn.setOnClickListener(v -> dismiss());

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }
}
