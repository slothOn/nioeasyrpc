package com.zxc.rpc.server;

import com.zxc.rpc.message.EasyRpcMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

@ChannelHandler.Sharable
public class RpcMessageHandler extends SimpleChannelInboundHandler<EasyRpcMessage> {

    final Map<String, BaseEasyRpcService> map;

    public RpcMessageHandler(Map<String, BaseEasyRpcService> map) {
        this.map = map;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, EasyRpcMessage msg) throws Exception {
        BaseEasyRpcService service = map.get(msg.serviceName + ":" + msg.serviceMethod);
        EasyRpcMessage response = service.execute(msg);
        ctx.channel().writeAndFlush(response);
    }
}
