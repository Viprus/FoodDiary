package com.faolan.main.foodDiary;

import android.app.Activity;
import android.content.SharedPreferences;

import com.google.api.services.calendar.model.Event;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class StorageHelper extends Activity {
    private static final String PREF_EVENT_STORAGE = "eventStorage";
    private static Gson gson = new Gson();

    public synchronized static List<Event> getData(SharedPreferences prefs){
        String json = prefs.getString(PREF_EVENT_STORAGE, "");
        return gson.fromJson(json, new TypeToken<List<Event>>(){}.getType());
    }

    public synchronized static void storeData(SharedPreferences prefs, List<Event> e){
        String json = gson.toJson(e);
        System.out.println(json);
        prefs.edit().putString(PREF_EVENT_STORAGE, json).apply();
    }
}
