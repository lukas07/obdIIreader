package com.example.lukas.bluetoothtest;

import android.Manifest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RunningTripActivity extends AppCompatActivity {

    private static final String CLASS = RunningTripActivity.class.getName();
    // Konstanten: SaveInstance
    private static final String STATE_TIMER = "timerValue";
    private static final String STATE_MARKERPOINTS = "marker";
    // Konstanten: obdHandler
        // Steuerung des Ladebalken
    private static final int INIT_STARTED = 1;
    private static final int INIT_SUCCESS = 2;
    private static final int INIT_STOPPED = 3;
        // Exception-Meldungen für UI-Activity
    private static final int NODATA_EXCEPTION = 10;
    private static final int CONNECT_EXCEPTION = 11;

    private TextView tv_speed;
    private TextView tv_fuel;
    private TextView tv_timer;
    private Button bt_stop;
    private ProgressBar pb_init;


    private ObdService service;
    private boolean serviceBound = false;
    private Thread initThread;
    private int orientation;
    private TripRecord record = TripRecord.getTripRecord();
    private SharedPref sharedPref;


    // Google Map und GPS-Daten
    private GoogleMapFragment mapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Geocoder geocoder;

    // Fahrtzeit
    final int MSG_START_TIMER = 0;
    final int MSG_STOP_TIMER = 1;
    final int MSG_UPDATE_TIMER = 2;
    final int MSG_SET_TIMER = 3;
    Stopwatch timer = new Stopwatch();
    final int REFRESH_RATE = 1000;
    private long timerValue = 0;
    // Handler, um den Timer auf der Oberfläche zu aktualisieren
    private Handler clockHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START_TIMER:
                    timer.start(); //start timer
                    clockHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
                    break;

                case MSG_SET_TIMER:
                    timer.setTimer(timerValue);
                    clockHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
                    break;

                case MSG_UPDATE_TIMER:
                    tv_timer.setText(""+ timer.toString());
                    clockHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIMER,REFRESH_RATE); //text view is updated every second,
                    break;                                  //though the timer is still running
                case MSG_STOP_TIMER:
                    timer.stop();//stop timer
                    break;

                default:
                    break;
            }
        }
    };

    // Handler, um die Daten des OBDII-Adapters zu empfangen und Aktualisierung der UI durchzuführen
    private Handler obdHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case INIT_STARTED:
                    pb_init.setVisibility(View.VISIBLE);
                    break;
                case INIT_STOPPED:
                    pb_init.setVisibility(View.GONE);
                    Log.e(CLASS, "Toast");
                    Toast.makeText(RunningTripActivity.this, getResources().getString(R.string.run_init_fail), Toast.LENGTH_LONG).show();
                    // Sonst wird der Timer noch kurz angezeigt
                    //tv_timer.setVisibility(View.INVISIBLE);
                    break;
                case INIT_SUCCESS:
                    pb_init.setVisibility(View.GONE);
                    bt_stop.setEnabled(true);
                    // Timer zur Anzeige der Fahrzeit starten
                    startTimer();
                    break;
                case NODATA_EXCEPTION:
                    Toast.makeText(RunningTripActivity.this, getResources().getString(R.string.run_no_data), Toast.LENGTH_LONG).show();
                    // TODO Wenn keine Daten mehr empfangen werden oder die Bt-Verbindung unterbrochen wurde direkt zu MainActivity oder sofort den Trip beenden?
                    finish();
                    sharedPref.setObdInitialized(false);
                    break;
                case CONNECT_EXCEPTION:
                    Toast.makeText(RunningTripActivity.this, getResources().getString(R.string.run_connect), Toast.LENGTH_LONG).show();
                    finish();
                default:
                    Bundle bundle = message.getData();
                    String speed = bundle.getString("speed");
                    String fuel = bundle.getString("consumption");

                    if(speed != null)
                        tv_speed.setText(speed);
                    if(fuel != null)
                        tv_fuel.setText(fuel);
            }
        }
    };

    // Stellt Verbindung mit dem OBD-Service her und startet diesen
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
            orientation = getRequestedOrientation();
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

            Log.e(CLASS, "Service connected");
            ObdService.ObdServiceBinder binder = (ObdService.ObdServiceBinder) serviceBinder;
            service = binder.getService();
            serviceBound = true;
            service.setHandler(obdHandler);
            // Auslagerung der Initialisierung in eigenen Thread, um UI gleichzeitig updaten zu können
            initThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        service.initObdConnection();
                    } catch (IOException ioe) {
                        unbindService(serviceConnection);
                        serviceBound = false;
                        Log.e(CLASS, "Service disconnected");
                        finish();
                    }
                }
            });
            initThread.start();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(CLASS, "Service disconnected");
            serviceBound = false;
        }
    };

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        showToast(getResources().getString(R.string.main_bt_manual_disabled), Toast.LENGTH_LONG);
                        finish();
                        break;
                }

            }
        }
    };

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                ContentResolver contentResolver = getApplicationContext().getContentResolver();
                int mode = Settings.Secure.getInt(
                        contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                if(mode == Settings.Secure.LOCATION_MODE_OFF)
                    showToast(getResources().getString(R.string.main_gps_manual_disabled), Toast.LENGTH_LONG);
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_trip);

        sharedPref = new SharedPref(this);

        // Referenzvariablen zu den Feldern deklarieren
        tv_speed = (TextView) findViewById(R.id.tv_speed);
        tv_fuel = (TextView) findViewById(R.id.tv_fuel);
        tv_timer = (TextView) findViewById(R.id.tv_timer);
        bt_stop = (Button) findViewById(R.id.bt_stop);
        pb_init = (ProgressBar) findViewById(R.id.pb_init);


        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrip();
            }
        });

        // Broadcast-Recevier, um Änderungen des Bluetooth-Status zu registrieren
        IntentFilter filterBt = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filterBt);
        // Broadcast-Recevier, um Änderungen des GPS-Status zu registrieren
        IntentFilter filterGps = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        registerReceiver(gpsReceiver, filterGps);

        // Werden zur Ermittlung der Endadresse benötigt, wenn der Trip gestoppt wird
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

        // OBD-Service zum Auslesen der Daten des Adapters starten
        Log.e(CLASS, "Bind OBD-Service");
        Intent serviceIntent = new Intent(getApplicationContext(), ObdService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        if(savedInstanceState != null) {
            // Falls der Timer bereits gelaufen ist, den Wert übernehmen
            timerValue = savedInstanceState.getLong(STATE_TIMER);
        }

        // Google Map hinzufügen
        mapFragment = GoogleMapFragment.newInstance(getApplicationContext(), 0, GoogleMapFragment.MAP_MODE_LIVE);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.running_container, mapFragment);
        fragmentTransaction.commit();

    }


    private void stopTrip() {
        // Timer der Fahrzeit stoppen
        clockHandler.sendEmptyMessage(MSG_STOP_TIMER);

        Log.e(CLASS, "Unbind Service");
        //OBD-Service stoppen
        service.stopSendOBDCommands();
        try {
            unbindService(serviceConnection);
            serviceBound = false;
        } catch (Exception e) {
            Log.e(CLASS, "Couldnt unbind");
        }

        // Updaten der GPS-Daten stoppen
        // TODO GPS-Daten stoppen??

        // Daten in Triprecord schreiben (Stopzeitpunkt, Endadresse,...)
        record = record.getTripRecord();

        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Long timestamp = Long.parseLong(timestampFormat.format(new Date()));
        record.setEndTimestamp(timestamp);

        // Die Startadresse ermitteln und im Record speichern
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            try {
                                // Die Adresse über den Geocoder ermitteln
                                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                Log.e(CLASS, "Geocoder: " + addresses.toString());
                                record.setEndAddress(addresses.get(0).getAddressLine(0));
                            } catch (IOException e) {
                                e.printStackTrace();
                                Log.e(CLASS, "Geocoder: " + e.toString());
                            }
                        }
                    }
                });

        // Arraylist der Locations wird in einen String gepackt, um diesen in die DB zu schreiben
        Gson gsonRoutePoints = new Gson();
        String routeString = gsonRoutePoints.toJson(mapFragment.getRoutePoints());
        Log.e(CLASS, "Route points: " + routeString);
        record.setRoutePoints(routeString);


        Intent intent = new Intent(RunningTripActivity.this, StoppedTripActivity.class);
        startActivity(intent);
        finish();
    }

    private void startTimer() {
        if(timerValue == 0)
            clockHandler.sendEmptyMessage(MSG_START_TIMER);
        else
            clockHandler.sendEmptyMessage(MSG_SET_TIMER);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setRequestedOrientation(orientation);
        if(serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        // Falls die Initliasisierung noch läuft abbrechen
        if(initThread.isAlive())
            initThread.interrupt();

        unregisterReceiver(bluetoothReceiver);
        unregisterReceiver(gpsReceiver);
    }

    @Override
    public void onSaveInstanceState (Bundle savedInstanceState) {
        // Den aktuellen Wert des Timers speichern
        savedInstanceState.putLong(STATE_TIMER, timer.getStartTime());

        super.onSaveInstanceState(savedInstanceState);
    }

    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }


    /**
     *
     *  Methoden bezüglich der Google Map
     *
     */
