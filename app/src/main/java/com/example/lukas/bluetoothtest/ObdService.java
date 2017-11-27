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
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
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

import static com.example.lukas.bluetoothtest.MainActivity.socket;

/**
 * Created by Lukas on 08.10.2017.
 */

public class ObdService extends Service {
    private static final String CLASS = ObdService.class.getName();


    private BluetoothSocket btSocket;
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

    public void initObdConnection() throws IOException{
        if (socket != null) {
            this.btSocket = socket;
        } else {
            Log.e(CLASS, "No socket connected");
            throw new IOException();
        }
        // Initialisieren des OBD Adapters
        for (int i=0; i < initCmds.length; i++) {
            try {
                initCmds[i].run(btSocket.getInputStream(), btSocket.getOutputStream());
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(CLASS, "Error while Init of OBD Adapter: " + initCmds[i].getName());
                new IOException();
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
            SpeedCommand speedCmd = new SpeedCommand();
            MassAirFlowCommand mafCmd = new MassAirFlowCommand();
            try {
                speedCmd.run(socket.getInputStream(), socket.getOutputStream());
                //mafCmd.run(socket.getInputStream(), socket.getOutputStream());

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(CLASS, "Error while sending OBD commands");
                Thread.currentThread().interrupt();
            }
            String speed = speedCmd.getFormattedResult();
            bundle.putString("speed", speed);

            // Verbrauch berechnen: (3600*MAF)/(9069.90*VSS)
            //int fuelConsumption = (int) ((int)(3600*mafCmd.getMAF())/(9069.90*speedCmd.getMetricSpeed()));
            //bundle.putString("fuel", String.valueOf(fuelConsumption));

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
