package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import com.example.smartfoodinventorytracker.inventory.Product;

import java.util.List;

public class ShoppingListItemAdapter extends RecyclerView.Adapter<ShoppingListItemAdapter.ViewHolder> {

    private Context context;
    private List<Product> productList;

    public ShoppingListItemAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView productName;

        public ViewHolder(View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName); // You can customize
        }
    }

    @NonNull
    @Override
    public ShoppingListItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.productName.setText(product.getName()); // Adjust as needed
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }
}
