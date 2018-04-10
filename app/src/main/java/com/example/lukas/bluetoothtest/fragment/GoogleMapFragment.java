package com.example.lukas.bluetoothtest.fragment;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lukas.bluetoothtest.R;
import com.example.lukas.bluetoothtest.io.MapLocationParser;
import com.example.lukas.bluetoothtest.trip.TripOpenHelper;
import com.example.lukas.bluetoothtest.trip.TripProvider;
import com.example.lukas.bluetoothtest.trip.TripRecord;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Lukas on 09.01.2018.
 */

public class GoogleMapFragment extends Fragment implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String CLASS = GoogleMapFragment.class.getName();

    // Modus der Kartenanzeige
    private int mapMode;
    // Nur Anzeige einer Route (in StoppedTrip und TripDetail)
    public static final int MAP_MODE_DISPLAY = 1;
    // Live-Anzeige des aktuellen Trips
    public static final int MAP_MODE_LIVE = 2;


    // Google Map und GPS-Daten
    private static final int MAP_CAM_ZOOM = 15;
    private GoogleMap mMap;
    private ArrayList<LatLng> routePoints;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private LocationRequest mLocationRequest;
    private FetchUrl fetchUrl;
    private ParserTask parserTask;

    private MapView mapView;
    private TextView tv_internet;

    public GoogleMapFragment () {

    }

    public static GoogleMapFragment newInstance (Context context, long rowid, int mode) {
        GoogleMapFragment mapFragment = new GoogleMapFragment();
        Bundle args = new Bundle();
        args.putInt("mode", mode);

        if(rowid > 0) {
            final Cursor cursor;
            cursor = context.getContentResolver().query(TripProvider.CONTENT_URI, null, "_id=?", new String[]{Long.toString(rowid)}, null);
            if (!cursor.moveToFirst()) {
                Log.e(TripDetailFragment.class.getName(), "Result set is empty");
                return null;
            }

            // Die Route als String auslesen und in eine Arraylist konvertieren --> auf Map anzeigen
            String routeString = cursor.getString(TripOpenHelper.COL_ID_ROUTE);
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<LatLng>>() {}.getType();
            ArrayList<LatLng> routePoints = gson.fromJson(routeString, type);
            args.putParcelableArrayList(TripOpenHelper.COL_ROUTE_POINTS, routePoints);

        } else if (rowid == -1) {
            // Die Route als String auslesen und in eine Arraylist konvertieren --> auf Map anzeigen
            String routeString = TripRecord.getTripRecord().getRoutePoints();
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<LatLng>>() {}.getType();
            ArrayList<LatLng> routePoints = gson.fromJson(routeString, type);
            args.putParcelableArrayList(TripOpenHelper.COL_ROUTE_POINTS, routePoints);
        }
        mapFragment.setArguments(args);

        return mapFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mapMode = getArguments().getInt("mode");
        if(mapMode == MAP_MODE_DISPLAY)
            routePoints = getArguments().getParcelableArrayList(TripOpenHelper.COL_ROUTE_POINTS);
        else
            routePoints = new ArrayList<>();

        // Google Map vorbereiten
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }


        /*mLastLocation = new Location("");
        mLastLocation.setLatitude(0);
        mLastLocation.setLongitude(0);*/

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        /*SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);*/

        // Obtain the SupportMapFragment and get notified when the map is ready to be used
        View v = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = (MapView) v.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        tv_internet = (TextView) v.findViewById(R.id.tv_internet);

        return v;
    }

    public void stopLocationUpdates() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Map clearen, damit die Route vom vorherigen Trip nicht mehr angezeigt wird
        mMap.clear();

        if(mapMode == MAP_MODE_LIVE) {
            //Initialize Google Play Services
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(getContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    buildGoogleApiClient();
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else if (mapMode == MAP_MODE_DISPLAY) {
            drawRouteOnMap();
        }

    }


    // Fügt die Route des Trips auf der Karte ein
    public void drawRouteOnMap() {
        int index = 1;
        while(index<routePoints.size()) {
            // Build the path
            String parameters = "";
            // Reduce index value to start next Roads-Request with the last location of the previous request
            index--;
            for (int i = 0; i < 100 && index < routePoints.size(); i++) {
                parameters += String.valueOf(routePoints.get(index).latitude) + ',' + String.valueOf(routePoints.get(index).longitude) + "|";
                index++;
            }
            // Remove the last "|"
            parameters = parameters.substring(0, parameters.length() - 1);

            // Interpolate the route
            String interpolate = "interpolate=true";


            // Building the url to the web service
            String url = "https://roads.googleapis.com/v1/snapToRoads?path=" + parameters + "&" + interpolate + "&" + "key=AIzaSyDwrfTLkZVf94qS2ow5xN5voU7EaQ1Lhe8";


            Log.d("newLocation", url.toString());
            FetchUrl FetchUrl = new FetchUrl();

            FetchUrl.execute(url);

        }

        // Startposition Marker setzen
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(routePoints.get(0));
        markerOptions.title("Start Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mCurrLocationMarker = mMap.addMarker(markerOptions);
        mMap.addMarker(markerOptions);
        // Endposition Marker setzen
        markerOptions.position(routePoints.get(routePoints.size()-1));
        markerOptions.title("End Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mMap.addMarker(markerOptions);

        // Kamera auf der Karte positionieren
        mMap.moveCamera(CameraUpdateFactory.newLatLng(routePoints.get(0)));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(MAP_CAM_ZOOM));

        Log.e(GoogleMapFragment.class.getName(), "Polyline added to map");
    }


    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = dest.latitude + "," + dest.longitude;


        // Building the parameters to the web service
        String parameters = str_origin + "|" + str_dest;

        // Interpolate the route
        String interpolate = "interpolate=true";


        // Building the url to the web service
        String url = "https://roads.googleapis.com/v1/snapToRoads?path=" + parameters + "&" + interpolate + "&" + "key=AIzaSyDwrfTLkZVf94qS2ow5xN5voU7EaQ1Lhe8";
        Log.d("URL: " , url);


        return url;
    }

    /**
     * A method to download json data from url
     */
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
                missingInternetConnection(false);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
                // Es können keine Daten empfangen werden
                missingInternetConnection(true);
                this.cancel(true);
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<HashMap<String, String>> route = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                MapLocationParser parser = new MapLocationParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                route = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",route.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return route;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            ArrayList<LatLng> points = new ArrayList<>();
            PolylineOptions lineOptions = new PolylineOptions();
            Log.d("onPostExecute", "onPostExecute: result: " + result.toString());

            // Fetching all the points in  route
            for (int j = 0; j < result.size(); j++) {
                HashMap<String, String> point = result.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            //routePoints.addAll(points);

            // Adding all the points in the route to LineOptions
            Log.d("onPostExecute", "onPostExecute: Points: " + points.toString());
            lineOptions.addAll(points);
            lineOptions.width(12);
            lineOptions.color(Color.RED);

            Log.d("onPostExecute","onPostExecute lineoptions decoded");

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    public synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getContext())
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
        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(CLASS, "ConnectionSuspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        // Map wird nur aktualisiert, wenn der Standort 10 Meter entfernt von dem vorherigen liegt
        //if (routePoints.isEmpty() || mLastLocation.distanceTo(location) > 5) {

        //mLastLocation = location;
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
        if(routePoints.isEmpty()) {
            mLastLocation = location;
            routePoints.add(latLng);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        mMap.animateCamera(CameraUpdateFactory.zoomTo(MAP_CAM_ZOOM));




        // Checks, whether start and end locations are captured
        if (routePoints.size() >= 1 && mLastLocation.distanceTo(location) > 10) {
            // Adding new item to the ArrayList
            routePoints.add(latLng);

            LatLng origin = routePoints.get(routePoints.size() - 2);
            LatLng dest = routePoints.get(routePoints.size() - 1);
            //LatLng origin = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            //LatLng dest = latLng;

            // Getting URL to the Google Directions API
            String url = getUrl(origin, dest);
            Log.d("newLocation", url.toString());
            fetchUrl = new FetchUrl();

            // Start downloading json data from Google Roads API
            fetchUrl.execute(url);
            //move map camera
            mMap.animateCamera(CameraUpdateFactory.newLatLng(origin));

            mLastLocation = location;
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    // Wenn keine Internetverbindung besteht, kann die Karte nicht aktualisiert werden --> TextView anzeigen
    private void missingInternetConnection(boolean value) {
        if (value == true) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(tv_internet != null)
                        tv_internet.setVisibility(View.VISIBLE);
                }
            });
        } else if (value == false ) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(tv_internet != null)
                        tv_internet.setVisibility(View.GONE);
                }
            });
        }
    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(getActivity(),
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
                    if (ContextCompat.checkSelfPermission(getContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(getContext(), "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    public ArrayList<LatLng> getRoutePoints() {
        return routePoints;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mapView.onDestroy();

        // Updaten der GPS-Daten stoppen
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        // Laufende Asynk-Tasks stoppen
        if(fetchUrl != null)
            fetchUrl.cancel(true);
        if(parserTask != null)
            parserTask.cancel(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);

        outState.putParcelableArrayList(TripOpenHelper.COL_ROUTE_POINTS, routePoints);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}
