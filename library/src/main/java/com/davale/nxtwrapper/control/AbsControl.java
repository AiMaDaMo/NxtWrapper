package com.davale.nxtwrapper.control;

import android.support.annotation.Nullable;

import com.davale.nxtwrapper.communicator.Communicator;

import timber.log.Timber;

public abstract class AbsControl {

	static final int MAX_SPEED = 750;

	int thrust = 0;
	int steering = 0;

    private int mode = 1;

	Communicator communicator;

    @Nullable
    ControlCallback mCallback;

    public AbsControl(@Nullable ControlCallback callback) {
        mCallback = callback;
    }

    void sendMotorData() {
        if (mCallback != null) {
            mCallback.drive(thrust * mode, steering * mode);
        }

        String toSend = toString();
        communicator.sendData(toSend);
	}

    public void soundToggle() {
        communicator.sendData("s");
    }

    public void beep() {
        communicator.sendData("b");
    }

    public void twoBeeps() {
        communicator.sendData("tb");
    }

    public void cameraUp() {
        communicator.sendData(mode == 1 ? "cu" : "cd");
    }

    public void cameraDown() {
        communicator.sendData(mode == 1 ? "cd" : "cu");
    }

    public void halt() {
        communicator.sendData("h");
    }

    public void mode() {
        mode *= -1;
    }

	@Override
	public String toString() {
        return thrust * mode + ";" + steering * mode;
	}

    public abstract void drive(int thrust, int steering);
}
