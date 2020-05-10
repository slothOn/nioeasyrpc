package com.zxc.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

@ChannelHandler.Sharable
public class RpcMessageConverter extends MessageToMessageCodec<ByteBuf, EasyRpcMessage> {
    public static final RpcMessageConverter INSTANCE = new RpcMessageConverter();

    @Override
    protected void encode(ChannelHandlerContext ctx, EasyRpcMessage msg, List<Object> out) throws Exception {
        ByteBuf buf = ctx.channel().alloc().ioBuffer();
        MessageUtil.encodeRpcMessage(buf, msg);
        out.add(buf);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        out.add(MessageUtil.decodeRpcMessage(msg));
    }
}
