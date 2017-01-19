package com.davale.nxtwrapper.network;

import android.support.annotation.NonNull;

import com.davale.nxtwrapper.network.callback.ServerCallback;
import com.davale.nxtwrapper.util.Preconditions;

import io.netty.channel.ChannelHandlerContext;

public final class ServerWorker {

    private ServerCallback mCallback;

    private static ServerWorker INSTANCE = new ServerWorker();


    private ServerWorker() {
    }

    public static ServerWorker getInstance() {
        return INSTANCE;
    }


    public void setCallback(@NonNull ServerCallback callback) {
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

    public void handlerAdded(ChannelHandlerContext context) {
        ensureCallback();
        mCallback.onHandlerAdded(context);
    }

    public void handlerRemoved(ChannelHandlerContext context) {
        ensureCallback();
        mCallback.onHandlerRemoved(context);
    }

    public void exception(ChannelHandlerContext context, Throwable throwable) {
        ensureCallback();
        mCallback.onException(context, throwable);
    }
}
