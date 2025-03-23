package com.example.smartfoodinventorytracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private Context context;
    private List<Product> itemList;
    private List<Product> originalList;
    private DatabaseReference databaseReference;
    public Sorting sorting = Sorting.NONE;
    final java.util.Set<String> recentlyDeletedBarcodes = new java.util.HashSet<>();
    public enum Sorting
    {
        EXP_DATE_ASC,
        EXP_DATE_DES,
        DATE_ADD_ASC,
        DATE_ADD_DES,
        NONE

    }
    public InventoryAdapter(InventoryActivity inventoryActivity, List<Product> itemList) {
        this.context = inventoryActivity;
        this.itemList = new ArrayList<>(itemList); // Current displayed list
        this.originalList = new ArrayList<>(itemList); // Full original list
        this.databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product"); // ✅ Connect to Firebase
    }

    public void updateList(List<Product> newList) {
        itemList.clear();
        itemList.addAll(newList);
        originalList.clear();
        originalList.addAll(newList);
        notifyDataSetChanged();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, barcode, DateAdded_h;
        ImageView productImage;
        TextView quantityBadge;
        TextView expiryBadge;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            barcode = itemView.findViewById(R.id.productBarcode);
            expiryBadge = itemView.findViewById(R.id.expiryBadge);
            DateAdded_h = itemView.findViewById(R.id.prodcutDateAdded);
            productImage = itemView.findViewById(R.id.productImage);
            quantityBadge = itemView.findViewById(R.id.quantityBadge);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = itemList.get(position);
        holder.name.setText(product.getName());
        holder.brand.setText("Brand: " + product.getBrand());

        // ✅ Show Expiry Date
        holder.expiryDate.setText(product.getExpiryDate() == null || product.getExpiryDate().isEmpty()
                ? "Expiry Date: Not set"
                : "Expiry Date: " + product.getExpiryDate());

        // ✅ Show Correct "Date Added"
        holder.DateAdded_h.setText(product.getDateAdded() == null || product.getDateAdded().isEmpty()
                ? "Date Added: Not set"
                : "Date Added: " + product.getDateAdded());

        Context context = holder.itemView.getContext();

        int quantity = product.getQuantity();
        holder.quantityBadge.setText("Qty: " + quantity);
        holder.quantityBadge.setVisibility(View.VISIBLE);

        int badgeColor;
                if (quantity >= 5) {
            badgeColor = ContextCompat.getColor(context, R.color.green);
        } else if (quantity >= 2) {
            badgeColor = ContextCompat.getColor(context, R.color.orange);
        } else {
            badgeColor = ContextCompat.getColor(context, R.color.red);
        }

        holder.quantityBadge.setBackgroundTintList(ColorStateList.valueOf(badgeColor));


        holder.itemView.setOnClickListener(v -> {
            FragmentManager fm = ((FragmentActivity) v.getContext()).getSupportFragmentManager();
            ProductDetailsDialogFragment dialog = ProductDetailsDialogFragment.newInstance(product);

            dialog.setProductDialogListener(new ProductDetailsDialogFragment.ProductDialogListener() {
                @Override
                public void onProductUpdated(Product updatedProduct) {
                    int currentPos = holder.getAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION) {
                        itemList.set(currentPos, updatedProduct);
                        int indexInOriginal = originalList.indexOf(product);
                        if (indexInOriginal != -1) {
                            originalList.set(indexInOriginal, updatedProduct);
                        }
                        notifyItemChanged(currentPos);
                    }
                }

                @Override
                public void onProductDeleted(String barcode) {
                    removeProductByBarcode(barcode);
                }

            });

            dialog.show(fm, "ProductDetailsDialog");
        });



        int iconResId = CategoryUtils.getCategoryIcon(product.getName());
        holder.productImage.setImageResource(iconResId);


        holder.itemView.setOnLongClickListener(v -> {
            if (product.getQuantity() > 1) {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Multiple Quantities")
                        .setMessage("This product has a quantity of " + product.getQuantity() + ". Do you want to delete one or all?")
                        .setPositiveButton("Delete One", (dialog, which) -> {
                            product.setQuantity(product.getQuantity() - 1);

                            databaseReference.child(product.getBarcode())
                                    .setValue(product)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(v.getContext(), "One item removed", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(v.getContext(), "Failed to update", Toast.LENGTH_SHORT).show());
                        })


                        .setNegativeButton("Delete All", (dialog, which) -> {
                            databaseReference.child(product.getBarcode())
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        int itemPosition = holder.getAdapterPosition();
                                        if (itemPosition != RecyclerView.NO_POSITION) {
                                            itemList.remove(itemPosition);
                                            notifyItemRemoved(itemPosition);
                                        }
                                        Toast.makeText(v.getContext(), "Product removed!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(v.getContext(), "Failed to remove", Toast.LENGTH_SHORT).show());
                        })
                        .setNeutralButton("Cancel", null)
                        .show();
            } else {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Delete Product")
                        .setMessage("Are you sure you want to remove this product?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            databaseReference.child(product.getBarcode())
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        int itemPosition = holder.getAdapterPosition();
                                        if (itemPosition != RecyclerView.NO_POSITION) {
                                            itemList.remove(itemPosition);
                                            notifyItemRemoved(itemPosition);
                                        }
                                        Toast.makeText(v.getContext(), "Product removed!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(v.getContext(), "Failed to remove", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
            return true;
        });



    }

    private void removeProductByBarcode(String barcode) {
        Log.d("InventoryAdapter", "Product deleted: " + barcode);
        Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show();
    }


    private void showDatePicker(ViewHolder holder, Product product) {
        Context context = holder.itemView.getContext();

        // ✅ Create a Date Picker Dialog
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;

            product.setExpiryDate(selectedDate); // ✅ Update local object

            holder.expiryDate.setText("Expiry Date: " + selectedDate); // ✅ Update UI instantly

            // ✅ Save new expiry date to Firebase
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product");
            databaseReference.child(product.getBarcode()).child("expiryDate").setValue(selectedDate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Expiry date updated!", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged(); // ✅ Refresh RecyclerView immediately
                    });

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    public void filter(String query) {
        itemList.clear();
        Log.d("Search", "Filtering for: " + query);  // ✅ Debugging log
        Log.d("Search", "Original list size: " + originalList.size());  // ✅ Check if originalList has data

        if (query.isEmpty()) {
            itemList.addAll(originalList);  // Reset to full list
        } else {
            for (Product product : originalList) {
                if (product.getName().toLowerCase().contains(query.toLowerCase())) {
                    itemList.add(product);
                }
            }
        }

        Log.d("Search", "Items after filtering: " + itemList.size()); // ✅ Log filtered items
        notifyDataSetChanged();
    }


    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
