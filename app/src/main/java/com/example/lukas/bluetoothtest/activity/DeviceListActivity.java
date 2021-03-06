package com.example.lukas.bluetoothtest.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.lukas.bluetoothtest.R;

import java.util.ArrayList;
import java.util.Set;

/**
 * Author: Lukas Breit
 *
 * Description:  In the DeviceListActivity are all bounded bluetooth devices listed, you can search for available new devices
 *               and select a device. The selected one is returned to the caller.
 *
 */

public class DeviceListActivity extends Activity{
    private static final String CLASS = "DeviceListActivity";

    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final int REQUEST_ACCESS_COARSE_LOCATION = 1;

    private Button bt_scan;
    // Indicates wheather a search has already been completed once; if true --> save new devices in saveInstance and add them to the new created activity
    private boolean discoveryDone = false;
    // The variable is set to true if a search is still running. It is necessary, because in the case that a search has not finished it is stopped in onDestroy. Furthermore it influences the creation of the new Activity (different title,...)
    private boolean discoveryRunning = false;

    private BluetoothAdapter btAdapter;
    private ArrayAdapter<String> newDevicesArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Receiver for recognizing changes of the bluetooth status
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);

        // Broadcast, if new devices are found
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(deviceReceiver, filter);

        // Broadcast, if a search has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(deviceReceiver, filter);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        bt_scan = (Button) findViewById(R.id.button_scan);
        bt_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setEnabled(false);
            }
        });


        ArrayAdapter<String> pairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        newDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // ListView for bounded devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(deviceClickListener);
        pairedListView.setSelectionFromTop(0, 0);

        // ListView for new devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(deviceClickListener);


        btAdapter = BluetoothAdapter.getDefaultAdapter();


        // If it has been already searched for new devices --> display results in the ListView
        if(savedInstanceState != null) {
            discoveryDone = savedInstanceState.getBoolean("discovery");
            if(discoveryDone) {
                // Turn on sub-title for new devices
                findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
                ArrayList<String> devices = savedInstanceState.getStringArrayList("devices");
                for(int i=0; i<devices.size(); i++) {
                    newDevicesArrayAdapter.add(devices.get(i));
                }
                discoveryRunning = savedInstanceState.getBoolean("running");
                if(discoveryRunning) {
                    bt_scan.setEnabled(false);
                    // Indicate scanning in the title
                    setProgressBarIndeterminateVisibility(true);
                    setTitle(R.string.devList_scanning);

                    // Turn on sub-title for new devices
                    findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
                }
            }
        }

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if(pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getString(R.string.devList_noPaired);
            pairedDevicesArrayAdapter.add(noDevices);
        }
    }

    private AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            btAdapter.cancelDiscovery();

            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            setResult(RESULT_OK, intent);
            finish();
        }
    };

    // Receiver for searching for new devices
    private BroadcastReceiver deviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    newDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                bt_scan.setEnabled(true);
                bt_scan.setText(R.string.devList_scan);
                setTitle(R.string.devList_selDev);
                discoveryRunning = false;
                if (newDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getString(R.string.devList_noFound);
                    newDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    // Receiver that recognizes changes of the bluetooth state
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        finish();
                        break;
                }

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // If a saerch is still running, but discoveryRunning is set to false
        if (btAdapter != null && discoveryRunning == false) {
            btAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(deviceReceiver);
        this.unregisterReceiver(bluetoothReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {
        Log.d(CLASS, "doDiscovery()");

        discoveryDone = true;

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.devList_scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // Clean up the adapter just before the search is started
        newDevicesArrayAdapter.clear();

        // If we're already discovering, stop it
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    AlertDialog.Builder builder= new AlertDialog.Builder(this);
                    builder.setTitle(R.string.devList_permission_title);
                    builder.setMessage(R.string.devList_permission_info);
                    builder.setNeutralButton(R.string.devList_permission_okay, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(DeviceListActivity.this,
                                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                                REQUEST_ACCESS_COARSE_LOCATION);
                                    }
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    btAdapter.startDiscovery();
                    bt_scan.setText(R.string.devList_scanning);
                    discoveryRunning = true;
                    break;
            }
        } else {
            btAdapter.startDiscovery();
            bt_scan.setText(R.string.devList_scanning);

            discoveryRunning = true;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(discoveryDone) {
            outState.putBoolean("discovery", discoveryDone);
            ArrayList<String> devices = new ArrayList<String>();
            for(int i=0; i<newDevicesArrayAdapter.getCount(); i++) {
                String device = newDevicesArrayAdapter.getItem(i).toString();
                devices.add(i, device);
            }
            outState.putStringArrayList("devices", devices);
            outState.putBoolean("running", discoveryRunning);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    btAdapter.startDiscovery();
                    bt_scan.setText(R.string.devList_scanning);
                    discoveryRunning = true;
                } else {
                    Log.d(CLASS, "Permission for disovery denied");
                }
                return;
            }
        }
    }
}
