package com.davale.nxtwrapper.communicator;

import java.io.DataOutputStream;
import java.io.IOException;

import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;
import timber.log.Timber;

public class BluetoothCommunicator extends Communicator {

    private DataOutputStream dataOut;

    private NXTConnector connector;

    public boolean connectTo(String name, String address) {
        Timber.e("Connecting to " + name + " " + address);

        connector = new NXTConnector();

        boolean connected = connector.connectTo(name, address, NXTCommFactory.BLUETOOTH);

        Timber.e("Nxt result: " + connected);

        if (!connected) {
            return false;
        }

        dataOut = connector.getDataOut();

        return true;
    }

    public void sendData(final String data) {
        Timber.e("Data: " + data);

        if (dataOut == null) {
            Timber.e("No data output available");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dataOut.writeUTF(data);
                    dataOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public NXTConnector getConnector() {
        return connector;
    }
}
