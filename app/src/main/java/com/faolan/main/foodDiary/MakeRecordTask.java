package com.faolan.main.foodDiary;

import android.os.AsyncTask;

import com.faolan.main.foodDiary.models.RecordSuccess;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MakeRecordTask extends AsyncTask<Void, Void, List<String>> {
    private com.google.api.services.calendar.Calendar mService = null;


    private final long ONE_MINUTE_IN_MILLIS=60000;
    private String type;
    private String calendarId;
    private int value;
    private Exception mLastError = null;

    public MakeRecordTask(GoogleAccountCredential mCredential, String calendarId, String type, int value) {
        this.calendarId = calendarId;
        this.type = type;
        this.value = value;
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, mCredential)
                .setApplicationName("Food Diary App")
                .build();
    }

    @Override
    protected List<String> doInBackground(Void... params) {
        try {
            return postToApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onPostExecute(List<String> output) {
        BusHelper.getBus().post(new RecordSuccess("Event Created Successfully"));
        System.out.println("Success");
        System.out.println(output);
    }

    @Override
    protected void onCancelled() {
        BusHelper.getBus().post(new RecordSuccess(mLastError.getMessage()));
        System.err.println("Fail");
    }

    private List<String> postToApi() throws IOException {
        DateTime startDateTime = new DateTime(System.currentTimeMillis() - (5 * ONE_MINUTE_IN_MILLIS));
        DateTime endDateTime = new DateTime(System.currentTimeMillis());

        EventDateTime start = new EventDateTime().setDateTime(startDateTime);
        EventDateTime end = new EventDateTime().setDateTime(endDateTime);

        String summary = type;
        if(value != 0){
            summary += ": Rating " + value;
        }

        Event event = new Event()
                .setSummary(summary)
                .setStart(start)
                .setEnd(end);

        event = mService.events()
                .insert(calendarId,event)
                .execute();

        ArrayList<String> result = new ArrayList<>();
        result.add(event.getSummary());
        return result;
    }
}