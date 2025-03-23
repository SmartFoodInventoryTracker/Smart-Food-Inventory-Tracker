package com.example.smartfoodinventorytracker;

import java.util.Arrays;
import java.util.List;
import java.io.Serializable;

public class Product implements Serializable {
    public String barcode;
    public String name;
    public String brand;
    public String expiryDate;
    public String dateAdded;
    public int quantity;
    private String imageUrl;

    // Required empty constructor for Firebase
    public Product() {
        this.quantity = 1; // Default quantity = 1
    }

    public Product(String barcode, String name, String brand) { //year-month-day
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.expiryDate = "Not set"; // Default Value
        this.quantity = 1;
       // expiryDate = new DateInfo(expirationdates);

    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public String getBrand() {
        return brand;
    }
    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        /*        String[] parts = expiryDate.split("/");

// Reorder the parts to put the year at index 0
        List<String> dateParts = Arrays.asList(parts[2], parts[1], parts[0]);
        this.expiryDate_info = new DateInfo(dateParts);*/
        this.expiryDate = expiryDate;
    }

    public Product copy() {
        Product product = new Product(this.barcode, this.name, this.brand);
        product.setDateAdded(this.dateAdded);
        product.setExpiryDate(this.expiryDate);
        product.setQuantity(this.quantity);
        product.setImageUrl(this.imageUrl);
        return product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;
        return barcode != null && barcode.equals(product.barcode);
    }

    @Override
    public int hashCode() {
        return barcode != null ? barcode.hashCode() : 0;
    }



    public String getDateAdded() {

        return this.dateAdded;
    }

    public void setDateAdded(String dateAdded) {
        /*        String[] parts = expiryDate.split("/");

// Reorder the parts to put the year at index 0
        List<String> dateParts = Arrays.asList(parts[2], parts[1], parts[0]);
        this.expiryDate_info = new DateInfo(dateParts);*/
        this.dateAdded = dateAdded;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

}
