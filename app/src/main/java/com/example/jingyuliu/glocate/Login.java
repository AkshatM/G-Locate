package com.example.jingyuliu.glocate;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import android.content.SharedPreferences;

public class Login extends Activity {

    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //amILoggedIn();
    }

    public void loginComplete(View view) {
        EditText editText = (EditText) findViewById(R.id.phone_number);
        EditText editText2 = (EditText) findViewById(R.id.user_name);
        Intent intent = new Intent(this, MapsActivity.class);
        String number = editText.getText().toString();
        String name = editText2.getText().toString();
        if (number == null || number.isEmpty()||name == null || name.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Enter a valid name and number", Toast.LENGTH_LONG).show();
        }
        else {
            intent.putExtra("mPhoneNumber", number);
            intent.putExtra("mName",name);
            startActivity(intent);
            SharedPreferences settings = getSharedPreferences(Login.PREFS_NAME, 0); // 0 - for private mode
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("hasLoggedIn", true);
            editor.commit();
            Login.this.finish();
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
