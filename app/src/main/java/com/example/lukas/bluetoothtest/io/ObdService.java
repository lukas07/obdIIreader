package com.example.lukas.bluetoothtest.io;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.lukas.bluetoothtest.R;
import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.MassAirFlowCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.NoDataException;
import com.github.pires.obd.exceptions.UnableToConnectException;
import com.github.pires.obd.exceptions.UnsupportedCommandException;

import java.io.IOException;

import static com.example.lukas.bluetoothtest.activity.MainActivity.socket;

/**
 * Created by Lukas on 08.10.2017.
 */

public class ObdService extends Service {
    private static final String CLASS = ObdService.class.getName();

    // Konstanten für den Handler
    // Steuerung des Ladebalken
    private static final int INIT_STARTED = 1;
    private static final int INIT_SUCCESS = 2;
    private static final int INIT_STOPPED = 3;
    // Exception-Meldungen für UI-Activity
    private static final int NODATA_EXCEPTION = 10;
    private static final int CONNECT_EXCEPTION = 11;

    // Kennzeichen, ob der Initialisierungs-Thread erfolgreich durchlaufen ist
    private boolean initReturn = false;


    private BluetoothSocket btSocket;
    private Handler handler;

    private boolean initSuccess = false;
    private int cnt_NoData = 0;

    private ObdCommand[] initCmds = {new ObdResetCommand(), new EchoOffCommand(), new LineFeedOffCommand(), new TimeoutCommand(60), new SelectProtocolCommand(ObdProtocols.AUTO)};
    private ObdCommand[] sendCmds = {new SpeedCommand(), new MassAirFlowCommand()};

