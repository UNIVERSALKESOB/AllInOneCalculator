package com.allinone.calc;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Keeps the last 50 calculations in SharedPreferences. */
public final class HistoryStore {

    private static final String PREFS = "calc_history";
    private static final String KEY = "entries";
    private static final int MAX = 50;

    private HistoryStore() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void add(Context c, String expression, String result) {
        JSONArray old = readRaw(c);
        JSONArray out = new JSONArray();
        try {
            JSONObject entry = new JSONObject();
            entry.put("e", expression);
            entry.put("r", result);
            out.put(entry);
        } catch (Exception ignored) {
        }
        for (int i = 0; i < old.length() && i < MAX - 1; i++) {
            out.put(old.opt(i));
        }
        prefs(c).edit().putString(KEY, out.toString()).apply();
    }

    public static List<String[]> load(Context c) {
        JSONArray arr = readRaw(c);
        List<String[]> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) {
                continue;
            }
            list.add(new String[]{o.optString("e"), o.optString("r")});
        }
        return list;
    }

    public static void clear(Context c) {
        prefs(c).edit().remove(KEY).apply();
    }

    private static JSONArray readRaw(Context c) {
        String raw = prefs(c).getString(KEY, null);
        if (raw == null) {
            return new JSONArray();
        }
        try {
            return new JSONArray(raw);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
