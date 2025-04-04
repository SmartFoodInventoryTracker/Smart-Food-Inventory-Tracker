package com.example.smartfoodinventorytracker.shopping_list;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ShoppingListCache {
    private static final String PREF_NAME = "ShoppingListPrefs";
    private static final String KEY_MAP = "KeyToNameMap";

    public static void saveListMap(Context context, Map<String, String> keyToNameMap) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        JSONObject json = new JSONObject(keyToNameMap);
        prefs.edit().putString(KEY_MAP, json.toString()).apply();
    }

    public static Map<String, String> loadListMap(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonStr = prefs.getString(KEY_MAP, "{}");

        Map<String, String> map = new HashMap<>();
        try {
            JSONObject json = new JSONObject(jsonStr);
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, json.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }
}
