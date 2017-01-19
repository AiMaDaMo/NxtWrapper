package lejos.pc.comm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

@SuppressWarnings("ALL")
public class NXTCommAndroid implements NXTComm {

    public static String INPUT = "255";

    public static final byte[] BYTES = new byte[0];

    private class ConnectThread extends Thread {
        private BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;
        private final SynchronousQueue<Boolean> connectQueue;

        public ConnectThread(BluetoothDevice bluetoothDevice, SynchronousQueue<Boolean> connectQueue) {
            this.bluetoothDevice = bluetoothDevice;
            this.connectQueue = connectQueue;

            BluetoothSocket bluetoothSocket = null;

            try {
                bluetoothSocket = bluetoothDevice
                        .createRfcommSocketToServiceRecord(SERIAL_PORT_SERVICE_CLASS_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.bluetoothSocket = bluetoothSocket;
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                bluetoothSocket = null;
            }
        }

        private void relayConnectionSuccess() {
            try {
                connectQueue.put(true);
            } catch (InterruptedException ignored) {
            }

            yield();
        }

        private void relyConnectionFailure(IOException e) {
            try {
                connectQueue.put(false);

                e.printStackTrace();
            } catch (InterruptedException ignored) {
            }

            if (bluetoothSocket != null) {
                cancel();
            }
        }

        @Override
        public void run() {
            setName("ConnectThread");

            Log.e(TAG, "Starting ConnectThread");

            // Make a connection to the BluetoothSocket
            // This is a blocking call and will only return on a
            // successful connection or an exception
            try {
                bluetoothSocket.connect();
            } catch (IOException e) {
                relyConnectionFailure(e);
                return;
            }

            relayConnectionSuccess();
            startIOThreads(bluetoothSocket, bluetoothDevice);
        }
    }

    private class ReadThread extends Thread {
        public InputStream inputStream;
        boolean running = true;
        LinkedBlockingQueue<byte[]> mReadQueue;

