package com.example.smartfoodinventorytracker;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.util.Set;

public class Bluetooth {
    private Set<Bluetooth> setB;
    private BluetoothAdapter bt;
    private BluetoothDevice esp32;
    private BluetoothSocket channel;
    public Bluetooth() {
        bt = BluetoothAdapter.getDefaultAdapter();
    }

    private void getDevices(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bt.getBondedDevices();
    }



}
