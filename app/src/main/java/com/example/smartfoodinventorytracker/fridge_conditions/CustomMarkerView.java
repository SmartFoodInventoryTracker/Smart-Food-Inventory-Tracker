package com.example.smartfoodinventorytracker.fridge_conditions;

import android.content.Context;
import android.widget.TextView;
import android.widget.RelativeLayout;

import com.example.smartfoodinventorytracker.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomMarkerView extends MarkerView {

    private final TextView timeTextView;
    private final TextView valueTextView;
    private final List<FridgeHistoryItem> historyItems;

    public CustomMarkerView(Context context, int layoutResource, List<FridgeHistoryItem> historyItems) {
        super(context, layoutResource);
        this.historyItems = historyItems;

        timeTextView = findViewById(R.id.marker_time);
        valueTextView = findViewById(R.id.marker_value);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (index >= 0 && index < historyItems.size()) {
            FridgeHistoryItem item = historyItems.get(index);
            timeTextView.setText(formatTime(item.dateTime));
            valueTextView.setText(String.valueOf(e.getY()));
        }
        super.refreshContent(e, highlight);
    }

    private String formatTime(String rawTime) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = parser.parse(rawTime);
            SimpleDateFormat formatter = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            return formatter.format(date);
        } catch (ParseException e) {
            return rawTime;
        }
    }
}
