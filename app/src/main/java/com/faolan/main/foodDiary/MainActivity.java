package com.faolan.main.foodDiary;

import com.faolan.main.foodDiary.models.RecordSuccess;
import com.faolan.main.foodDiary.models.RequestSuccess;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.*;

import com.google.gson.Gson;
import com.squareup.otto.Subscribe;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends Activity {
    GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    SharedPreferences prefs;
    Gson gson;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gson = new Gson();
        prefs = this.getPreferences(MODE_PRIVATE);
        BusHelper.getBus().register(this);
        setContentView(R.layout.activity_main);

        mProgress = new ProgressDialog(this);

        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs = this.getPreferences(MODE_PRIVATE);
        if (isGooglePlayServicesAvailable()) {
            refreshResults();
        } else {
            TextView mOutputText = (TextView) findViewById(R.id.textView_output);
            mOutputText.setText("Google Play Services required: " + "after installing, close and relaunch this app.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                        String accountName =
                                data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                        if (accountName != null) {
                            mCredential.setSelectedAccountName(accountName);
                            SharedPreferences settings =
                                    getPreferences(Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    TextView mOutputText = (TextView) findViewById(R.id.textView_output);
                    mOutputText.setText("Account unspecified.");
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Subscribe
    public void createEventComplete(RecordSuccess model){
        Toast.makeText(this, model.message ,Toast.LENGTH_LONG).show();
        refreshResults();
    }

    @Subscribe
    public void requestComplete(RequestSuccess model){
        System.out.println("request complete...");
        System.out.println(model.message);
        mProgress.cancel();
        Toast.makeText(this, "Request Successful" ,Toast.LENGTH_LONG).show();
    }

    private void executeTask(AsyncTask<Void, Void, List<String>> task){
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                task.execute();
            } else {
                TextView mOutputText = (TextView) findViewById(R.id.textView_output);
                mOutputText.setText("No network connection available.");
            }
        }
    }

    private void refreshResults() {
        mProgress.setMessage("Calling Google Calendar API ...");
        mProgress.show();
        executeTask(new MakeRequestTask(mCredential, prefs));
    }

    public void recordPoop(View view){
        final String calendarID = prefs.getString("calendarID", "");
        final EditText input = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setMessage("Please Enter a Bristol Scale value between 1 and 7")
                .setTitle("Record Movement")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeTask(new MakeRecordTask(mCredential, calendarID, "Bowel Movement", Integer.parseInt(input.getText().toString())));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    public void recordFood(View view){
        final String calendarID = prefs.getString("calendarID", "");
        final EditText input = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setMessage("Please describe the meal")
                .setTitle("Record Food")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        executeTask(new MakeRecordTask(mCredential, calendarID, "Food: " + input.getText().toString(), 0));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    private void chooseAccount() {
        startActivityForResult(
                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                MainActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

}


