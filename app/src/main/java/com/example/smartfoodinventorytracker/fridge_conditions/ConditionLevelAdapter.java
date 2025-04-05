package com.example.smartfoodinventorytracker.fridge_conditions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartfoodinventorytracker.R;

import java.util.List;

public class ConditionLevelAdapter extends RecyclerView.Adapter<ConditionLevelAdapter.LevelViewHolder> {

    private final List<String> levelDescriptions;

    public ConditionLevelAdapter(List<String> descriptions) {
        this.levelDescriptions = descriptions;
    }

    @NonNull
    @Override
    public LevelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_condition_slide, parent, false);
        return new LevelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
        holder.levelText.setText(levelDescriptions.get(position));
    }

    @Override
    public int getItemCount() {
        return levelDescriptions.size();
    }

    static class LevelViewHolder extends RecyclerView.ViewHolder {
        TextView levelText;

        LevelViewHolder(@NonNull View itemView) {
            super(itemView);
            levelText = itemView.findViewById(R.id.levelText);
        }
    }
}
