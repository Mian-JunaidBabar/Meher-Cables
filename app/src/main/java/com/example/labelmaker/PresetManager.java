package com.example.labelmaker;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class to manage saving and loading JSON presets via SharedPreferences.
 */
public class PresetManager {

    private final SharedPreferences prefs;
    private final Gson gson;

    public PresetManager(Context context, String prefsName) {
        this.prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Saves any object as a JSON string under the given preset name.
     */
    public void savePreset(String presetName, Object data) {
        String json = gson.toJson(data);
        prefs.edit().putString(presetName, json).apply();
    }

    /**
     * Loads a basic object (like a custom State class) from a saved preset.
     */
    public <T> T loadPreset(String presetName, Class<T> clazz) {
        String json = prefs.getString(presetName, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads a List of objects from a saved preset using a TypeToken.
     * Useful for List<ColumnConfig> or List<RowModel>.
     */
    public <T> T loadPreset(String presetName, Type typeOfT) {
        String json = prefs.getString(presetName, null);
        if (json == null) return null;
        try {
            return gson.fromJson(json, typeOfT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a list of all saved preset names.
     */
    public List<String> getAllPresetNames() {
        Map<String, ?> allEntries = prefs.getAll();
        return new ArrayList<>(allEntries.keySet());
    }

    /**
     * Deletes a specific preset.
     */
    public void deletePreset(String presetName) {
        prefs.edit().remove(presetName).apply();
    }
}
