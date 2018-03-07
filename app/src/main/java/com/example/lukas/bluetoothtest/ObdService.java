package com.example.lukas.bluetoothtest;

import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.github.pires.obd.commands.fuel.ConsumptionRateCommand;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_01_20;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_21_40;
import com.github.pires.obd.commands.protocol.AvailablePidsCommand_41_60;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.NoDataException;
import com.github.pires.obd.exceptions.UnableToConnectException;

import java.io.IOException;

import static com.example.lukas.bluetoothtest.MainActivity.socket;

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


    private SharedPref sharedPreferences;
    private BluetoothSocket btSocket;
    private Handler handler;

    private int cnt_NoData = 0;

    private ObdCommand[] initCmds = {new EchoOffCommand(), new LineFeedOffCommand(), new TimeoutCommand(60), new SelectProtocolCommand(ObdProtocols.AUTO)};

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

    // Sendet, die Initialisierungbefehle an den OBD-Adapter
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

    // Beim erstmaligen Aufruf wird die OBD-Schnittstelle initialisiert und anschließend die dauerhafte Abfrage der Daten gestartet;
    // bei wiederholenden Aufrufen wird nur die dauerhafte Abfrage gestartet (da Initialisierung nur einmal nötig ist)
    public void initObdConnection() throws IOException{
        if (socket != null) {
            this.btSocket = socket;
        } else {
            Log.e(CLASS, "No socket connected");
            throw new IOException();
        }

        // Initialisieren des OBD Adapters beim erstmaligen Abrufen der Daten seitdem das Bluetooth-Socket vorhanden ist
        sharedPreferences  = new SharedPref(this);
        if(!sharedPreferences.getObdInitialized()) {
            initThread.start();
            // Benachrichtigung an UI-Activity, um Ladebalken anzuzeigen
            handler.sendEmptyMessage(INIT_STARTED);

            try {
                // 10 Sekunden abwarten, ob Initialisierung abgeschlossen werden kann
                initThread.join(10000);
                Log.e(CLASS, "Thread came out of join");
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(CLASS, "Error in Thread.join");
            }

            // Falls der Thread nach 10 Sekunden immer noch läuft --> abbrechen
            if(initThread.isAlive()) {
                Log.e(CLASS, "Initialization still alive-->interrupt");
                initThread.interrupt();
                // Benachrichtigung an UI-Activity, dass Initialisierung abgebrochen wurde --> Ladebalken nicht mehr anzeigen
                handler.sendEmptyMessage(INIT_STOPPED);
                //socket.close();
                throw new IOException();
            // anders wird das dauerhafte Senden der OBD-Commands gestartet
            } else {
                AvailablePidsCommand_01_20 pids = new AvailablePidsCommand_01_20();
                AvailablePidsCommand_21_40 pids2 = new AvailablePidsCommand_21_40();
                AvailablePidsCommand_41_60 pids3 = new AvailablePidsCommand_41_60();
                try {
                    pids.run(socket.getInputStream(), socket.getOutputStream());
                    String test = pids.getCalculatedResult();
                    pids2.run(socket.getInputStream(), socket.getOutputStream());
                    test = pids2.getCalculatedResult();
                    //pids3.run(socket.getInputStream(), socket.getOutputStream());
                    //test = pids3.getCalculatedResult();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // dauerhafte Abfrage der OBD-Daten starten
                sendThread.start();
                // Benachrichtigung an UI-Activity, dass Initialisierung erfolgreich war --> Ladebalken nicht mehr anzeigen, Stop-Button anzeigen
                handler.sendEmptyMessage(INIT_SUCCESS);

                // Initialisierungs-Kennzeichen auf true setzen
                sharedPreferences.setObdInitialized(true);
                //MainActivity.obd_initialized = true;
            }
        // bei wiederholenden Starten des Services wird nur das Senden der Commands gestartet (z.B. nach Orientierungswechsel)
        } else {
            sendThread.start();
            handler.sendEmptyMessage(INIT_SUCCESS);
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

    // Sendet die OBD-Befehle an den Adapter und meldet die Ergebnisse an die UI-Activity
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
                //consCmd.run(socket.getInputStream(), socket.getOutputStream());
                cnt_NoData = 0;

            } catch (NoDataException nde) {
                nde.printStackTrace();
                Log.e(CLASS, "No Data Exception thrown");
                //Thread.currentThread().interrupt();
                cnt_NoData++;
                if(cnt_NoData > 5);
                    handler.sendEmptyMessage(NODATA_EXCEPTION);
            } catch (UnableToConnectException utce){
                utce.printStackTrace();
                Log.e(CLASS, "Connection to the OBD-Adapter has been interrupted");
                Thread.currentThread().interrupt();
                handler.sendEmptyMessage(CONNECT_EXCEPTION);
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
            //String consumption = consCmd.getFormattedResult();
            //bundle.putString("consumption", consumption);

            message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
        }
    }

    public void stopSendOBDCommands() {
        sendThread.interrupt();
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
        if(initThread.isAlive())
            initThread.interrupt();
        if(sendThread.isAlive())
            sendThread.interrupt();
    }

    public class ObdServiceBinder extends Binder{
        public ObdService getService() {
            return ObdService.this;
        }
    }
}
