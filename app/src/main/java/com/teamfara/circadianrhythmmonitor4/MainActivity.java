//Zach Highley-Gergel
//Created by zachhgg on 10/24/15.
//Some information taken from (1) http://developer.android.com/guide/topics/connectivity/bluetooth.html


package com.teamfara.circadianrhythmmonitor4;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    //Fields
    private final static int REQUEST_ENABLE_BT = 1;
    UUID myUUID = UUID.randomUUID();
    private static final int DISCOVER_DURATION = 300;
    private static final int REQUEST_BLU = 1;
    ArrayAdapter<String> mArrayAdapter;
    private boolean targetTimeZoneIsAhead = false;
    private boolean targetTimeZoneIsBehind = false;
    int currentWakeTime = 420;
    int currentSleepTime = 1320;
    int targetWakeTime = 0;
    int targetSleepTime = 0;
    private int daysLeft = 0;
    private int timeDifference = 0;
    private ArrayList<Integer> lightInfo;


    //Executed at the beginning of the function
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Checks if bluetooth is enabled if it isn't requests that it is enabled
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBIntent, REQUEST_ENABLE_BT);
        }

        //Makes the device discoverable to other devices
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
    }

    //Sets the sleepTime text view
    private void printSleepTime(String sleep) {
        TextView textView = (TextView) findViewById(R.id.sleepTime);
        textView.setText(sleep);
    }

    //Sets the wakeTime text view
    private void printWakeTime(String wake) {
        TextView textView = (TextView) findViewById(R.id.wakeTime);
        textView.setText(wake);
    }

    //Receives the information that is edited by the user and sets the time change for that day if Ahead
    public void setTimeZoneAhead(View view) {

        //Edit text
        EditText dLeft = (EditText)findViewById(R.id.daysLeft);
        EditText timeD = (EditText)findViewById(R.id.timeChange);

        //sets time zone as ahead and recieves the text info
        targetTimeZoneIsAhead = true;
        daysLeft = Integer.parseInt(dLeft.getText().toString());
        timeDifference = 60*(Integer.parseInt(timeD.getText().toString()));

        //sets target wake time and prints
        targetWakeTime = currentWakeTime + timeDifference/daysLeft;
        printWakeTime(convertMinutesToRealTime(targetWakeTime));

        //sets target sleep time and prints
        targetSleepTime = currentSleepTime + timeDifference/daysLeft;
        printSleepTime(convertMinutesToRealTime(targetSleepTime));

        //Creates targeted light information array
        createLightInfo();
    }

    //Receives the information that is edited by the user and sets the time change for that day if Behind
    public void setTimeZoneBehind(View view) {

        //Edit text
        EditText dLeft = (EditText)findViewById(R.id.daysLeft);
        EditText timeD = (EditText)findViewById(R.id.timeChange);

        //sets time zone as ahead and recieves the text info
        targetTimeZoneIsBehind = true;
        daysLeft = Integer.parseInt(dLeft.getText().toString());
        timeDifference = 60*(Integer.parseInt(timeD.getText().toString()));

        //sets target wake time and prints
        targetWakeTime = currentWakeTime - timeDifference/daysLeft;
        printWakeTime(convertMinutesToRealTime(targetWakeTime));

        //sets target sleep time and prints
        targetSleepTime = currentSleepTime - timeDifference/daysLeft;
        printSleepTime(convertMinutesToRealTime(targetSleepTime));

        //Creates targeted light information array
        createLightInfo();
    }

    //Converts the minute information to readable 24 hour digital clock
    private String convertMinutesToRealTime(int time) {

        int hours = 0;
        int minutes = 0;

        //When total minutes is exactly divisible by 60
        if (time % 60 == 0) {
            hours = time/60;
        }

        //When total minutes are not exactly divisible by 60
        else {
            minutes = time%60;
            hours = time/60;
        }

        //Makes sure the printed time information isn't in more than two numbers per side
        if (hours > 9 && minutes > 9) {
            return Integer.toString(hours) + ":" + Integer.toString(minutes);
        }
        else if(hours <10 && minutes < 10) {
            return "0" + Integer.toString(hours) + ":" + "0" + Integer.toString(minutes);
        }
        else if (hours < 10 && minutes > 9) {
            return "0" + Integer.toString(hours) + ":" + Integer.toString(minutes);
        }
        else {
            return Integer.toString(hours) + ":0" + Integer.toString(minutes);
        }
    }

    //Creates the light information based off set target wake and sleep time for that day
    public void createLightInfo() {

        int dayLightValue = 150;
        int sleepLightValue = 3;
        int count = 1;
        int count2 = 61;
        ArrayList<Integer> temp = new ArrayList<Integer>();

            //change to 30 minute increment -
            //120 minutes before wake time it starts getting brighter
            for (int i = targetWakeTime - 30; i < targetWakeTime; i++) {
                temp.add((dayLightValue/30) * count);
                count++;
            }

            //Between wake and  4 hours before sleep time it stays that daylight LUX value
            for (int k = targetWakeTime; k < targetSleepTime - 240; k++) {
                temp.add(dayLightValue);
            }

            //From 4 hours to 3 hours before bed time the light slowly dims to target sleep light value
            for (int j = targetSleepTime - 240; j < targetSleepTime - 180; j++) {
                temp.add(dayLightValue - 2*147/count2);
                count2 = count2-1;
            }

            //stays sleep light value until 2 hours before waking up
            for (int z = 0; z < 150; z++) {
                temp.add(sleepLightValue);
            }
        lightInfo = temp;
        }

    //When the body temperature information button is clicked go to that view and send the required information to the next View
    public void goToBodyInfo(View view) {
        Intent intent = new Intent(this, bodyTempInfo.class);
        intent.putExtra("timeDifference", timeDifference);
        intent.putExtra("daysLeft", daysLeft);
        intent.putExtra("targetTimeZoneIsAhead", targetTimeZoneIsAhead);
        intent.putExtra("targetTimeZoneIsBehind", targetTimeZoneIsBehind);
        startActivity(intent);

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

    //Goes to the connectTest class view and sends required information
    public void sendLight(View view) {
        Intent intent = new Intent(this, ConnectTest.class);
        intent.putExtra("lightInfo", lightInfo);
        startActivity(intent);
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    //(1)
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };
}







