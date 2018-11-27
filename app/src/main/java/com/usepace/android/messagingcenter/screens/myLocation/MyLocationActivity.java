package com.usepace.android.messagingcenter.screens.myLocation;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.usepace.android.messagingcenter.R;
import com.usepace.android.messagingcenter.utils.LocationUtils;
import com.usepace.android.messagingcenter.utils.PermissionUtils;
import com.usepace.android.messagingcenter.utils.ViewUtils;

import java.util.ArrayList;

public class MyLocationActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionUtils.PermissionResultCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MyLocationActivity.class.getSimpleName();
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;

    private Toolbar toolbar;
    private Button btn_send_location;
    private TextView tv_address;


    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private View mapView;
    private String geocodeLocation;
    private double latitude;
    private double longitude;
    private boolean isPermissionGranted;


    private ArrayList<String> permissions=new ArrayList<>();
    private PermissionUtils permissionUtils;

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

        btn_send_location = findViewById(R.id.btn_send_location);
        tv_address = findViewById(R.id.tv_address);

        btn_send_location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAndReturnData();
            }
        });

//        if (checkPlayServices()) {
//            buildGoogleApiClient();
//        }
    }

    private void initToolBar() {
        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.share_location));
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
                googleApiAvailability.getErrorDialog(this,resultCode,
                        PLAY_SERVICES_REQUEST).show();
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_LONG).show();
            }
            return false;
        }
        return true;
    }

    private void checkPermissions()
    {
        try {
            permissionUtils=new PermissionUtils(this);

            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

            permissionUtils.check_permission(permissions, getString(R.string.need_gps_permission),1);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.mGoogleMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);

        } else {
            if(googleMap != null) {
                googleMap.setMyLocationEnabled(true);
            }
            loadMapView();
        }

        checkPermissions();

    }

    private void loadMapView() {
        try {
            buildGoogleApiClient();
            if (mapView != null && mapView.findViewById(Integer.parseInt("1")) != null) {
                View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                        locationButton.getLayoutParams();

                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                layoutParams.setMargins(0, 0, ViewUtils.getValueInDp(this, 30), ViewUtils.getValueInDp(this, 120));

                if(mGoogleMap != null) {
                    mGoogleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                        @Override
                        public void onCameraMoveStarted(int i) {

                        }
                    });

                    mGoogleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                        @Override
                        public void onCameraIdle() {
                            LatLng position = mGoogleMap.getCameraPosition().target;

                            showGeoLocation(position);
                        }
                    });

                    LocationUtils.setCameraToReceivedLocation(mGoogleMap, latitude, longitude);
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
                        if (ContextCompat.checkSelfPermission(this,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {

                            //Request location updates:
                            if(mGoogleMap != null) {
                                mGoogleMap.setMyLocationEnabled(true);
                            }
                            loadMapView();
                        }

                    } else {
                        Toast.makeText(this, "Please grant location permission", Toast.LENGTH_LONG).show();

                    }
                }
                break;
            }
            if(permissionUtils != null) {
                permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getLocation() {
        if (isPermissionGranted) {

            try
            {
                Location mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);

                if(mLastLocation != null) {

                    latitude = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();

                    LatLng position = new LatLng(latitude, longitude);
                    showGeoLocation(position);
                    LocationUtils.setCameraToReceivedLocation(mGoogleMap, latitude, longitude);
                }
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }
        }

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MyLocationActivity.this, REQUEST_CHECK_SETTINGS);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });


    }

    private void showGeoLocation(LatLng latLng) {
        try {
            latitude = latLng.latitude;
            longitude = latLng.longitude;
            geocodeLocation = LocationUtils.getAddress(this, latLng);
            tv_address.setVisibility(View.VISIBLE);
            tv_address.setText(geocodeLocation);
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

    @Override
    public void PermissionGranted(int request_code) {
        Log.i("PERMISSION","GRANTED");
        isPermissionGranted=true;
//        if (checkPlayServices()) {
//            buildGoogleApiClient();
//        }
    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {

    }

    @Override
    public void PermissionDenied(int request_code) {

    }

    @Override
    public void NeverAskAgain(int request_code) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if(isPermissionGranted) {
            getLocation();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }
}