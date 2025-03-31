package com.example.smartfoodinventorytracker;

import static java.sql.Types.NULL;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

public class Bluetooth {

    private final UUID id = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private List<BluetoothDevice> actual;
    private BluetoothAdapter bt;
    private BluetoothDevice esp32;
    private BluetoothSocket channel;

    private Context cont;
    public Bluetooth(Context context) {
        bt = BluetoothAdapter.getDefaultAdapter();
        cont=context;
    }

    private List<BluetoothDevice> getDevices(){
        if (ActivityCompat.checkSelfPermission(cont, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            actual = null;
            return actual;
        }
        for(BluetoothDevice device:bt.getBondedDevices())
        {
            if(device.getName().contains("ESP32")){
                actual.add(device);
            }
        }
        return actual;
    }


    private void setConnection() throws IOException {
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
        channel=actual.get(0).createRfcommSocketToServiceRecord(id);
        channel.connect();
    }

    private void transmitCredentials(String cred) throws IOException {

        OutputStream out = channel.getOutputStream();
        out.write(cred.getBytes());

    }






}
