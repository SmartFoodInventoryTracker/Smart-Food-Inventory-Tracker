package com.example.smartfoodinventorytracker;

import static java.sql.Types.NULL;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

public class Bluetooth {

    private final UUID id = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice actual;
    private BluetoothAdapter bt;
    private BluetoothDevice esp32;
    private BluetoothSocket channel;

    private Context cont;
    public Bluetooth(Context context) {
        bt = BluetoothAdapter.getDefaultAdapter();
        cont=context;
        getDevices();
        displayDevices();
    }

    private void getDevices() {
        if (ActivityCompat.checkSelfPermission(cont, android.Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return;  // just return empty list
        }

        for (BluetoothDevice device : bt.getBondedDevices()) {
            if (device.getName() != null && device.getName().contains("ESP")) {
                actual = device;
            }
        }
    }

    public void displayDevices()
    {
        if (ActivityCompat.checkSelfPermission(cont, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        getDevices();
        if(actual!=null) {
            String name = actual.getName();
            Log.d("MyActivity", name);
        }
        else
            Log.d("MyActivity", "no ESP device");

    }


    public void setConnection() throws IOException {
        if (ActivityCompat.checkSelfPermission(cont, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        if (actual==null) {
            throw new IOException("No ESP32 device found among bonded devices.");
        }
        try{
        channel = actual.createRfcommSocketToServiceRecord(id);
        channel.connect(); // <-- attempt connection

        // If successful
        Log.d("Bluetooth", "Device connected: " + actual.getName());
        // Keep or manage the socket as needed

        } catch (IOException connectException) {
        Log.d("Bluetooth", "Device not connected: " + actual.getName());
        // Handle or ignore failed connection
        }
    }

    public void transmitCredentials(String cred) throws IOException {
            setConnection();
            OutputStream out = channel.getOutputStream();
            out.write(cred.getBytes());
    }

}
