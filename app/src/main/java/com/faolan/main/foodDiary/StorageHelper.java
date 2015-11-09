package com.faolan.main.foodDiary;

import android.app.Activity;
import android.content.SharedPreferences;

import com.google.api.services.calendar.model.Events;
import com.google.gson.Gson;

public class StorageHelper extends Activity {
    private static final String PREF_EVENT_STORAGE = "eventStorage";
    private static Gson gson = new Gson();

    public synchronized static Events getData(SharedPreferences prefs){
        String json = prefs.getString(PREF_EVENT_STORAGE, "");
        return gson.fromJson(json, Events.class);
    }

    public synchronized static void storeData(SharedPreferences prefs, Events e){
        String json = gson.toJson(e);
        prefs.edit().putString(PREF_EVENT_STORAGE, json).apply();
    }
}
