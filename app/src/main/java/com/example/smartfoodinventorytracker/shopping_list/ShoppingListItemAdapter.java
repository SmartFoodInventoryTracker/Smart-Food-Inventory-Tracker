package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;

import java.util.List;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final boolean[] expandedStates;
    private final boolean isShoppingMode;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, category, quantity, brand, notes;
        View expandableSection;
        CardView card;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            category = itemView.findViewById(R.id.productCategory);
            quantity = itemView.findViewById(R.id.productQuantity);
            brand = itemView.findViewById(R.id.productBrand);
            notes = itemView.findViewById(R.id.productNotes);
            expandableSection = itemView.findViewById(R.id.expandableSection);
            card = (CardView) itemView;
        }
    }



    public ShoppingListItemAdapter(Context context, List<Product> productList, boolean isShoppingMode) {
        this.context = context;
        this.productList = productList;
        this.expandedStates = new boolean[productList.size()];
        this.isShoppingMode = isShoppingMode; // ✅ This line is mandatory
    }

    @Override
    public ShoppingListItemAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ShoppingListItemAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.name.setText(product.getName());
        holder.quantity.setText("Qty: " + product.getQuantity());
        holder.brand.setText("Brand: " + (product.getBrand() != null ? product.getBrand() : "No Name"));
        // Handle expand/collapse
        boolean isExpanded = expandedStates[position];
        holder.expandableSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        if (!isShoppingMode) {
            holder.card.setOnClickListener(v -> {
                expandedStates[position] = !expandedStates[position];
                notifyItemChanged(position);
            });
        } else {
            holder.card.setOnClickListener(null); // disable toggle in shopping mode
        }

    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}
