package com.example.smartfoodinventorytracker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;

public class ProductDetailsDialogFragment extends DialogFragment {

    private Product product;
    private ProductDialogListener listener;

    public interface ProductDialogListener {
        void onProductUpdated(Product updatedProduct);
        void onProductDeleted(String barcode);
    }

    public void setProductDialogListener(ProductDialogListener listener) {
        this.listener = listener;
    }

    public static ProductDetailsDialogFragment newInstance(Product product) {
        ProductDetailsDialogFragment fragment = new ProductDetailsDialogFragment();
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

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_product_details, null);

        ImageView productImage = view.findViewById(R.id.productImageDialog);
        EditText name = view.findViewById(R.id.nameText);
        EditText brand = view.findViewById(R.id.brandText);
        EditText expiry = view.findViewById(R.id.expiryText);
        EditText quantity = view.findViewById(R.id.quantityText);

        LinearLayout saveCancelRow = view.findViewById(R.id.saveCancelRow);
        Button editBtn = view.findViewById(R.id.editButton);
        Button deleteBtn = view.findViewById(R.id.deleteButton);
        Button closeBtn = view.findViewById(R.id.closeButton);
        Button saveBtn = view.findViewById(R.id.saveButton);
        Button cancelEditBtn = view.findViewById(R.id.cancelEditButton);

        // Set initial data
        name.setText(product.getName());
        brand.setText(product.getBrand());
        expiry.setText(product.getExpiryDate());
        quantity.setText(String.valueOf(product.getQuantity()));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .into(productImage);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }

        editBtn.setOnClickListener(v -> {
            name.setEnabled(true);
            brand.setEnabled(true);
            expiry.setEnabled(true);
            quantity.setEnabled(true);

            saveCancelRow.setVisibility(View.VISIBLE);
            editBtn.setVisibility(View.GONE);
            deleteBtn.setVisibility(View.GONE);
            closeBtn.setVisibility(View.GONE);
        });

        cancelEditBtn.setOnClickListener(v -> {
            name.setText(product.getName());
            brand.setText(product.getBrand());
            expiry.setText(product.getExpiryDate());
            quantity.setText(String.valueOf(product.getQuantity()));

            name.setEnabled(false);
            brand.setEnabled(false);
            expiry.setEnabled(false);
            quantity.setEnabled(false);

            saveCancelRow.setVisibility(View.GONE);
            editBtn.setVisibility(View.VISIBLE);
            deleteBtn.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.VISIBLE);
        });

        saveBtn.setOnClickListener(v -> {
            String newName = name.getText().toString().trim();
            String newBrand = brand.getText().toString().trim();
            String newExpiry = expiry.getText().toString().trim();
            int newQuantity = Integer.parseInt(quantity.getText().toString().trim());

            product.name = newName;
            product.brand = newBrand;
            product.setExpiryDate(newExpiry);
            product.setQuantity(newQuantity);

            FirebaseDatabase.getInstance().getReference("inventory_product")
                    .child(product.getBarcode())
                    .setValue(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Product updated", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onProductUpdated(product); // ✅ notify
                        dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
        });

        expiry.setOnClickListener(v -> {
            if (!expiry.isEnabled()) return;

            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        expiry.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseDatabase.getInstance().getReference("inventory_product")
                                .child(product.getBarcode())
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                                    if (listener != null) listener.onProductDeleted(product.getBarcode()); // ✅ notify
                                    dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        closeBtn.setOnClickListener(v -> dismiss());

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }
}
