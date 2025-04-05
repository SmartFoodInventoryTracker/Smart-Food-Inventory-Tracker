package com.example.smartfoodinventorytracker.fridge_conditions;

import java.util.ArrayList;
import java.util.List;

public class FridgeHistoryItem {
    public String dateTime;
    public double temperature;
    public double humidity;
    public Integer co;
    public Integer lpg;
    public Integer smoke;

    public FridgeHistoryItem(String dateTime, double temperature, double humidity, int co, int lpg, int smoke) {
        this.dateTime = dateTime;
        this.temperature = temperature;
        this.humidity = humidity;
        this.co = co;
        this.lpg = lpg;
        this.smoke = smoke;
    }
    public static List<Number> extractMetricList(List<FridgeHistoryItem> historyList, String metric) {
        List<Number> values = new ArrayList<>();
        switch (metric) {
            case "CO₂":
                for (FridgeHistoryItem item : historyList)
                    values.add(item.co); // Extract CO₂ values
                break;
            case "NH₄":
                for (FridgeHistoryItem item : historyList)
                    values.add(item.smoke); // Extract CO₂ values
                break;
            case "LPG":
                for (FridgeHistoryItem item : historyList)
                    values.add(item.lpg); // Extract LPG values
                break;
            case "Temperature":
                for (FridgeHistoryItem item : historyList)
                    values.add(item.temperature); // Extract temperature values
                break;
            case "Humidity":
                for (FridgeHistoryItem item : historyList)
                    values.add(item.humidity); // Extract temperature values
                break;
            // Add other cases as needed
        }
        return values;
    }

}
