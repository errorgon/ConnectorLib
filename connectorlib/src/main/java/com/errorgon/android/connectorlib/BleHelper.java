package com.errorgon.android.connectorlib;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.List;
import java.util.UUID;

public class BleHelper {


    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private static BluetoothDevice bluetoothDevice;
    private String bluetoothDeviceAddress;
    private String connectionState;
    private static final long SCAN_PERIOD = 5000;
    private Handler handler;

    public final static String ACTION_GATT_CONNECTED = "com.atakmap.android.plugintemplate.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.atakmap.android.plugintemplate.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.atakmap.android.plugintemplate.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.atakmap.android.plugintemplate.ACTION_DATA_AVAILABLE";
    public final static String ACTION_DEVICE_NOT_FOUND = "com.atakmap.android.plugintemplate.ACTION_DEVICE_NOT_FOUND";
    public final static String EXTRA_DATA = "com.atakmap.android.plugintemplate.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.atakmap.android.plugintemplate.DEVICE_DOES_NOT_SUPPORT_UART";

    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private static String nameFilter = "";
    private static String addressFilter = "";

    private Context atakContext;

    private static BleHelper INSTANCE = null;

    private BleHelper() {    }

    public static BleHelper getInstance() {
        if (INSTANCE == null) {
            synchronized (BleHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BleHelper();
                }
            }
        }
        return INSTANCE;
    }

    public boolean initialize(Context atakContext, Context pluginContext) {
        this.atakContext = atakContext;

        handler = new Handler();

        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) atakContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            return false;
        }
        scanLeDevice(true);
        return true;
    }

    public void startBle() {
        scanLeDevice(true);
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = intentAction;
                broadcastUpdate(intentAction);
                System.out.println("Connected to GATT server.");
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = intentAction;
                System.out.println("Disconnected from GATT server.");
                // when unplugged we end up here. We should start scanning here unless we're connected to Serial
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                enableTXNotification();
            } else {
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress) && bluetoothGatt != null) {
            if (bluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            System.out.println("Device not found. Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        bluetoothDevice = device;
        bluetoothGatt = device.connectGatt(atakContext, false, gattCallback);
        System.out.println("Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        return true;
    }


    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.disconnect();
        scanLeDevice(false);
//        bluetoothGatt.close();
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(atakContext).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
            intent.putExtra(EXTRA_DATA, new String(characteristic.getValue()));
        }
        LocalBroadcastManager.getInstance(atakContext).sendBroadcast(intent);
    }

    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        System.out.println("bluetoothGatt closed");
        bluetoothDeviceAddress = null;
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            System.out.println("BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public void enableTXNotification() {
        BluetoothGattService RxService = bluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        bluetoothGatt.setCharacteristicNotification(TxChar,true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
    }

    public void writeRXCharacteristic(byte[] value) {
        BluetoothGattService RxService = bluetoothGatt.getService(RX_SERVICE_UUID);
        if (RxService == null) {
            System.out.println("Rx service not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            System.out.println("Rx charateristic not found!");
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        RxChar.setValue(value);
        boolean status = bluetoothGatt.writeCharacteristic(RxChar);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    System.out.println("Stop Scan");
                    bluetoothLeScanner.stopScan(scanCallback);
                    if (connectionState == null || connectionState.equals(ACTION_GATT_DISCONNECTED) || bluetoothGatt == null) {
                        broadcastUpdate(ACTION_DEVICE_NOT_FOUND);
                    }
                }
            }, SCAN_PERIOD);
            System.out.println("Start Scan");
            bluetoothLeScanner.startScan(scanCallback);
        } else {
            bluetoothLeScanner.stopScan(scanCallback);
            close();
        }

    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            try {
                if (result.getDevice().getName().equals(nameFilter)) {
                    scanLeDevice(false);
                    bluetoothLeScanner.stopScan(scanCallback);
                    connect(result.getDevice().getAddress());
                }
            } catch (Exception e) {

            }
        }
    };

    public static String getNameFilter() {
        return nameFilter;
    }

    public static void setNameFilter(String nameFilter) {
        BleHelper.nameFilter = nameFilter;
    }

    public static String getAddressFilter() {
        return addressFilter;
    }

    public static void setAddressFilter(String addressFilter) {
        BleHelper.addressFilter = addressFilter;
    }

    public static BluetoothDevice getBleDevice() {
        return bluetoothDevice;
    }

}
