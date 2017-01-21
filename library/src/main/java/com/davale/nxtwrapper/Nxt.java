package com.davale.nxtwrapper;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.Formatter;
import android.widget.Toast;

import com.davale.nxtwrapper.communicator.BluetoothCommunicator;
import com.davale.nxtwrapper.control.AbsControl;
import com.davale.nxtwrapper.control.ControlCallback;
import com.davale.nxtwrapper.control.DriveControl;
import com.davale.nxtwrapper.network.NettyServer;
import com.davale.nxtwrapper.network.ServerWorker;
import com.davale.nxtwrapper.network.callback.ServerCallback;
import com.davale.nxtwrapper.util.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import io.netty.channel.ChannelHandlerContext;
import lejos.pc.comm.NXTInfo;
import timber.log.Timber;

import static android.content.Context.WIFI_SERVICE;

public class Nxt extends Thread implements ServerCallback {

    private static Map<String, Nxt> CONNECTIONS = new HashMap<>();

    // Builder fields

    @NonNull
    private Context mContext;

    @NonNull
    private String mName;

    @NonNull
    private String mAddress;

    @NonNull
    private ConnectionResult mListener;

    @NonNull
    private BluetoothCommunicator communicator;

    @NonNull
    private AbsControl mControl;

    @Nullable
    private ControlCallback mControlCallback;

    private boolean mUseNetwork;


    // Private fields

    private boolean mIsConnected;
    private boolean mStarted;


    // Server

    private String mLocalIp;

    private NettyServer mNettyServer;
    private ServerWorker mServerWorker;


    private Nxt(Builder builder) {
        Preconditions.checkNotNull(builder.context, "A non-null context must be provided.");
        Preconditions.checkNotNull(builder.name, "A non-null name must be provided.");
        Preconditions.checkNotNull(builder.address, "A non-null address must be provided.");
        Preconditions.checkNotNull(builder.listener, "A non-null listener must be provided.");

        mName = builder.name;
        mAddress = builder.address;
        mListener = builder.listener;

        mUseNetwork = builder.useNetwork;

        mControlCallback = builder.controlCallback;

        communicator = new BluetoothCommunicator();
        mControl = new DriveControl(mControlCallback, communicator);

        CONNECTIONS.put(builder.name, this);
    }

    public static Nxt getConnection(String key) {
        if (CONNECTIONS.containsKey(key)) {
            return CONNECTIONS.get(key);
        }

        throw new NoSuchElementException("Unable to find connection '" + key + "'");
    }


    @Override
    public void run() {
        Looper.prepare();

        setName(Nxt.class.getName());

        if (!communicator.connectTo(mName, mAddress)) {
            mIsConnected = false;

            String message = "Connection to " + mName + " @ " + mAddress + " failed";

            Timber.e(message);

            mListener.onFailure(this, message);
        } else {
            mIsConnected = true;

            NXTInfo info = communicator.getConnector().getNXTInfo();
            String message = "Connected to " + info.name + " @ " + info.deviceAddress;

            Timber.e(message);

            mListener.onSuccess(this);

            if (mUseNetwork) {
                startServer();
            }
        }

        Looper.loop();
    }

    @Override
    public void onMessageReceived(ChannelHandlerContext ctx, String message) {
        Timber.v("Inbound netty message: %s", message);
        communicator.sendData(message);
    }

    @Override
    public void onHandlerAdded(ChannelHandlerContext ctx) {
        Timber.e("Client connected: %s", ctx.channel().localAddress());
    }

    @Override
    public void onHandlerRemoved(ChannelHandlerContext ctx) {
        Timber.e("Client disconnected: %s", ctx.channel().localAddress());
    }

    @Override
    public void onException(ChannelHandlerContext ctx, Throwable throwable) {
        Timber.e(throwable, "Exception on client %s", ctx.channel().localAddress());
    }


    // =============================================================================================

    public AbsControl getControl() {
        if (!mIsConnected) {
            throw new IllegalStateException("Not connected to NXT");
        }

        return mControl;
    }

    public String getNxtName() {
        return mName;
    }

    public String getNxtAddress() {
        return mAddress;
    }


    // =============================================================================================

    public void connect() {
        if (mIsConnected) {
            Timber.e("Already connected to %s @ %s", mName, mAddress);
            return;
        }

        if (mStarted) {
            Timber.e("Nxt already started, ignoring...");
            return;
        }

        start();
        mStarted = true;
    }


    public boolean isConnected() {
        return mIsConnected;
    }


    // =============================================================================================

    public interface ConnectionResult {
        void onSuccess(Nxt nxt);

        void onFailure(Nxt nxt, String message);
    }

    public static class Builder {

        Context context;

        String name;
        String address;

        ConnectionResult listener;

        ControlCallback controlCallback;

        boolean useNetwork;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder address(String address) {
            if (!address.matches("([\\da-fA-F]{2}(?:\\:|$)){6}")) {
                throw new IllegalArgumentException("Invalid MAC address format provided: '"
                        + address + "'");
            }

            this.address = address;
            return this;
        }

        public Builder listener(ConnectionResult listener) {
            this.listener = listener;
            return this;
        }

        public Builder useNetwork(boolean useNetwork) {
            this.useNetwork = useNetwork;
            return this;
        }

        public Builder controlCallback(ControlCallback callback) {
            controlCallback = callback;
            return this;
        }

        public Nxt build() {
            return new Nxt(this);
        }
    }


    // ======================================== SERVER =============================================

    private void startServer() {
        Timber.i("Starting netty server on port 14390");

        mServerWorker = ServerWorker.getInstance();
        mServerWorker.setCallback(this);

        WifiManager manager = (WifiManager) mContext.getApplicationContext()
                .getSystemService(WIFI_SERVICE);

        mLocalIp = Formatter.formatIpAddress(manager.getConnectionInfo().getIpAddress());

        Timber.e("Started server on %s", mLocalIp);
        Toast.makeText(mContext, "Started server on " + mLocalIp, Toast.LENGTH_LONG).show();

        mNettyServer = new NettyServer();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mNettyServer.run();
                } catch (InterruptedException e) {
                    Timber.e(e, "Could not bind server");
                }
            }
        }).start();
    }

    public String getServerIp() {
        if (!mUseNetwork) {
            throw new IllegalStateException("IP-Address only available when using network mode.");
        }

        return mLocalIp;
    }
}