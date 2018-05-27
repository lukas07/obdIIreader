package com.example.lukas.bluetoothtest.io;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Author: Lukas Breit
 *
 * Description: The MapLocationParser receives a JSONObject and returns a list of lists containing latitude and longitude
 *
 */

public class MapLocationParser {

    // Receives a JSONObject and returns a list of lists containing latitude and longitude
    public List<HashMap<String,String>> parse(JSONObject jObject){

        List<HashMap<String, String>> route = new ArrayList<>() ;
        JSONArray jRoute;

        try {

            jRoute = jObject.getJSONArray("snappedPoints");

            for(int i=0; i<jRoute.length();i++) {
                HashMap<String, String> hm = new HashMap<>();
                Double lat = (Double)((JSONObject)((JSONObject)jRoute.get(i)).get("location")).get("latitude");
                Double lng = (Double)((JSONObject)((JSONObject)jRoute.get(i)).get("location")).get("longitude");
                hm.put("lat", String.valueOf(lat));
                hm.put("lng", String .valueOf(lng));
                route.add(hm);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }


        return route;
    }
}

