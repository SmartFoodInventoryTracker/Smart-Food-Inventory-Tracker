package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartfoodinventorytracker.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private Context context;
    private List<Object> mixedDataList;

    public ShoppingListAdapter(Context context, List<Object> mixedDataList) {
        this.context = context;
        this.mixedDataList = mixedDataList;
    }

    // Header ViewHolder
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView headerTitle;
        public HeaderViewHolder(View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.headerTitle);
        }
    }

    // Item ViewHolder
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView listName, itemCount;
        ImageView deleteIcon;
        public ItemViewHolder(View itemView) {
            super(itemView);
            listName = itemView.findViewById(R.id.listName);
            itemCount = itemView.findViewById(R.id.itemCount);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return (mixedDataList.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.shopping_list_sections, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_shopping_list, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_HEADER) {
            String header = (String) mixedDataList.get(position);
            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;
            headerHolder.headerTitle.setText(header);
        } else {
            ShoppingList shoppingList = (ShoppingList) mixedDataList.get(position);
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.listName.setText(shoppingList.name);
            itemHolder.itemCount.setText(shoppingList.itemCount + (shoppingList.itemCount == 1 ? " item" : " items"));

            // Delete icon click listener
            itemHolder.deleteIcon.setOnClickListener(v -> {
                if (shoppingList.name.equalsIgnoreCase("last_used")) {
                    Toast.makeText(context, "You can't delete the last used list.", Toast.LENGTH_SHORT).show();
                    return;
                }
                new AlertDialog.Builder(context)
                        .setTitle("Delete List")
                        .setMessage("Are you sure you want to delete \"" + shoppingList.name + "\"?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            DatabaseReference ref = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(userId)
                                    .child("shopping-list")
                                    .child(shoppingList.key);
                            ref.removeValue().addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "List deleted", Toast.LENGTH_SHORT).show();
                                mixedDataList.remove(position);
                                notifyItemRemoved(position);
                            }).addOnFailureListener(e ->
                                    Toast.makeText(context, "Failed to delete list", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // On item click: open ShoppingListDetailActivity
            itemHolder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ShoppingListDetailActivity.class);
                intent.putExtra("listKey", shoppingList.key);
                intent.putExtra("listName", shoppingList.name);
                context.startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return mixedDataList.size();
    }
}
