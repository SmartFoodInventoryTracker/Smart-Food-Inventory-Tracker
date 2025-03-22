package com.example.smartfoodinventorytracker;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.ViewHolder> {
    private List<Product> itemList;
    private List<Product> originalList;
    private DatabaseReference databaseReference;
    public Sorting sorting = Sorting.NONE;
    public enum Sorting
    {
        EXP_DATE_ASC,
        EXP_DATE_DES,
        DATE_ADD_ASC,
        DATE_ADD_DES,
        NONE

    }
    public InventoryAdapter(List<Product> itemList) {
        this.itemList = new ArrayList<>(itemList); // Current displayed list
        this.originalList = new ArrayList<>(itemList); // Full original list
        this.databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product"); // ✅ Connect to Firebase
    }

    public void updateList(List<Product> newList) {
        Log.d("Adapter", "Updating list with " + newList.size() + " items"); // ✅ Debugging log

        itemList.clear();
        itemList.addAll(newList);

        originalList.clear();  // ✅ Ensure original list is updated
        originalList.addAll(newList);

        notifyDataSetChanged();
    }



    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, brand, barcode, expiryDate, DateAdded_h;
        ImageView productImage;

        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            brand = itemView.findViewById(R.id.productBrand);
            barcode = itemView.findViewById(R.id.productBarcode);
            expiryDate = itemView.findViewById(R.id.productExpiryDate);
            DateAdded_h = itemView.findViewById(R.id.prodcutDateAdded);
            productImage = itemView.findViewById(R.id.productImage);
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

        // ✅ Show Expiry Date
        holder.expiryDate.setText(product.getExpiryDate() == null || product.getExpiryDate().isEmpty()
                ? "Expiry Date: Not set"
                : "Expiry Date: " + product.getExpiryDate());

        // ✅ Show Correct "Date Added"
        holder.DateAdded_h.setText(product.getDateAdded() == null || product.getDateAdded().isEmpty()
                ? "Date Added: Not set"
                : "Date Added: " + product.getDateAdded());

        holder.itemView.setOnClickListener(v -> {
            FragmentManager fm = ((FragmentActivity) v.getContext()).getSupportFragmentManager();
            ProductDetailsDialogFragment dialog = ProductDetailsDialogFragment.newInstance(product);

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
                        originalList.remove(product);
                        notifyItemRemoved(currentPos);
                        Toast.makeText(v.getContext(), "Product deleted", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            dialog.show(fm, "ProductDetailsDialog");
        });



        // 🔁 Set icon based on product name (French + English)
        int iconResId = getCategoryIcon(product.getName());
        holder.productImage.setImageResource(iconResId);


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

    private void showDatePicker(ViewHolder holder, Product product) {
        Context context = holder.itemView.getContext();

        // ✅ Create a Date Picker Dialog
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(context, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;

            product.setExpiryDate(selectedDate); // ✅ Update local object

            holder.expiryDate.setText("Expiry Date: " + selectedDate); // ✅ Update UI instantly

            // ✅ Save new expiry date to Firebase
            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("inventory_product");
            databaseReference.child(product.getBarcode()).child("expiryDate").setValue(selectedDate)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Expiry date updated!", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged(); // ✅ Refresh RecyclerView immediately
                    });

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
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

    private String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
    }

    private int getCategoryIcon(String productName) {
        productName = normalizeText(productName); // Normalize accents + lower case

        class Category {
            int iconResId;
            List<String> keywords;
            Category(int iconResId, String... keywords) {
                this.iconResId = iconResId;
                this.keywords = Arrays.asList(keywords);
            }
        }

        List<Category> categories = new ArrayList<>();
        categories.add(new Category(R.drawable.ic_milk, "milk", "lait"));
        categories.add(new Category(R.drawable.ic_cheese, "cheese", "fromage", "shredded"));
        categories.add(new Category(R.drawable.ic_juice, "juice", "jus"));
        categories.add(new Category(R.drawable.ic_eggs, "egg", "oeuf", "oeufs"));
        categories.add(new Category(R.drawable.ic_water, "water", "eau"));
        categories.add(new Category(R.drawable.ic_oats, "oats", "oat"));

        // Fruits
        categories.add(new Category(R.drawable.ic_fruits, "apple", "banana", "orange", "mango", "apricot", "fruit", "fraise", "blueberries", "berries", "pommes", "bananes", "mangue", "abricot", "fruits"));

        // Meat & Fish
        categories.add(new Category(R.drawable.ic_meat, "meat", "steak", "beef", "poulet", "chicken", "saucisse", "sausage", "viande", "boeuf"));
        categories.add(new Category(R.drawable.ic_fish, "fish", "tuna", "salmon", "saumon", "poisson", "thon", "morue", "shrimps", "crabe", "cod", "crab"));

        // Vegetables
        categories.add(new Category(R.drawable.ic_vegetable, "lettuce", "carrot", "maïs", "spinach", "pomme de terre", "garlic", "vegetables", "laitue", "carotte", "epinard", "ail", "legumes", "potato"));

        // Nuts & Grains
        categories.add(new Category(R.drawable.ic_nuts, "peanut", "almond", "nuts", "noix", "amande", "arachide", "datte", "date"));

        // Cereals
        categories.add(new Category(R.drawable.ic_cereal, "cereal", "granola", "muesli", "cereale"));

        // Snacks
        categories.add(new Category(R.drawable.ic_snacks, "chips", "crackers", "snack", "bar", "barre", "gateau", "cookie"));

        // Canned
        categories.add(new Category(R.drawable.ic_canned, "can", "canned", "soup", "beans", "conserve", "haricots"));

        // Condiments
        categories.add(new Category(R.drawable.ic_condiments, "ketchup", "mustard", "mayo", "sauce", "vinaigrette"));

        // Bread & bakery
        categories.add(new Category(R.drawable.ic_bread, "bread", "bun", "buns", "bagel", "croissant", "brioche", "toast", "roll"));

        // Pasta
        categories.add(new Category(R.drawable.ic_pasta, "spaghetti", "penne", "macaroni", "farfalle", "rigatoni", "pasta", "pate"));

        // Dessert / Sweet
        categories.add(new Category(R.drawable.ic_dessert, "chocolate", "chocolat", "vanilla", "vanille", "cacao", "cocoa", "sweet", "sucre", "dessert"));

        // Score matching
        int bestScore = 0;
        int bestIcon = R.drawable.ic_food_default;

        for (Category category : categories) {
            int score = 0;
            for (String keyword : category.keywords) {
                if (productName.equals(keyword)) {
                    score += 3; // ✅ Strong match
                } else if (productName.contains(keyword)) {
                    // ✅ Boost "juice"/"jus" matching
                    if (keyword.equals("juice") || keyword.equals("jus")) {
                        score += 3;
                    } else {
                        score += 1;
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestIcon = category.iconResId;
            }
        }

        return bestIcon;
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }
}
