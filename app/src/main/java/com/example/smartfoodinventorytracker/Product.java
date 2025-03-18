package com.example.smartfoodinventorytracker;

import java.util.Arrays;
import java.util.List;

public class Product {
    public String barcode;
    public String name;
    public String brand;
    public String expiryDate;
    public String dateAdded;

    // Required empty constructor for Firebase
    public Product() {}

    public Product(String barcode, String name, String brand) { //year-month-day
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
        this.expiryDate = "Not set"; // Default Value
       // expiryDate = new DateInfo(expirationdates);

    }



    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
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

    public Product copy()
    {
        Product product = new Product(this.barcode, this.name, this.brand);
        product.setDateAdded(this.dateAdded);
        product.setExpiryDate(this.expiryDate);
        return product;
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
}
