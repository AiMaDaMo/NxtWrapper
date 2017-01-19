package com.davale.nxtwrapper.network.handler;

import com.davale.nxtwrapper.network.NettyServer;
import com.davale.nxtwrapper.network.ServerWorker;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import timber.log.Timber;

public final class ServerHandler extends SimpleChannelInboundHandler<String> {

    public ServerHandler() {
    }

    @Override
    public void handlerAdded(ChannelHandlerContext context) throws Exception {
        NettyServer.channels.add(context.channel());
        ServerWorker.getInstance().handlerAdded(context);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext context) throws Exception {
        NettyServer.channels.remove(context.channel());
        ServerWorker.getInstance().handlerRemoved(context);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
        ServerWorker.getInstance().exception(context, cause);
        context.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext context, String message) throws Exception {
        ServerWorker.getInstance().parseData(context, message);
    }

    public static void sendMessage(String data) {
        Timber.e("Sending data: " + data);

        for (Channel c : NettyServer.channels) {
            c.writeAndFlush(data + "\r\n");
        }
    }
}
