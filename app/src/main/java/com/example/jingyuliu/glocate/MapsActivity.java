package com.example.jingyuliu.glocate;

import android.app.Dialog;
import android.widget.Spinner;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.view.MenuItem;
import android.view.View;
import android.app.AlertDialog;
import android.view.View.OnClickListener;
import android.content.IntentSender;
import android.content.Intent;
import android.graphics.Color;
import android.content.DialogInterface;
import android.location.Location;
import android.location.Geocoder;
import android.location.Address;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.TileOverlay;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    // Play service
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;
    private boolean HeatMapEnabled = false;
    private boolean usingServerData = false;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    private AutoCompleteTextView mSearch;

    private boolean zoomToMyLocation = false;
    private boolean firstTimeInvoked = true;
    private int init_zoom_level = 13;

    private String TAG = "glocate.view";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private HeatmapTileProvider mHeatMapProvider;
    private TileOverlay mOverlay;
    private List<LatLng> mInterstingPoints;
    private LatLng newlocation;
    private Geocoder gc;

    // Option for heatmap
    // Create the gradient.
    private int[] colors = {
            Color.rgb(102, 225, 0), // green
            Color.rgb(255, 153, 0)    // red
    };

    private float[] startPoints = {
            0.2f, 1f
    };
    private Gradient gradient = new Gradient(colors, startPoints);

    /*
     * Initialize the Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        mMap.setMyLocationEnabled(true);
        mSearch = (AutoCompleteTextView) findViewById(R.id.et_location);
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        gc = new Geocoder(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_file, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                final Dialog settings_dialog = new Dialog(this);
                settings_dialog.setContentView(R.layout.settings_dialog);
                settings_dialog.setTitle("Settings");
                settings_dialog.show();
                Button OkayButton = (Button) settings_dialog.findViewById(R.id.Okay);
                final CheckBox Checker = (CheckBox) settings_dialog.findViewById(R.id.checkbox);
                if (usingServerData){
                    Checker.setChecked(true);
                }
                OkayButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Spinner mySpinner = (Spinner) settings_dialog.findViewById(R.id.spinner1);
                        init_zoom_level = Integer.parseInt(mySpinner.getSelectedItem().toString().replaceAll("[^\\d]", ""));
                        if (Checker.isChecked()){
                            usingServerData = true;
                        }
                        else{
                            usingServerData = false;
                        }
                        settings_dialog.dismiss();
                    }
                });
                break;
            case R.id.heatmap:
                toggleHeatMap();
                if (HeatMapEnabled){
                    Toast.makeText(getApplicationContext(),
                            "Heat map enabled. Please wait a few minutes for changes to take effect.", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(),
                            "Heat map disabled. Please wait a few minutes for changes to take effect.", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void mapRandomizer(double latitude, double longitude) {
        mInterstingPoints.clear();
        float zoom = mMap.getCameraPosition().zoom;
        Log.d(TAG, "Zoom level is:" + zoom);
        for(int i=0; i< 100; i++) {
            double lat = (Math.random()-0.5)/zoom/zoom+latitude;
            double longi = (Math.random()-0.5)/zoom/zoom+longitude;
            double distance_to_us = distance(lat,longi,latitude,longitude)*1.609344*1000;
            if(distance_to_us <= 500)
                mInterstingPoints.add(new LatLng(lat, longi));
        }
    }


    private void addRandmoHeatMap (double latitude, double longitude) {
        // Get the data: latitude/longitude positions.
        // Create a heat map tile provider.
        // Get the data: latitude/longitude positions.
        // Create a heat map tile provider.
        if (HeatMapEnabled) {
            if (!usingServerData){
                mapRandomizer(latitude, longitude);
            }
            if (mInterstingPoints.size() != 0) {
                Log.d(TAG, "Moo! " + TextUtils.join(", ", mInterstingPoints));
                mHeatMapProvider = new HeatmapTileProvider.Builder()
                        .data(mInterstingPoints)
                        .gradient(gradient)
                        .build();
                // Add a tile overlay to the map, using the heat map tile provider.
                // Refresh map
                if (mOverlay != null) mOverlay.remove();
                mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mHeatMapProvider));
            }
        } else {
            if (mOverlay != null) {
                mOverlay.remove();
            }
        }
    }


    private void readList(final Location location) {
        RequestParams params = new RequestParams();
        //params.put("phone", "5129037891"); // use "5129037891" or Danny's number
        //params.put("name", "DL");
        //params.put("email", "ljy1681@gmail.com");
        //params.put("longitude", location.getLatitude());
        //params.put("latitude", location.getLongitude());
        MyFuckingClient.get("around/me", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray jsonArr) {
                // Pull out the first event on the public timeline
                Log.d(TAG, "Posting route success array");
                Log.d(TAG, String.valueOf(jsonArr));
                try {
                    mInterstingPoints = parseList(jsonArr);
                    if (firstTimeInvoked) {
                        addRandmoHeatMap(location.getLatitude(), location.getLongitude());
                    }
                    else {
                        addRandmoHeatMap(newlocation.latitude, newlocation.longitude);
                    }
                    Log.d(TAG, "Bubble! " + TextUtils.join(", ", mInterstingPoints));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "JSON EXCEPTION T T");
                }
            }
        });

    }

    public List<LatLng> parseList(JSONArray arr) throws JSONException {
        List<LatLng> list = new ArrayList<LatLng>();
        for(int i = 0; i < arr.length(); i++){
            JSONObject jsonobj = arr.getJSONObject(i);
            double lat, longi;
            lat = Double.parseDouble((jsonobj.get("latitude").toString()));
            longi = Double.parseDouble(jsonobj.get("longitude").toString());
            Log.d(TAG, i + "th " + " lat " + String.valueOf(lat) + " longi " + String.valueOf(longi));
            list.add(new LatLng(lat, longi));
        }
        Log.d(TAG, "Meow! " + TextUtils.join(", ", list));
        return list;
    }

    public boolean toggleHeatMap() {
        return HeatMapEnabled = !HeatMapEnabled;
    }
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        startPeriodicUpdates();
    }

    /*
         * Called by Location Services if the connection to the
         * location client drops because of an error.
         */
    @Override
    public void onDisconnected() { }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
        }
    }


    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }


    @Override
    public void onPause() {
        super.onPause();
    }


    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();

        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

    }


    /*
     * Called when the system detects that this Activity is now visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }


    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {
        // invoked once, when you open map for the first time.
            postMyLocation(location.getLongitude(),location.getLatitude());
            Log.d(TAG, "Woof! " + " long " + location.getLongitude() + " lat " + location.getLatitude());
            LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, 15);
            if (!zoomToMyLocation) {
                mMap.animateCamera(yourLocation);
                zoomToMyLocation = true;
            }

            Log.d(TAG, "Zoom in at latitude:" + coordinate.latitude + ", longitude: " + coordinate.longitude);
            Log.d(TAG, "Reloading heatmap");
            readList(location);
        }

    public void commitSearch(View button1){
        //When button is pressed, it performs this action.
        String address = mSearch.getText().toString();
        try {
            final List<Address> foundAddresses = gc.getFromLocationName(address, 5); // Search addresses
            if (foundAddresses == null || foundAddresses.isEmpty()) {
                Toast.makeText(getApplicationContext(),
                        "Address does not exist", Toast.LENGTH_LONG).show();
            } else {
                AlertDialog.Builder alertDialogbuilder = new AlertDialog.Builder(this);
                alertDialogbuilder.setTitle("Pick your location");
                final CharSequence[] dynamite = parseResults(foundAddresses);
                alertDialogbuilder.setItems(parseResults(foundAddresses), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSearch.setText(dynamite[which]);
                        try{
                            Address firstresult = foundAddresses.get(which);
                            LatLng newcoordinate = new LatLng(firstresult.getLatitude(), firstresult.getLongitude());
                            Log.d(TAG, "Gogo "+ firstresult.getLatitude() + " " + firstresult.getLongitude());
                            newlocation = newcoordinate;
                            CameraUpdate newLocation = CameraUpdateFactory.newLatLngZoom(newcoordinate, init_zoom_level);
                            zoomToMyLocation = false;
                            if (!zoomToMyLocation) {
                                mMap.animateCamera(newLocation);
                                zoomToMyLocation = true;
                            }
                        }
                        catch (Exception IOException){
                            //implement whatever is necessary, but otherwise do nothing
                        }
                    }
                });
                AlertDialog alertDialog2 = alertDialogbuilder.create();
                alertDialog2.show();
                if (firstTimeInvoked) {
                    firstTimeInvoked = false;
                }
            }
        }
        catch (Exception e)
        {
            // Do nothing
        }
    }

    public CharSequence[] parseResults (List<Address> listable){//should return CharSequence
        // parse list of address to form a coherent character sequence for alert dialog box
        ArrayList<String> cutelist = new ArrayList<String>();
        for(int i = 0; i < listable.size(); i++) {
            Address Cool = listable.get(i);
            String sum = "";
            for (int j = 0;true;j++) {
                if (Cool.getAddressLine(j) != null) {
                    if (j == 0){
                        sum = sum + Cool.getAddressLine(j);
                    }
                    else {
                        sum = sum + ", " + Cool.getAddressLine(j);
                    }
                } else {
                    break;
                }
            }
            cutelist.add(sum);
        }
        CharSequence[] desiredSequence = cutelist.toArray(new CharSequence[cutelist.size()]);
        return desiredSequence;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     *
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     *
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setMyLocationEnabled(true);
        }

            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
//               does nothing
    }


    private void postMyLocation(double longitude, double latitude) {
        RequestParams params = new RequestParams();
        Intent intent = getIntent();
        String mPhoneNumber = intent.getStringExtra("mPhoneNumber");
        String mName = intent.getStringExtra("mName");
        params.put("phone", mPhoneNumber);
        params.put("name", mName);
        params.put("email", "ljy1681@gmail.com");
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        MyFuckingClient.post("about/me", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray
                Log.d(TAG, "Fucking yeah.");
                Log.d(TAG, String.valueOf(response));
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                // Pull out the first event on the public timeline
                Log.d(TAG, "Fuking yeah.");
            }
        });
    }


    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }


    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

}
