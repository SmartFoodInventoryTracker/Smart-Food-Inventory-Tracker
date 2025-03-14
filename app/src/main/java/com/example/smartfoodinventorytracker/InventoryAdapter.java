package com.example.smartfoodinventorytracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<Product> itemList;
    private DatabaseReference databaseReference;

    public InventoryAdapter(List<Product> itemList) {
        this.itemList = itemList;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product"); // ✅ Connect to Firebase
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, barcode, expiryDate;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            barcode = itemView.findViewById(R.id.productBarcode);
            expiryDate = itemView.findViewById(R.id.productExpiryDate);
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

        // ✅ Show expiry date or "Not set" if missing
        if (product.getExpiryDate() == null || product.getExpiryDate().isEmpty()) {
            holder.expiryDate.setText("Expiry Date: Not set");
        } else {
            holder.expiryDate.setText("Expiry Date: " + product.getExpiryDate());
        }

        // ✅ Open Date Picker when item is clicked
        holder.itemView.setOnClickListener(v -> showDatePicker(holder, product));
    

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


    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
