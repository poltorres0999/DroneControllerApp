package compoltorres0999.httpsgithub.dronecontroller;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    public static final String IP_ADDRESS = "IP_ADDRESS";
    public static final String PORT = "PORT";

    private EditText ipAddress;
    private EditText commandPort;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ipAddress = findViewById(R.id.deviceIp);
        commandPort = findViewById(R.id.commandPort);
    }

    public void startConnection(View view) {
        Intent startClient = new Intent(this, ClientActivity.class);

        String Ip = ipAddress.getText().toString();
        int port = Integer.parseInt(commandPort.getText().toString());
        startClient.putExtra(IP_ADDRESS, Ip);
        startClient.putExtra(PORT, port);
        startActivity(startClient);
    }

}
