package com.example.myapp.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SearchHistoryStore {
    private static final String PREFS_NAME = "search_history_prefs";
    private static final String KEY_HISTORY = "search_history";
    private static final String KEY_QUERY = "query";
    private static final String KEY_TIMESTAMP = "timestampMs";
    private static final int MAX_ENTRIES = 20;

    private SearchHistoryStore() {
    }

    public static void recordQuery(@NonNull Context context, @NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        List<Entry> entries = loadHistory(context);
        for (Iterator<Entry> iterator = entries.iterator(); iterator.hasNext(); ) {
            Entry entry = iterator.next();
            if (entry.query.equalsIgnoreCase(trimmed)) {
                iterator.remove();
            }
        }
        entries.add(0, new Entry(trimmed, System.currentTimeMillis()));
        if (entries.size() > MAX_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, MAX_ENTRIES));
        }
        saveHistory(context, entries);
    }

    @NonNull
    public static List<Entry> loadHistory(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_HISTORY, null);
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Entry> entries = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                String query = object.optString(KEY_QUERY, "").trim();
                long timestamp = object.optLong(KEY_TIMESTAMP, 0L);
                if (!query.isEmpty()) {
                    entries.add(new Entry(query, timestamp));
                }
            }
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }
        return entries;
    }

    public static void clearHistory(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    public static void deleteQuery(@NonNull Context context, @NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        List<Entry> entries = loadHistory(context);
        boolean removed = false;
        for (Iterator<Entry> iterator = entries.iterator(); iterator.hasNext(); ) {
            Entry entry = iterator.next();
            if (entry.query.equalsIgnoreCase(trimmed)) {
                iterator.remove();
                removed = true;
            }
        }
        if (removed) {
            saveHistory(context, entries);
        }
    }

    private static void saveHistory(@NonNull Context context, @NonNull List<Entry> entries) {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) {
            JSONObject object = new JSONObject();
            try {
                object.put(KEY_QUERY, entry.query);
                object.put(KEY_TIMESTAMP, entry.timestampMs);
                array.put(object);
            } catch (JSONException ignored) {
                // Skip invalid entries.
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply();
    }

    public static final class Entry {
        private final String query;
        private final long timestampMs;

        private Entry(@NonNull String query, long timestampMs) {
            this.query = query;
            this.timestampMs = timestampMs;
        }

        @NonNull
        public String getQuery() {
            return query;
        }

        public long getTimestampMs() {
            return timestampMs;
        }

    }
}
