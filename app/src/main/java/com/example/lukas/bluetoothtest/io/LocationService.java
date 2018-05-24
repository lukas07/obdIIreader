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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Lukas on 23.05.2018.
 */

public class LocationService extends Service { // implements GoogleApiClient.ConnectionCallbacks,
        //GoogleApiClient.OnConnectionFailedListener {
        //LocationListener {
    private static final String CLASS = LocationService.class.getName();

    public static final String KEY_LAT = "lat";
    public static final String KEY_LNG = "lng";

    private Handler handler;

    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    public void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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


/*
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(LocationService.class.getName(), "ConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(LocationService.class.getName(), "ConnectionFailed");
    }

    @Override
    public void onLocationChanged(Location location) {
        Bundle bundle = new Bundle();

        bundle.putDouble(KEY_LAT, location.getLatitude());
        bundle.putDouble(KEY_LNG, location.getLongitude());

        Message message = new Message();
        message.setData(bundle);
        handler.sendMessage(message);
        Log.d(LocationService.class.getName(), "LOCATIONUPDATE: " + location.toString());
    }*/



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