/*

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        // Map clearen, damit die Route vom vorherigen Trip nicht mehr angezeigt wird
        mMap.clear();
    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    */
/**
     * A method to download json data from url
     *//*

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    */
/**
     * A class to parse the Google Places in JSON format
     *//*

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                MapLocationParser parser = new MapLocationParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        // Map wird nur aktualisiert, wenn der Standort 10 Meter entfernt von dem vorherigen liegt
        if (mLastLocation.distanceTo(location) > 10 || routePoints.isEmpty()) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
                mCurrLocationMarker.remove();
            }

            //Place current location marker
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
            mCurrLocationMarker = mMap.addMarker(markerOptions);

            //move map camera
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(MAP_CAM_ZOOM));

            // Adding new item to the ArrayList
            routePoints.add(latLng);

            // Checks, whether start and end locations are captured
            if (routePoints.size() >= 2) {
                LatLng origin = routePoints.get(routePoints.size() - 2);
                LatLng dest = routePoints.get(routePoints.size() - 1);

                // Getting URL to the Google Directions API
                String url = getUrl(origin, dest);
                Log.d("newLocation", url.toString());
                FetchUrl FetchUrl = new FetchUrl();

                // Start downloading json data from Google Directions API
                FetchUrl.execute(url);
                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }
*/

}
