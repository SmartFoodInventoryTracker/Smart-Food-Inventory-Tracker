package com.example.smartfoodinventorytracker.fridge_conditions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartfoodinventorytracker.R;
import java.util.List;

public class FridgeHistoryAdapter extends RecyclerView.Adapter<FridgeHistoryAdapter.ViewHolder> {

    private final List<FridgeHistoryItem> historyList;

    public FridgeHistoryAdapter(List<FridgeHistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fridge_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FridgeHistoryItem item = historyList.get(position);
        holder.dateTimeText.setText(item.dateTime);
        holder.tempText.setText("Temperature: " + item.temperature + "°C");
        holder.humidityText.setText("Humidity: " + item.humidity + "%");
        holder.coText.setText("CO: " + item.co + " ppm");
        holder.lpgText.setText("LPG: " + item.lpg + " ppm");
        holder.smokeText.setText("NH₄: " + item.smoke + " ppm");
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTimeText, tempText, humidityText, coText, lpgText, smokeText;

        ViewHolder(View itemView) {
            super(itemView);
            dateTimeText = itemView.findViewById(R.id.dateTimeText);
            tempText = itemView.findViewById(R.id.tempText);
            humidityText = itemView.findViewById(R.id.humidityText);
            coText = itemView.findViewById(R.id.coText);
            lpgText = itemView.findViewById(R.id.lpgText);
            smokeText = itemView.findViewById(R.id.smokeText);
        }
    }
}
