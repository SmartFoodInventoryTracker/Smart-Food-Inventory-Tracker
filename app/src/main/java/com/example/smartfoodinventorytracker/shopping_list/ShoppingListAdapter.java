package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;

import java.util.List;
import java.util.Map;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {

    private Context context;
    private List<Map.Entry<String, Integer>> listData;

    public ShoppingListAdapter(Context context, List<Map.Entry<String, Integer>> listData) {
        this.context = context;
        this.listData = listData;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView listName, itemCount;

        public ViewHolder(View itemView) {
            super(itemView);
            listName = itemView.findViewById(R.id.listName);
            itemCount = itemView.findViewById(R.id.itemCount);
        }
    }

    @NonNull
    @Override
    public ShoppingListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShoppingListAdapter.ViewHolder holder, int position) {
        Map.Entry<String, Integer> entry = listData.get(position);
        String listName = entry.getKey();
        int itemCount = entry.getValue();

        holder.listName.setText(listName);
        holder.itemCount.setText(itemCount + (itemCount == 1 ? " item" : " items"));
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }
}