    // Thread, der das dauerhafte Senden der OBD Commands übernimmt
    Thread sendThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                sendObdCommands();
            } catch (InterruptedException e) {
                sendThread.interrupt();
            }

        }
    });

    // Beim erstmaligen Aufruf wird die OBD-Schnittstelle initialisiert und anschließend die dauerhafte Abfrage der Daten gestartet;
    // bei wiederholenden Aufrufen wird nur die dauerhafte Abfrage gestartet (da Initialisierung nur einmal nötig ist)
    public void startObdConnection() throws IOException{
        if (socket != null) {
            this.btSocket = socket;
        } else {
            Log.e(CLASS, "No socket connected");
            throw new IOException();
        }

        handler.sendEmptyMessage(INIT_STARTED);
        if (runInitCmds()) {
            Log.e(CLASS, "OBD-Adapter successfully initialized");
            sendThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sendObdCommands();
                    } catch (InterruptedException e) {
                        sendThread.interrupt();
                    }

                }
            });
            sendThread.start();
            // Nach dem ersten, erfolgreichen Auslesen von Daten wird der Ladebalken ausgeschaltet
            initSuccess = true;
        } else {
            Log.e(CLASS, "OBD-Adapter could not be initialized successfully");
            handler.sendEmptyMessage(INIT_STOPPED);
            throw new IOException();
        }
    }

    private boolean runInitCmds() {
        final Thread initThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < initCmds.length; i++) {
                    try {
                        initCmds[i].run(btSocket.getInputStream(), btSocket.getOutputStream());
                        if (initCmds[i].getClass() == ObdResetCommand.class) {
                            //Below is to give the adapter enough time to reset before sending the commands, otherwise the first startup commands could be ignored.
                            try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
                        }
                        initReturn = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(CLASS, "Error while Init of OBD Adapter: " + initCmds[i].getName());
                        initReturn = false;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(CLASS, "Error while Init of OBD Adapter: " + initCmds[i].getName());
                       initReturn = false;
                    }
                }
                //return true;
            }
        });
        initThread.start();
        try {
            // 10 Sekunden abwarten, ob Initialisierung abgeschlossen werden kann
            // TODO: Prüfen wie weit niedrig die Zeit gesetzt werden kann
            initThread.join(5000);
            Log.e(CLASS, "Thread came out of join");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error in Thread.join");
        }

        // Falls der Thread nach 10 Sekunden immer noch läuft --> abbrechen
        if(initThread.isAlive()) {
            Log.e(CLASS, "Initialization still alive-->interrupt");
            initThread.interrupt();
            return false;
        // anders wird das dauerhafte Senden der OBD-Commands gestartet
        } else if (initReturn == false){
            return false;
        } else {
            return true;
        }
    }

    // Sendet die OBD-Befehle an den Adapter und meldet die Ergebnisse an die UI-Activity
    private void sendObdCommands() throws InterruptedException {
        Bundle bundle = new Bundle();
        Message message;
        String test = Thread.currentThread().getName();

        while(!sendThread.isInterrupted()) {
            if (sendThread.isInterrupted() || !sendThread.isAlive()) {
                break;
            }
            for (int i=0; i<sendCmds.length; i++) {
                try {
                    sendCmds[i].run(socket.getInputStream(), socket.getOutputStream());
                    String value = sendCmds[i].getFormattedResult();

                    if (sendCmds[i].getClass() == MassAirFlowCommand.class) {
                        MassAirFlowCommand maf = (MassAirFlowCommand) sendCmds[1];
                        SpeedCommand speed = (SpeedCommand) sendCmds[0];
                        int fuelConsumption = (int) ((int) (3600 * maf.getMAF()) / (9069.90 * speed.getMetricSpeed()));
                        value = String.valueOf(fuelConsumption);
                    }
                    bundle.putString(sendCmds[i].getName(), value);

                    // Nach der Initialisierung den Ladebalken abschalten + Timer starten
                    if (initSuccess) {
                        handler.sendEmptyMessage(INIT_SUCCESS);
                        initSuccess = false;
                    }
                    cnt_NoData = 0;
                } catch (NoDataException nde) {
                    nde.printStackTrace();
                    Log.e(CLASS, "No Data Exception thrown");
                    //Thread.currentThread().interrupt();
                    cnt_NoData++;
                    if (cnt_NoData > 5) {
                        if(runInitCmds()) {
                            Log.e(CLASS, "OBD has been re-inited");
                        } else {
                            Log.e(CLASS, "OBD couldn't be inited again --> NO_DATA-Exception");
                            handler.sendEmptyMessage(NODATA_EXCEPTION);
                        }
                        //Thread.currentThread().interrupt();
                        //handler.sendEmptyMessage(NODATA_EXCEPTION);
                    }
                } catch (UnsupportedCommandException uce) {
                    uce.printStackTrace();
                    Log.e(CLASS, "Unsupported Command: " + uce.getMessage());
                    bundle.putString(sendCmds[i].getName(), getApplicationContext().getResources().getString(R.string.obd_unsupported));
                } catch (UnableToConnectException utce) {
                    utce.printStackTrace();
                    Log.e(CLASS, "Connection to the OBD-Adapter has been interrupted");
                    sendThread.interrupt();
                    handler.sendEmptyMessage(CONNECT_EXCEPTION);
                    throw new InterruptedException();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(CLASS, "Error while sending OBD commands");
                    sendThread.interrupt();
                    throw new InterruptedException();
                    //Thread.currentThread().interrupt();
                }
            }
            message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    public void stopSendOBDCommands() {
        if(sendThread.isAlive())
            sendThread.interrupt();
    }

    public void startSendOBDCommands() {
        if(!sendThread.isInterrupted())
            sendThread.interrupt();
        // Neuen Thread erzeugen
        sendThread = null;
        sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendObdCommands();
                } catch (InterruptedException e) {
                    sendThread.interrupt();
                }

            }
        });
        sendThread.start();
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

        Log.e(CLASS, "SEND INTERRUPTED: " + sendThread.isInterrupted());
        sendThread.interrupt();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.e(CLASS, "SEND INTERRUPTED 2: " + sendThread.isInterrupted());
        if(!sendThread.isInterrupted()) {
            sendThread.interrupt();
            Log.e(CLASS, "SEND INTERRUPTED");
        }
    }

    public class ObdServiceBinder extends Binder{
        public ObdService getService() {
            return ObdService.this;
        }
    }
}