        public ReadThread(BluetoothSocket socket, LinkedBlockingQueue<byte[]> mReadQueue) {
            try {
                this.mReadQueue = mReadQueue;
                inputStream = socket.getInputStream();

                Log.e(TAG, "socket is connected to: " + socket.getRemoteDevice().getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            running = false;
            mReadQueue.clear();
        }

        private byte[] read() {
            int lsb = -1;

            try {
                lsb = inputStream.read();
            } catch (Exception e) {
                running = false;
                Log.e(TAG, "read err lsb", e);
            }

            if (lsb < 0) {
                return null;
            }
            int msb = 0;

            try {
                msb = inputStream.read();
            } catch (IOException e1) {
                Log.e(TAG, "ReadThread read error msb", e1);
            }

            if (msb < 0) {
                return null;
            }

            int len = lsb | msb << 8;
            byte[] bb = new byte[len];
            for (int i = 0; i < len; i++) {
                try {
                    bb[i] = (byte) inputStream.read();
                } catch (IOException e) {
                    Log.e(TAG, "ReadThread read error data", e);
                }
            }

            return bb;
        }

        @Override
        public void run() {
            byte[] bytes;

            while (running) {
                Thread.yield();

                bytes = read();

                if (bytes != null && bytes.length > 2) {
                    INPUT = new String(Arrays.copyOfRange(bytes, 2, bytes.length));

                    try {
                        mReadQueue.put(bytes);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class WriteThread extends Thread {
        public OutputStream outputStream;
        private boolean running = true;
        LinkedBlockingQueue<byte[]> mWriteQueueT;

        public WriteThread(BluetoothSocket socket, LinkedBlockingQueue<byte[]> mWriteQueue) {
            try {
                outputStream = socket.getOutputStream();
                this.mWriteQueueT = mWriteQueue;
            } catch (IOException e) {
                Log.e(TAG, "WriteThread OutputStream error ", e);
            }
        }

        public void cancel() {
            running = false;
            mReadQueue.clear();
        }

        @Override
        public void run() {
            while (running) {
                try {
                    byte[] data;
                    data = mWriteQueueT.take();
                    write(data);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        void write(byte... data) {
            byte[] lsbMsb = new byte[2];
            lsbMsb[0] = (byte) data.length;
            lsbMsb[1] = (byte) (data.length >> 8 & 0xff);

            try {
                outputStream.write(concat(lsbMsb, data));
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private BluetoothAdapter mBtAdapter;

    private NXTInfo nxtInfo;

    private final String TAG = "NXTCommAndroid";
    protected String mConnectedDeviceName;

    private ConnectThread mConnectThread;
    private ReadThread mReadThread;
    private WriteThread mWriteThread;

    private static final UUID SERIAL_PORT_SERVICE_CLASS_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private LinkedBlockingQueue<byte[]> mReadQueue;
    private LinkedBlockingQueue<byte[]> mWriteQueue;

    @Override
    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
        Log.e(TAG, "closing threads and socket");
        cancelIOThreads();
        cancelConnectThread();
        mConnectedDeviceName = "";
    }

    @Override
    public InputStream getInputStream() {
        return new NXTCommInputStream(this);
    }

    @Override
    public OutputStream getOutputStream() {
        return new NXTCommOutputStream(this);
    }

    @Override
    public boolean open(NXTInfo nxt) throws NXTCommException {
        return open(nxt, PACKET);
    }

    @Override
    public boolean open(NXTInfo nxt, int mode) throws NXTCommException {
        if (mode == RAW) {
            throw new NXTCommException("RAW mode not implemented");
        }

        BluetoothDevice nxtDevice;
        SynchronousQueue<Boolean> connectQueue = new SynchronousQueue<>();

        if (mBtAdapter == null) {
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        }

        nxtDevice = mBtAdapter.getRemoteDevice(nxt.deviceAddress);

        try {

            mConnectThread = new ConnectThread(nxtDevice, connectQueue);
            mConnectThread.start();

            //blocking call to wait for connection status
            Boolean socketEstablished = connectQueue.take();
            Thread.yield();

            boolean socketConnected = socketEstablished;
            if (socketConnected) {
                nxt.connectionState = mode == LCP ? NXTConnectionState.LCP_CONNECTED : NXTConnectionState.PACKET_STREAM_CONNECTED;
            } else {
                nxt.connectionState = NXTConnectionState.DISCONNECTED;
            }
            nxtInfo = nxt;

            return socketConnected;
        } catch (Exception e) {
            Log.e(TAG, "ERROR in open: ", e);
            nxt.connectionState = NXTConnectionState.DISCONNECTED;

            throw new NXTCommException("ERROR in open: " + nxt.name + " failed: " + e.getMessage());
        }
    }

    /**
     * Will block until data is available
     *
     * @return read data
     */
    @Override
    public byte[] read() throws IOException {
        byte[] bytes = null;

        while (bytes == null) {
            bytes = mReadQueue.poll();
            Thread.yield();
        }

        return bytes;
    }

    @Override
    public NXTInfo[] search(String name, int protocol) throws NXTCommException {
        List<NXTInfo> nxtInfos = new ArrayList<>();
        Collection<BluetoothDevice> devices = new ArrayList<>();

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        for (BluetoothDevice device : pairedDevices) {
            //Log.e(TAG, "paired devices :" + device.getName() + "\n" + device.getAddress());
            if (device.getBluetoothClass().getMajorDeviceClass() == 2048) {
                devices.add(device);
            }
        }

        for (BluetoothDevice d : devices) {
            Log.e(TAG, "creating nxtInfo");
            nxtInfo = new NXTInfo();

            nxtInfo.name = d.getName();
            if (nxtInfo.name == null || nxtInfo.name.isEmpty()) {
                nxtInfo.name = "Unknown";
            }
            nxtInfo.deviceAddress = d.getAddress();
            nxtInfo.protocol = NXTCommFactory.BLUETOOTH;

            if (name == null || name.equals(nxtInfo.name)) {
                nxtInfos.add(nxtInfo);
            }
        }

        NXTInfo[] infos = new NXTInfo[nxtInfos.size()];
        for (int i = 0; i < infos.length; i++) {
            infos[i] = nxtInfos.get(i);

        }
        return infos;
    }

    /**
     * Sends a request to the Nxt brick.
     *
     * @param message Data to send.
     */
    @Override
    public synchronized byte[] sendRequest(byte[] message, int replyLen) throws IOException {

        write(message);

        if (replyLen == 0)
            return BYTES;

        byte[] b = read();

        if (b.length != replyLen) {
            throw new IOException("Unexpected reply length");
        }

        return b;
    }

    /**
     * Put data into write queue to be written by write thread
     * <p>
     * Will block if no space in queue. Queue size is 2147483647, so this is not
     * likely.
     *
     * @param data Data to send.
     */
    @Override
    public void write(byte[] data) throws IOException {
        try {
            if (data != null) {
                mWriteQueue.put(data);
            }
            Thread.yield();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private byte[] concat(byte[] data1, byte... data2) {
        int l1 = data1.length;
        int l2 = data2.length;

        byte[] data = new byte[l1 + l2];
        System.arraycopy(data1, 0, data, 0, l1);
        System.arraycopy(data2, 0, data, l1, l2);
        return data;
    }

    public synchronized void startIOThreads(BluetoothSocket socket, BluetoothDevice device) {
        cancelIOThreads();

        mReadQueue = new LinkedBlockingQueue<>();
        mWriteQueue = new LinkedBlockingQueue<>();

        mWriteThread = new WriteThread(socket, mWriteQueue);
        mReadThread = new ReadThread(socket, mReadQueue);

        mWriteThread.start();
        mReadThread.start();
    }

    private void cancelConnectThread() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private void cancelIOThreads() {
        if (mReadThread != null) {
            mReadThread.cancel();
            mReadThread = null;
        }

        if (mWriteThread != null) {
            mWriteThread.cancel();
            mWriteThread = null;
        }
    }
}
