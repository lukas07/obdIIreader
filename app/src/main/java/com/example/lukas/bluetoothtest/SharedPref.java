package com.example.lukas.bluetoothtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.DateFormat;

/**
 * Created by Lukas on 14.02.2018.
 */

public class SharedPref {
    private Context context;
    private SharedPreferences sharedPreferences;

    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.sharedPref), context.MODE_PRIVATE);
    }

    public void setObdInitialized (Boolean value) {
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(context.getString(R.string.pref_obd_initialized), value);
        editor.commit();
    }

    public boolean getObdInitialized () {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_obd_initialized), false);
    }
}
