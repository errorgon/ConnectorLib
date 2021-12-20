# PeriphConnection

[![](https://jitpack.io/v/errorgon/ConnectorLib.svg)](https://jitpack.io/#errorgon/ConnectorLib)

Android library for automatic BLE and Serial connections with a microcontroller.

This example uses an [Adafruit Feather M0 Bluefruit LE](https://www.adafruit.com/product/2995) with [this](https://www.amazon.com/CableCreation-Braided-480Mbps-Compatible-MacBook/dp/B0744BKDRD) OTG
cable and a Samsung Galaxy S20

The library will prefer a serial connection with the device over BLE. The app will stop scanning for BLE once you connected the serial device. It will resume scamming once disconnected.

## Installation

Add the jitpack repository to your top-level build.gradle (Project)
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add the library dependency to build.gradle (Module)
```
dependencies {
    implementation 'com.github.errorgon:connectorlib:0.0.6'
}
```

## Usage
Set the BridgeListener for the callbacks:
```
BridgeManager.setBridgeListener(this);
```
Add the names of the devices to filter. The names of the devices will probably be different
```
BridgeManager.setBleFilterName("Adafruit Bluefruit LE");
BridgeManager.setSerialFilter("Feather M0");
```

Initialize the manager class for the connections:
```
BridgeManager.initialize(pluginContext, atakContext);
```

Lastly, implement BridgeMessageListener and these methods:
```
@Override
public void onIncomingBridgeMessage(String src, String msg) {
}

@Override
public void onBridgeStateChange(BridgeManager.BridgeConnectionStatus status) {
}
```
onIncomingBridgeMessage is called anytime there is a message from the device either through BLE or the serial connection.
src will be BLE or SERIAL and msg will contain the message.

onBridgeStateChange is called anytime there is a change to the connection status of the device.
In this example, SERIAL_ATTACHED state is still not considered a full serial connection. Your Android device should prompt you to accept the
connection when you plug in the device. After, SERIAL_CONNECTED will be passed into onBridgeStateChange and you can begin communicating.
```
@Override
public void onBridgeStateChange(BridgeManager.BridgeConnectionStatus status) {
    switch (status) {
        case BLE_ATTACHED:
            System.out.println("BLE");
            break;
        case SERIAL_CONNECTED:
            System.out.println("SERIAL");
            break;
        case BLE_DETACHED:
        case SERIAL_ATTACHED:
        case SERIAL_DETACHED:
            System.out.println("NONE");
            break;
        default:
            break;
    }
}
```

The BridgeManager class has a static method for sending a string message. The method returns true if you're connected to the BLE or Serial device.
```
BridgeManager.sendMessage(msg);
```
