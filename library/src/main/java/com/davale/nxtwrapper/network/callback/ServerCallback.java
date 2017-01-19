package com.davale.nxtwrapper.network.callback;

import io.netty.channel.ChannelHandlerContext;

public interface ServerCallback {

    void onMessageReceived(ChannelHandlerContext ctx, String message);

    void onHandlerAdded(ChannelHandlerContext ctx);

    void onHandlerRemoved(ChannelHandlerContext ctx);

    void onException(ChannelHandlerContext ctx, Throwable throwable);
}
