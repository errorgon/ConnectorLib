# PeriphConnection

Android library for automatic BLE and Serial connections with a microcontroller.


```

MainActivity implements BridgeMessageListener


BridgeManager.getInstance().setBridgeListener(this);
        BridgeManager.setBleFilterName("Adafruit Bluefruit LE");
        BridgeManager.setSerialFilter("Feather M0");
        BridgeManager.initialize(pluginContext, atakContext);


    @Override
    public void onIncomingBridgeMessage(String src, String msg) {
        System.out.println("onIncomingBridgeMessage: " + msg);
        incoming.setText("From " + src + ": " + msg);
    }

    @Override
    public void onBridgeStateChange(BridgeManager.BridgeConnectionStatus status) {
```