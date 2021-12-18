BridgeManager.getInstance().setBridgeListener(this);
BridgeManager.setBleFilterName("Adafruit Bluefruit LE");
BridgeManager.setSerialFilter("Feather M0");
BridgeManager.initialize(pluginContext, atakContext);