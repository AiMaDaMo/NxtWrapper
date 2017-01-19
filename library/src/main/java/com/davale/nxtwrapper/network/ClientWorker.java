package com.davale.nxtwrapper.network;

import android.support.annotation.NonNull;

import com.davale.nxtwrapper.network.callback.ClientCallback;
import com.davale.nxtwrapper.network.callback.ServerCallback;
import com.davale.nxtwrapper.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.ChannelHandlerContext;

public final class ClientWorker {

    private ClientCallback mCallback;

    private static ClientWorker INSTANCE = new ClientWorker();


    private ClientWorker() {
    }

    public static ClientWorker getInstance() {
        return INSTANCE;
    }


    public void setCallback(@NonNull ClientCallback callback) {
        Preconditions.checkNotNull(callback, "A non-null callback must be provided.");
        mCallback = callback;
    }

    private void ensureCallback() {
        Preconditions.checkNotNull(mCallback, "No callback found");
    }


    public void parseData(ChannelHandlerContext context, String message) {
        ensureCallback();
        mCallback.onMessageReceived(context, message);
    }

    public void connected(ChannelHandlerContext context) {
        ensureCallback();
        mCallback.onConnected(context);
    }

    public void disconnected(ChannelHandlerContext context) {
        ensureCallback();
        mCallback.onDisconnected(context);
    }

    public void exception(ChannelHandlerContext context, Throwable throwable) {
        ensureCallback();
        mCallback.onException(context, throwable);
    }
}

