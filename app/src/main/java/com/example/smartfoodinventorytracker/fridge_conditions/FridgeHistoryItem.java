package com.example.smartfoodinventorytracker.fridge_conditions;

public class FridgeHistoryItem {
    public String dateTime;
    public double temperature;
    public double humidity;
    public int co;
    public int lpg;
    public int smoke;

    public FridgeHistoryItem(String dateTime, double temperature, double humidity, int co, int lpg, int smoke) {
        this.dateTime = dateTime;
        this.temperature = temperature;
        this.humidity = humidity;
        this.co = co;
        this.lpg = lpg;
        this.smoke = smoke;
    }
}
