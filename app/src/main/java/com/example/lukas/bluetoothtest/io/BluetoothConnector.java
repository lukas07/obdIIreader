package com.example.lukas.bluetoothtest.io;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by Lukas on 08.10.2017.
 */

public class BluetoothConnector {
    private static final String CLASS = BluetoothConnector.class.getName();

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    public static BluetoothSocket connectDevice(BluetoothDevice device) throws IOException{
        BluetoothSocket socket = null;
        try{
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error while establishing bluetooth connection");
            throw new IOException();
        }
        return socket;
    }
}
