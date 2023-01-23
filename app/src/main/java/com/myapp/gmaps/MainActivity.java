package com.myapp.gmaps;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends FragmentActivity {

    GoogleMap map;
    Spinner spinner;
    Button btnFind;
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient fusedLocationProviderClient;
    double currentLat = 0;
    double currentLong = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinner = findViewById(R.id.sp_type);
        btnFind = findViewById(R.id.bt_find);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);

        final String[] placeTypeList = {"atm", "bank", "hospital", "movie_theater", "restaurant"};
        String[] placeNameList = {"ATM", "Bank", "Hospital", "Movie Theater", "Restaurant"};

        spinner.setAdapter(new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, placeNameList));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);//WHAT IS A FUSED LOCATION PRODVIDER OBJECT & WHAT IS THE LOCATION SERVICES CLASS

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.d("blue", "permission granted");
            getCurrentLocation();
        }else{

            Log.d("blue", "Requesting permission");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

        btnFind.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Log.d("blue", "Current Lat: " + currentLat);
                Log.d("blue", "Current Long: " + currentLong);

                int i = spinner.getSelectedItemPosition();
                String url = "https://maps.google.com/maps/api/place/nearbysearch/json?" +
                        "location=" + currentLat + "," + currentLong +
                        "&radius=5000" + "&type=" + placeTypeList[i] +
                        "&sensor=true" + "&key=" + getResources().getString(R.string.google_map_key);

                Log.d("blue", "url: " + url);

                new PlaceTask().execute(url);//WHAT IS THE PLACE TASK OBJECT
            }

        });

    }

    public void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d("blue", "permission not granted");
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();//WHAT IS THE TASK OBJECT

        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                    if(location != null){

                        currentLat = location.getLatitude();
                        currentLong = location.getLongitude();

                        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                            @Override
                            public void onMapReady(@NonNull GoogleMap googleMap) {

                                map = googleMap;

                                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLat, currentLong), 10));

                                Log.d("blue", "onMapReady() has ran");
                            }
                        });//WHAT IS ONMMAPREADYCALLBACK & WHAT IS GETMAPASYNC

                        Log.d("blue", "location is not null");
                    }

                    if(location == null){

                        Log.d("blue", "location is null");
                    }

                    Log.d("blue", "onSuccess() has ran");
            }
        });
    }

    public void onRequestPermissionResult(int requestCode, String[] permission, int[] grantResults) {

        if(requestCode == 44){

            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                getCurrentLocation();
            }
        }
    }

    private class PlaceTask extends AsyncTask<String, Integer, String> {


        @Override
        protected String doInBackground(String... strings) {

            Log.d("blue", "value of doInBackground parameter: " + strings[0]);

            String data = null;

            try {

                data = downloadUrl(strings[0]);
            }catch(IOException e){

                e.printStackTrace();
                Log.d("blue", "try block did not work");
            }

            Log.d("blue", "doInBackground method has ran");
            return data;
        }

        protected void onPostExecute(String s){

            new ParserTask().execute(s);
            Log.d("blue", "onPostExecute method has ran");
        }

        private String downloadUrl(String string) throws IOException {

            URL url = new URL(string);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if(connection == null){

                Log.d("blue", "connection is null");
            }

            connection.connect();

            InputStream stream = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            StringBuilder builder = new StringBuilder();

            String line = "";

            while((line = reader.readLine()) != null){

                builder.append(line);
                Log.d("blue", "while loop running");
            }

            String data = builder.toString();
            reader.close();

            Log.d("blue", "The data is " + data);

            return data;
        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String, String>>> {


        @Override
        protected List<HashMap<String, String>> doInBackground(String... strings) {

            JsonParser jsonParser = new JsonParser();//WHAT IS A JSON PARSER OBJECT

            List<HashMap<String, String>> mapList = null;
            JSONObject object =  null;//WHAT IS A JSON OBJECT

            try{

                object = new JSONObject(strings[0]);
                mapList = jsonParser.parseResult(object);
            }catch(JSONException e){

                e.printStackTrace();
            }

            return mapList;
        }

        protected void onPostExecute(List<HashMap<String, String>> hashMaps){

            map.clear();

            for(int i = 0; i < hashMaps.size(); i++){

                HashMap<String, String> hashMapList = hashMaps.get(i);

                double lat = Double.parseDouble(hashMapList.get("lat"));
                double lng = Double.parseDouble(hashMapList.get("lng"));

                String name = hashMapList.get("name");

                LatLng latLng = new LatLng(lat, lng);

                MarkerOptions options = new MarkerOptions();
                options.position(latLng);
                options.title(name);

                map.addMarker(options);
            }
        }
    }

}