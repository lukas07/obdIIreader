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
 * Author: Lukas Breit
 *
 * Description: The OBDService is responsible for sending the OBD commands to the adapter and receiving the responses of it.
 *              The received data is send to the bounded activity (RunningTripActivity).
 *
 */

public class ObdService extends Service {
    private static final String CLASS = ObdService.class.getName();

    // Constants of the handler
    // Progress Bar
    private static final int INIT_STARTED = 1;
    private static final int INIT_SUCCESS = 2;
    private static final int INIT_STOPPED = 3;
    // Exception messages for the UI activity
    private static final int NODATA_EXCEPTION = 10;
    private static final int CONNECT_EXCEPTION = 11;

    // Flag weather the initialisation has been completed successfully
    private boolean initReturn = false;


    private BluetoothSocket btSocket;
    private Handler handler;

    private boolean initSuccess = false;
    private int cnt_NoData = 0;

    private ObdCommand[] initCmds = {new ObdResetCommand(), new EchoOffCommand(), new LineFeedOffCommand(), new TimeoutCommand(60), new SelectProtocolCommand(ObdProtocols.AUTO)};
    private ObdCommand[] sendCmds = {new SpeedCommand(), new MassAirFlowCommand()};

    // Thread for sending the OBD commands
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

    // Before starting the data exchange with the interface the OBD adapter has to be initialized. Afterwards the permanent exchange
    // is started
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
            // After the adapter is initialized successfully the progress bar is set unvisible
            initSuccess = true;
        } else {
            Log.e(CLASS, "OBD-Adapter could not be initialized successfully");
            handler.sendEmptyMessage(INIT_STOPPED);
            throw new IOException();
        }
    }

    // Initialize the OBD adapter
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
            }
        });
        initThread.start();
        try {
            // Wait 5 seconds weather the initialisation is completed. Is necessary, because it won't be stopped if it is not possible to complete it
            initThread.join(5000);
            Log.e(CLASS, "Thread came out of join");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(CLASS, "Error in Thread.join");
        }

        // If the Thread is still running after 5 seconds interrupt it
        if(initThread.isAlive()) {
            Log.e(CLASS, "Initialization still alive-->interrupt");
            initThread.interrupt();
            return false;
        // Otherwise the process has finished
        } else if (initReturn == false){
            return false;
        } else {
            return true;
        }
    }

    // Sends permanently the OBD commands to the adapter and sends the results to the UI activity
    private void sendObdCommands() throws InterruptedException {
        Bundle bundle = new Bundle();
        Message message;

        while(!sendThread.isInterrupted()) {
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

                    // After the init process hide the progress bar and start timer
                    if (initSuccess) {
                        handler.sendEmptyMessage(INIT_SUCCESS);
                        initSuccess = false;
                    }
                    cnt_NoData = 0;
                } catch (NoDataException nde) {
                    nde.printStackTrace();
                    Log.e(CLASS, "No Data Exception thrown");
                    cnt_NoData++;
                    // In some situations the adapter is not available (e.g. when starting the car)
                    // --> wait 5 times the exception is thrown until the init process is restarted
                    if (cnt_NoData > 5) {
                        if(runInitCmds()) {
                            Log.e(CLASS, "OBD has been re-inited");
                        } else {
                            Log.e(CLASS, "OBD couldn't be inited again --> NO_DATA-Exception");
                            handler.sendEmptyMessage(NODATA_EXCEPTION);
                        }
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

    // Starts the permanent sending of the OBD commands (used when bluetooth connection has been disconnected)
    public void startSendOBDCommands() {
        if(!sendThread.isInterrupted())
            sendThread.interrupt();
        // Create new thread
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
