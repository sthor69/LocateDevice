package com.storassa.android.locatedevice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends Activity {

    private final String SERVER_URI = "tcp://172.20.1.243:1883";
    private final String CLIENT_ID = "CLIENT_ID";

    private Button mainButton;
    private TextView mainText;

    private boolean isGPSEnabled, isNetworkEnabled;

    MqttAndroidClient mqttAndroidClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) getApplicationContext()
                .getSystemService(LOCATION_SERVICE);

        // getting GPS status
        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        mainButton = (Button) findViewById(R.id.main_btn);
        mainButton.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                if (!isGPSEnabled)
                    ;

                else if (!isNetworkEnabled)
                    ;

                startService(new Intent(MainActivity.this, LocationService.class));

                mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), SERVER_URI, CLIENT_ID);

                mqttAndroidClient.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable cause) {

                        while (!isNetworkAvailable()) {

                            try {
                                Thread.sleep(10000);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }

                        connect();
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });

                connect();
            }

        });

        mainText = (TextView)findViewById(R.id.main_text);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void connect() {
        try {
            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttConnectOptions.setCleanSession(false);

            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    mainText.setText("MQTT connected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }


}

