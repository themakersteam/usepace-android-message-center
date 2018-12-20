package com.usepace.android.messagingcenter.screens.myLocation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.usepace.android.messagingcenter.R;
import com.usepace.android.messagingcenter.utils.LocationUtils;
import com.usepace.android.messagingcenter.utils.ViewUtils;

public class MyLocationActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MyLocationActivity.class.getSimpleName();
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;

    private Toolbar toolbar;
    private LinearLayout layout_send_location;
    private ImageButton button_get_current_location;
    private ImageButton button_send_location;
    private TextView tv_address;
    private TextView tv_send_location;
    private RadioGroup toggle_map_view;
    private RadioButton rb_map;
    private RadioButton rb_satellite;


//    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private GoogleMap mGoogleMap;
    private View mapView;
    private String geocodeLocation;
    private double latitude;
    private double longitude;

    private Location mCurrentLocation;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_my_location);

        initToolBar();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);

        layout_send_location = findViewById(R.id.layout_send_location);
        button_get_current_location = findViewById(R.id.button_get_current_location);
        button_send_location = findViewById(R.id.button_send_location);
        toggle_map_view = findViewById(R.id.toggle_map_view);
        tv_address = findViewById(R.id.tv_address);
        tv_send_location = findViewById(R.id.tv_send_location);
        rb_map = findViewById(R.id.rb_map);
        rb_satellite = findViewById(R.id.rb_satellite);

        layout_send_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAndReturnData();
            }
        });

        button_send_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAndReturnData();
            }
        });

        button_get_current_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buildGoogleApiClient();
            }
        });

        //Set default color of unselected radio button
        rb_map.setTextColor(Color.WHITE);
        rb_satellite.setTextColor(Color.BLACK);
        toggle_map_view.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rb_map) {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    rb_map.setTextColor(Color.WHITE);
                    rb_satellite.setTextColor(Color.BLACK);
                } else {
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    rb_map.setTextColor(Color.BLACK);
                    rb_satellite.setTextColor(Color.WHITE);
                }
            }
        });

        createLocationRequest();

    }

    private void initToolBar() {
        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.message_center_send_location_title));
        }
    }

    private void createLocationRequest() {
        try {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                moveBack();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPlayServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_REQUEST).show();
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);

        }

        loadMapView();
        buildGoogleApiClient();

    }

    private void loadMapView() {
        try {
            if (checkPlayServices()) {

                if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
                    View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                            locationButton.getLayoutParams();

                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
                    layoutParams.setMargins(0, ViewUtils.getValueInDp(this, 60), ViewUtils.getValueInDp(this, 30), 0);

                    if (mGoogleMap != null) {
                        mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                            @Override
                            public void onCameraMoveStarted(int i) {

                            }
                        });

                        mGoogleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                            @Override
                            public void onCameraIdle() {
                                LatLng position = mGoogleMap.getCameraPosition().target;
                                Location location = new Location("");
                                if (position != null) {
                                    location.setLatitude(position.latitude);
                                    location.setLongitude(position.longitude);
                                    showGeoLocation(location);
                                }
                            }
                        });
                        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                        LocationUtils.setCameraToReceivedLocation(mGoogleMap, latitude, longitude);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        try {
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_LOCATION: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted, yay! Do the
                        // location-related task you need to do.
                        getLocation();

                    } else {
                        Toast.makeText(this, "Please grant location permission", Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(MyLocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(MyLocationActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MyLocationActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

            }
            else {
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MyLocationActivity.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                    mCurrentLocation = location;
                                    showGeoLocation(mCurrentLocation);
                                    LocationUtils.setCameraToReceivedLocation(mGoogleMap, latitude, longitude);
                                }
                            }
                        });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    protected synchronized void buildGoogleApiClient() {
        try {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(mLocationRequest);

            SettingsClient client = LocationServices.getSettingsClient(this);
            Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

            task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                @Override
                public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                    getLocation();
                }
            });

            task.addOnFailureListener(this, new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof ResolvableApiException) {
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MyLocationActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showGeoLocation(Location location) {
        try {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            latitude = latLng.latitude;
            longitude = latLng.longitude;
            geocodeLocation = LocationUtils.getAddress(this, latLng);

            if(mCurrentLocation == null) {
                tv_send_location.setText(getString(R.string.message_center_send_current_location));
                return;
            }

            if(mCurrentLocation.distanceTo(location) <= 15) {
                tv_send_location.setText(getString(R.string.message_center_send_current_location));
            } else {
                tv_send_location.setText(getString(R.string.message_center_send_your_location));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setAndReturnData() {
        try {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("geoDesc", geocodeLocation);
            resultIntent.putExtra("lat", latitude);
            resultIntent.putExtra("lng", longitude);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void moveBack() {
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_CANCELED, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        moveBack();
    }
}