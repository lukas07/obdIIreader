package com.example.lukas.bluetoothtest.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
 * Created by Lukas on 06.10.2017.
 */

public class DeviceListActivity extends Activity {
    private static final String TAG = "DeviceListActivity";

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private Button bt_scan;
    // Gibt an, ob bereits einmal eine Suche gestartet wurde; falls "true" --> die ermittelten Devices werden in saveInstance gespeichert
    // und bei Erzeugung der neuen Activity angezeigt
    private boolean discoveryDone = false;
    // Die Variable wird auf "true" gesetzt, falls eine Suche (startDiscovery()) gerade läuft. Nach Beenden der Suche (ACTION_DISCOVERY_FINISHED)
    // wird die Variable wieder auf false gesetzt. Wird benötigt, da bei Status "true" die Suche in der onDestroy-Methode nicht gestoppt wird und
    // diverse Anpassungen bei der Erzeugung der neuen Activity durchgeführt werden (Bsp.:anderer Titel, Scan-Button gesperrt,...)
    private boolean discoveryRunning = false;

    private BluetoothAdapter btAdapter;
    private ArrayAdapter<String> newDevicesArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Receiver zur Bluetooth-Status Überwachung registrieren
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);

        // Broadcast, wenn neue Geräte erkannt werden
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(deviceReceiver, filter);

        // Broadcast, wenn Suche nach Geräten beendet wurde
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

        // ListView für gekoppelte Geräte
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(deviceClickListener);

        // ListView für neue Geräte
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(newDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(deviceClickListener);


        btAdapter = BluetoothAdapter.getDefaultAdapter();


        // Falls bereits nach neuen Devices gesucht wurde den Listview mit den Ergebnissen anzeigen
        if(savedInstanceState != null) {
            discoveryDone = savedInstanceState.getBoolean("discovery");
            if(discoveryDone) {
                // Turn on sub-title for new devices
                findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
                // Listview füllen
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

    // Receiver zur Suche weiterer Devices
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
                setTitle(R.string.devList_selDev);
                discoveryRunning = false;
                if (newDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getString(R.string.devList_noFound);
                    newDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

    // Receiver, der Veränderungen des Bluetooth-Status registriert
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

        // Falls eine Suche noch läuft, obwohl die das Kennzeichen discoveryRunning auf false gesetzt ist, die Suche stoppen
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
        Log.d(TAG, "doDiscovery()");

        // Nach dem ersten Suchen nach weiteren Devices wird der Listview der neuen Devices dauerhaft angezeigt
        discoveryDone = true;

        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.devList_scanning);

        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // Die Elemente des Adapters vor der Suche löschen
        newDevicesArrayAdapter.clear();

        // If we're already discovering, stop it
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        btAdapter.startDiscovery();

        discoveryRunning = true;
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
}
