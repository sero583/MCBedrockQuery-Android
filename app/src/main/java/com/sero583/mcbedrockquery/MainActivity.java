package com.sero583.mcbedrockquery;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void runQuery(View v) {
        EditText editTextIP = (EditText) findViewById(R.id.ip);
        EditText editTextPort = (EditText) findViewById(R.id.port);
        String ip = editTextIP.getText().toString();

        if(ip==null||ip.isEmpty()==true) {
            Toast.makeText(this, "Please fill the first field with an IP", Toast.LENGTH_SHORT).show();
            return;
        }

        String inputPort = editTextPort.getText().toString();
        Integer port = inputPort == null || inputPort.isEmpty() == true ? ServerInfo.DEFUALT_PORT : Integer.parseInt(inputPort);

        QueryRequest request = new QueryRequest(this);
        request.execute(new ServerInfo(ip, port));
    }
}
