package com.errorgon.android.connectorlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BridgeManager {

    private static String serialFilter = "";
    private static String bleFilterName = "";
    private static String bleAddressFilter = "";

    private static Context context;

    private static BridgeMessageListener onBridgeMessageListener;

    private static BridgeManager INSTANCE = null;

    public enum BridgeConnectionStatus{SERIAL_DETACHED, SERIAL_ATTACHED, SERIAL_CONNECTED, BLE_DETACHED, BLE_ATTACHED, UNKNOWN}

    private static BridgeConnectionStatus bridgeConnectionStatus = BridgeConnectionStatus.UNKNOWN;

    private BridgeManager() {    }

    public static BridgeManager getInstance() {
        if (INSTANCE == null) {
            synchronized (BridgeManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BridgeManager();
                }
            }
        }
        return INSTANCE;
    }

    public static void setBridgeListener(BridgeMessageListener mBridgeMessageListener) {
        onBridgeMessageListener = mBridgeMessageListener;
    }

    public static void initialize(Context context) {
        BridgeManager.context = context;
        SerialHelper.setDeviceNameFilter(serialFilter);
        BleHelper.setNameFilter(bleFilterName);
        initSerial();
        initBle();
    }



    public static boolean sendMessage(String msg) {
        try {
            synchronized (BridgeManager.class) {
                SerialHelper.getInstance().getSerialDevice().write(msg.getBytes());
                System.out.println("Sent via Serial");
            }
        } catch (Exception e) {
            try {
                synchronized (BridgeManager.class) {
                    // TODO: 12/20/2021 Parse into 20b chunks 
                    BleHelper.getInstance().writeRXCharacteristic(msg.getBytes());
                    System.out.println("Sent via BLE");
                }
            } catch (Exception e1) {
                System.out.println("e1");
                e1.printStackTrace();
                return false;
            }
        }
        return true;
    }

    static BroadcastReceiver serialReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SerialHelper.SERIAL_DEVICE_DETACHED.equals(action) || SerialHelper.SERIAL_DEVICE_PERMISSION_NOT_GRANTED.equals(action)) {
                SerialHelper.getInstance().close();
                BleHelper.getInstance().startBle();
                bridgeConnectionStatus = BridgeConnectionStatus.SERIAL_DETACHED;
                onBridgeMessageListener.onBridgeStateChange(bridgeConnectionStatus);
            } else if (SerialHelper.SERIAL_DEVICE_ATTACHED.equals(action)) {
                bridgeConnectionStatus = BridgeConnectionStatus.SERIAL_ATTACHED;
                onBridgeMessageListener.onBridgeStateChange(bridgeConnectionStatus);
                BleHelper.getInstance().disconnect();
            } else if (SerialHelper.SERIAL_DEVICE_PERMISSION.equals(action)) {
                BleHelper.getInstance().disconnect();
                bridgeConnectionStatus = BridgeConnectionStatus.SERIAL_CONNECTED;
                onBridgeMessageListener.onBridgeStateChange(bridgeConnectionStatus);
            } else if (SerialHelper.SERIAL_DEVICE_MESSAGE.equals(action)) {
                BleHelper.getInstance().disconnect();
                String message = intent.getStringExtra(SerialHelper.SERIAL_DEVICE_MESSAGE);
                if (message != null && !message.isEmpty() && !message.equals("")) {
                    onBridgeMessageListener.onIncomingBridgeMessage("Serial", message);
                }
            }
        }
    };

    static BroadcastReceiver bleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BleHelper.ACTION_GATT_DISCONNECTED.equals(action)) {
                bridgeConnectionStatus = BridgeConnectionStatus.BLE_DETACHED;
                onBridgeMessageListener.onBridgeStateChange(bridgeConnectionStatus);
                if (SerialHelper.getInstance().getSerialDevice() == null) {
                    BleHelper.getInstance().startBle();
                }
            } else if (BleHelper.ACTION_GATT_CONNECTED.equals(action)) {
                if (SerialHelper.getInstance().getSerialDevice() != null) {
                    BleHelper.getInstance().disconnect();
                } else {
                    bridgeConnectionStatus = BridgeConnectionStatus.BLE_ATTACHED;
                    onBridgeMessageListener.onBridgeStateChange(bridgeConnectionStatus);
                }
            } else if (BleHelper.ACTION_DATA_AVAILABLE.equals(action)) {
                if (SerialHelper.getInstance().getSerialDevice() != null) {
                    BleHelper.getInstance().disconnect();
                } else {
                    onBridgeMessageListener.onIncomingBridgeMessage("BLE", intent.getStringExtra(BleHelper.EXTRA_DATA));
                }

            } else if (BleHelper.ACTION_DEVICE_NOT_FOUND.equals(action)) {
                if (SerialHelper.getInstance().getSerialDevice() == null) {
                    BleHelper.getInstance().startBle();
                }
            }
        }
    };


    private static void initSerial() {
        IntentFilter serialFilter = new IntentFilter();
        serialFilter.addAction(SerialHelper.SERIAL_DEVICE_ATTACHED);
        serialFilter.addAction(SerialHelper.SERIAL_DEVICE_DETACHED);
        serialFilter.addAction(SerialHelper.SERIAL_DEVICE_PERMISSION);
        serialFilter.addAction(SerialHelper.SERIAL_DEVICE_PERMISSION_NOT_GRANTED);
        serialFilter.addAction(SerialHelper.SERIAL_DEVICE_MESSAGE);

        context.registerReceiver(serialReceiver, serialFilter);

        SerialHelper.getInstance().initialize(context);
    }


    private static void initBle() {
        IntentFilter bleFilter = new IntentFilter();
        bleFilter.addAction(BleHelper.ACTION_GATT_CONNECTED);
        bleFilter.addAction(BleHelper.ACTION_GATT_DISCONNECTED);
        bleFilter.addAction(BleHelper.ACTION_GATT_SERVICES_DISCOVERED);
        bleFilter.addAction(BleHelper.ACTION_DATA_AVAILABLE);
        bleFilter.addAction(BleHelper.ACTION_DEVICE_NOT_FOUND);
        bleFilter.addAction(BleHelper.DEVICE_DOES_NOT_SUPPORT_UART);

        LocalBroadcastManager.getInstance(context).registerReceiver(bleReceiver, bleFilter);

        BleHelper.getInstance().initialize(context);
    }


    public static String getSerialFilter() {
        return serialFilter;
    }

    public static void setSerialFilter(String serialFilter) {
        BridgeManager.serialFilter = serialFilter;
    }

    public static String getBleFilterName() {
        return bleFilterName;
    }

    public static void setBleFilterName(String bleFilterName) {
        BridgeManager.bleFilterName = bleFilterName;
    }

    public static String getBleAddressFilter() {
        return bleAddressFilter;
    }

    public static void setBleAddressFilter(String bleAddressFilter) {
        BridgeManager.bleAddressFilter = bleAddressFilter;
    }

    public static String getBleDeviceAddress() {
        String address = "";
        try {
            address = BleHelper.getBleDevice().getAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    public static String getBleDeviceName() {
        String name = "";
        try {
            name = BleHelper.getBleDevice().getName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return name;
    }

}
