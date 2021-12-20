package com.errorgon.android.periphconnection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.errorgon.android.connectorlib.BridgeManager;
import com.errorgon.android.connectorlib.BridgeMessageListener;

public class MainActivity extends AppCompatActivity implements BridgeMessageListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BridgeManager.getInstance().setBridgeListener(this);
        BridgeManager.setBleFilterName("Adafruit Bluefruit LE");
        BridgeManager.setSerialFilter("Feather M0");
        BridgeManager.initialize(getApplicationContext(), getApplicationContext());

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BridgeManager.sendMessage(">SEND<");
            }
        });

    }

    @Override
    public void onIncomingBridgeMessage(String src, String msg) {
        System.out.println(src + " " + msg  );
    }

    @Override
    public void onBridgeStateChange(BridgeManager.BridgeConnectionStatus status) {
        System.out.println(status);
    }
}