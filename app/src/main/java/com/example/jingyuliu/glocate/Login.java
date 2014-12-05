package com.example.jingyuliu.glocate;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;


public class Login extends Activity {

    public final static String mPhoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    public void loginComplete(View view) {
        EditText editText = (EditText) findViewById(R.id.phone_number);
        Intent intent = new Intent(this, MapsActivity.class);
        String message = editText.getText().toString();
        if (message == null || message.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Enter a valid number", Toast.LENGTH_LONG).show();
        } else {
            intent.putExtra(mPhoneNumber, message);
            startActivity(intent);
        }
    }
}
