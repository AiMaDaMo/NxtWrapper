package com.davale.nxtwrapper;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.format.Formatter;
import android.widget.Toast;

import com.davale.nxtwrapper.communicator.BluetoothCommunicator;
import com.davale.nxtwrapper.communicator.NettyCommunicator;
import com.davale.nxtwrapper.control.AbsControl;
import com.davale.nxtwrapper.control.DriveControl;
import com.davale.nxtwrapper.network.NettyClient;
import com.davale.nxtwrapper.network.NettyServer;
import com.davale.nxtwrapper.network.ServerWorker;
import com.davale.nxtwrapper.network.callback.ClientCallback;
import com.davale.nxtwrapper.network.callback.ServerCallback;
import com.davale.nxtwrapper.util.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.netty.channel.ChannelHandlerContext;
import lejos.pc.comm.NXTInfo;
import timber.log.Timber;

import static android.content.Context.WIFI_SERVICE;

public class NxtClient extends Thread implements ClientCallback {

    private static Map<String, NxtClient> CONNECTIONS = new HashMap<>();


    // Builder fields

    @NonNull
    private Context mContext;

    @NonNull
    private String mAddress;

    @NonNull
    private ConnectionResult mListener;

    @NonNull
    private NettyCommunicator communicator;

    @NonNull
    private AbsControl mControl;


    private boolean mIsConnecting;

    private NettyClient mClient;


    private NxtClient(Builder builder) {
        Preconditions.checkNotNull(builder.context, "A non-null context must be provided.");
        Preconditions.checkNotNull(builder.address, "A non-null address must be provided.");
        Preconditions.checkNotNull(builder.listener, "A non-null listener must be provided.");

        mAddress = builder.address;
        mListener = builder.listener;

        mClient = new NettyClient(mAddress);

        communicator = new NettyCommunicator(mClient);
        mControl = new DriveControl(communicator);

        CONNECTIONS.put(builder.address, this);
    }

    public static NxtClient getConnection(String key) {
        if (CONNECTIONS.containsKey(key)) {
            return CONNECTIONS.get(key);
        }

        throw new NoSuchElementException("Unable to find connection '" + key + "'");
    }


    @Override
    public void run() {
        try {
            mClient.run();
        } catch (Exception e) {
            Timber.e(e, "Could not connect to server.");
            mIsConnecting = false;

            mListener.onFailure(this, e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(ChannelHandlerContext ctx, String message) {

    }

    @Override
    public void onConnected(ChannelHandlerContext ctx) {
        mListener.onSuccess(this);
    }

    @Override
    public void onDisconnected(ChannelHandlerContext ctx) {

    }

    @Override
    public void onException(ChannelHandlerContext ctx, Throwable throwable) {

    }


    // =============================================================================================

    public void connect() {
        if (mIsConnecting) {
            Timber.e("Connection attempt already running.");
            return;
        }

        mIsConnecting = true;
        start();
    }


    // =============================================================================================

    public AbsControl getControl() {
        return mControl;
    }

    public String getIpAddress() {
        return mAddress;
    }


    // =============================================================================================

    public interface ConnectionResult {
        void onSuccess(NxtClient nxt);

        void onFailure(NxtClient nxt, String message);
    }

    public static class Builder {

        Context context;

        String address;

        ConnectionResult listener;

        AbsControl control;

        boolean useNetwork;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder address(String address) {
            this.address = address;
            return this;
        }

        public Builder listener(ConnectionResult listener) {
            this.listener = listener;
            return this;
        }

        public NxtClient build() {
            return new NxtClient(this);
        }
    }
}