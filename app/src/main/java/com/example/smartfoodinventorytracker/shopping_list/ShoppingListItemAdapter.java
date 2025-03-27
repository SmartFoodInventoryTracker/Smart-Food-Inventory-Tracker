package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;
import com.example.smartfoodinventorytracker.inventory.CategoryUtils;

import java.util.List;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final boolean isShoppingMode;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, dateAdded;
        ImageView productImage;
        CardView card;

        public ViewHolder(View itemView) {
            super(itemView);
            // Bind views from item_shopping_list_detail.xml
            name = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            dateAdded = itemView.findViewById(R.id.prodcutDateAdded);
            productImage = itemView.findViewById(R.id.productImage);
            card = (CardView) itemView;
        }
    }

    public ShoppingListItemAdapter(Context context, List<Product> productList, boolean isShoppingMode) {
        this.context = context;
        this.productList = productList;
        this.isShoppingMode = isShoppingMode;
    }

    @NonNull
    @Override
    public ShoppingListItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout; ensure that item_shopping_list_detail.xml exists in res/layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingListItemAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.name.setText(product.getName());
        holder.brand.setText("Brand: " + product.getBrand());
        holder.dateAdded.setText("Date Added: " + (product.getDateAdded() != null ? product.getDateAdded() : "Not set"));
        // Use CategoryUtils to set a product icon image
        int iconResId = CategoryUtils.getCategoryIcon(product.getName());
        holder.productImage.setImageResource(iconResId);

        // In shopping mode, we disable any click actions (or set shopping-specific behavior)
        if (!isShoppingMode) {
            holder.card.setOnClickListener(v -> {
                // For example: expand details or open an edit dialog
            });
        } else {
            holder.card.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}
