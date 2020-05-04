package com.zxc.rpc;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

@ChannelHandler.Sharable
public class RpcMessageHandler extends SimpleChannelInboundHandler<EasyRpcMessage> {

    final Map<String, EasyRpcService> map;

    public RpcMessageHandler(Map<String, EasyRpcService> map) {
        this.map = map;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, EasyRpcMessage msg) throws Exception {
        EasyRpcService service = map.get(msg.serviceName + ":" + msg.serviceMethod);
        EasyRpcMessage response = service.execute(msg);
        ctx.channel().writeAndFlush(response);
    }
}
