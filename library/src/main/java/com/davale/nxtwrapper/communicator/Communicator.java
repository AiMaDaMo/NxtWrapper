package com.davale.nxtwrapper.communicator;

import java.io.DataOutputStream;
import java.io.IOException;

import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;
import timber.log.Timber;

public abstract class Communicator {

    Communicator() {
    }

    public abstract void sendData(final String data);
}
