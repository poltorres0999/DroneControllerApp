package compoltorres0999.httpsgithub.dronecontroller;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

import io.github.controlwear.virtual.joystick.android.JoystickView;

import static compoltorres0999.httpsgithub.dronecontroller.DroneClientServer.DroneSegment.*;
import static compoltorres0999.httpsgithub.dronecontroller.MainActivity.*;

public class ClientActivity extends AppCompatActivity {

    private static DroneClientServer client;
    private TelemetryTask telemetryTask;
    private StartConnectionTask StartConnectionTask;
    private StopTelemetryTask stopTelemetryTask;
    private Thread rcThread  = new Thread(this::onRcThreadRun);
    private static boolean telemetryStarted = false;

    private static short roll;
    private static short pitch;
    private static short yaw;
    private static short throttle;
    private static int rcTimer = 25; //milliseconds

    private static short minStrengthValue = 900;
    private static short middleStrengthValue = 1500;
    private static short maxStrengthValue = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Intent clientIntent = getIntent();
        String ip = clientIntent.getStringExtra(IP_ADDRESS);
        int port = clientIntent.getIntExtra(PORT, 0);

        //Creates and starts the server that will communicate with the MultiWii Raspberry Server.
        this.client = new DroneClientServer(new InetSocketAddress(ip, port));

        this.StartConnectionTask = new StartConnectionTask();
        this.StartConnectionTask.execute();

        TextView throttleV = findViewById(R.id.throttleV);
        TextView yawV = findViewById(R.id.yawV);
        TextView pitchV = findViewById(R.id.pitchV);
        TextView rollV = findViewById(R.id.rollV);

        //Creates the AsyncTask to handle telemetry execution.
        this.telemetryTask = new TelemetryTask(client);
        this.stopTelemetryTask = new StopTelemetryTask();


        //Creates right and left joysticks and defines it's behaviour.

