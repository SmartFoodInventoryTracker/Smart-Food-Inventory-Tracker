package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.example.smartfoodinventorytracker.R;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ShoppingListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_shopping_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ImageView addLogo = findViewById(R.id.addLogo);
        addLogo.setOnClickListener(v -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.shopping_list_dialog_add_list, null);
            EditText listNameInput = dialogView.findViewById(R.id.listNameInput);

            new AlertDialog.Builder(this)
                    .setTitle("Create New Shopping List")
                    .setView(dialogView)
                    .setPositiveButton("Create", (dialog, which) -> {
                        String listName = listNameInput.getText().toString().trim();
                        if (listName.isEmpty()) {
                            Toast.makeText(this, "List name cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference shoppingRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(userId)
                                .child("shopping-list");

                        shoppingRef.get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DataSnapshot snapshot = task.getResult();

                                if (snapshot.hasChild(listName)) {
                                    Toast.makeText(this, "A list with this name already exists", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Count custom lists (exclude "last_used")
                                long customListCount = 0;
                                for (DataSnapshot listSnap : snapshot.getChildren()) {
                                    if (!"last_used".equals(listSnap.getKey())) {
                                        customListCount++;
                                    }
                                }

                                if (customListCount >= 10) {
                                    Toast.makeText(this, "You’ve reached the maximum of 10 saved lists", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // ✅ Safe to create
                                String listId = shoppingRef.push().getKey();

                                if (listId != null) {
                                    Map<String, Object> listData = new HashMap<>();
                                    listData.put("name", listName);
                                    listData.put("createdAt", System.currentTimeMillis() / 1000);
                                    listData.put("items", new HashMap<>()); // empty initially

                                    shoppingRef.child(listId).setValue(listData)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "List created", Toast.LENGTH_SHORT).show();
                                                loadShoppingListsFromFirebase();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(this, "Failed to create list: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                e.printStackTrace();
                                            });
                                }

                            } else {
                                Toast.makeText(this, "Failed to check existing lists", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        loadShoppingListsFromFirebase();

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void updateEmptyMessageVisibility(List<?> list) {
        TextView emptyMessage = findViewById(R.id.emptyListMessage);
        RecyclerView recyclerView = findViewById(R.id.shoppingListsRecyclerView);

        if (list == null || list.isEmpty()) {
            emptyMessage.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessage.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadShoppingListsFromFirebase() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference shoppingRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("shopping-list");

        shoppingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Map.Entry<String, Integer>> listEntries = new ArrayList<>();

                for (DataSnapshot listSnapshot : snapshot.getChildren()) {
                    // Get the list name from the "name" field
                    String listName = listSnapshot.child("name").getValue(String.class);
                    if(listName == null) {
                        listName = "Unnamed List";
                    }
                    // Count items under "items"
                    int itemCount = (int) listSnapshot.child("items").getChildrenCount();
                    listEntries.add(new AbstractMap.SimpleEntry<>(listName, itemCount));
                }

                // Sort lists: e.g., if you want "last_used" on top, modify accordingly:
                listEntries.sort((a, b) -> {
                    if (a.getKey().equals("last_used")) return -1;
                    if (b.getKey().equals("last_used")) return 1;
                    return a.getKey().compareToIgnoreCase(b.getKey());
                });

                // Set up RecyclerView
                RecyclerView recyclerView = findViewById(R.id.shoppingListsRecyclerView);
                recyclerView.setLayoutManager(new LinearLayoutManager(ShoppingListActivity.this));
                ShoppingListAdapter adapter = new ShoppingListAdapter(ShoppingListActivity.this, listEntries);
                recyclerView.setAdapter(adapter);

                updateEmptyMessageVisibility(listEntries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShoppingListActivity.this, "Failed to load lists", Toast.LENGTH_SHORT).show();
            }
        });
    }

}