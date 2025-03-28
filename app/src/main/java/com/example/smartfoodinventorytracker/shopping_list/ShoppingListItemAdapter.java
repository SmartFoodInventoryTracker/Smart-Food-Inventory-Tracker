package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.example.smartfoodinventorytracker.inventory.CategoryUtils;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final boolean isShoppingMode;
    private final String userId;
    private final String listKey;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, quantityBadge;
        ImageView productImage;
        CardView card;

        public ViewHolder(View itemView) {
            super(itemView);
            // Bind views from item_shopping_list_detail.xml
            name = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            quantityBadge = itemView.findViewById(R.id.quantityBadge);
            productImage = itemView.findViewById(R.id.productImage);
            card = (CardView) itemView;
        }
    }

    public ShoppingListItemAdapter(Context context, List<Product> productList, boolean isShoppingMode, String userId, String listKey) {
        this.context = context;
        this.productList = productList;
        this.isShoppingMode = isShoppingMode;
        this.userId = userId;
        this.listKey = listKey;
    }

    @NonNull
    @Override
    public ShoppingListItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your item layout file.
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingListItemAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.name.setText(product.getName());
        holder.brand.setText("Brand: " + product.getBrand());
        holder.quantityBadge.setText("Qty: " + product.getQuantity());
        holder.quantityBadge.setVisibility(View.VISIBLE);
        int iconResId = CategoryUtils.getCategoryIcon(product.getName());
        holder.productImage.setImageResource(iconResId);

        // Set the click listener to launch the shopping product details dialog.
        holder.card.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return;

            if (context instanceof FragmentActivity) {
                FragmentManager fm = ((FragmentActivity) context).getSupportFragmentManager();
                ShoppingProductDetailsDialogFragment dialog = ShoppingProductDetailsDialogFragment.newInstance(productList.get(adapterPosition));
                dialog.setUserId(userId);
                dialog.setShoppingProductDialogListener(new ShoppingProductDetailsDialogFragment.ShoppingProductDialogListener() {
                    @Override
                    public void onProductUpdated(Product updatedProduct) {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            productList.set(pos, updatedProduct);
                            notifyItemChanged(pos);
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
                dialog.show(fm, "ShoppingProductDetailsDialog");
            } else {
                Toast.makeText(context, "Unable to edit product", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}
