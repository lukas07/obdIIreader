package com.example.lukas.bluetoothtest;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Lukas on 28.11.2017.
 */

public class TripOpenHelper extends SQLiteOpenHelper {
    private static final String CLASS = TripOpenHelper.class.getName();

    private static Context context;
    private static final String DB_NAME = "trips.db";
    private static final int DB_VERSION = 1;

    private static TripOpenHelper instance;

    public static final String TABLE_NAME_TRIPS = "trips";
    public static final String ROWID = "_id";
    public static final String COL_DRIVER_NAME = "driver";
    public static final String COL_DRIVE_MODE = "mode";
    public static final String COL_MILEAGE_START = "mileageStart";
    public static final String COL_MILEAGE_END = "mileageEnd";
    public static final String COL_TS_START = "tsStart";
    public static final String COL_TS_END = "tsEnd";
    public static final String COL_ADDRESS_START = "addressStart";
    public static final String COL_ADDRESS_END = "addressEnd";

    private static final String CREATE_TABLE_TRIPS =
            "CREATE TABLE " + TABLE_NAME_TRIPS +" (" +
                    ROWID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_DRIVER_NAME + " TEXT NOT NULL, " +
                    COL_DRIVE_MODE + " TEXT NOT NULL, " +
                    COL_MILEAGE_START + " INTEGER NOT NULL, " +
                    COL_MILEAGE_END + " INTEGER NOT NULL, " +
                    COL_TS_START + " INTEGER NOT NULL, " +
                    COL_TS_END + " INTEGER NOT NULL, " +
                    COL_ADDRESS_START + " TEXT NOT NULL, " +
                    COL_ADDRESS_END + " TEXT NOT NULL);";

    private static final String DROP_TABLE_TRIPS =
            "DROP TABLE IF EXISTS " + TABLE_NAME_TRIPS + ";";


    public static synchronized TripOpenHelper getHelper(Context parContext) {
        if(instance == null) {
            instance = new TripOpenHelper(parContext);
        }
        return instance;
    }


    public TripOpenHelper(Context parContext) {
        super(parContext, DB_NAME, null, DB_VERSION);
        context = parContext;
    }

    /*
    // TODO Datensatz in DB schreiben
    public void insert(String driver, int startMileage, int endMileage, String driveMode, long startTimestamp, long endTimestamp, String startAddress, String endAddress) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_DRIVER_NAME, driver);
            values.put(COL_MILEAGE_START, startMileage);
            values.put(COL_MILEAGE_END, endMileage);
            values.put(COL_DRIVE_MODE, driveMode);
            values.put(COL_TS_START, startTimestamp);
            values.put(COL_TS_END, endTimestamp);
            //values.put(COL_ADDRESS_START, startAddress);
            //values.put(COL_ADDRESS_END, endAddress);
            // TODO nur zum Testen, solange keine Adresse ermittelt wird
            values.put(COL_ADDRESS_START, "start");
            values.put(COL_ADDRESS_END, "end");
            db.insertOrThrow(TABLE_NAME_TRIPS, null, values);
            Toast.makeText(context, R.string.stop_inserted, Toast.LENGTH_SHORT).show();
        } catch (SQLiteException e) {
            Toast.makeText(context, R.string.stop_insert_failed, Toast.LENGTH_LONG).show();
            Log.e(CLASS, "Error while inserting record: " + e.getLocalizedMessage());
        }
    }

    // TODO Alle Datensätze aus DB auslesen
    public Cursor query() {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TABLE_NAME_TRIPS, null, null, null, null, null, COL_TS_START + " DESC");
    }
    */

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRIPS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Was muss hier gemacht werden?
    }
}
