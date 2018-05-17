package com.example.lukas.bluetoothtest.trip;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Lukas on 28.11.2017.
 */

public class TripOpenHelper extends SQLiteOpenHelper {
    private static final String CLASS = TripOpenHelper.class.getName();

    private static Context context;
    private static final String DB_NAME = "trips.db";
    private static final int DB_VERSION = 1;

    private static TripOpenHelper instance;

    // Trips-Tabelle
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
    public static final String COL_ROUTE_POINTS = "routePoints";
    public static final String COL_REASON = "reason";

    public static final int COL_ID_DRIVER = 1;
    public static final int COL_ID_MODE = 2;
    public static final int COL_ID_STARTMIL = 3;
    public static final int COL_ID_ENDMIL= 4;
    public static final int COL_ID_STARTTS = 5;
    public static final int COL_ID_ENDTS = 6;
    public static final int COL_ID_STARTADD = 7;
    public static final int COL_ID_ENDADD= 8;
    public static final int COL_ID_ROUTE = 9;
    public static final int COL_ID_REASON = 10;

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
                    COL_ADDRESS_END + " TEXT NOT NULL, " +
                    COL_ROUTE_POINTS + " TEXT NOT NULL, " +
                    COL_REASON + " TEXT NOT NULL);";

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


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_TRIPS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
