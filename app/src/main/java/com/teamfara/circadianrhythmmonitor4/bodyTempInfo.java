//Zach Highley-Gergel
//Created by zachhgg on 10/24/15.
//Body Temperature Information View

package com.teamfara.circadianrhythmmonitor4;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

//Graphing abilities taken from https://github.com/PhilJay/MPAndroidChart
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class bodyTempInfo extends AppCompatActivity {

    //Fields
    ArrayList<Double> currentBodyTemp;
    ArrayList<Double> targetBodyTemp;
    boolean targetTimeZoneIsBehind;
    boolean targetTimeZoneIsAhead;
    int daysLeft;
    int timeDifference;

    private void displayBodyTemp(String bodyTemp) {
        TextView textView = (TextView) findViewById(R.id.bodyTempInfoArray);
        textView.setText(bodyTemp);
    }

    //Reads the text from a file on the phone
    public void displayCurrentBodyTempArray(View view) throws FileNotFoundException {

        //Get the text file
        ArrayList<Double> list = new ArrayList<>();
        File sdcard = Environment.getExternalStorageDirectory();

        //Read text from file
        File file = new File(sdcard, "Download/temp.txt");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                list.add(Double.parseDouble(line));
                text.append('\n');
            }
            br.close();
        }
        catch(IOException e) {
            throw new Error(e);
        }
        currentBodyTemp = list;
        //displayBodyTemp("Your Body Temperature Readings are: " + text);
    }

    //Function that executes as soon as class is started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_temp_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Recieves the information from the previous View
        Intent mIntent = getIntent();
        timeDifference = mIntent.getIntExtra("timeDifference", 0);
        Intent nIntent = getIntent();
        daysLeft = nIntent.getIntExtra("daysLeft", 0);
        Intent oIntent = getIntent();
        targetTimeZoneIsAhead = oIntent.getBooleanExtra("targetTimeZoneIsAhead", false);
        Intent pIntent = getIntent();
        targetTimeZoneIsBehind = pIntent.getBooleanExtra("targetTimeZoneIsBehind", false);

        //Sets the body temperature from inputted file
        try {
            displayCurrentBodyTempArray(findViewById(R.id.bodyTempInfoArray));
        } catch (FileNotFoundException e) {
            throw new Error(e);
        }

        targetBodyTempCalcForGraph(/*findViewById(R.id.chartTarget)*/);
        LineChart chart = (LineChart) findViewById(R.id.chart);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //reduces noise of current body temperature craph
        reduceNoise();

        //sets the data and shows the graph for current Body temperature
        int i = 0;
        ArrayList<Entry> vals = new ArrayList<Entry>();
        for (i=0; i<currentBodyTemp.size(); i++) {
            Entry entry = new Entry(currentBodyTemp.get(i).floatValue(), i);
            vals.add(entry);
        }

        //Sets up Axis and orientation for the current body temperature graph
        YAxis leftAxis = chart.getAxisLeft();
        YAxis rightAxis = chart.getAxisRight();
        leftAxis.setStartAtZero(false);
        rightAxis.setStartAtZero(false);
        leftAxis.setLabelCount(10, false);
        leftAxis.setAxisMinValue(37);
        leftAxis.setAxisMaxValue(38);
        rightAxis.setAxisMinValue(37);
        rightAxis.setAxisMaxValue(38);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        LineDataSet set = new LineDataSet(vals, "Body Temperature");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);

        //Sets the data for the current body temperature graph
        ArrayList<LineDataSet> dataSet = new ArrayList<LineDataSet>();
        ArrayList<String> xVals = new ArrayList<String>();
        dataSet.add(set);
        for (i=0; i<currentBodyTemp.size(); i++) {
            xVals.add(String.valueOf(i));
        }
        LineData data = new LineData(xVals, dataSet);
        chart.setData(data);
        //chart.invalidate();


        //sets and displays new, targeted graph
        targetBodyTempReduceNoise();
        ArrayList<Entry> vals1 = new ArrayList<Entry>();
        LineChart chartTarget = (LineChart) findViewById(R.id.chartTarget);
        for (int p=0; p<targetBodyTemp.size(); p++) {
            Entry entry1 = new Entry(targetBodyTemp.get(p).floatValue(), p);
            vals1.add(entry1);
        }

        //Sets up axis and orientation for targeted body temperature graph
        YAxis leftAxis1 = chartTarget.getAxisLeft();
        YAxis rightAxis1 = chartTarget.getAxisRight();
        leftAxis1.setStartAtZero(false);
        rightAxis1.setStartAtZero(false);
        leftAxis1.setLabelCount(10, false);
        leftAxis1.setAxisMinValue(37);
        leftAxis1.setAxisMaxValue(38);
        rightAxis1.setAxisMinValue(37);
        rightAxis1.setAxisMaxValue(38);
        XAxis xAxis1 = chartTarget.getXAxis();
        xAxis1.setPosition(XAxis.XAxisPosition.BOTTOM);

        //Sets the data for the target body temperature graph
        LineDataSet set1 = new LineDataSet(vals1, "Target Body Temperature");
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);
        ArrayList<LineDataSet> dataSet1 = new ArrayList<LineDataSet>();
        ArrayList<String> xVals1 = new ArrayList<String>();
        dataSet1.add(set1);
        for (i=0; i<targetBodyTemp.size(); i++) {
            xVals1.add(String.valueOf(i));
        }
        LineData data1 = new LineData(xVals1, dataSet1);
        chartTarget.setData(data1);
    }

    //Function to reduce noise in current body temperature information; low pass filter
    private void reduceNoise() {

        ArrayList<Double> filteredCurrentBodyTemp = new ArrayList<Double>();

        //averages every 60 minutes into one value and puts back in original array
        int count = 1;
        double sum = 0;
        for (int i = 0; i < currentBodyTemp.size(); i++) {
            sum = sum + currentBodyTemp.get(i);
            if (count % 61 == 0) {
                filteredCurrentBodyTemp.add(sum/60);
                sum = 0;
            }
            count++;
        }
        currentBodyTemp = filteredCurrentBodyTemp;
    }

    //Function to reduce noise in target body temperature information; low pass filter
    private void targetBodyTempReduceNoise() {

        ArrayList<Double> filteredCurrentBodyTemp = new ArrayList<Double>();

        //averages every 60 minutes into one value
        int count = 1;
        double sum = 0;
        for (int i = 0; i < targetBodyTemp.size(); i++) {
            sum = sum + targetBodyTemp.get(i);
            if (count % 61 == 0) {
                filteredCurrentBodyTemp.add(sum/60);
                sum = 0;
            }
            count++;
        }
        targetBodyTemp = filteredCurrentBodyTemp;
    }

    //Calculates the array for the targeted body temperature graph
    private void targetBodyTempCalcForGraph() {

        ArrayList<Double> tempTargetBodyTemp = new ArrayList<Double>();

        //Shifts graph to the right if the time zone is set as Ahead
        if (targetTimeZoneIsAhead) {
            int i = 0;
            int k = 0;
            for (i = currentBodyTemp.size() - (timeDifference/daysLeft); i < currentBodyTemp.size(); i++) {
                tempTargetBodyTemp.add(currentBodyTemp.get(i));
            }
            for (k = 0; k < currentBodyTemp.size() - timeDifference/daysLeft; k++)
                tempTargetBodyTemp.add(currentBodyTemp.get(k));
        }

        //Shifts the graph to the left if the time zone is set as behind
        else if (targetTimeZoneIsBehind) {
            for (int j = timeDifference/daysLeft; j < currentBodyTemp.size(); j++) {
                tempTargetBodyTemp.add(currentBodyTemp.get(j));
            }
            for (int l = 0; l <timeDifference/daysLeft; l++) {
                tempTargetBodyTemp.add(currentBodyTemp.get(l));
            }
        }
        targetBodyTemp = tempTargetBodyTemp;
    }
}
