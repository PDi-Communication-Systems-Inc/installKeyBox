package com.pdiarm.installkeybox;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Properties;

public class InstallKeyBox extends Activity {
    public final String TAG = "InstallKeyBoxActivity";
    private KeyBoxData kba;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install_key_box);

        Switch fileSwitch = (Switch) findViewById(R.id.file_switch);
        EditText idEdtTxt = (EditText) findViewById(R.id.deviceID_editText);
        EditText devKeyEdtTxt = (EditText) findViewById(R.id.deviceKey_editText);
        EditText keyDataEdtTxt = (EditText) findViewById(R.id.keyData_editText);
        EditText crcEdtTxt = (EditText) findViewById(R.id.CRC_editText);
        TextView deviceIDDetected = (TextView) findViewById(R.id.deviceKey_editText);

        // Define using the file switch behavior
        fileSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "fileSwitch onClick() triggered");
                Switch fileSwitch = (Switch) findViewById(R.id.file_switch);
                EditText fileLocEntry = (EditText) findViewById(R.id.fileloc_EdtVw);
                EditText idEdtTxt = (EditText) findViewById(R.id.deviceID_editText);
                EditText devKeyEdtTxt = (EditText) findViewById(R.id.deviceKey_editText);
                EditText keyDataEdtTxt = (EditText) findViewById(R.id.keyData_editText);
                EditText crcEdtTxt = (EditText) findViewById(R.id.CRC_editText);

                if (fileSwitch.isChecked()) {
                    fileLocEntry.setEnabled(true);
                    fileLocEntry.hasFocus();
                    idEdtTxt.setEnabled(false);
                    devKeyEdtTxt.setEnabled(false);
                    keyDataEdtTxt.setEnabled(false);
                    crcEdtTxt.setEnabled(false);
                } else {
                    fileLocEntry.setText("");
                    fileLocEntry.setEnabled(false);
                    idEdtTxt.setEnabled(true);
                    devKeyEdtTxt.setEnabled(true);
                    keyDataEdtTxt.setEnabled(true);
                    crcEdtTxt.setEnabled(true);
                }
            }
        });

        // Restore the app's previous state
        if (savedInstanceState != null) {
            kba.setDeviceID(savedInstanceState.getString(
                    KeyBoxData.DEVICE_KEY_STRING, "Not Defined Yet"));
            kba.setDeviceKey(savedInstanceState.getString(
                    KeyBoxData.DEVICE_KEY_STRING, "Not Defined Yet"));
            kba.setKeyData(savedInstanceState.getString(
                    KeyBoxData.KEY_DATA_STRING, "Not Defined Yet"));
            kba.setCrc(savedInstanceState.getString(
                    KeyBoxData.CRC_STRING, "Not Defined Yet"));

            idEdtTxt.setText(kba.getDeviceID());
            devKeyEdtTxt.setText(kba.getDeviceKey());
            keyDataEdtTxt.setText(kba.getKeyData());
            crcEdtTxt.setText(kba.getCrc());
        }

        Button pgmKeyBoxBtn = (Button) findViewById(R.id.pgm_keybox_btn);
        pgmKeyBoxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Switch fileSwitch = (Switch) findViewById(R.id.file_switch);
                EditText fileLocEntry = (EditText) findViewById(R.id.fileloc_EdtVw);
                EditText idEdtTxt = (EditText) findViewById(R.id.deviceID_editText);
                EditText devKeyEdtTxt = (EditText) findViewById(R.id.deviceKey_editText);
                EditText keyDataEdtTxt = (EditText) findViewById(R.id.keyData_editText);
                EditText crcEdtTxt = (EditText) findViewById(R.id.CRC_editText);

                // If a file has been specified, process it and fill in the fields first
                if (fileSwitch.isChecked()) {
                    File f = new File(fileLocEntry.getText().toString());
                    KeyBoxData kba = null;
                    if (f.exists()) {
                        ProcessWidevineKeyboxes processkbs = new ProcessWidevineKeyboxes(f);
                        ArrayList<KeyBoxData> kbaList = processkbs.getKba();

                        // a list was returned just get the first one for now
                        if (kbaList != null) {
                            Properties properties = System.getProperties();
                            String serialNo = properties.getProperty("ro.serialno");

                            /* Find the keybox data object whose device id matches target serial no
                               -- use the lsat one if not found
                             */
                            for(int i = 0; i < kbaList.size(); i++) {
                                kba = kbaList.get(i);
                                if (kba.getDeviceID().equals(serialNo)) {
                                    break;
                                }
                            }
                        }

                        // fill in those edit text fields
                        if (kba != null) {
                            idEdtTxt.setText(kba.getDeviceID());
                            devKeyEdtTxt.setText(kba.getDeviceKey());
                            keyDataEdtTxt.setText(kba.getKeyData());
                            crcEdtTxt.setText(kba.getCrc());
                        }
                    }
                }

                // get device id text
                String devIdStr = idEdtTxt.getText().toString();
                Log.d(TAG, "devIDStr="+devIdStr);

                // get device key text
                String devKeyStr = devKeyEdtTxt.getText().toString();

                // get dey data text
                String keyDataStr = keyDataEdtTxt.getText().toString();

                // get CRC
                String crcStr = crcEdtTxt.getText().toString();

                String command = "/system/bin/install-file-key-box " +
                        devIdStr + " " + devKeyStr + " " + keyDataStr + " " + crcStr;
                command = command.toLowerCase();
                Log.i(TAG, "pgmKeyBoxBtn onClick(): Command to execute: " + command);

                try {
                    Process process = Runtime.getRuntime().exec(command);

                    // Reads stdout.
                    // NOTE: You can write to stdin of the command using
                    //       process.getOutputStream().
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    int read;
                    char[] buffer = new char[4096];
                    StringBuilder output = new StringBuilder();
                    while ((read = reader.read(buffer)) > 0) {
                        output.append(buffer, 0, read);
                    }
                    reader.close();

                    // Waits for the command to finish.
                    process.waitFor();

                    // place standard output into response text view widget
                    TextView rspTxtView = (TextView) findViewById(R.id.responseTxtView);
                    rspTxtView.setText(output.toString());
                } catch (IOException ioe) {
                    Log.e(TAG, "pgmKeyBoxBtn onClick(): " + ioe.toString());
                } catch (InterruptedException ie) {
                    Log.e(TAG, "pgmKeyBoxBtn onClick(): " + ie.toString());
                }
            }
        });

        // dusplay device id at top of activity
        Properties properties = System.getProperties();
        String serialNo = properties.getProperty("ro.serialno");
        if (serialNo != null) {
            deviceIDDetected.setText(getResources().getString(R.string.device_id_txt_view_prefix)
                                     + " " + serialNo);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        // switching tasks, app being replaced by OS, etc., preserve state
        super.onSaveInstanceState(state);
        if (kba != null) {
            state.putString(KeyBoxData.DEVICE_ID_STRING, kba.getDeviceID());
            state.putString(KeyBoxData.DEVICE_KEY_STRING, kba.getDeviceKey());
            state.putString(KeyBoxData.KEY_DATA_STRING, kba.getKeyData());
            state.putString(KeyBoxData.CRC_STRING, kba.getCrc());
        }

    }
}
