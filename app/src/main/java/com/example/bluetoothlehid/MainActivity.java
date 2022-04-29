package com.example.bluetoothlehid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private BluetoothLeHidServer mBluetoothLeHidServer;
    private Button Key_A;
    private Spinner hostDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        upgradeRootPermission(getPackageCodePath());
        checkPermission();

        Intent intent = new Intent(this, BluetoothLeHidServer.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public static boolean upgradeRootPermission(String pkgCodePath) {
        Process process = null;
        DataOutputStream os = null;
        try {
            String cmd = "chmod 777 " + pkgCodePath;
            process = Runtime.getRuntime().exec("su"); //切换到root帐号
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    void initView() {
        Key_A = findViewById(R.id.Key_A);
        Key_A.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("KeyEvent", "A is clicked.");
                mBluetoothLeHidServer.sendKeyEvent("A");
            }
        });

        hostDevices = findViewById(R.id.hostDevices);
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            List<BluetoothDevice> mBondedBtDevices = new ArrayList<>(mBluetoothAdapter.getBondedDevices());
            List<String> hostDevicesList = new ArrayList<>();
            hostDevicesList.add("None");
            if (!mBondedBtDevices.isEmpty()) {
                for (BluetoothDevice device : mBondedBtDevices) {
                    hostDevicesList.add(device.getName());
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, hostDevicesList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            hostDevices.setAdapter(adapter);
            hostDevices.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Log.i("hostDevices", "onItemSelected: " + i);
                    if (i == 0) mBluetoothLeHidServer.disconnect();
                    else {
                        mBluetoothLeHidServer.connectHost(mBondedBtDevices.get(i - 1));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    Toast.makeText(MainActivity.this, "User cancel connect.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> permissionList = new ArrayList<String>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (!permissionList.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 25);
            } else {
                Log.d("ChkPermis", "All Permissions has been granted.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 25:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            Log.e("ChkPermis", permissions[i] + " is denied!");
                        }
                    }
                }
                break;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBluetoothLeHidServer = ((BluetoothLeHidServer.BluetoothLeHidServerBinder) iBinder).getService();
            initView();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e("Service", "Disconnected");
        }
    };
}