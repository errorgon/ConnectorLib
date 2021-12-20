package com.errorgon.android.periphconnection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.errorgon.android.connectorlib.BridgeManager;
import com.errorgon.android.connectorlib.BridgeMessageListener;

public class MainActivity extends AppCompatActivity implements BridgeMessageListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BridgeManager.setBridgeListener(this);
        BridgeManager.setBleFilterName("Adafruit Bluefruit LE");
        BridgeManager.setSerialFilter("Feather M0");
        BridgeManager.initialize(this, this);

    }

    @Override
    public void onIncomingBridgeMessage(String src, String msg) {

    }

    @Override
    public void onBridgeStateChange(BridgeManager.BridgeConnectionStatus status) {

    }
}