        //Joystick left handles the values of yaw (x) and throttle (y).
        JoystickView joyLeft = findViewById(R.id.JoystickLeft);
        joyLeft.setOnMoveListener((angle, strength) -> {
            yaw = normalizeX(angle, strength);
            throttle = normalizeY(angle, strength);
            yawV.setText(Short.toString(yaw));
            throttleV.setText(Short.toString(throttle));
        });
        //Joystick right handles the values of yaw (x) and throttle (y).
        JoystickView joyRight = findViewById(R.id.JoystickRight);
        joyRight.setOnMoveListener((angle, strength) -> {
            roll = normalizeX(angle, strength);
            pitch = normalizeY(angle, strength);
            rollV.setText(Short.toString(roll));
            pitchV.setText(Short.toString(pitch));
        });
        rcThread.setName("Raul");
        rcThread.start();
    }

    public void startTelemetry (View view) throws IOException {

        if (!telemetryStarted) {
            this.telemetryTask.execute();
        }
    }

    public void stopTelemetry (View view) {
        this.stopTelemetryTask.execute();
    }

    public void arm (View view) throws IOException {
        //need to ensure that there is not interception between arm setRc commands and rcThread
        this.client.sendArm();
    }

    public void disarm (View view) throws IOException {
        //need to ensure that there is not interception between arm setRc commands and rcThread
        this.client.sendDiasArm();

    }

    public short normalizeX (int angle, int strength) {

        if (angle > 90 && angle < 180) {
            return scaleDown(Math.cos(Math.toRadians(180-angle)) * strength);
        } else if (angle > 180 && angle < 270) {
            return scaleDown(Math.cos(Math.toRadians(angle-180)) * strength);
        } else if  (angle > 270 && angle < 360) {
            return scaleUp(Math.cos(Math.toRadians(360-angle)) * strength);
        } else if (angle > 0 && angle < 90) {
            return scaleUp(Math.cos(Math.toRadians(angle)) * strength);
        } else if (angle == 90 || angle == 270) {
            return 1500;
        } else if (angle == 180) {
            return scaleDown(strength);
        } else {
            return scaleUp(strength);
        }

    }

    public short normalizeY (int angle, int strength) {

        if (angle > 90 && angle < 180) {
            return scaleUp(Math.cos(Math.toRadians(angle-90)) * strength);
        } else if (angle > 180 && angle < 270) {
            return scaleDown(Math.cos(Math.toRadians(270-angle)) * strength);
        } else if  (angle > 270 && angle < 360) {
            return scaleDown(Math.cos(Math.toRadians(angle-270)) * strength);
        } else if (angle > 0 && angle < 90) {
            return scaleUp(Math.cos(Math.toRadians(90-angle)) * strength);
        } else if (angle == 180 || angle == 0) {
            return 1500;
        } else if (angle == 90) {
            return scaleUp(strength);
        } else {
            return scaleDown(strength);
        }

    }

    public short scaleDown(double iValue) {
        return (short) (middleStrengthValue - (iValue * ((middleStrengthValue - minStrengthValue) / 100)));
    }

    public short scaleUp(double iValue) {
        return (short) (iValue * ((maxStrengthValue - middleStrengthValue) / 100) + middleStrengthValue);
    }

    private void onRcThreadRun() {

        while (true) {

            try {
                client.sendSetRC(roll, pitch, yaw, throttle);
                Thread.sleep(rcTimer);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private class TelemetryTask extends AsyncTask<Void, Short, Void> {

        private DroneClientServer client;
        private TextView altitude;
        private TextView vario;
        private TextView accx;
        private TextView accy;
        private TextView accz;
        private TextView gyrx;
        private TextView gyry;
        private TextView gyrz;
        private TextView magx;
        private TextView magy;
        private TextView magz;

        public TelemetryTask(DroneClientServer client) {

            this.client = client;

            this.altitude = (TextView) findViewById(R.id.estaltV);
            this.vario = (TextView) findViewById(R.id.varioV);
            this.accx = (TextView) findViewById(R.id.accxV);
            this.accy = (TextView) findViewById(R.id.accyV);
            this.accz = (TextView) findViewById(R.id.acczV);
            this.gyrx = (TextView) findViewById(R.id.gyrxV);
            this.gyry = (TextView) findViewById(R.id.gyryV);
            this.gyrz = (TextView) findViewById(R.id.gyrzV);
            this.magx = (TextView) findViewById(R.id.magxV);
            this.magy = (TextView) findViewById(R.id.magyV);
            this.magz = (TextView) findViewById(R.id.magzV);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            final DatagramPacket datagramPacket = client.generateDatagramPacket((short) 120);

            if (!telemetryStarted) {
                try {
                    client.datagramSocket.send(datagramPacket);
                    client.datagramSocket.receive(datagramPacket);
                    if (new DroneClientServer.DroneSegment(datagramPacket.getData()).code == 121) {
                        telemetryStarted = true;

                        while (true){
                            final DatagramPacket datagramPacketResponse=new DatagramPacket(new byte[40],40);
                            try {
                                client.datagramSocket.receive(datagramPacketResponse);
                                final DroneClientServer.DroneSegment droneSegment=new DroneClientServer.DroneSegment(datagramPacketResponse.getData());

                                switch (droneSegment.code) {
                                    case 102:

                                        RawImu raw = new RawImu(droneSegment.payload);

                                        publishProgress(droneSegment.code, raw.accx, raw.accy,
                                                raw.accz, raw.gyrx, raw.gyry, raw.gyrz, raw.magx, raw.magy, raw.magz);
                                        break;

                                    case 109:
                                        Altitude altitude
                                                = new Altitude(droneSegment.payload);
                                        publishProgress(droneSegment.code, altitude.estalt, altitude.vario);
                                        break;
                                    default:
                                        break;
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Short... values) {

            switch (values[0]) {
                case 102:
                    //ACC
                    this.accx.setText(String.format("%s", values[1]));
                    this.accy.setText(String.format("%s", values[2]));
                    this.accz.setText(String.format("%s", values[3]));
                    //GYR
                    this.gyrx.setText(String.format("%s", values[4]));
                    this.gyry.setText(String.format("%s", values[5]));
                    this.gyrz.setText(String.format("%s", values[6]));
                    //MAG
                    this.magx.setText(String.format("%s", values[7]));
                    this.magy.setText(String.format("%s", values[8]));
                    this.magz.setText(String.format("%s", values[9]));
                    break;

                case 109:
                    this.altitude.setText(String.format("%s", values[1]));
                    this.vario.setText(String.format("%s", values[2]));
                    break;
                default:
                    break;
                //More case's can be added depending the capabilities of the FC.

            }
        }

        private void setTelemetryCallback () {

            this.client.setOnTelemetryCallback((telemetryDatagramPacket) -> {
                switch (telemetryDatagramPacket.code) {
                    case 102:

                        RawImu raw = new RawImu(telemetryDatagramPacket.payload);

                        publishProgress(telemetryDatagramPacket.code, raw.accx, raw.accy,
                                raw.accz, raw.gyrx, raw.gyry, raw.gyrz, raw.magx, raw.magy, raw.magz);
                        break;

                    case 109:
                        Altitude altitude
                                = new Altitude(telemetryDatagramPacket.payload);
                        publishProgress(telemetryDatagramPacket.code, altitude.estalt, altitude.vario);
                        break;
                    default:

                        throw new IllegalStateException("Im fucked");
                }
                //More case's can be added depending the capabilities of the FC.

            });
        }
    }

    public class StartConnectionTask extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... contexts) {
            try {
                client.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class StopTelemetryTask extends AsyncTask<Context, Void, Void> {

        @Override
        protected Void doInBackground(Context... contexts) {
            try {
                client.stopTelemetry();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (telemetryTask.isCancelled() == false ) {
                telemetryTask.cancel(true);
            }

            return null;
        }

    }
}
