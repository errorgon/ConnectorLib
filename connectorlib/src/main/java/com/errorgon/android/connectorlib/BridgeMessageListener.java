package com.errorgon.android.connectorlib;

public interface BridgeMessageListener {
    void onIncomingBridgeMessage(String src, String msg);

    void onBridgeStateChange(BridgeManager.BridgeConnectionStatus status);

}