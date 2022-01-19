package com.here.emergencyapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.here.sdk.core.Anchor2D;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapImage;
import com.here.sdk.mapview.MapImageFactory;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.routing.CalculateRouteCallback;
import com.here.sdk.routing.CarOptions;
import com.here.sdk.routing.Route;
import com.here.sdk.routing.RoutingEngine;
import com.here.sdk.routing.RoutingError;
import com.here.sdk.routing.Waypoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Context context;
    private MapView mapView;
    private final List<MapMarker> mapMarkerList = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION = 1;
    private RoutingEngine routingEngine;
    private List<Waypoint> waypoints = new ArrayList<>();
    private MapPolyline routePolyline;
    private MapImage mapImage;
    int numPic;
    double longi, lati;
    TextView textView;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Get a MapView instance from the layout.
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        textView = (TextView) findViewById(R.id.textView);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
        else{
            getLocation();
        }


        mapView.setOnReadyListener(new MapView.OnReadyListener() {
            @Override
            public void onMapViewReady() {
                // This will be called each time after this activity is resumed.
                // It will not be called before the first map scene was loaded.
                // Any code that requires map data may not work as expected beforehand.
                Log.d(TAG, "HERE Rendering Engine attached.");
            }
        });
        loadMapScene();
    }


    //Load Map
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadMapScene() {
        // Load a scene from the HERE SDK to render the map with a map scheme.
        mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
            @Override
            public void onLoadScene(@Nullable MapError mapError) {
                if (mapError == null) {
                    double distanceInMeters = 1000 * 10;
                    mapView.getCamera().lookAt(
                            new GeoCoordinates(lati, longi), distanceInMeters);

                    routing();
                    numPic = 1;
                    addPOIMapMarker(lati, longi, numPic);
                } else {
                    Log.d(TAG, "Loading map failed: mapError: " + mapError.name());
                }
            }
        });

    }

    //Map Marker
    private void addPOIMapMarker(double lati, double longi, int numPic) {
        if (numPic == 1) {
            mapImage = MapImageFactory.fromResource(this.getResources(), R.drawable.poi);
        }
        else{
            mapImage = MapImageFactory.fromResource(this.getResources(), R.drawable.poi1);
        }
        // The bottom, middle position should point to the location.
        // By default, the anchor point is set to 0.5, 0.5.
        Anchor2D anchor2D = new Anchor2D(0.5F, 1);
        MapMarker mapMarker = new MapMarker(new GeoCoordinates(lati, longi), mapImage, anchor2D);

        mapView.getMapScene().addMapMarker(mapMarker);
        mapMarkerList.add(mapMarker);
    }

    // Get Location
    @SuppressLint("MissingPermission")
    private void getLocation(){
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            textView.setText(String.valueOf(location.getLongitude()));
                            lati = location.getLatitude();
                            longi = location.getLongitude();
                        }
                    }
                });
    }

    //Routing
    private void routing(){
        try {
            routingEngine = new RoutingEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of RoutingEngine failed: " + e.error.name());
        }
        Waypoint startWaypoint = new Waypoint(new GeoCoordinates(lati,longi));
        Waypoint destinationWaypoint = new Waypoint(new GeoCoordinates(37.4029990,-122.070));
        numPic = 2;
        addPOIMapMarker(37.4029990, -122.070, numPic);

        List<Waypoint> waypoints =
                new ArrayList<>(Arrays.asList(startWaypoint, destinationWaypoint));
        routingEngine.calculateRoute(
                waypoints,
                new CarOptions(),
                new CalculateRouteCallback() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onRouteCalculated(@Nullable RoutingError routingError, @Nullable List<Route> routes) {
                        if (routingError == null) {
                            Route route = routes.get(0);
                            showRouteOnMap(route);
                        } else {

                        }
                    }
                });


    }

    //Show Route
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showRouteOnMap(Route route){
        /*ArrayList<GeoCoordinates> coordinates = new ArrayList<>();
        coordinates.add(new GeoCoordinates(lati,longi));
        coordinates.add(new GeoCoordinates(37.4019983,-122.090));*/
        GeoPolyline routeGeoPolyline;
        try {
            routeGeoPolyline = new GeoPolyline(route.getPolyline());
        } catch(InstantiationErrorException e){
            return;
        }

        float width = 20;
        Color lineColor = Color.valueOf(0, 0.56f, 0.54f, 0.63f);
        MapPolyline routePolyline = new MapPolyline(routeGeoPolyline, width, lineColor);
        mapView.getMapScene().addMapPolyline(routePolyline);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}