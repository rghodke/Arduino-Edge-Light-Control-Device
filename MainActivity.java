/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.ledconnect;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;

import com.example.android.common.logger.Log;
import com.example.android.common.logger.LogFragment;
import com.example.android.common.logger.LogWrapper;
import com.example.android.common.logger.MessageOnlyLogFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

/*
 * Application to connect to a multi-color LED over the network and control
 * its color and brightness. Uses AsyncTask for HTTP requests in the background
 */

public class MainActivity extends FragmentActivity {

    public static final String TAG = "LED Connect";

    // Reference to the text fragment for showing events.
    private LogFragment mLogFragment;
    // References to individual sliders
    private SeekBar seekRed;
    private SeekBar seekBlu;
    private SeekBar seekGrn;
    private SeekBar seekBrt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);

        seekRed = (SeekBar) findViewById(R.id.seekRED);
        seekBlu = (SeekBar) findViewById(R.id.seekBLU);
        seekGrn = (SeekBar) findViewById(R.id.seekGRN);
        seekBrt = (SeekBar) findViewById(R.id.seekBRT);

        seekRed.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar,int progressValue, boolean fromUser) {
                    // Display the value
                    //textView.setText(progress + "/" + seekBar.getMax());
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Do something here, if you want to do anything at the start of touching the seekbar
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Do something here, if you want to do anything at the end of touching the seekbar
                    EditText editText = (EditText) findViewById(R.id.edit_message);
                    String strServer = editText.getText().toString();
                    String str = String.format("http://%s/?RED=%d", strServer, seekBar.getProgress());
                    new DownloadTask().execute(str);
                }
            }
        );

        seekGrn.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar,int progressValue, boolean fromUser) {
                        // Display the value
                        //textView.setText(progress + "/" + seekBar.getMax());
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Do something here, if you want to do anything at the start of touching the seekbar
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        EditText editText = (EditText) findViewById(R.id.edit_message);
                        String strServer = editText.getText().toString();
                        String str = String.format("http://%s/?GREEN=%d", strServer, seekBar.getProgress());
                        new DownloadTask().execute(str);
                    }
                }
        );

        seekBlu.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar,int progressValue, boolean fromUser) {
                        // Display the value
                        //textView.setText(progress + "/" + seekBar.getMax());
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Do something here, if you want to do anything at the start of touching the seekbar
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        EditText editText = (EditText) findViewById(R.id.edit_message);
                        String strServer = editText.getText().toString();
                        String str = String.format("http://%s/?BLUE=%d", strServer, seekBar.getProgress());
                        new DownloadTask().execute(str);
                    }
                }
        );

        seekBrt.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar,int progressValue, boolean fromUser) {
                        // Display the value
                        //textView.setText(progress + "/" + seekBar.getMax());
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // Do something here, if you want to do anything at the start of touching the seekbar
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        EditText editText = (EditText) findViewById(R.id.edit_message);
                        String strServer = editText.getText().toString();
                        String str = String.format("http://%s/?BRIGHT=%d", strServer, seekBar.getProgress());
                        new DownloadTask().execute(str);
                    }
                }
        );

        // Initialize the framework.
        initializeLogging();

        // query current values and set sliders
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String strServer = editText.getText().toString();
        String str = String.format("http://%s/", strServer);
        new DownloadTask().execute(str);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String strServer = editText.getText().toString();

        switch (item.getItemId()) {
            // Send color values to Arduino controller.
            case R.id.fetch_action:
                String str = String.format("http://%s/?RED=%d&GREEN=%d&BLUE=%d&BRIGHT=%d", strServer,
                        seekRed.getProgress(),seekGrn.getProgress(),seekBlu.getProgress(),seekBrt.getProgress());
                new DownloadTask().execute(str);
                return true;

            // Clear the log view fragment.
            case R.id.clear_action:
                mLogFragment.getLogView().setText("");
                // query current values and set sliders
                str = String.format("http://%s/", strServer);
                new DownloadTask().execute(str);
                return true;
        }
        return false;
    }

    /*
     * AsyncTask, to fetch the data in the background
     */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                return loadFromNetwork(urls[0]);
            } catch (IOException e) {
              return getString(R.string.connection_error);
            }
        }

        /**
         * Uses the logging framework to display the output of the fetch
         * operation in the log fragment.
         */
        @Override
        protected void onPostExecute(String result) {
            int rr = 0;
            seekRed = (SeekBar) findViewById(R.id.seekRED);
            seekBlu = (SeekBar) findViewById(R.id.seekBLU);
            seekGrn = (SeekBar) findViewById(R.id.seekGRN);
            seekBrt = (SeekBar) findViewById(R.id.seekBRT);

            if (result.toLowerCase().contains("error")) {
                Log.i(TAG, result);
                // reset all sliders
                seekRed.setProgress(0);
                seekBlu.setProgress(0);
                seekGrn.setProgress(0);
                seekBrt.setProgress(0);
            } else {
                // if result is valid, split it into name value pairs and set progress indicators
                String delims = "[&]+";
                String[] tokens = result.split(delims);
                for (int i = 0; i < tokens.length; i++) {
                    String[] nvpair = tokens[i].split("[=]");
                    if(nvpair.length == 2){
                        String strValue = nvpair[1].trim();
                        rr = Integer.valueOf(strValue);
                        if (nvpair[0].equals("RED"))    seekRed.setProgress(rr);
                        if (nvpair[0].equals("BLUE"))   seekBlu.setProgress(rr);
                        if (nvpair[0].equals("GREEN"))  seekGrn.setProgress(rr);
                        if (nvpair[0].equals("BRIGHT")) seekBrt.setProgress(rr);
                    }
                }
            }
        }
    }

    /** Initiates the fetch operation. */
    private String loadFromNetwork(String urlString) throws IOException {
        InputStream stream = null;
        String str ="";

        try {
            stream = downloadUrl(urlString);
            str = readIt(stream, 100);
       } finally {
           if (stream != null) {
               stream.close();
            }
        }
        return str;
    }

    /*
     * @param urlString A string representation of a URL.
     * @return An InputStream retrieved from a successful HttpURLConnection.
     */
    private InputStream downloadUrl(String urlString) throws IOException {
        // BEGIN_INCLUDE(get_inputstream)
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Start the query
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
        // END_INCLUDE(get_inputstream)
    }

    /*
     * @param stream InputStream containing HTML from targeted site.
     * @param len Length of string that this method returns.
     * @return String concatenated according to len parameter.
     */
    private String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    /** Create a chain of targets that will receive log data */
    public void initializeLogging() {
        // Using Log, front-end to the logging chain, emulates
        // android.util.log method signatures.

        // Wraps Android's native log framework
        LogWrapper logWrapper = new LogWrapper();
        Log.setLogNode(logWrapper);
        // A filter that strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);
        // On screen logging via a fragment with a TextView.
        mLogFragment =
                (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);
        msgFilter.setNext(mLogFragment.getLogView());
    }
}
