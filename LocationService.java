package com.storassa.android.locatedevice;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class LocationService extends Service implements LocationListener {

    private final String SERVER_URI = "tcp://172.20.1.243:1883";
    private final String CLIENT_ID = "CLIENT_ID";
    private final String TOPIC = "LOCATION_TOPIC";
    private final long MIN_TIME_BW_UPDATES = 10;
    private final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 100;
    Location location;
    private double latitude, longitude;
    private LocationManager locationManager;
    private MqttAndroidClient mqttAndroidClient;
    private String publishMessage;


    public LocationService() {
    }

    private Location getLastLocation(LocationManager locMgr) {
        try {
            return locMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        publishLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleStartCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleStartCommand(Intent intent) {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

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

        try {

            if (location == null) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                Log.d("GPS Enabled", "GPS Enabled");
            }

        } catch (SecurityException e) {
            throw new RuntimeException(e);
        }

        connect();

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
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                }
            });


        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private void publishLocation(Location _location) {

        if (_location != null) {

            publishMessage = Double.toString(latitude) + ":" + Double.toString(longitude);


            try {

                MqttMessage message = new MqttMessage();
                message.setPayload(publishMessage.getBytes());
                mqttAndroidClient.publish(TOPIC, message);

                if (!mqttAndroidClient.isConnected()) {
                    connect();
                }

            } catch (MqttException e) {
                System.err.println("Error Publishing: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
