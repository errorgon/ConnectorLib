package com.errorgon.android.connectorlib;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Iterator;

public class SerialHelper {


    UsbDevice usbDevice;
    UsbSerialDevice serialDevice;
    UsbManager usbManager;
    UsbDeviceConnection usbConnection;
    IntentFilter serialFilter;
    private static String deviceNameFilter = "";


    private static final String ACTION_USB_PERMISSION = "com.android.usb.USB_PERMISSION";

    public static final String SERIAL_DEVICE_PERMISSION = "com.errorgon.android.connectorlib.USB_PERMISSION";
    public static final String SERIAL_DEVICE_PERMISSION_NOT_GRANTED = "com.errorgon.android.connectorlib.USB_PERMISSION_NOT_GRANTED";
    public static final String SERIAL_DEVICE_ATTACHED = "com.errorgon.android.connectorlib.USB_DEVICE_ATTACHED";
    public static final String SERIAL_DEVICE_DETACHED = "com.errorgon.android.connectorlib.USB_DEVICE_DETACHED";
    public static final String SERIAL_DEVICE_MESSAGE = "com.errorgon.android.connectorlib.USB_DEVICE_MESSAGE";

    private static SerialHelper INSTANCE = null;

    private Context context;

    private SerialHelper() {}

    public static SerialHelper getInstance() {
        if (INSTANCE == null) {
            synchronized (SerialHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SerialHelper();
                }
            }
        }
        return INSTANCE;
    }

    public void initialize(Context context) {
        this.context = context;
        usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);

        serialFilter = new IntentFilter();
        serialFilter.addAction(ACTION_USB_PERMISSION);
        serialFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        serialFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(usbReceiver, serialFilter);

        findDevice();
    }

    public static String getDeviceNameFilter() {
        return deviceNameFilter;
    }

    public static void setDeviceNameFilter(String nameFilter) {
        deviceNameFilter = nameFilter;
    }

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(final byte[] arg0) {
            final String inMsg = new String(arg0);
            if ((inMsg != null) && (inMsg.length() > 0) && (inMsg != "")) {
                broadcastUpdate(SERIAL_DEVICE_MESSAGE, inMsg);
            }
        }
    };

    BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                usbDevice = null;
                serialDevice = null;
                broadcastUpdate(SERIAL_DEVICE_DETACHED);
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                findDevice();
                broadcastUpdate(SERIAL_DEVICE_ATTACHED);
            } else if (ACTION_USB_PERMISSION.equals(action)) {
                try {
                    usbConnection = usbManager.openDevice(usbDevice);
                    serialDevice = UsbSerialDevice.createUsbSerialDevice(usbDevice, usbConnection);
                    serialDevice.open();
                    serialDevice.setBaudRate(115200);
                    serialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialDevice.setParity(UsbSerialInterface.PARITY_ODD);
                    serialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialDevice.read(mCallback);
                    broadcastUpdate(SERIAL_DEVICE_PERMISSION);
                } catch (Exception e) {
                    System.out.println("Serial Permission Not granted");
                    broadcastUpdate(SERIAL_DEVICE_PERMISSION_NOT_GRANTED);
                }

            }
        }
    };

    void findDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while(deviceIterator.hasNext()){
            usbDevice = deviceIterator.next();
            if (usbDevice.getProductName().equals(deviceNameFilter)) {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(usbDevice, pendingIntent);
                break;
            }
        }
    }

    public UsbSerialDevice getSerialDevice() { return serialDevice; }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        context.sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String msg) {
        final Intent intent = new Intent(action);
        intent.putExtra(action, msg);
        context.sendBroadcast(intent);
    }

    public void close() {
        try {
            serialDevice.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            serialDevice = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
