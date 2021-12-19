# PeriphConnection

[![](https://jitpack.io/v/errorgon/ConnectorLib.svg)](https://jitpack.io/#errorgon/ConnectorLib)

Android library for automatic BLE and Serial connections with a microcontroller.

This example uses an [Adafruit Feather M0 Bluefruit LE](https://www.adafruit.com/product/2995) with [this](https://www.amazon.com/CableCreation-Braided-480Mbps-Compatible-MacBook/dp/B0744BKDRD) OTG
cable and a Samsung Galaxy S20

The library will prefer a serial connection with the device over BLE.

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
Set the BridgeListener for the callbacks.
```
BridgeManager.getInstance().setBridgeListener(this);
```
Add the names of the devices to filter for
```
BridgeManager.setBleFilterName("Adafruit Bluefruit LE");
BridgeManager.setSerialFilter("Feather M0");
```

```
BridgeManager.initialize(pluginContext, atakContext);
```


```

MainActivity implements BridgeMessageListener





    @Override
    public void onIncomingBridgeMessage(String src, String msg) {
        System.out.println("onIncomingBridgeMessage: " + msg);
        incoming.setText("From " + src + ": " + msg);
    }

    @Override
    public void onBridgeStateChange(BridgeManager.BridgeConnectionStatus status) {
```