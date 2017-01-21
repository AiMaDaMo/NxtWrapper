package com.davale.nxtwrapper.control;

import android.support.annotation.Nullable;

import com.davale.nxtwrapper.communicator.Communicator;

public class DriveControl extends AbsControl {

	public DriveControl(@Nullable ControlCallback callback, Communicator communicator) {
		super(callback);

		this.communicator = communicator;
	}

	@Override
    public void drive(int thrust, int steering) {
        this.thrust = thrust * MAX_SPEED / 10;
        this.steering = steering;

        sendMotorData();
	}
}
