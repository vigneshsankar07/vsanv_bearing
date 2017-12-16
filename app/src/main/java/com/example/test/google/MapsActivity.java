package com.example.test.google;

import android.*;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.AvoidType;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationSource, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DirectionCallback
,GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraMoveCanceledListener,
        GoogleMap.OnCameraIdleListener {
    SharedPreferences.Editor editor;
    SharedPreferences state;
    GoogleMap mMap;
    List<Step> step;
    int Toll_lis=0;
    LocationManager locationManager;
    LatLng startLatLng,destination,tollpoint;
    GoogleApiClient mGoogleApiClient;
    private OnLocationChangedListener mMapLocationListener = null;
    Marker pickup, destionation,tollmarker;
    String[] Strstylelist = new String[] {"Silver","Night","Retro","Dark","Aubergine","Normal"};
    TextView style;
    String styles;
    LatLng prevLatLng = new LatLng(0, 0);
    float previousBearing = 0;
    int position = 0;
    Marker currentLocMarker, pickUPrDropMarker;
    boolean doubleBackToExitPressedOnce = false;
    Location mCurrentLocation, lStart, lEnd;
    static  String distance ="empty",Avoidtype="empty",transport="empty";
    PlaceAutocompleteFragment autocompleteFragment,place_autocomplete_fragment2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        editor = getSharedPreferences(Constants.MY_PREFS_NAME, MODE_PRIVATE).edit();
        autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        place_autocomplete_fragment2 = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment2);
        autocompleteFragment.setHint("Start Location");
        place_autocomplete_fragment2.setHint("Destination Location");
         style = findViewById(R.id.style);
        state = getSharedPreferences(Constants.MY_PREFS_NAME, MODE_PRIVATE);
        styles = state.getString("mapstyle", null);
        if(state.getString("position", null)!=null){
            position = Integer.parseInt(state.getString("position", null));
        }
        style.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapstyle();
            }
        });
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                autocompleteFragment.setText(place.getName());
                pickup = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ub__ic_pin_pickup)));
                startLatLng =place.getLatLng();
            }

            @Override
            public void onError(Status status) {

            }
        });
        place_autocomplete_fragment2.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                place_autocomplete_fragment2.setText(place.getName());
                destionation = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ub__ic_pin_dropoff)));
                destination = place.getLatLng();
                direction(startLatLng,destination);

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(pickup.getPosition());
                builder.include(destionation.getPosition());
                LatLngBounds bounds = builder.build();
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds,90));
            }

            @Override
            public void onError(Status status) {

            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        mGoogleApiClient.connect();
       /* mMap.setOnCameraIdleListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraMoveCanceledListener(this);*/
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
           // "Silver","Night","Retro","Dark","Aubergine ","Normal"
            if(styles!=null){
                if(styles.matches("Silver")){
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                    this, R.raw.silver));
                }else if(styles.matches("Night")){
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                    this, R.raw.night));
                }else if(styles.matches("Retro")){
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                    this, R.raw.retro));
                }else if(styles.matches("Dark")){
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                    this, R.raw.dark));
                }else if(styles.matches("Aubergine")){
                    boolean success = googleMap.setMapStyle(
                            MapStyleOptions.loadRawResourceStyle(
                                    this, R.raw.style));
                    System.out.println("enter the mapstyle");
                }else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                }
            }


        } catch (Resources.NotFoundException e) {
            System.out.println("enter exception"+e);
        }


        mCurrentLocation = getFusedLocation();

        if (mCurrentLocation != null) {
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

            System.out.println("INSIDE LOCAION CHANGE" + mCurrentLocation.getLatitude() + mCurrentLocation.getLongitude());

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)                              // Sets the center of the map to current location
                    .zoom(Constants.MAP_ZOOM_SIZE)
                    .tilt(0)                                     // Sets the tilt of the camera to 0 degrees
                    .build();

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            if (currentLocMarker == null) {

                currentLocMarker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.position))
                        .position(latLng)
                        .flat(true)
                        .anchor(0.5f, 0.5f)
                        .rotation(mCurrentLocation.getBearing()));
            } else {

                currentLocMarker.setPosition(latLng);
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        final Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        System.out.println("enter the"+direction.getStatus());
        if(direction.isOK()) {
            if(currentLocMarker!=null){
                currentLocMarker.remove();
            }

            ArrayList<LatLng> directionPositionList = direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint();
          //  System.out.println("enter google json reopnse:"+ direction.getRouteList().get(0).getLegList().get(0).getDirectionPoint());
            int n =direction.getRouteList().size();
            for (int h=0;h<n;h++){
                List<Leg> leg = direction.getRouteList().get(h).getLegList();
                System.out.println("enter google json reopnse:"+direction.getRouteList().get(h).getLegList());
                int m = leg.size();
                for(int j=0;j<m;j++){
                    step = leg.get(j).getStepList();
                    int k  =  step.size();
                    for (int i=0;i<k;i++){
                        String final_instruct = getStringfromhtml(step.get(i).getHtmlInstruction());
                        String gettollrods = gettollrods(step.get(i).getHtmlInstruction());
                        System.out.println("enter toll list:"+step.get(i).getHtmlInstruction().toLowerCase());
                        if(gettollrods.matches("toll roll")){
                            Toll_lis +=1;
                            System.out.println("enter toll rolls:"+Toll_lis);
                            Double start = step.get(i).getStartLocation().getLatitude();
                            Double end = step.get(i).getStartLocation().getLongitude();
                            tollpoint = new LatLng(start,end);
                            tollmarker = mMap.addMarker(new MarkerOptions().position(tollpoint).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ub__ic_pin_pickup)));
                        }
                    }
                }
            }



            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.position);
            setAnimation(mMap,directionPositionList,bitmap);
            mMap.addPolyline(DirectionConverter.createPolyline(this, directionPositionList, 5, Color.GREEN));

            pickup = mMap.addMarker(new MarkerOptions().position(startLatLng).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ub__ic_pin_pickup)));

            destionation = mMap.addMarker(new MarkerOptions().position(destination).icon(BitmapDescriptorFactory.fromResource(R.mipmap.ub__ic_pin_dropoff)));

        }
    }

    @Override
    public void onDirectionFailure(Throwable t) {

    }
    private String getStringfromhtml(String htmlstring) {
        return Html.fromHtml(htmlstring).toString().trim();
    }
    public String gettollrods(String htmlstring) {
        if(Html.fromHtml(htmlstring).toString().trim().contains("Partial toll road") |Html.fromHtml(htmlstring).toString().trim().contains("Toll road")){
            return "toll roll";
        }else {
            return "not";
        }
    }
    @Override
    public void onLocationChanged(Location location) {
        LatLng curPos;
        float curPosBearing;

        if (mCurrentLocation != null) {
            System.out.println("ONLOCATIOn CHANGE bearing" + mCurrentLocation.getBearing());
            curPos = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            curPosBearing = mCurrentLocation.getBearing();

        } else {
            curPos = new LatLng(location.getLatitude(), location.getLongitude());
            curPosBearing = location.getBearing();
            System.out.println("location null");
        }

        mCurrentLocation = location;
        if (mMapLocationListener != null) {
            mMapLocationListener.onLocationChanged(location);
        }

        if (mMap != null) {
            try {

                System.out.println("Key moved ===>" + mCurrentLocation.getSpeed());
                //mMap.clear();

                zoomCameraToPosition(curPos);

                if (currentLocMarker == null) {


                    currentLocMarker = mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.position))
                            .position(curPos)
                            .flat(true));
                    currentLocMarker.setAnchor(0.5f, 0.5f);
                    currentLocMarker.setRotation(curPosBearing);
                    //currentLocMarker.remove();
                } else {
                    if (mCurrentLocation.getBearing() != 0.0)
                        previousBearing = mCurrentLocation.getBearing();

                    if (prevLatLng != new LatLng(0, 0)) {

                        if (!(curPos.equals(prevLatLng))) {

                            double[] startValues = new double[]{prevLatLng.latitude, prevLatLng.longitude};

                            double[] endValues = new double[]{curPos.latitude, curPos.longitude};

                            System.out.println("Start location===>" + startValues[0] + "  " + startValues[1]);
                            System.out.println("end location===>" + endValues[0] + "  " + endValues[1]);

                            System.out.println("inside locationchange bearing" + mCurrentLocation.getBearing());

                            //animateMarkerTo(currentLocMarker, startValues, endValues, mCurrentLocation.getBearing());

                        } else {
                            System.out.println("outside locationchange bearing" + mCurrentLocation.getBearing());
                            if (mCurrentLocation.getBearing() == 0.0)
                                currentLocMarker.setRotation(previousBearing);
                            else
                                currentLocMarker.setRotation(mCurrentLocation.getBearing());
                            // currentLocMarker.setRotation(mCurrentLocation.getBearing());
                        }
                    } else {
                        currentLocMarker.setPosition(curPos);
                        currentLocMarker.setRotation(mCurrentLocation.getBearing());
                    }

                    prevLatLng = new LatLng(curPos.latitude, curPos.longitude);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    public void zoomCameraToPosition(LatLng curPos) {

        boolean contains = mMap.getProjection().getVisibleRegion().latLngBounds.contains(curPos);

        if (!contains) {
            // MOVE CAMERA
            // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(animatedValue[0],animatedValue[1]),17.0f));

            float zoomPosition;
          /*  if (tripState == null || tripState.matches("endClicked") || tripState.matches("btnendClicked"))
                zoomPosition = Constants.MAP_ZOOM_SIZE;
            else*/
            zoomPosition = Constants.MAP_ZOOM_SIZE_ONTRIP;

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(curPos)                              // Sets the center of the map to current location
                    .zoom(zoomPosition)
                    .tilt(0)                                     // Sets the tilt of the camera to 0 degrees
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mMapLocationListener = onLocationChangedListener;

    }

    @Override
    public void deactivate() {
        mMapLocationListener = null;
    }
    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onCameraIdle() {
        Toast.makeText(this, "The camera has stopped moving.",
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onCameraMoveCanceled() {
        Toast.makeText(this, "Camera movement canceled.",
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onCameraMove() {
        Toast.makeText(this, "The camera is moving.",
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            Toast.makeText(this, "The user gestured on the map.",
                    Toast.LENGTH_SHORT).show();
        } else if (reason == GoogleMap.OnCameraMoveStartedListener
                .REASON_API_ANIMATION) {
            Toast.makeText(this, "The user tapped something on the map.",
                    Toast.LENGTH_SHORT).show();
        } else if (reason == GoogleMap.OnCameraMoveStartedListener
                .REASON_DEVELOPER_ANIMATION) {
            Toast.makeText(this, "The app moved the camera.",
                    Toast.LENGTH_SHORT).show();
        }

    }

public void direction(LatLng startLatLng, LatLng destionation) {
    mMap.clear();
    GoogleDirection.withServerKey("AIzaSyDIyP_pzptmaknp5KYMIW58ZomjwxMQNQs")
            .from(startLatLng)
            .to(destionation)
            .transportMode(TransportMode.DRIVING)
            .execute(MapsActivity.this);
   /* if(!Avoidtype.equals("empty")&&!transport.equals("empty"))
    {
        GoogleDirection.withServerKey("AIzaSyDIyP_pzptmaknp5KYMIW58ZomjwxMQNQs")
                .from(startLatLng)
                .to(destionation)
                .avoid(Avoidtype)
                .transportMode(transport)
                .transportMode(TransportMode.DRIVING)
                .execute(MapsActivity.this);
    }else {
        GoogleDirection.withServerKey("AIzaSyDIyP_pzptmaknp5KYMIW58ZomjwxMQNQs")
                .from(startLatLng)
                .to(destionation)
                .transportMode(TransportMode.DRIVING)
                .execute(MapsActivity.this);
    }*/



}
public void distance(){
    final Dialog dialogTripSummary = new Dialog(MapsActivity.this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
    dialogTripSummary.setContentView(R.layout.transportmode_layout);
    dialogTripSummary.setCancelable(false);
    TextView meter = (TextView) dialogTripSummary.findViewById(R.id.meter);
    TextView kmeter = (TextView) dialogTripSummary.findViewById(R.id.kmeter);
    TextView toll = (TextView) dialogTripSummary.findViewById(R.id.toll);
    TextView highway = (TextView) dialogTripSummary.findViewById(R.id.highway);
    TextView ferries = (TextView) dialogTripSummary.findViewById(R.id.ferries);
    TextView indoor = (TextView) dialogTripSummary.findViewById(R.id.indoor);
    final TextView bus = (TextView) dialogTripSummary.findViewById(R.id.bus);
    TextView bycycle = (TextView) dialogTripSummary.findViewById(R.id.bycycle);
    TextView drivering = (TextView) dialogTripSummary.findViewById(R.id.drivering);
    TextView train = (TextView) dialogTripSummary.findViewById(R.id.train);
    ImageButton close = (ImageButton) dialogTripSummary.findViewById(R.id.close);
    TextView done = (TextView) dialogTripSummary.findViewById(R.id.done);
    close.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialogTripSummary.dismiss();
                }
            });
        }
    });
    done.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(new Intent(getApplicationContext(),MapsActivity.class));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialogTripSummary.dismiss();
                }
            });
        }
    });
    //String distance ="empty",Avoidtype="empty",transport="empty";

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()){

                case R.id.meter:
                    // do your code
                    distance ="meter";
                    break;
                case R.id.kmeter:
                    // do your code
                    distance ="kmeter";
                    break;
                case R.id.toll:
                    Avoidtype = AvoidType.TOLLS;
                    break;
                  case R.id.highway:
                    // do your code
                      Avoidtype = AvoidType.HIGHWAYS;
                    break;
                 case R.id.ferries:
                    // do your code
                     Avoidtype = AvoidType.FERRIES;
                    break;

                case  R.id.indoor:
                    Avoidtype = AvoidType.INDOOR;
                    break;
                case R.id.bus:
                    transport  =TransportMode.WALKING;
                    break;
                case R.id.bycycle:
                    transport =  TransportMode.BICYCLING;
                    break;
                case R.id.drivering:
                    transport =  TransportMode.DRIVING;
                    break;
                case R.id.train:
                    transport =  TransportMode.TRANSIT;
                    break;
                default:
                    break;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialogTripSummary.show();
                }
            });


        }
    };
    meter.setOnClickListener(onClickListener);
    kmeter.setOnClickListener(onClickListener);
    toll.setOnClickListener(onClickListener);
    highway.setOnClickListener(onClickListener);
    ferries.setOnClickListener(onClickListener);
    indoor.setOnClickListener(onClickListener);
    bus.setOnClickListener(onClickListener);
    bycycle.setOnClickListener(onClickListener);
    drivering.setOnClickListener(onClickListener);
    train.setOnClickListener(onClickListener);

}
public void mapstyle(){
    System.out.println("enter the current"+Strstylelist);
    final Dialog mapstyle = new Dialog(MapsActivity.this, android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
    mapstyle.setContentView(R.layout.change_mapview);
    mapstyle.setCancelable(false);
    ListView listView = (ListView) mapstyle.findViewById(R.id.listview);
    ImageButton close = (ImageButton) mapstyle.findViewById(R.id.close);
    TextView done = (TextView) mapstyle.findViewById(R.id.done);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice,Strstylelist);
    listView.setAdapter(adapter);
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            mapstyle.show();
        }
    });
    listView.setItemChecked(position, true);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            String value = (String)adapterView.getItemAtPosition(i);
            System.out.println("enter the current"+value);
            System.out.println("enter the current"+i);
            editor.putString("mapstyle", value);
            editor.putString("position", String.valueOf(i));
            editor.apply();
        }
    });

    close.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapstyle.dismiss();
                }
            });
        }
    });
    done.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(new Intent(getApplicationContext(),MapsActivity.class));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mapstyle.dismiss();
                }
            });
        }
    });
}



    public static void setAnimation(GoogleMap myMap, final List<LatLng> directionPoint, final Bitmap bitmap) {


        Marker marker = myMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                .position(directionPoint.get(0))
                .flat(true));

        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(directionPoint.get(0), 10));

        animateMarker(myMap, marker, directionPoint, false);
    }


    private static void animateMarker(GoogleMap myMap, final Marker marker, final List<LatLng> directionPoint,
                                      final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = myMap.getProjection();
        final long duration = 30000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                if (i < directionPoint.size())
                    marker.setPosition(directionPoint.get(i));
                i++;


                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    public Location getFusedLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }

        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        System.out.println("Location Provoider:" + " Fused Location");

        if (mCurrentLocation == null) {

            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            System.out.println("Location Provoider:" + " Fused Location Fail: GPS Location");

            if (locationManager != null) {

                //To avoid duplicate listener
                try {
                    locationManager.removeUpdates(this);
                    System.out.print("remove location listener success");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.print("remove location listener failed");
                }

                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        Constants.MIN_TIME_BW_UPDATES,
                        Constants.MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (mCurrentLocation == null) {

                    System.out.println("Location Provoider:" + " GPS Location Fail: Network Location");

                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            Constants.MIN_TIME_BW_UPDATES,
                            Constants.MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                    mCurrentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }
        }

        return mCurrentLocation;
    }

    @Override
    public void onBackPressed() {

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            this.finishAffinity();
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
            System.exit(0);
            finish();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit the app", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }
}
