package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.example.smartfoodinventorytracker.inventory.CategoryUtils;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final String userId;
    private final String listKey;
    private static final int MAX_QUANTITY = 50;
    // New mode flag: false = Edit mode; true = Shopping mode.
    private boolean isShoppingMode;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName, productBrand, quantityBadge;
        ImageButton btnPlus, btnMinus;
        ImageView productImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productName   = itemView.findViewById(R.id.productName);
            productBrand  = itemView.findViewById(R.id.productBrand);
            quantityBadge = itemView.findViewById(R.id.quantityBadge);
            btnPlus       = itemView.findViewById(R.id.btnPlus);
            btnMinus      = itemView.findViewById(R.id.btnMinus);
            productImage  = itemView.findViewById(R.id.productImage);
        }
    }

    // Updated constructor includes the isShoppingMode flag.
    public ShoppingListItemAdapter(Context context, List<Product> productList, boolean isShoppingMode, String userId, String listKey) {
        this.context = context;
        this.productList = productList;
        this.isShoppingMode = isShoppingMode;
        this.userId = userId;
        this.listKey = listKey;
    }

    public void setShoppingMode(boolean shoppingMode) {
        this.isShoppingMode = shoppingMode;
    }

    @NonNull
    @Override
    public ShoppingListItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingListItemAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);

        // Basic binding
        holder.productName.setText(product.getName());
        holder.productBrand.setText("Brand: " + product.getBrand());
        holder.quantityBadge.setText(String.valueOf(product.getQuantity()));

        // Set category icon.
        int iconResId = CategoryUtils.getCategoryIcon(product.getName());
        holder.productImage.setImageResource(iconResId);

        // Increment: check for MAX_QUANTITY before incrementing.
        holder.btnPlus.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Product current = productList.get(pos);
            int currentQty = current.getQuantity();
            if (currentQty >= MAX_QUANTITY) {
                Toast.makeText(context, "Maximum quantity is " + MAX_QUANTITY, Toast.LENGTH_SHORT).show();
                return;
            }
            int newQty = currentQty + 1;
            current.setQuantity(newQty);
            holder.quantityBadge.setText(String.valueOf(newQty));
            updateProductInFirebase(current);
        });

        // Decrement
        holder.btnMinus.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Product currentProduct = productList.get(pos);
            int newQty = currentProduct.getQuantity() - 1;
            if (newQty <= 0) {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Item")
                        .setMessage("Do you want to delete this item?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(userId)
                                    .child("shopping-list")
                                    .child(listKey)
                                    .child("items")
                                    .child(currentProduct.getBarcode())
                                    .removeValue()
                                    .addOnSuccessListener(aVoid -> {
                                        productList.remove(pos);
                                        notifyItemRemoved(pos);
                                        Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context, "Failed to remove item", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                currentProduct.setQuantity(newQty);
                holder.quantityBadge.setText(String.valueOf(newQty));
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userId)
                        .child("shopping-list")
                        .child(listKey)
                        .child("items")
                        .child(currentProduct.getBarcode())
                        .setValue(currentProduct);
            }
        });

        // Long press launches the edit dialog.
        holder.itemView.setOnLongClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return true;

            ShoppingProductDetailsDialogFragment dialog = ShoppingProductDetailsDialogFragment.newInstance(productList.get(adapterPosition));
            dialog.setUserId(userId);
            // Pass the current mode to the dialog so it can display the expiry field if needed.
            dialog.setShoppingMode(isShoppingMode);
            dialog.setShoppingProductDialogListener(new ShoppingProductDetailsDialogFragment.ShoppingProductDialogListener() {
                @Override
                public void onProductUpdated(Product updatedProduct) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        productList.set(pos, updatedProduct);
                        notifyItemChanged(pos);
                        updateProductInFirebase(updatedProduct); // This saves name/brand/etc. changes too
                    }
                }

                @Override
                public void onProductDeleted(String barcode) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(userId)
                                .child("shopping-list")
                                .child(listKey)
                                .child("items")
                                .child(barcode)
                                .removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    productList.remove(pos);
                                    notifyItemRemoved(pos);
                                    Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Failed to delete product", Toast.LENGTH_SHORT).show());
                    }
                }
            });
            dialog.show(((FragmentActivity) context).getSupportFragmentManager(), "ShoppingProductDetailsDialog");
            return true;
        });
    }

    private void updateProductInFirebase(Product product) {
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("shopping-list")
                .child(listKey)
                .child("items")
                .child(product.getBarcode())
                .setValue(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}
