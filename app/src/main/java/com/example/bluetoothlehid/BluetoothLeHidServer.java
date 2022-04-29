package com.example.bluetoothlehid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class UUIDs {
    public final static UUID UUID_SERVICE_HUMAN_INTERFACE_DEVICE = UUID.fromString("00001812-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_BOOT_KEYBOARD_INPUT_REPORT = UUID.fromString("00002A22-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_BOOT_KEYBOARD_OUTPUT_REPORT = UUID.fromString("00002A32-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_HID_INFORMATION = UUID.fromString("00002A4A-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_REPORT_MAP = UUID.fromString("00002A4B-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_HID_CONTROL_POINT = UUID.fromString("00002A4C-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_REPORT = UUID.fromString("00002A4D-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_PROTOCOL_MODE = UUID.fromString("00002A4E-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_DESCRIPTOR_REPORT_REFERENCE = UUID.fromString("00002908-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    public final static UUID UUID_SERVICE_DEVICE_INFORMATION = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_MODEL_NUMBER_STRING = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_SERIAL_NUMBER_STRING = UUID.fromString("00002A25-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_FIRMWARE_REVISION_STRING = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_HARDWARE_REVISION_STRING = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_SOFTWARE_REVISION_STRING = UUID.fromString("00002A28-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_MANUFACTURER_NAME_STRING = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_PNP_ID = UUID.fromString("00002A50-0000-1000-8000-00805F9B34FB");

    public final static UUID UUID_SERVICE_BATTERY_SERVICE = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    public final static UUID UUID_CHAR_BATTERY_LEVEL = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");
}

class Keyboard {
    public final static int AD_TIMEOUT = 180;

    public static final int DEVICE_INFO_MAX_LENGTH = 20;
    public static final String manufacturer = "FaultyChow";
    public static final String deviceName = "BLE HID";
    public static final String serialNumber = "12345678";
    public static final byte[] PnP_ID = {0x02, 0x0E, 0X00, 0X12, 0X34, 0X01, 0X67};

    public static final byte[] RESPONSE_HID_INFORMATION = {0x11, 0x01, 0x00, 0x03};

    public static final Map<String, Integer> KEY_MAP = new HashMap<String, Integer>() {
        {
            put("A", 0x04);
            put("B", 0x05);
            put("C", 0x06);
            put("D", 0x07);
            put("E", 0x08);
            put("F", 0x09);
            put("G", 0x0A);
            put("H", 0x0B);
            put("I", 0x0C);
            put("J", 0x0D);
            put("K", 0x0E);
            put("L", 0x0F);
            put("M", 0x10);
            put("N", 0x11);
            put("O", 0x12);
            put("P", 0x13);
            put("Q", 0x14);
            put("R", 0x15);
            put("S", 0x16);
            put("T", 0x17);
            put("U", 0x18);
            put("V", 0x19);
            put("W", 0x1A);
            put("X", 0x1B);
            put("Y", 0x1C);
            put("Z", 0x1D);
        }
    };

    public static final byte[] REPORT_MAP = {
            0x05, 0x01,                     //  USAGE_PAGE (Generic Desktop)
            0x09, 0x06,                     //  USAGE (Keyboard)
            (byte) 0xa1, 0x01,              //  COLLECTION (Application)
            (byte) 0x85, 0x01,              //  REPORT_ID (1)
            0x75, 0x01,                     //  Report Size (1)
            (byte) 0x95, 0x08,              //  Report Count (8)
            0x05, 0x07,                     //  Usage Page (Key Codes)
            0x19, (byte) 0xE0,              //  Usage Minimum (224)
            0x29, (byte) 0xE7,              //  Usage Maximum (231)
            0x15, 0x00,                     //  Logical Minimum (0)
            0x25, 0x01,                     //  Logical Maximum (1)
            (byte) 0x81, 0x02,              //  Input (Data, Variable, Absolute); Modifier byte
            (byte) 0x95, 0x01,              //  Report Count (1)
            0x75, 0x08,                     //  Report Size (8)
            (byte) 0x81, 0x01,              //  Input (Constant); Reserved byte
            (byte) 0x95, 0x05,              //  Report Count (5)
            0x75, 0x01,                     //  Report Size (1)
            0x05, 0x08,                     //  Usage Page (LEDs)
            0x19, 0x01,                     //  Usage Minimum (1)
            0x29, 0x05,                     //  Usage Maximum (5)
            (byte) 0x91, 0x02,              //  Output (Data, Variable, Absolute); LED report
            (byte) 0x95, 0x01,              //  Report Count (1)
            0x75, 0x03,                     //  Report Size (3)
            (byte) 0x91, 0x01,              //  Output (Constant); LED report padding
            (byte) 0x95, 0x06,              //  Report Count (6)
            0x75, 0x08,                     //  Report Size (8)
            0x15, 0x00,                     //  Logical Minimum (0)
            0x25, 0x65,                     //  Logical Maximum (101)
            0x05, 0x07,                     //  Usage Page (Key Codes)
            0x19, 0x00,                     //  Usage Minimum (0)
            0x29, 0x65,                     //  Usage Maximum (101)
            (byte) 0x81, 0x00,              //  Input (Data, Array); Key array (6 bytes)
            (byte) 0xc0                     //  END_COLLECTION
    };
}

public class BluetoothLeHidServer extends Service {
    private final static String TAG = "BleHidSvc";

    private BluetoothLeHidServerBinder mBinder = new BluetoothLeHidServerBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothGattService mGattService_HumanInterfaceDevice;
    private BluetoothGattCharacteristic InputReport;
    private BluetoothGattService mGattService_DeviceInformation;
    private BluetoothGattService mGattService_BatteryService;

    private int waitForAddService;
    private int waitForNotify;
    private BluetoothDevice mHostDevice;

    public class BluetoothLeHidServerBinder extends Binder {
        BluetoothLeHidServer getService() {
            return BluetoothLeHidServer.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void initServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Bluetooth connect permission not granted");
            return;
        }
        if (mBluetoothManager != null) {
            mBluetoothGattServer = mBluetoothManager.openGattServer(this, mBluetoothGattServerCallback);
            mBluetoothGattServer.clearServices();

            initService_DeviceInformation();
            waitForAddService = -1;
            mBluetoothGattServer.addService(mGattService_DeviceInformation);
            while (waitForAddService == -1) ;

            initService_BatteryService();
            waitForAddService = -1;
            mBluetoothGattServer.addService(mGattService_BatteryService);
            while (waitForAddService == -1) ;

            initService_HumanInterfaceDevice();
            waitForAddService = -1;
            mBluetoothGattServer.addService(mGattService_HumanInterfaceDevice);
            while (waitForAddService == -1) ;

            List<BluetoothGattService> services = mBluetoothGattServer.getServices();
            if (services.size() == 3) {
                Log.i(TAG, "All services had been added.");
            } else {
                Log.e(TAG, "Some services were missing.");
                for (BluetoothGattService service : services) {
                    Log.e(TAG, "Exist service UUID = " + service.getUuid().toString());
                }
            }
        } else
            Log.e(TAG, "BluetoothManager is null");
    }

    private void initService_DeviceInformation() {
        mGattService_DeviceInformation = new BluetoothGattService(UUIDs.UUID_SERVICE_DEVICE_INFORMATION, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        {   // Manufacturer Name
            final BluetoothGattCharacteristic manufacturerName = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_MANUFACTURER_NAME_STRING,
                    BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            manufacturerName.setValue(Keyboard.manufacturer.getBytes(StandardCharsets.UTF_8));
            while (!mGattService_DeviceInformation.addCharacteristic(manufacturerName)) ;
        }

        {   // Serial Number
            final BluetoothGattCharacteristic serialNumber = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_SERIAL_NUMBER_STRING,
                    BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            serialNumber.setValue(Keyboard.serialNumber.getBytes(StandardCharsets.UTF_8));
            while (!mGattService_DeviceInformation.addCharacteristic(serialNumber)) ;
        }

        {   // PnP ID
            final BluetoothGattCharacteristic PnP_ID = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_PNP_ID,
                    BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            PnP_ID.setValue(Keyboard.PnP_ID);
            while (!mGattService_DeviceInformation.addCharacteristic(PnP_ID)) ;
        }
    }

    private void initService_BatteryService() {
        mGattService_BatteryService = new BluetoothGattService(UUIDs.UUID_SERVICE_BATTERY_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        final BluetoothGattCharacteristic batteryLevel = new BluetoothGattCharacteristic(
                UUIDs.UUID_CHAR_BATTERY_LEVEL,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        batteryLevel.setValue(new byte[]{0x64});
        final BluetoothGattDescriptor clientCharacteristicConfiguration = new BluetoothGattDescriptor(
                UUIDs.UUID_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        clientCharacteristicConfiguration.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        batteryLevel.addDescriptor(clientCharacteristicConfiguration);

        mGattService_BatteryService.addCharacteristic(batteryLevel);
    }

    private void initService_HumanInterfaceDevice() {
        mGattService_HumanInterfaceDevice = new BluetoothGattService(UUIDs.UUID_SERVICE_HUMAN_INTERFACE_DEVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        {   // boot keyboard input report
            BluetoothGattCharacteristic bootKeyboardInputReport = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_BOOT_KEYBOARD_INPUT_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);

            BluetoothGattDescriptor clientCharacteristicConfiguration = new BluetoothGattDescriptor(
                    UUIDs.UUID_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
            clientCharacteristicConfiguration.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bootKeyboardInputReport.addDescriptor(clientCharacteristicConfiguration);

            while (!mGattService_HumanInterfaceDevice.addCharacteristic(bootKeyboardInputReport)) ;
        }

        {   // boot keyboard output report
            BluetoothGattCharacteristic bootKeyboardOutputReport = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_BOOT_KEYBOARD_OUTPUT_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            while (!mGattService_HumanInterfaceDevice.addCharacteristic(bootKeyboardOutputReport)) ;
        }

        {   // HID Information
            BluetoothGattCharacteristic information = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_HID_INFORMATION,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            information.setValue(Keyboard.RESPONSE_HID_INFORMATION);
            while (!mGattService_HumanInterfaceDevice.addCharacteristic(information)) ;
        }

        {   // Report Map
            BluetoothGattCharacteristic reportMap = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_REPORT_MAP,
                    BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
            reportMap.setValue(Keyboard.REPORT_MAP);
            while (!mGattService_HumanInterfaceDevice.addCharacteristic(reportMap)) ;
        }

        {   // HID Control Point
            BluetoothGattCharacteristic controlPoint = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_HID_CONTROL_POINT,
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            controlPoint.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            while (!mGattService_HumanInterfaceDevice.addCharacteristic(controlPoint)) ;
        }

        {   // Input Report
            InputReport = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);

            BluetoothGattDescriptor clientCharacteristicConfiguration = new BluetoothGattDescriptor(
                    UUIDs.UUID_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            clientCharacteristicConfiguration.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            InputReport.addDescriptor(clientCharacteristicConfiguration);

            BluetoothGattDescriptor inputReference = new BluetoothGattDescriptor(
                    UUIDs.UUID_DESCRIPTOR_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            inputReference.setValue(new byte[]{0x01, 0x01});
            InputReport.addDescriptor(inputReference);

            while (!mGattService_HumanInterfaceDevice.addCharacteristic(InputReport)) ;
        }

        {   // Output Report
            BluetoothGattCharacteristic outputReport = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            outputReport.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            BluetoothGattDescriptor clientCharacteristicConfiguration = new BluetoothGattDescriptor(
                    UUIDs.UUID_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            clientCharacteristicConfiguration.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            outputReport.addDescriptor(clientCharacteristicConfiguration);

            BluetoothGattDescriptor outputReference = new BluetoothGattDescriptor(
                    UUIDs.UUID_DESCRIPTOR_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            outputReport.setValue(new byte[]{0x01, 0x02});
            outputReport.addDescriptor(outputReference);

            while (!mGattService_HumanInterfaceDevice.addCharacteristic(outputReport)) ;
        }

        {   // Feature Report
            BluetoothGattCharacteristic featureReport = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_REPORT,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            featureReport.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            BluetoothGattDescriptor clientCharacteristicConfiguration = new BluetoothGattDescriptor(
                    UUIDs.UUID_DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            clientCharacteristicConfiguration.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            featureReport.addDescriptor(clientCharacteristicConfiguration);

            BluetoothGattDescriptor featureReference = new BluetoothGattDescriptor(
                    UUIDs.UUID_DESCRIPTOR_REPORT_REFERENCE,
                    BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
            featureReport.setValue(new byte[]{0x01, 0x03});
            featureReport.addDescriptor(featureReference);

            while (!mGattService_HumanInterfaceDevice.addCharacteristic(featureReport)) ;
        }

        {   // Protocol Mode
            BluetoothGattCharacteristic protocolMode = new BluetoothGattCharacteristic(
                    UUIDs.UUID_CHAR_PROTOCOL_MODE,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
            protocolMode.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            protocolMode.setValue(new byte[]{0x01});
            while (!mGattService_HumanInterfaceDevice.addCharacteristic(protocolMode)) ;
        }
    }

    public void initBleAdvertiser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "initBleAdvertiser: " + "Bluetooth Advertise permission not granted");
            return;
        }
        AdvertiseSettings mAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(Keyboard.AD_TIMEOUT)
                .build();
        AdvertiseData mAdvertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(UUIDs.UUID_SERVICE_DEVICE_INFORMATION))
                .addServiceUuid(new ParcelUuid(UUIDs.UUID_SERVICE_BATTERY_SERVICE))
                .addServiceUuid(new ParcelUuid(UUIDs.UUID_SERVICE_HUMAN_INTERFACE_DEVICE))
                .build();
        AdvertiseData mScanResultData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(UUIDs.UUID_SERVICE_DEVICE_INFORMATION))
                .addServiceUuid(new ParcelUuid(UUIDs.UUID_SERVICE_BATTERY_SERVICE))
                .addServiceUuid(new ParcelUuid(UUIDs.UUID_SERVICE_HUMAN_INTERFACE_DEVICE))
                .build();
        mBluetoothLeAdvertiser.startAdvertising(mAdvertiseSettings, mAdvertiseData, mScanResultData, mAdvertiseCallback);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "onCreate: " + "Bluetooth Connect permission not granted");
            return;
        }
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        initBleAdvertiser();
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onDestroy() {
        mBluetoothGattServer.close();
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }

    public void sendKeyEvent(String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "onCreate: " + "Bluetooth Connect permission not granted");
            return;
        }
        if (mHostDevice != null) {
            InputReport.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            InputReport.setValue(new byte[]{0, 0, 4, 0, 0, 0, 0, 0});
            if (mBluetoothGattServer.notifyCharacteristicChanged(mHostDevice, InputReport, false))
                Log.i(TAG, "Send Key down event success.");
            else
                Log.e(TAG, "Can't Send Key down event.");
            InputReport.setValue(new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
            if (mBluetoothGattServer.notifyCharacteristicChanged(mHostDevice, InputReport, false))
                Log.i(TAG, "Send Key up event success.");
            else
                Log.e(TAG, "Can't Send Key up event.");
        } else
            Log.e(TAG, "Can't Send Key event, Host device is null.");
    }

    //==============================================================================================
    //  Callback
    //==============================================================================================
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        private final static String tag = "AdvertCallback";

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i(tag, "Advertiser init Success.");
            initServer();
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.e(tag, "Advertiser init Failure.");
        }
    };

    @SuppressLint("MissingPermission")
    private BluetoothGattServerCallback mBluetoothGattServerCallback = new BluetoothGattServerCallback() {
        private final static String tag = "GattSvcCallback";
        private final static String tag_d = "GattSvcCb_D";

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (newState == BluetoothAdapter.STATE_CONNECTED) {
                mHostDevice = device;
                Log.i(tag, "Connect to Host: " + mHostDevice.getName());
//                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
//                    device.createBond();
//                } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
//                    mBluetoothGattServer.connect(device, true);
//                }
//                if (mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT) != BluetoothAdapter.STATE_CONNECTED) {
//                    mHostDevice = device;
//                    Log.i(tag, "Connect to Host: " + mHostDevice.getName());
//                }
            } else if (newState == BluetoothAdapter.STATE_DISCONNECTED) {
                Log.e(tag, "Disconnect to Host: " + mHostDevice.getName());
                mHostDevice = null;
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            super.onServiceAdded(status, service);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(tag, "Service added: " + service.getUuid());
                Log.d(tag_d, "Service: " + service.getUuid().toString() + " Have Characteristics:");
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    Log.d(tag_d, "UUID = " + characteristic.getUuid() + " Value = " + characteristic.getStringValue(0));
                }
            } else {
                Log.e(tag, "Service: " + service.getUuid().toString() + " Added Failure.");
            }
            waitForAddService = status;
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            //super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.i(tag, "Read Request from " + device.getName() + ": " + characteristic.getUuid().toString());
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.i(tag, "Write Request from " + device.getName() + ": " + characteristic.getUuid().toString());
            if (responseNeeded) {
                Log.i(tag, "sending response to write request for characteristic: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, new byte[]{});
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            Log.i(tag, "Read Request from " + device.getName() + ": " + descriptor.getUuid().toString());
            mBluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                waitForNotify = BluetoothGatt.GATT_SUCCESS;
                Log.i(tag, "Notification sent to " + device.getName());
            } else {
                waitForNotify = BluetoothGatt.GATT_FAILURE;
                Log.e(tag, "Notification failed to " + device.getName());
            }
        }
    };

    public void disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted");
            return;
        }
        if (mHostDevice != null) {
            mBluetoothGattServer.cancelConnection(mHostDevice);
            mHostDevice = null;
        }
    }

    public void connectHost(BluetoothDevice hostDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission BLUETOOTH_CONNECT not granted");
            return;
        }
        if (mHostDevice != null)
            mBluetoothGattServer.cancelConnection(mHostDevice);
        mBluetoothGattServer.connect(hostDevice, false);
        mHostDevice = hostDevice;
    }
}
