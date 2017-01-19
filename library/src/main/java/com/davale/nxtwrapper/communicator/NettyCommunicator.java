package com.davale.nxtwrapper.communicator;

import com.davale.nxtwrapper.network.NettyClient;

import java.io.DataOutputStream;
import java.io.IOException;

import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;
import timber.log.Timber;

public class NettyCommunicator extends Communicator {

    private NettyClient mClient;

    public NettyCommunicator(NettyClient client) {
        mClient = client;
    }

    @Override
    public void sendData(final String data) {
        Timber.e("Data: " + data);

        if (mClient == null) {
            Timber.e("No netty client available");
            return;
        }

        mClient.send(data);
    }
}
