package com.example.lukas.bluetoothtest.io;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;



/**
 * Author: Lukas Breit
 *
 * Description: The LocationService is listening for location updates and sends them to the bounded activity (RunningTripActivity).
 *
 */

public class LocationService extends Service {
    private static final String CLASS = LocationService.class.getName();

    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";

    private Handler handler;

    public LocationManager locationManager;
    public MyLocationListener listener;


    public void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 500, 10, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(listener);
    }

    // Listener for a changed location; sends the data to the bounded activity via handler
    public class MyLocationListener implements LocationListener
    {
        public void onLocationChanged(final Location location)
        {
            Bundle bundle = new Bundle();

            bundle.putDouble(KEY_LAT, location.getLatitude());
            bundle.putDouble(KEY_LNG, location.getLongitude());

            Message message = new Message();
            message.setData(bundle);
            handler.sendMessage(message);
            Log.d(CLASS, "LOCATIONUPDATE: " + location.toString());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(CLASS, "status changed");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(CLASS, "provider enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(CLASS, "provider disabled");
        }

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocationServiceBinder();
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public class LocationServiceBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }
}
