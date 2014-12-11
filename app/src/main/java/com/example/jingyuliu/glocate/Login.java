package com.example.jingyuliu.glocate;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import android.content.SharedPreferences;

public class Login extends Activity {

    public static final String PREFS_NAME = "MyPrefsFile"; //creates SharedPrefs file name for storing data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // creates login page
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); //sets UI interface to activity_login
        amILoggedIn(); //checks if you've already logged in
    }

    public void loginComplete(View view) { //what do when you've pressed the Log In button on screen
        EditText editText = (EditText) findViewById(R.id.phone_number);
        EditText editText2 = (EditText) findViewById(R.id.user_name);
        Intent intent = new Intent(this, MapsActivity.class); //saves the data you entered into an intent
        String number = editText.getText().toString();
        String name = editText2.getText().toString();
        if (number == null || number.isEmpty()||name == null || name.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Enter a valid name and number", Toast.LENGTH_LONG).show();
        }
        else {
            startActivity(intent);
            SharedPreferences settings = getSharedPreferences(Login.PREFS_NAME, 0); // 0 - for private mode
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("hasLoggedIn", true);
            editor.putString("mPhoneNumber", number);
            editor.putString("mName", name);
            editor.commit();
            //saves hasLoggedIn and other useful string data
            Login.this.finish(); //Login activity self-destructs
        }
    }

    public void amILoggedIn(){
        SharedPreferences settings = getSharedPreferences(Login.PREFS_NAME, 0);
        //Get "hasLoggedIn" value. If the value doesn't exist yet false is returned
        boolean hasLoggedIn = settings.getBoolean("hasLoggedIn", false);
        if (hasLoggedIn){
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);
            Login.this.finish();
        }
    }

}
