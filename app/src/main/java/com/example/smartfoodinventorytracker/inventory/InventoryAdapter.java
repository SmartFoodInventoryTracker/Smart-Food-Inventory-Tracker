package com.example.smartfoodinventorytracker.inventory;

import android.app.AlertDialog;
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

import com.example.smartfoodinventorytracker.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private Context context;
    private String userId;

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
    public InventoryAdapter(Context context, List<Product> itemList, String userId) {
        this.context = context;
        this.userId = userId;
        this.itemList = new ArrayList<>(itemList);
        this.originalList = new ArrayList<>(itemList);
        this.databaseReference = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("inventory_product");
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

        /*// ✅ Show Expiry Date
        holder.expiryDate.setText(product.getExpiryDate() == null || product.getExpiryDate().isEmpty()
                ? "Expiry Date: Not set"
                : "Expiry Date: " + product.getExpiryDate());*/

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

        // 🟡 Expiry Badge Logic
        String expiryText = getExpiryText(product.getExpiryDate());
        holder.expiryBadge.setText(expiryText);
        holder.expiryBadge.setVisibility(View.VISIBLE);

        int expiryColor = getExpiryColor(expiryText, context);
        holder.expiryBadge.setBackgroundTintList(ColorStateList.valueOf(expiryColor));

        // 🖼️ Category Icon
        int iconResId = CategoryUtils.getCategoryIcon(product.getName());
        holder.productImage.setImageResource(iconResId);


        holder.itemView.setOnClickListener(v -> {
            Context viewContext = v.getContext();

            while (viewContext instanceof android.content.ContextWrapper && !(viewContext instanceof FragmentActivity)) {
                viewContext = ((android.content.ContextWrapper) viewContext).getBaseContext();
            }

            if (viewContext instanceof FragmentActivity) {
                FragmentManager fm = ((FragmentActivity) viewContext).getSupportFragmentManager();
                ProductDetailsDialogFragment dialog = ProductDetailsDialogFragment.newInstance(product);
                dialog.setUserId(userId);

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
                        int currentPos = holder.getAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION) {
                            itemList.remove(currentPos);
                            notifyItemRemoved(currentPos);
                            originalList.remove(product);
                        }
                    }
                });

                dialog.show(fm, "ProductDetailsDialog");
            }

        });

    }

    private void removeProductByBarcode(String barcode) {
        Log.d("InventoryAdapter", "Product deleted: " + barcode);
        Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show();
    }

    private String getExpiryText(String expiryDate) {
        if (expiryDate == null || expiryDate.equals("Not set")) return "No expiry set";

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            LocalDate exp = LocalDate.parse(expiryDate, formatter);
            LocalDate today = LocalDate.now();
            long days = ChronoUnit.DAYS.between(today, exp);

            if (days < 0) return "Expired";
            else if (days == 0) return "Expires today";
            else if (days < 3) return "Expires in " + days + " day" + (days > 1 ? "s" : "");
            else if (days < 5) return "Expires in " + days + " days";
            else if (days < 14) return "Expires in " + days + " days";
            else if (days < 30) {
                long weeks = days / 7;
                return "Expires in " + weeks + " week" + (weeks > 1 ? "s" : "");
            } else {
                long months = days / 30;
                return "Expires in " + months + " month" + (months > 1 ? "s" : "");
            }
        } catch (Exception e) {
            return "Invalid date";
        }
    }


    private int getExpiryColor(String text, Context context) {
        if (text.equals("Expired")) {
            return ContextCompat.getColor(context, R.color.red);
        } else if (text.contains("Expires today")) {
            return ContextCompat.getColor(context, R.color.orange);
        } else if (text.contains("Expires in")) {
            try {
                String[] parts = text.split(" ");
                int number = Integer.parseInt(parts[2]);

                if (text.contains("day")) {
                    if (number <= 3) {
                        return ContextCompat.getColor(context, R.color.orange);
                    } else {
                        return ContextCompat.getColor(context, R.color.green);
                    }
                } else {
                    // weeks/months -> green
                    return ContextCompat.getColor(context, R.color.green);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return ContextCompat.getColor(context, android.R.color.darker_gray);
            }
        } else {
            return ContextCompat.getColor(context, android.R.color.darker_gray);
        }
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
