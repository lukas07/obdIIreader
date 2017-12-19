package com.example.lukas.bluetoothtest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.AbsoluteLoadCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.fuel.FuelLevelCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_21_40;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_41_60;
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

    Thread initThread = new Thread(new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < initCmds.length; i++) {
                try {
                    initCmds[i].run(btSocket.getInputStream(), btSocket.getOutputStream());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(CLASS, "Error while Init of OBD Adapter: " + initCmds[i].getName());
                    new IOException();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(CLASS, "Error while Init of OBD Adapter: " + initCmds[i].getName());
                    new IOException();
                }
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

        // Initialisieren des OBD Adapters beim erstmaligen Abrufen der Daten seitdem das Bluetooth-Socket vorhanden ist
        if(!MainActivity.obd_initialized) {
            initThread.start();
            try {
                // 10 Sekunden abwarten, ob Initialisierung abgeschlossen werden kann
                initThread.join(10000);
                Log.e(CLASS, "Initialization interrupted");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(CLASS, "Initialization has finished successfully");
            }
            // Falls der Thread nach 10 Sekunden immer noch läuft --> abbrechen und Verbindung schließen
            if(initThread.isAlive()) {
                Log.e(CLASS, "Initialization still alive-->interrupt");
                initThread.interrupt();
                //socket.close();
                throw new IOException();
            } else {
                AvailablePidsCommand_01_20 pids = new AvailablePidsCommand_01_20();
                AvailablePidsCommand_21_40 pids2 = new AvailablePidsCommand_21_40();
                AvailablePidsCommand_41_60 pids3 = new AvailablePidsCommand_41_60();
                try {
                    pids.run(socket.getInputStream(), socket.getOutputStream());
                    String test = pids.getResult();
                    pids2.run(socket.getInputStream(), socket.getOutputStream());
                    test = pids2.getResult();
                    pids3.run(socket.getInputStream(), socket.getOutputStream());
                    test = pids3.getResult();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // dauerhafte Abfrage der OBD-Daten starten
                t.start();
            }
        } else {
            t.start();
        }

    }

    public void closeSocket() throws IOException {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error: Closing socket");
            throw new IOException();
        }
    }


    private void sendObdCommands() throws InterruptedException {
        Bundle bundle = new Bundle();
        Message message;

        while(!Thread.currentThread().isInterrupted()) {
            SpeedCommand speedCmd = new SpeedCommand();
            MassAirFlowCommand mafCmd = new MassAirFlowCommand();
            ConsumptionRateCommand consCmd = new ConsumptionRateCommand();
            try {
                speedCmd.run(socket.getInputStream(), socket.getOutputStream());
                //mafCmd.run(socket.getInputStream(), socket.getOutputStream());
                consCmd.run(socket.getInputStream(), socket.getOutputStream());

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
            String consumption = consCmd.getFormattedResult();
            bundle.putString("consumption", consumption);

            message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    public void stopSendOBDCommands() {
        t.interrupt();
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
