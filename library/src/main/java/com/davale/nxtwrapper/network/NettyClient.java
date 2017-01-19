package com.davale.nxtwrapper.network;

import com.davale.nxtwrapper.network.init.ClientInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import timber.log.Timber;

public final class NettyClient {

    private final String host;
    private final EventLoopGroup group;

    private String messageToServer;

    public NettyClient(String host) {
        this.host = host;

        messageToServer = "";

        group = new NioEventLoopGroup();
    }


    public void run() throws Exception {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .handler(new ClientInitializer());

            Channel ch = bootstrap.connect(host, 14930).sync().channel();
            ChannelFuture lastWriteFuture = null;

            while (true) {
                if (messageToServer == null) {
                    break;
                }

                if (!messageToServer.isEmpty()) {
                    lastWriteFuture = ch.writeAndFlush(messageToServer + "\r\n");
                    messageToServer = "";
                }
            }

            if (lastWriteFuture != null) {
                lastWriteFuture.sync();
            }
        } finally {
            group.shutdownGracefully();
        }
    }

    public void stop() {
        messageToServer = null;
    }

    public void send(String data) {
        Timber.e("Sending data: " + data);
        messageToServer = data;
    }
}