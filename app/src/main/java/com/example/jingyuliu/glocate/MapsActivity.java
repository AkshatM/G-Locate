package com.example.jingyuliu.glocate;

import android.app.Dialog;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.CheckBox;
import android.view.MenuItem;
import android.view.View;
import android.app.AlertDialog;
import android.view.View.OnClickListener;
import android.content.IntentSender;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.gms.maps.model.Marker;
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
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    // Play service
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;
    
    private AutoCompleteTextView mSearch; //AutoComplete text view identifier in activity_maps
    private boolean HeatMapEnabled = false; //controls if heat map is on or off
    private boolean usingServerData = false; // controls what data heat map uses.
    private Marker marker; //used to control removal of friendFind marker instead of all markers
    private boolean zoomToMyLocation = false; //one time variable - for use only when app starts, to prevent unwanted behaviour
    private boolean firstTimeInvoked = true; //one time variable - used to prevent null pointer exceptions for some variables
    private int init_zoom_level = 13; // used to control global zoom settings
    private String TAG = "glocate.view";
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private HeatmapTileProvider mHeatMapProvider;
    private TileOverlay mOverlay;
    private List<LatLng> mInterestingPoints;
    private LatLng newlocation;
    private Geocoder gc;
    public static final String PREFS_NAME = "MyPrefsFile"; //creates SharedPrefs file name for storing data

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
        super.onCreate(savedInstanceState); //required initiation function
        setContentView(R.layout.activity_maps); //establishes UI layout
        setUpMapIfNeeded(); //generates map view
        mMap.setMyLocationEnabled(true); //sets your location on map
        mSearch = (AutoCompleteTextView) findViewById(R.id.et_location); //basic identifier for AutoCompleteTextView in XML

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
        gc = new Geocoder(this); // creates new Geocoder instance, to be used for finding coordinates from a name
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // lets Android know what to do when menu button is pressed
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_file, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // this is invoked when menu options are selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                // if settings is invoked, perform this action
                final Dialog settings_dialog = new Dialog(this);
                settings_dialog.setContentView(R.layout.settings_dialog);
                settings_dialog.setTitle("Settings");
                settings_dialog.show(); //create settings dialog box with specified content view
                Button OkayButton = (Button) settings_dialog.findViewById(R.id.Okay); //create button interface
                final CheckBox Checker = (CheckBox) settings_dialog.findViewById(R.id.checkbox); //create checkbox interface
                if (usingServerData){
                    Checker.setChecked(true); // if variable is true, it should continue to be true when user calls for dialog box again
                }
                OkayButton.setOnClickListener(new OnClickListener() {
                    // if Okay button is clicked, perform this action
                    @Override
                    public void onClick(View v) {
                        Spinner mySpinner = (Spinner) settings_dialog.findViewById(R.id.spinner1);
                        init_zoom_level = Integer.parseInt(mySpinner.getSelectedItem().toString().replaceAll("[^\\d]", "")); // set zoom level to user settings
                        if (Checker.isChecked()){ // if user selects this option or if option is already selected
                            usingServerData = true;
                        }
                        else{
                            usingServerData = false;
                        }
                        settings_dialog.dismiss(); //get rid of dialog box
                    }
                });
                break;
            case R.id.heatmap:
                // if heatmap option is invoked, do this
                toggleHeatMap();
                // toggleHeatMap toggles boolean value of EnableHeatMap - EnableHeatMap turns heat map on and off.
                if (HeatMapEnabled){
                    Toast.makeText(getApplicationContext(),
                            "Heat map enabled. Please wait a few minutes for changes to take effect.", Toast.LENGTH_LONG).show();
                    // send toast to user letting him know what to expect.
                }
                else
                {
                    Toast.makeText(getApplicationContext(),
                            "Heat map disabled. Please wait a few minutes for changes to take effect.", Toast.LENGTH_LONG).show();
                    // send toast to user letting him know what to expect.
                }
                break;
            case R.id.friend_finder:
                // if friend finder option selected
                final Dialog friend_dialog = new Dialog(this); 
                friend_dialog.setContentView(R.layout.friendfinder); // set user interface for dialog box
                friend_dialog.setTitle("Friend Finder");
                friend_dialog.show(); // show dialog box
                Button searchButton = (Button) friend_dialog.findViewById(R.id.friend_search_btn); //get access to button interface
                final EditText edit = (EditText) friend_dialog.findViewById(R.id.friend_search); //get access to EditText interface
                searchButton.setOnClickListener(new OnClickListener() {
                    // if searchbutton is pressed, do this
                    @Override
                    public void onClick(View v) {
                        final String phone_number = edit.getText().toString(); //get phone number
                        findFriend(phone_number); //tell server to send back location of friend and display it
                        friend_dialog.dismiss(); // get rid of dialog box
                    }
                });
            default:
                // otherwise, if nothing is pressed
                break;
        }
        return true;
    }

    private void mapRandomizer(double latitude, double longitude) {
        // Generates random points - used for demonstration purposes. Returns a set of points within a fixed distance from you.
        mInterestingPoints.clear(); //gets rid of mInterestingPoints array if already has data in it
        float zoom = mMap.getCameraPosition().zoom; // parameter for distance function
        Log.d(TAG, "Zoom level is:" + zoom);
        for(int i=0; i< 100; i++) {
            double lat = (Math.random()-0.5)/zoom/zoom+latitude; //randomly generates latitudes
            double longi = (Math.random()-0.5)/zoom/zoom+longitude; // randomly generates longitudes
            double distance_to_us = distance(lat,longi,latitude,longitude)*1.609344*1000; //calculates distance to lat/lng coordinates 
            if(distance_to_us <= 500) //self-explanatory
                mInterestingPoints.add(new LatLng(lat, longi));
        }
    }


    private void addRandomHeatMap (double latitude, double longitude) {
        // Get the data: latitude/longitude positions.
        // Create a heat map tile provider.
        if (HeatMapEnabled) { // if user wants heatmap
            if (!usingServerData){ //and if user doesn't care about only showing data from server
                mapRandomizer(latitude, longitude); //randomly generate points instead - if usingServerData is true, we don't do this.
            }
            if (mInterestingPoints.size() != 0) {
                mHeatMapProvider = new HeatmapTileProvider.Builder()
                        .data(mInterestingPoints)
                        .gradient(gradient)
                        .build();
                // Add a tile overlay to the map, using the heat map tile provider.
                // Refresh map
                if (mOverlay != null) mOverlay.remove();
                mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mHeatMapProvider));
            }
        } else { //is heatmap is disabled, get rid of last heatmap
            if (mOverlay != null) {
                mOverlay.remove();
            }
        }
    }

    private void findFriend(final String phone_num) {
        // findFriend finds your friend's location and shows his location on a map for you
        RequestParams params = new RequestParams(); //required
        MyClient.get("find/" + phone_num, params, new JsonHttpResponseHandler() {
            // if GET call was successful
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject jsonobj) {
                // Pull out the first event on the public timeline
                try {
                    double lat = Double.parseDouble((jsonobj.get("latitude").toString()));
                    double lon = Double.parseDouble(jsonobj.get("longitude").toString());
                    LatLng newcoordinate = new LatLng(lat, lon); //create new coordinate from JSONOBJ data
                    //add marker + change location
                    CameraUpdate newLocation = CameraUpdateFactory.newLatLngZoom(newcoordinate, init_zoom_level); // zoom to new location
                    zoomToMyLocation = false;
                    if (!zoomToMyLocation) {
                        mMap.animateCamera(newLocation);
                        marker = mMap.addMarker(new MarkerOptions()
                                .position(newcoordinate)
                                .draggable(false));
                        zoomToMyLocation = true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            // if no such phone number exists, do this
            public void onFailure(int statusCode, Header[] headers, String jsonresponse, Throwable throwable) {
                Toast.makeText(getApplicationContext(),
                        "No friend with that number in database", Toast.LENGTH_LONG).show();
                // let user know what just happened
            }
        });


    }


    private void readList(final Location location) {
        // gets all data points from server. Does not depend on current location.
        RequestParams params = new RequestParams(); //required for MyClient
        MyClient.get("around/me", params, new JsonHttpResponseHandler() {
            // if GET from server is successful (i.e. page exists, which it always does)
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray jsonArr) {
                // Pull out the first event on the public timeline
                try {
                    mInterestingPoints = parseList(jsonArr); //set this variable to all the points in jsonArr
                    addRandomHeatMap(newlocation.latitude, newlocation.longitude); //random heat map is now activated, centered at appropriate lat/lng
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public List<LatLng> parseList(JSONArray arr) throws JSONException {
        // tells Java how to parse through jsonArray for our data points
        List<LatLng> list = new ArrayList<LatLng>();
        for(int i = 0; i < arr.length(); i++){
            JSONObject jsonobj = arr.getJSONObject(i);
            double lat, longi;
            lat = Double.parseDouble((jsonobj.get("latitude").toString()));
            longi = Double.parseDouble(jsonobj.get("longitude").toString());
            list.add(new LatLng(lat, longi));
        }
        return list;
    }

    public boolean toggleHeatMap() {
        // changes boolean value of HeatMapEnabled
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
            // Was never implemented, and was unnecessary.
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
        // invoked periodically. Basically sets camera to zoom in on your location at start, then restricts itself to pulling data points from server and sending your location to server.
            postMyLocation(location.getLongitude(),location.getLatitude()); //posts your location to server
            LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
            if (firstTimeInvoked){
               newlocation = coordinate; //sets global variable to this if app has just been started.
            }
            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(coordinate, init_zoom_level);
            if (!zoomToMyLocation) {
                mMap.animateCamera(yourLocation); // actually forces camera to move to newlocation
                zoomToMyLocation = true;
            }
            readList(location); //gets all data from server
        }

    public void commitSearch(View button1){
        //When find button is pressed in search bar, it performs this action.
        String address = mSearch.getText().toString();
        try {
            final List<Address> foundAddresses = gc.getFromLocationName(address, 5); // Searches and returns list of upto 5 addresses.
            if (foundAddresses == null || foundAddresses.isEmpty()) { //if no results were found
                Toast.makeText(getApplicationContext(),
                        "Address does not exist", Toast.LENGTH_LONG).show();
                // let user know what happened
            } else {
                AlertDialog.Builder alertDialogbuilder = new AlertDialog.Builder(this); //create suggestions dialog box
                alertDialogbuilder.setTitle("Pick your location");
                final CharSequence[] dynamite = parseResults(foundAddresses); //creates list of options
                alertDialogbuilder.setItems(parseResults(foundAddresses), new DialogInterface.OnClickListener() { //shows options in dialog box
                    public void onClick(DialogInterface dialog, int which) { //if an item is pressed (item location is given by int which, which tells index of item selected)
                        mSearch.setText(dynamite[which]); //change autocomplete text view to read the results of selection
                        try{
                            Address firstresult = foundAddresses.get(which);
                            LatLng newcoordinate = new LatLng(firstresult.getLatitude(), firstresult.getLongitude()); //finds coordinates of selection
                            firstTimeInvoked = false;
                            newlocation = newcoordinate; //updates global variable for other functions
                            CameraUpdate newLocation = CameraUpdateFactory.newLatLngZoom(newcoordinate, init_zoom_level); //changes view to new location
                            zoomToMyLocation = false;
                            if (!zoomToMyLocation) {
                                mMap.animateCamera(newLocation);
                                mMap.clear(); //gets rid of all markers, including friends
                                mMap.addMarker(new MarkerOptions() //adds a marker
                                        .position(newcoordinate)
                                        .title(dynamite[which].toString())
                                        .draggable(false));
                                zoomToMyLocation = true;
                            }
                        }
                        catch (Exception IOException){
                            //implement whatever is necessary, but otherwise do nothing
                        }
                    }
                });
                AlertDialog alertDialog2 = alertDialogbuilder.create();
                alertDialog2.show(); //show above alert dialog
            }
        }
        catch (Exception e)
        {
            // Do nothing
        }
    }

    public CharSequence[] parseResults (List<Address> listable){//should return CharSequence
        // parse list of address to form a coherent character sequence for suggestions box
        // result is a description of the address that ordinary hummans can understand
        ArrayList<String> cutelist = new ArrayList<String>();
        for(int i = 0; i < listable.size(); i++) { //could be made more efficient, but oh well.
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
        // responsible for posting your location to the server
        RequestParams params = new RequestParams();
        SharedPreferences shared = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Intent intent = getIntent(); //gets data from login screen app
        // Original intent based activity
        //String mPhoneNumber = intent.getStringExtra("mPhoneNumber");
        //String mName = intent.getStringExtra("mName");
        String mPhoneNumber = shared.getString("mPhoneNumber","");
        String mName = shared.getString("mName","");
        params.put("phone", mPhoneNumber);
        params.put("name", mName);
        params.put("email", "ljy1681@gmail.com");
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        MyClient.post("about/me", params, new JsonHttpResponseHandler() { //posts to the server
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray
                // does nothing however
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                // Pull out the first event on the public timeline
                // does nothing however
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
        // determines distance between your current location and the another pair of coordinates
        // used in random heat map generation
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
