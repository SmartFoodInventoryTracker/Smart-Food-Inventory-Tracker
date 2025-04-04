package com.example.smartfoodinventorytracker.inventory;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.smartfoodinventorytracker.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.smartfoodinventorytracker.utils.AppConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import android.text.InputFilter;

public class ProductDetailsDialogFragment extends DialogFragment {

    private Product product;
    private ProductDialogListener listener;
    private String userId;

    public void setUserId(String userId) {
        this.userId = userId;
    }

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

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_shopping_product_details, null);

        EditText nameInput = view.findViewById(R.id.nameInput);
        EditText brandInput = view.findViewById(R.id.brandInput);
        EditText expiryInput = view.findViewById(R.id.expiryInput);
        EditText quantityInput = view.findViewById(R.id.quantityInput);
        ImageView calendarIcon = view.findViewById(R.id.calendarIcon);
        ImageView quantityPlus = view.findViewById(R.id.quantityPlus);
        ImageView quantityMinus = view.findViewById(R.id.quantityMinus);
        Button btnDone = view.findViewById(R.id.btnDone);
        Button btnCancel = view.findViewById(R.id.btnCancel);
        Button btnDelete = view.findViewById(R.id.btnDelete);

        nameInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(AppConstants.MAX_CHAR) });
        brandInput.setFilters(new InputFilter[] { new InputFilter.LengthFilter(AppConstants.MAX_CHAR) });


        // Pre-fill fields
        nameInput.setText(product.getName());
        // If the brand is "N/A", show an empty field instead
        if ("N/A".equals(product.getBrand())) {
            brandInput.setText("");
        } else {
            brandInput.setText(product.getBrand());
        }
        expiryInput.setText(product.getExpiryDate());
        quantityInput.setText(String.valueOf(product.getQuantity()));

        // Enable/disable name/brand fields based on barcode type
        nameInput.setEnabled(true);
        brandInput.setEnabled(true);

        // Show date picker when clicking input or calendar icon
        View.OnClickListener dateClickListener = v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(
                    requireContext(),
                    (datePicker, year, month, dayOfMonth) -> {
                        String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                        expiryInput.setText(selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            dialog.show();
        };
        expiryInput.setOnClickListener(dateClickListener);
        calendarIcon.setOnClickListener(dateClickListener);

        // Quantity adjustment
        quantityPlus.setOnClickListener(v -> {
            try {
                int qty = Integer.parseInt(quantityInput.getText().toString().trim());
                if (qty > 99) {
                    Toast.makeText(getContext(), "Maximum quantity is 99", Toast.LENGTH_SHORT).show();
                    return;
                }
                quantityInput.setText(String.valueOf(qty + 1));
            } catch (NumberFormatException e) {
                quantityInput.setText("1");
            }
        });

        quantityMinus.setOnClickListener(v -> {
            try {
                int qty = Integer.parseInt(quantityInput.getText().toString().trim());
                if (qty > 1) {
                    quantityInput.setText(String.valueOf(qty - 1));
                } else {
                    Toast.makeText(getContext(), "Minimum quantity is 1", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                quantityInput.setText("1");
            }
        });

        // Save changes
        btnDone.setOnClickListener(v -> {
            final String newName = nameInput.getText().toString().trim();

            // Convert an empty brand field to "N/A"
            String tempBrand = brandInput.getText().toString().trim();
            if (tempBrand.isEmpty()) {
                tempBrand = "N/A";
            }
            final String newBrand = tempBrand;

            final String newExpiry = expiryInput.getText().toString().trim();

            final int newQty;
            try {
                newQty = Integer.parseInt(quantityInput.getText().toString().trim());
                if (newQty <= 0) {
                    Toast.makeText(getContext(), "Quantity must be at least 1", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newQty > 99) {
                    Toast.makeText(getContext(), "Maximum quantity per item is 99", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid quantity", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newName.isEmpty()) {
                Toast.makeText(getContext(), "Product name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference inventoryRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("inventory_product");

            inventoryRef.get().addOnSuccessListener(snapshot -> {
                final int currentSize = (int) snapshot.getChildrenCount();
                if (!snapshot.hasChild(product.getBarcode()) && currentSize >= 99) {
                    Toast.makeText(getContext(), "Inventory limit reached (99 items max)", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create a temp product with updated fields
                Product updated = new Product(product.getBarcode(), newName, newBrand);
                updated.setExpiryDate(newExpiry);
                updated.setQuantity(newQty);
                updated.setDateAdded(getCurrentDate());

                boolean merged = false;

                for (DataSnapshot child : snapshot.getChildren()) {
                    Product other = child.getValue(Product.class);
                    if (other == null) continue;
                    if (other.getBarcode().equals(product.getBarcode())) continue;

                    if (productsMatch(other, updated)) {
                        int totalQty = other.getQuantity() + updated.getQuantity();
                        if (totalQty > 99) totalQty = 99;

                        other.setQuantity(totalQty);
                        other.setDateAdded(getCurrentDate());

                        inventoryRef.child(other.getBarcode()).setValue(other);
                        inventoryRef.child(product.getBarcode()).removeValue();

                        Toast.makeText(getContext(), "Merged with existing item", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onProductDeleted(product.getBarcode());
                        dismiss();
                        merged = true;
                        break;
                    }
                }

                if (!merged) {
                    // Just update this one
                    product.name = newName;
                    product.brand = newBrand;
                    product.setExpiryDate(newExpiry);
                    product.setQuantity(newQty);
                    product.setDateAdded(getCurrentDate());

                    inventoryRef.child(product.getBarcode()).setValue(product)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Product updated", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onProductUpdated(product);
                                dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
                }
            });
        });

        // Cancel = just close
        btnCancel.setOnClickListener(v -> dismiss());

        // Delete product
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(userId)
                                .child("inventory_product")
                                .child(product.getBarcode())
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                                    if (listener != null) listener.onProductDeleted(product.getBarcode());
                                    dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("No", null)
                    .show();
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

    private boolean productsMatch(Product a, Product b) {
        return a.getName().equalsIgnoreCase(b.getName()) &&
                a.getBrand().equalsIgnoreCase(b.getBrand()) &&
                a.getExpiryDate().equalsIgnoreCase(b.getExpiryDate());
    }

    private String getCurrentDate() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
        return today.format(formatter);
    }
}
