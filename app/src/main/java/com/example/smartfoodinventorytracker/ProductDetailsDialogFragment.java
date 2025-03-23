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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

        // 🧩 UI refs
        ImageView productImage = view.findViewById(R.id.productImageDialog);
        TextView name = view.findViewById(R.id.nameText);
        TextView brand = view.findViewById(R.id.brandText);
        TextView expiryBadge = view.findViewById(R.id.expiryBadge);
        TextView quantityBadge = view.findViewById(R.id.quantityBadge);
        EditText expiryText = view.findViewById(R.id.expiryText);
        EditText quantityText = view.findViewById(R.id.quantityText);
        ImageView quantityMinus = view.findViewById(R.id.quantityMinus);
        ImageView quantityPlus = view.findViewById(R.id.quantityPlus);

        LinearLayout readOnlyContainer = view.findViewById(R.id.readOnlyContainer);
        LinearLayout editableContainer = view.findViewById(R.id.editableContainer);
        LinearLayout saveCancelRow = view.findViewById(R.id.saveCancelRow);
        LinearLayout buttonRow = view.findViewById(R.id.buttonRow);

        Button editBtn = view.findViewById(R.id.editButton);
        Button deleteBtn = view.findViewById(R.id.deleteButton);
        Button saveBtn = view.findViewById(R.id.saveButton);
        Button cancelEditBtn = view.findViewById(R.id.cancelEditButton);

        ImageView expiryCalendarIcon = view.findViewById(R.id.expiryCalendarIcon);

        // Load data
        name.setText(product.getName());
        brand.setText(product.getBrand());
        expiryText.setText(product.getExpiryDate());
        quantityText.setText(String.valueOf(product.getQuantity()));
        quantityBadge.setText("Qty: " + product.getQuantity());
        productImage.setImageResource(CategoryUtils.getCategoryIcon(product.getName()));

        // Expiry badge
        String expiryLabel = getExpiryText(product.getExpiryDate());
        expiryBadge.setText(expiryLabel);

        int expiryColor = getExpiryColor(product.getExpiryDate());
        expiryBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(expiryColor));
        expiryBadge.setVisibility(View.VISIBLE);

        // Quantity badge
        int qtyColor = getQuantityColor(product.getQuantity());
        quantityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(qtyColor));
        quantityBadge.setVisibility(View.VISIBLE);

        // Calendar picker logic (shared)
        View.OnClickListener dateClickListener = v -> {
            if (!expiryText.isEnabled()) return;

            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view1, year, month, dayOfMonth) -> {
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        expiryText.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        };
        expiryText.setOnClickListener(dateClickListener);
        expiryCalendarIcon.setOnClickListener(dateClickListener);

        // Enable edit mode
        editBtn.setOnClickListener(v -> {
            boolean isManual = product.getBarcode() == null || product.getBarcode().startsWith("manual_");
            name.setEnabled(isManual);
            brand.setEnabled(isManual);
            expiryText.setEnabled(true);
            quantityText.setEnabled(true);

            editableContainer.setVisibility(View.VISIBLE);
            readOnlyContainer.setVisibility(View.GONE);
            saveCancelRow.setVisibility(View.VISIBLE);
            buttonRow.setVisibility(View.GONE);
        });

        // Cancel edit mode
        cancelEditBtn.setOnClickListener(v -> {
            name.setText(product.getName());
            brand.setText(product.getBrand());
            expiryText.setText(product.getExpiryDate());
            quantityText.setText(String.valueOf(product.getQuantity()));

            name.setEnabled(false);
            brand.setEnabled(false);
            expiryText.setEnabled(false);
            quantityText.setEnabled(false);

            editableContainer.setVisibility(View.GONE);
            readOnlyContainer.setVisibility(View.VISIBLE);
            saveCancelRow.setVisibility(View.GONE);
            buttonRow.setVisibility(View.VISIBLE);
        });

        saveBtn.setOnClickListener(v -> {
            String newExpiry = expiryText.getText().toString().trim();
            int newQty = Integer.parseInt(quantityText.getText().toString().trim());

            product.setExpiryDate(newExpiry);
            product.setQuantity(newQty);

            // 🔁 Update UI badges before closing
            expiryBadge.setText(getExpiryText(newExpiry));
            expiryBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getExpiryColor(newExpiry)));

            quantityBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getQuantityColor(newQty)));



            FirebaseDatabase.getInstance().getReference("inventory_product")
                    .child(product.getBarcode())
                    .setValue(product)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Product updated", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onProductUpdated(product);
                        dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
        });

        quantityMinus.setOnClickListener(v -> {
            try {
                int qty = Integer.parseInt(quantityText.getText().toString().trim());
                if (qty > 1) {
                    quantityText.setText(String.valueOf(qty - 1));
                }
            } catch (NumberFormatException e) {
                quantityText.setText("1"); // fallback
            }
        });

        quantityPlus.setOnClickListener(v -> {
            try {
                int qty = Integer.parseInt(quantityText.getText().toString().trim());
                quantityText.setText(String.valueOf(qty + 1));
            } catch (NumberFormatException e) {
                quantityText.setText("1"); // fallback
            }
        });

        deleteBtn.setOnClickListener(v -> {
            if (product.getQuantity() > 1) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Multiple Quantities")
                        .setMessage("This product has a quantity of " + product.getQuantity() + ". Do you want to delete one or all?")
                        .setPositiveButton("Delete One", (dialog, which) -> {
                            product.setQuantity(product.getQuantity() - 1);
                            FirebaseDatabase.getInstance().getReference("inventory_product")
                                    .child(product.getBarcode())
                                    .setValue(product)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "One item removed", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Delete All", (dialog, which) -> {
                            FirebaseDatabase.getInstance().getReference("inventory_product")
                                    .child(product.getBarcode())
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
                        })
                        .setNeutralButton("Cancel", null)
                        .show();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Product")
                        .setMessage("Are you sure you want to delete this product?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            FirebaseDatabase.getInstance().getReference("inventory_product")
                                    .child(product.getBarcode())
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();
    }

    // 🔍 Expiry label
    private String getExpiryText(String expiryDate) {
        if (expiryDate == null || expiryDate.equals("Not set")) return "No expiry set";

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            LocalDate exp = LocalDate.parse(expiryDate, formatter);
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, exp);

            if (days < 0) return "Expired";
            else if (days == 0) return "Expires today";
            else if (days <= 14) return "Expires in " + days + " day" + (days > 1 ? "s" : "");
            else if (days < 30) return "Expires in " + (days / 7) + " week" + (days >= 14 ? "s" : "");
            else return "Expires in 1 month+";
        } catch (Exception e) {
            return "Invalid date";
        }
    }

    private int getExpiryColor(String expiryDate) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            LocalDate exp = LocalDate.parse(expiryDate, formatter);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), exp);

            if (days < 0) return ContextCompat.getColor(requireContext(), R.color.red);
            else if (days < 3) return ContextCompat.getColor(requireContext(), R.color.orange);
            else if (days < 5) return ContextCompat.getColor(requireContext(), R.color.yellow);
            else return ContextCompat.getColor(requireContext(), R.color.green);
        } catch (Exception e) {
            return ContextCompat.getColor(requireContext(), android.R.color.darker_gray);
        }
    }

    private int getQuantityColor(int quantity) {
        if (quantity >= 5) return ContextCompat.getColor(requireContext(), R.color.green);
        else if (quantity >= 2) return ContextCompat.getColor(requireContext(), R.color.orange);
        else return ContextCompat.getColor(requireContext(), R.color.red);
    }
}
