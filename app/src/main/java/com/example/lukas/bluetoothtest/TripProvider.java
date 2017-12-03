package com.example.lukas.bluetoothtest;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

public class TripProvider extends ContentProvider {
    private static final String AUTHORITY = "com.example.lukas.bluetoothtest.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TripOpenHelper.TABLE_NAME_TRIPS);

    private TripOpenHelper dbHelper;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        // Typ 1 für den URI com.example.lukas.bluetoothtest.provider/trips
        uriMatcher.addURI(AUTHORITY, TripOpenHelper.TABLE_NAME_TRIPS, 1);
        // Typ 2 für URIs, die eine Zeile der Trips-Tabelle ansprechen
        uriMatcher.addURI(AUTHORITY, TripOpenHelper.TABLE_NAME_TRIPS + "/#", 2);
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.e(TripProvider.class.getName(), "Delete is not allowed");
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.e(TripProvider.class.getName(), "Insertion started");
        long newRow;
        if(uriMatcher.match(uri) == 1) {
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                newRow = db.insertOrThrow(TripOpenHelper.TABLE_NAME_TRIPS, null, values);
            } catch (NullPointerException exp) {
                Toast.makeText(getContext(), R.string.stop_insert_failed, Toast.LENGTH_SHORT).show();
                throw new NullPointerException("Database Error");
            }
            Toast.makeText(getContext(), R.string.stop_inserted, Toast.LENGTH_SHORT).show();
            return ContentUris.withAppendedId(uri, newRow);
        } else
            Toast.makeText(getContext(), R.string.stop_insert_failed, Toast.LENGTH_SHORT).show();
            throw new IllegalArgumentException("URI invalid");
    }

    @Override
    public boolean onCreate() {
        dbHelper = TripOpenHelper.getHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor result;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        switch (uriMatcher.match(uri)) {
            case 1:
                result = db.query(TripOpenHelper.TABLE_NAME_TRIPS, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case 2:
                result = db.query(TripOpenHelper.TABLE_NAME_TRIPS, projection, "_id=?", new String[] {uri.getLastPathSegment()}, null, null, null);
                break;
            default:
                throw new IllegalArgumentException("URI invalid");
        }
        return result;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.e(TripProvider.class.getName(), "Update is not allowed");
        return 0;
    }
}
