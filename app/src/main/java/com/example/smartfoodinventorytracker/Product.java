package com.example.smartfoodinventorytracker;

public class Product {
    public String barcode;
    public String name;
    public String brand;

    // Required empty constructor for Firebase
    public Product() {}

    public Product(String barcode, String name, String brand) {
        this.barcode = barcode;
        this.name = name;
        this.brand = brand;
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
}
