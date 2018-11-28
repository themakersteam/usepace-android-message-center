package com.usepace.android.messagingcenter.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.text.TextUtils;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationUtils
{

    final static int GLOBE_WIDTH = 256; // a constant in Google's map projection
    final static int ZOOM_MAX = 21;

    public static boolean checkGpsStatus(Context context){
        final LocationManager manager = (LocationManager) context.getSystemService( Context.LOCATION_SERVICE );
        return !(manager != null && !manager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    public static Address getAddress(Context context, double latitude, double longitude)
    {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(context, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude,longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            if(addresses != null && addresses.size() > 0) {
                return addresses.get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static String getLocationName(Context context, LatLng latLng)
    {
        String locationName = "";
        Address locationAddress = getAddress(context, latLng.latitude, latLng.longitude);

        if(locationAddress!=null)
        {
            String name = locationAddress.getFeatureName();

            if(!TextUtils.isEmpty(name))
            {
                locationName = name;
            }

        }
        return locationName;

    }

    public static String getAddress(Context context, LatLng latLng)
    {
        String geocodeLocation = "";
        Address locationAddress = getAddress(context, latLng.latitude, latLng.longitude);

        if(locationAddress!=null)
        {
            String address = locationAddress.getAddressLine(0);
            String address1 = locationAddress.getAddressLine(1);
            String city = locationAddress.getLocality();
            String state = locationAddress.getAdminArea();
            String country = locationAddress.getCountryName();
            String postalCode = locationAddress.getPostalCode();

            if(!TextUtils.isEmpty(address))
            {
                geocodeLocation = address;

                if (!TextUtils.isEmpty(address1))
                    geocodeLocation+="\n"+address1;

                if (!TextUtils.isEmpty(city))
                {
                    geocodeLocation+="\n"+city;

                    if (!TextUtils.isEmpty(postalCode))
                        geocodeLocation+=" - "+postalCode;
                }
                else
                {
                    if (!TextUtils.isEmpty(postalCode))
                        geocodeLocation+="\n"+postalCode;
                }

                if (!TextUtils.isEmpty(state))
                    geocodeLocation+="\n"+state;

                if (!TextUtils.isEmpty(country))
                    geocodeLocation+="\n"+country;
            }

        }
        return geocodeLocation;

    }

    public static void setCameraToReceivedLocation(GoogleMap googleMap, double lat, double lng) {

        try {
            LatLng homeLocation = new LatLng(lat, lng);

            if(googleMap != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(homeLocation, 15.0f));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
