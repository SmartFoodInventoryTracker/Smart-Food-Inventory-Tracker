package com.example.smartfoodinventorytracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<Product> itemList;
    private List<Product> originalList;
    private DatabaseReference databaseReference;
    public Sorting sorting = Sorting.NONE;
    public enum Sorting
    {
        EXP_DATE_ASC,
        EXP_DATE_DES,
        DATE_ADD_ASC,
        DATE_ADD_DES,
        NONE

    }
    public InventoryAdapter(List<Product> itemList) {
        this.itemList = new ArrayList<>(itemList); // Current displayed list
        this.originalList = new ArrayList<>(itemList); // Full original list
        this.databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product"); // ✅ Connect to Firebase
    }

    public void updateList(List<Product> newList) {
        Log.d("Adapter", "Updating list with " + newList.size() + " items"); // ✅ Debugging log

        itemList.clear();
        itemList.addAll(newList);

        originalList.clear();  // ✅ Ensure original list is updated
        originalList.addAll(newList);

        notifyDataSetChanged();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, barcode, expiryDate, DateAdded_h;
        ImageView productImage;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            barcode = itemView.findViewById(R.id.productBarcode);
            expiryDate = itemView.findViewById(R.id.productExpiryDate);
            DateAdded_h = itemView.findViewById(R.id.prodcutDateAdded);
            productImage = itemView.findViewById(R.id.productImage);
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

        // ✅ Open Date Picker when item is clicked
        holder.itemView.setOnClickListener(v -> showDatePicker(holder, product));

        String imageUrl = product.getImageUrl();

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Log.d("GlideImage", "Loading image from: " + imageUrl);
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.productImage);
        } else {
            Log.w("GlideImage", "Image URL is null or empty, using placeholder.");
            holder.productImage.setImageResource(R.drawable.placeholder_image);
        }




        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to remove this product?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        String barcodeToDelete = product.getBarcode(); // ✅ Store the barcode before deletion

                        // ✅ Delete from Firebase first
                        databaseReference.child(barcodeToDelete).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    // ✅ Find the correct index after Firebase confirms deletion
                                    int itemPosition = holder.getAdapterPosition();
                                    if (itemPosition != RecyclerView.NO_POSITION) {
                                        itemList.remove(itemPosition);
                                        notifyItemRemoved(itemPosition);
                                    }
                                    Toast.makeText(v.getContext(), "Product removed!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(v.getContext(), "Failed to remove", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
            return true; // ✅ Event is handled
        });


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
