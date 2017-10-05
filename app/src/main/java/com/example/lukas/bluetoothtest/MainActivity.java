package com.example.lukas.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter btAdapter;
    private Button btAcivation;
    private Button btSearch;
    private Button send;
    private String connectedDeviceName = null;
    private StringBuffer outStringBuffer;

    //Konstanten
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bluetooth aktivieren
        btAcivation = (Button) findViewById(R.id.bt_activation);
        btAcivation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Bluetooth Adapter
                btAdapter = BluetoothAdapter.getDefaultAdapter();
                if(btAdapter == null) {
                    Toast.makeText(MainActivity.this, "No Bluetooth supported!", Toast.LENGTH_SHORT).show();
                } else {
                    // Bluetooth ggf. aktivieren
                    if (!btAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {
                        Toast.makeText(MainActivity.this, "Bluetooth already enabled", Toast.LENGTH_SHORT).show();
                        btSearch.setEnabled(true);
                    }
                }
            }
        });

        // Nach Geräten suchen
        btSearch = (Button) findViewById(R.id.bt_search);
        btSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ArrayList deviceStr = new ArrayList();
                final ArrayList devices = new ArrayList();

                Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        deviceStr.add(device.getName() + "\n" + device.getAddress());
                        devices.add(device.getAddress());
                    }

                    final AlertDialog.Builder btDialog = new AlertDialog.Builder(MainActivity.this);
                    final ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.select_dialog_singlechoice, deviceStr.toArray(new String[deviceStr.size()]));

                    btDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            String deviceAddress = (String) devices.get(position);
                            String deviceData = (String) deviceStr.get(position);

                            // Verbindung aufbauen
                            BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
                            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                            try {
                                BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                                socket.connect();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            Toast.makeText(MainActivity.this, "You have selected " + deviceData, Toast.LENGTH_SHORT).show();
                        }
                    });
                    btDialog.setTitle("Choose a device");
                    btDialog.setIcon(R.mipmap.ic_launcher);
                    btDialog.show();




/*                    dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    dialogBuilder.setAdapter(arrayBtNames, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String selDev = arrayBtNames.getItem(which);
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "You have selected: " + selDev, Toast.LENGTH_LONG).show();
                        }
                    });
                    dialogBuilder.show();*/
                }
            }
        });
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "Bluetooth enabled!", Toast.LENGTH_SHORT).show();
                btSearch.setEnabled(true);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "Bluetooth not activated!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

                    /*dialogBuilder.setItems(btNames, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            Toast.makeText(MainActivity.this, "You have selected one", Toast.LENGTH_LONG).show();
                        }
                    });
                    AlertDialog dialog = dialogBuilder.create();
                    dialog.show();*/


                    /*LayoutInflater inflater = getLayoutInflater();
                    View convertView = (View) inflater.inflate(R.layout.paired_devices, null);
                    dialogBuilder.setView(convertView);
                    dialogBuilder.setTitle("Paired Devices");
                    ListView lv = (ListView) convertView.findViewById(R.id.listView);
                    ArrayAdapter<String> arrayBtNames = new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_singlechoice, btNames);
                    lv.setAdapter(arrayBtNames);
                    AlertDialog showDialog = dialogBuilder.create();
                    showDialog.show();*/