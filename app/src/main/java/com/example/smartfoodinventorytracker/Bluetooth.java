package com.example.smartfoodinventorytracker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.util.Set;

public class Bluetooth {
    private Set<BluetoothDevice> fetchedB, actual;
    private BluetoothAdapter bt;
    private BluetoothDevice esp32;
    private BluetoothSocket channel;

    private Context cont;
    public Bluetooth(Context context) {
        bt = BluetoothAdapter.getDefaultAdapter();
        cont=context;
    }

    private void getDevices(){
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
        fetchedB=bt.getBondedDevices();
        for(BluetoothDevice device:fetchedB)
        {
            if(device.getName().contains("ESP32"){
                actual.add(device);
            }
        }
    }





}
