package com.faolan.main.foodDiary;

import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.faolan.main.foodDiary.models.RequestSuccess;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
    private com.google.api.services.calendar.Calendar mService = null;

    private String calendarId;
    private Exception mLastError = null;
    private static final String CALENDAR_NAME = "Food Diary";
    SharedPreferences prefs;

    public MakeRequestTask(GoogleAccountCredential credential, SharedPreferences prefs) {
        System.out.println("Making Request");
        this.prefs = prefs;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Food Diary App")
                .build();
    }

    @Override
    protected List<String> doInBackground(Void... params) {
        try {
            getDataFromApi();

            return new ArrayList<>();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    private String createCalendar() throws IOException{
        //initializing for first time setup
        Calendar calendar = new Calendar();
        calendar.setSummary(CALENDAR_NAME);
        return mService.calendars().insert(calendar).execute().getId();
    }

    private Events getDataFromApi() throws IOException {
        System.out.println("Getting Data from API");
        DateTime now = new DateTime(System.currentTimeMillis());
        List<CalendarListEntry> calendars = mService.calendarList().list().execute().getItems();
        boolean exists = false;
        for(CalendarListEntry calendar : calendars){
            if (calendar.getSummary().equalsIgnoreCase(CALENDAR_NAME)){
                calendarId = calendar.getId();
                exists = true;
            }
        }
        if(!exists){
            calendarId = createCalendar();
        }
        prefs.edit().putString("calendarID", calendarId).apply();
        Events events = mService.events().list(calendarId)
                .setMaxResults(10)
                .setTimeMax(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        StorageHelper.storeData(prefs, events.getItems());
        List<Event> items = events.getItems();
        System.out.println("Got Data, should be doing stuff");
        BusHelper.getBus().post(new RequestSuccess("Called Google Successfully", items));
        return events;
    }


    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onPostExecute(List<String> output) {

    }

    @Override
    protected void onCancelled() {

    }
}