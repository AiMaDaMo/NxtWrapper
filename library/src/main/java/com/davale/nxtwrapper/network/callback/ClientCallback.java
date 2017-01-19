package com.davale.nxtwrapper.network.callback;

import io.netty.channel.ChannelHandlerContext;

public interface ClientCallback {

    void onMessageReceived(ChannelHandlerContext ctx, String message);

    void onConnected(ChannelHandlerContext ctx);

    void onDisconnected(ChannelHandlerContext ctx);

    void onException(ChannelHandlerContext ctx, Throwable throwable);
}
