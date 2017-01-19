package com.davale.nxtwrapper.sample;

import android.app.Activity;
import android.os.Bundle;

import com.davale.nxtwrapper.Nxt;
import com.davale.nxtwrapper.NxtClient;

import timber.log.Timber;

public class MainActivity extends Activity implements Nxt.ConnectionResult, NxtClient.ConnectionResult {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Timber.plant(new Timber.DebugTree());

        Nxt nxt = new Nxt.Builder(this)
                .address("12:34:56:78:90:00")
                .name("NXT1")
                .listener(this)
                .useNetwork(true)
                .build();

        nxt.connect();

        NxtClient client = new NxtClient.Builder(this)
                .address("127.0.0.1")
                .listener(this)
                .build();

        client.connect();
    }

    @Override
    public void onSuccess(Nxt nxt) {
        Timber.e("Success!");

        nxt.getControl().beep();
    }

    @Override
    public void onFailure(Nxt nxt, String message) {
        Timber.e("Failure: %s", message);
    }

    @Override
    public void onSuccess() {
        Timber.e("Successfully connected to server!");
    }

    @Override
    public void onFailure(String message) {
        Timber.e("Could not connect to server: %s", message);
    }
}
