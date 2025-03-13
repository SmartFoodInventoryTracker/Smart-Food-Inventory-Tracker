package com.example.smartfoodinventorytracker;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<Product> itemList;
    private DatabaseReference databaseReference;

    public InventoryAdapter(List<Product> itemList) {
        this.itemList = itemList;
        this.databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product"); // ✅ Connect to Firebase
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, barcode;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            barcode = itemView.findViewById(R.id.productBarcode);
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
        holder.barcode.setText("Barcode: " + product.getBarcode());

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

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
