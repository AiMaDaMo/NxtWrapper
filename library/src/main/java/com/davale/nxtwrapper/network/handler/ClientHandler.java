package com.davale.nxtwrapper.network.handler;

import com.davale.nxtwrapper.network.ClientWorker;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class ClientHandler extends SimpleChannelInboundHandler<String> {

    public ClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ClientWorker.getInstance().connected(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ClientWorker.getInstance().exception(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        ClientWorker.getInstance().parseData(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ClientWorker.getInstance().disconnected(ctx);
    }
}