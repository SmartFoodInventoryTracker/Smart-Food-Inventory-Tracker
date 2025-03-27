package com.example.smartfoodinventorytracker.shopping_list;

public class ShoppingList {
    public String key;       // The Firebase push key
    public String name;      // The list name as entered by the user
    public int itemCount;    // Number of items in this list (for display)

    // Default constructor required for Firebase deserialization
    public ShoppingList() {
    }

    public ShoppingList(String key, String name, int itemCount) {
        this.key = key;
        this.name = name;
        this.itemCount = itemCount;
    }
}
