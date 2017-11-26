package com.example.lukas.bluetoothtest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Lukas on 08.10.2017.
 */

public class ObdService extends Service {
    private static final String CLASS = ObdService.class.getName();

    private BluetoothSocket socket;
    private BluetoothDevice device;
    private Handler handler;

    private ObdCommand[] initCmds = {new EchoOffCommand(), new LineFeedOffCommand(), new TimeoutCommand(60), new SelectProtocolCommand(ObdProtocols.AUTO)};

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

    public void initObdConnection(BluetoothDevice btDevice) throws IOException {
        device = btDevice;
        try{
            socket = BluetoothConnector.connectDevice(device);
        } catch (IOException ioe) {
            throw new IOException();
        }

        // Initialisieren des OBD Adapters
        for (int i=0; i < initCmds.length; i++) {
            try {
                initCmds[i].run(socket.getInputStream(), socket.getOutputStream());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(CLASS, "Error while Init off OBD Adapter: " + initCmds[i].getName());
            }
        }

        t.start();
    }

    public void closeSocket() throws IOException {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error: Closing socket");
            new IOException();
        }
    }


    private void sendObdCommands() throws InterruptedException {
        Bundle bundle = new Bundle();
        Message message;

        while(!Thread.currentThread().isInterrupted()) {
            // TODO dauerhafte Abfrage der Daten --> UI-Update
            ObdCommand speedCmd = new SpeedCommand();
            try {
                speedCmd.run(socket.getInputStream(), socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(CLASS, "Error in " + speedCmd.getName());
                Thread.currentThread().interrupt();
            }
            String obdResult = speedCmd.getFormattedResult();
            bundle.putString("result", obdResult);
            message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ObdServiceBinder();
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(CLASS, "Destroying service");
        if(t != null) {
            t.interrupt();
        }
    }

    public class ObdServiceBinder extends Binder{
        public ObdService getService() {
            return ObdService.this;
        }
    }
}
