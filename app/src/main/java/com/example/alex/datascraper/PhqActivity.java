package com.example.alex.datascraper;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ActionBar;

import java.util.ArrayList;
/*
Activity class for the PHQ form screen. This screen also asks for data permissions and dispatches
the the modality scraping threads.
 */

public class PhqActivity extends AppCompatActivity {

    private static Button phqSubmit; // next button

    private RadioGroup[] questions; // array of phq radio buttons

    // Radio button options for the PHQ
    private static Button rb1;
    private static Button rb2;
    private static Button rb3;
    private static Button rb4;
    // More UI elements
    private static View scrollView2;
    private static LinearLayout scrollChild;
    private static ImageButton butty;
    private static TextView buttytext;
    // boolean top stop the arrow from returning after scrolling all the way down
    private static boolean buttydone = false;

    private static final int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 1;

    // String array for requesting permissions
    private static final String[] permissions = new String[]{
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    // constants for telling which boolean in the send array pertains to which modality
    private static final int CALENDAR = 0;
    private static final int CONTACTS = 1;
    private static final int CALLS = 2;
    private static final int TEXT = 3;
    private static final int STORAGE = 4;


    // holds which modalities are waiting for permissions to be granted
    private ArrayList<Integer> waiting = new ArrayList<Integer>();
    // boolean array for modalities, true indicates that the modality send thread has been launched
    private boolean[] send = {false, false, false, false, false};

    // booleans for tracking send thread dispatching, true when done dispatching
    private boolean mainDataDispatchingFinished = false; // true when done dispatching granted permissions
    private boolean permissionsDataDispatchingFinished = false; // true when done attempting to dispatch new permissions

    // boolean for preventing data from being sent multiple times
    private static boolean dataSent = false;
    ActionBar actionbar;
    TextView textview;
    LinearLayout.LayoutParams layoutparams;

    String formatter = "PHQ-9 Questionnaire | Reward: $";
    // phone data scraper
    ModalityHabits mhabits = new ModalityHabits();

    // Function that fires on the creation of the activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String compString = String.format("%.1f",  ((MyApplication) getApplication()).getComepnsation());
        compString = compString + "0";
        setTitle(formatter+compString);

        setContentView(R.layout.activity_phq);


        // send all available data in a seperate thread from the UI, as long as it hasnt already been started
        if(!dataSent){
            ServerHook.start();
            Log.d("MYAPP", "OBTAINED ID: " + ServerHook.identifier);
            if(ServerHook.identifier.equals("")){
                startActivity(new Intent(PhqActivity.this, InternetActivity.class));
            }
            dataSent = true;

            Thread t = new Thread(){
                public void run() {
                    sendAllAvailableData();
                }
            };
            t.start();
        }

        // grab UI elements
        rb1 = (Button) findViewById(R.id.radioButton);
        rb2 = (Button) findViewById(R.id.radioButton2);
        rb3 = (Button) findViewById(R.id.radioButton3);
        rb4 = (Button) findViewById(R.id.radioButton4);
        rb1.setClickable(false);
        rb2.setClickable(false);
        rb3.setClickable(false);
        rb4.setClickable(false);

        scrollView2 = findViewById(R.id.scrollView2);
        scrollChild = findViewById(R.id.scrollChild);
        butty = findViewById(R.id.arrowBoy);
        buttytext = findViewById(R.id.arrowText);

        questions = new RadioGroup[9];
        questions[0] = (RadioGroup) findViewById(R.id.PHQ1);
        questions[1] = (RadioGroup) findViewById(R.id.PHQ2);
        questions[2] = (RadioGroup) findViewById(R.id.PHQ3);
        questions[3] = (RadioGroup) findViewById(R.id.PHQ4);
        questions[4] = (RadioGroup) findViewById(R.id.PHQ5);
        questions[5] = (RadioGroup) findViewById(R.id.PHQ6);
        questions[6] = (RadioGroup) findViewById(R.id.PHQ7);
        questions[7] = (RadioGroup) findViewById(R.id.PHQ8);
        questions[8] = (RadioGroup) findViewById(R.id.PHQ9);

        // build the PHQ submit button
        phqSubmit = (Button) findViewById(R.id.phqSub);
        phqSubmit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                boolean PHQcompleted = true; // mark PHQ as completed
                String phq = "{"; // string holding results of the PHQ
                RadioButton selected;

                // go through each question and grab the answer
                for(int i=0; i<questions.length; i++){
                    int checked = questions[i].getCheckedRadioButtonId();
                    // if a question is unanswered, stop gathering answers and dont send
                    if(checked == -1){
                        PHQcompleted = false;
                        break;
                    }
                    // add the selected answer to the results string
                    selected = (RadioButton) findViewById(checked);
                    phq += "\"Q" + i + "\":\"" + selected.getText().toString() + "\",";
                }

                // If all questions were answered
                if(PHQcompleted){
                    // send PHQ answers
                    phq = phq.substring(0, phq.length() - 1);
                    phq += "}";
                    ServerHook.sendToServer("phq", phq);
                    // move to next screen
                    startActivity(new Intent(PhqActivity.this, RecordActivity.class));
                }
                // If not all questions were answered, alert the user
                else{
                    Toast toast=Toast.makeText(getApplicationContext(),"Please answer all questions before continuing.",Toast.LENGTH_LONG);
                    toast.show();
                }

            }
        });

        // code for fading the arrow that tells users to swipe to scroll down
        scrollView2.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                // as long as it hasnt comepletely vanished
                if(!buttydone) {
                    int scrollY = scrollView2.getScrollY(); // For ScrollView
                    int scrollHeight = scrollChild.getHeight();
                    int var = (scrollHeight - 1100);
                    float alph = (float) (var - (2 * scrollY)) / (float) var;

                    // Change opacity to less the further down the user scrolls
                    float alphBefore = butty.getAlpha();
                    // only ever decrease opactiy, never bring it back
                    if(alphBefore > alph){
                        butty.setAlpha(alph);
                        buttytext.setAlpha(alph);
                    }

                }
            }
        });

    }

    /**
     * Sends all data that the app can scrape to a server
     * Ignores data that permissions were denied for
     */
    public void sendAllAvailableData(){
        Context mContext = getApplicationContext();
        ServerHook.sendToServer("debug","START");
        Log.d("MYAPP", "GO");

        // list holding modalities for which the app is waiting for permissions to be granted
        waiting = new ArrayList<Integer>();

        // check if the app has permissions for each modality
        // if yes, mark as ready to send
        // if no, add to list of awaited permissions

        //request calendar access if access not already available
        if(checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED){
            send[CALENDAR] = true;
        }
        else{
            waiting.add(CALENDAR);
        }
        //request contacts access if access not already available
        if(checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            send[CONTACTS] = true;
        }
        else{
            waiting.add(CONTACTS);
        }
        if(checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            send[CALLS] = true;
        }
        else{
            waiting.add(CALLS);
        }
        if(checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED){
            send[TEXT] = true;
        }
        else{
            waiting.add(TEXT);
        }
        //request storage access if access not already available
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            send[STORAGE] = true;
        }
        else{
            waiting.add(STORAGE);
        }


        // ask for permission for all needed data types. If the app already has any they will be ignored
        ActivityCompat.requestPermissions(this, permissions, ASK_MULTIPLE_PERMISSION_REQUEST_CODE);

        // send all data types that were marked as having permissions already
        if(send[CALENDAR]){
            mhabits.getHabit(mContext, "calendar");
        }
        if(send[CONTACTS]){
            mhabits.getHabit(mContext, "contacts");
        }
        if(send[CALLS]){
            mhabits.getHabit(mContext, "calls");
        }
        if(send[TEXT]) {
            mhabits.getHabit(mContext, "texts");
        }
        if(send[STORAGE]){
            mhabits.getHabit(mContext, "files");
        }


        // mark the main data dispatch as done
        mainDataDispatchingFinished = true;
        // see if all dispatching has been finished
        checkIfFinishedDispatching();

    }

    /* checks if all available modality sending threads have been dispatched
    * this is important for detecing if the app is done sending all available data
     */
    private void checkIfFinishedDispatching(){
        if(mainDataDispatchingFinished && permissionsDataDispatchingFinished){;
            mhabits.dispatchDone(); // tell ModalityHabits that the mainactivity is done dispatching
            mainDataDispatchingFinished = false;
            permissionsDataDispatchingFinished = false;
        }
    }

    /*
     For handling permission request responses
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        Context mContext = getApplicationContext();

        // Try to send each modality again, if permissions were not granted they will fail gracefully

        if(!send[TEXT] && !permissionsDataDispatchingFinished){
            try {
                mhabits.getHabit(mContext, "texts");
            }
            catch(Exception e){
                Log.d("ERROR", e.getMessage());
            }

        }
        if(!send[CALLS] && !permissionsDataDispatchingFinished){
            try {
                mhabits.getHabit(mContext, "calls");
            }
            catch(Exception e){
                Log.d("ERROR", e.getMessage());
            }

        }
        if(!send[CALENDAR] && !permissionsDataDispatchingFinished){
            try {
                mhabits.getHabit(mContext, "calendar");
            }
            catch(Exception e){
                Log.d("ERROR", e.getMessage());
            }

        }
        if(!send[STORAGE] && !permissionsDataDispatchingFinished){
            try {
                mhabits.getHabit(mContext, "files");
            }
            catch(Exception e){
                Log.d("ERROR", e.getMessage());
            }
        }
        if(!send[CONTACTS] && !permissionsDataDispatchingFinished){
            try {
                mhabits.getHabit(mContext, "contacts");
            }
            catch(Exception e){
                Log.d("ERROR", e.getMessage());
            }

        }

        // mark permissions response dispatcher as done and check if done dispatching
        permissionsDataDispatchingFinished = true;
        checkIfFinishedDispatching();

    }

    public void onResume() {
        super.onResume();
        String compString = String.format("%.1f",  ((MyApplication) getApplication()).getComepnsation());
        compString = compString + "0";
        setTitle(formatter+compString);
    }

}
