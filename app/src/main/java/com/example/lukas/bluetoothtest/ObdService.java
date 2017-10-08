package com.example.lukas.bluetoothtest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Created by Lukas on 08.10.2017.
 */

public class ObdService extends Service {
    private static final String CLASS = ObdService.class.getName();

    private BluetoothSocket socket;
    private BluetoothDevice device;

    Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                sendObdCommands();
            } catch (InterruptedException e) {
                t.interrupt();
            }

        }
    });

    public void initObdConnection(String devAddress) throws IOException {
        device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(devAddress);
        try{
            socket = BluetoothConnector.connectDevice(device);
        } catch (IOException ioe) {
            throw new IOException();
        }

        // EchoOff ...

        t.start();
    }


    private void sendObdCommands() throws InterruptedException {
        while(!Thread.currentThread().isInterrupted()) {
            // TO-DO
            //dauerhafte Abfrage der Daten --> UI-Update
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ObdService getService() {
        return ObdService.this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(CLASS, "Destroying service");

        t.interrupt();
    }
}
