package com.example.lukas.bluetoothtest;

import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.control.ModuleVoltageCommand;
import com.github.pires.obd.commands.engine.OilTempCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AirIntakeTemperatureCommand;
import com.github.pires.obd.enums.FuelType;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;


public class MainActivity extends AppCompatActivity {
    // Konstanten
    private static final String CLASS = MainActivity.class.getName();
    private static final int REQUEST_ENABLE_BT = 1234;

    // Attribute
    private boolean bluetoothEnabled = false;
    private BluetoothAdapter btAdapter;
    private BluetoothDevice btdevice;
    private ObdService service;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.e(CLASS, "Service connected");
            service = ((ObdService) binder).getService();
            try {
                service.initObdConnection(btdevice.getAddress());
                btn_connectDev.setText(getResources().getString(R.string.dev_connected));
            } catch (IOException ioe) {
                unbindService(serviceConnection);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(CLASS, "Service disconnected");
        }
    };

    private Button btn_activateBt;
    private Button btn_connectDev;
    private Button btn_startTrip;
    private Button btn_stopTrip;



    private TextView btStatusText;
    private boolean obdDeviceConnected = false;

    private BluetoothSocket socket;
    private Button btOilTemp;
    private Button btModVol;
    private Button btConnect;
    private Button send;
    private String connectedDeviceName = null;

    private String answer;


    //Konstanten


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btn_activateBt = (Button) findViewById(R.id.btn_activateBt);
        btn_activateBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateBt();
            }
        });


        btn_connectDev = (Button) findViewById(R.id.btn_connectDev);
        btn_connectDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDevice();
            }
        });

        btn_startTrip = (Button) findViewById(R.id.btn_startTrip);
        btn_startTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTrip();
                btn_stopTrip.setEnabled(true);
            }
        });

        btn_stopTrip = (Button) findViewById(R.id.btn_stopTrip);
        btn_stopTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrip();
            }
        });





        btOilTemp = (Button) findViewById(R.id.btOilTmp);
        btOilTemp.setEnabled(false);
        btOilTemp.setText("AirTemp");
        btOilTemp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObdCommand oilTmp = new AirIntakeTemperatureCommand();
                try {
                    oilTmp.run(socket.getInputStream(), socket.getOutputStream());
                    answer = oilTmp.getFormattedResult();
                    showToast("OilTemp: " + answer, Toast.LENGTH_LONG);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(MainActivity.class.getName(), "OilTmp IO Exception");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(MainActivity.class.getName(), "OilTmp InterruptedException");
                }
            }
        });

        btModVol = (Button) findViewById(R.id.btModVol);
        btModVol.setEnabled(false);
        btModVol.setText("Speed");
        btModVol.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ObdCommand speed = new SpeedCommand();
                try {
                    speed.run(socket.getInputStream(), socket.getOutputStream());
                    answer = speed.getFormattedResult();
                    showToast("Speed: " + answer, Toast.LENGTH_LONG);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void showToast(String message, int duration) {
        Toast.makeText(MainActivity.this, message, duration).show();
    }


    // Bluetooth-Verbindung wird aktiviert
    public void activateBt () {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null) {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.bt_support), Toast.LENGTH_SHORT).show();
            Log.e(CLASS, "No Bluetooth support");
        } else {
            // Bluetooth ggf. aktivieren
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Log.e(CLASS, "Bluetooth already enabled");
                btn_connectDev.setEnabled(true);
                bluetoothEnabled = true;
                btn_activateBt.setText(getResources().getString(R.string.bt_enabled));
                btn_activateBt.setEnabled(false);
            }
        }
    }


    // Liste der gekoppelten Geräte anzeigen und Verbindung zu ausgewähltem Gerät aufbauen
    public void selectDevice() {
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

                    btdevice = btAdapter.getRemoteDevice(deviceAddress);
                    Log.e(CLASS, "Selected device: " + btdevice);
                    btn_connectDev.setText(getResources().getString(R.string.dev_selected));
                    btn_connectDev.setEnabled(false);
                    /*UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                    try {
                        socket = device.createRfcommSocketToServiceRecord(uuid);
                        socket.connect();
                        Log.e(MainActivity.class.getName(), "connected to the device " + device.getName());
                        btOilTemp.setEnabled(true);
                        btModVol.setEnabled(true);
                        obdDeviceConnected = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(MainActivity.class.getName(), "Not able to connect to the device");
                    }


                    Toast.makeText(MainActivity.this, "You have selected " + deviceData, Toast.LENGTH_SHORT).show();

                    if(obdDeviceConnected) {
                        try {
                            new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ;
                        try {
                            new TimeoutCommand(62).run(socket.getInputStream(), socket.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ;
                        try {
                            new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        ;



                        ObdCommand job = new FuelLevelCommand();
                        try {
                            job.run(socket.getInputStream(), socket.getOutputStream());
                            answer = job.getFormattedResult();
                            Toast.makeText(MainActivity.this, "FuelLevel: " + answer, Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(MainActivity.class.getName(), "FuelLevel IO Exception");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.e(MainActivity.class.getName(), "FuelLevel InterruptedException");
                        }

                    }*/
                }
            });
            btDialog.setTitle(getResources().getString(R.string.dev_title));
            btDialog.setIcon(R.mipmap.ic_launcher);
            btDialog.show();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.bt_enabled), Toast.LENGTH_SHORT).show();
                Log.e(CLASS, "Bluetooth enabled");
                btn_activateBt.setText(getResources().getString(R.string.bt_enabled));
                btn_activateBt.setEnabled(false);
                btn_connectDev.setEnabled(true);
                bluetoothEnabled = true;
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, getResources().getString(R.string.bt_notEnabled), Toast.LENGTH_SHORT).show();
                Log.e(CLASS, "Error while enabling Bluetooth");
            }
        }
    }

    // OBDService binden
    private void startTrip() {
        Log.e(CLASS, "Bind OBD-Service");
        Intent serviceIntent = new Intent(this, ObdService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopTrip() {
        Log.e(CLASS, "Unbind Service");
        unbindService(serviceConnection);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error while closing socket");
        }
    }
}

// Nach Geräten suchen
        /*btSearch = (Button) findViewById(R.id.bt_search);
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
*/



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

    /*protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case NO_BT_SUPPORT:
                builder.setMessage("Your device does not support Bluetooth!");
                return builder.create();
            case BT_DISABLED:
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return builder.create();
        }

        return null;
    }
}*/