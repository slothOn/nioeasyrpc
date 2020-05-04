package com.zxc.rpc;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.AbstractMessageLite;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

// bind to an RPC service
public class RpcClient {
    Bootstrap bootstrap;
    Channel channel;

    RpcResponseHolder rpcResponseHolder;

    public RpcClient() {
        rpcResponseHolder = new RpcResponseHolder();
    }

    private void connect() {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(RpcMessageConverter.INSTANCE);
                        ch.pipeline().addLast(new RpcResponseHandler(rpcResponseHolder));
                    }
                });

        ChannelFuture future = bootstrap.connect("localhost", 8080);
        try {
            future.await();
            if (future.isSuccess()) {
                this.channel = future.channel();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Object getService(Class<?> serviceClass) {
        this.connect();
        InvocationHandler handler = new RpcInvocationHandler(this.channel, this.rpcResponseHolder);
        return Proxy.newProxyInstance(
                serviceClass.getClassLoader(), new Class[] {serviceClass}, handler);
    }

    static class RpcInvocationHandler implements InvocationHandler {
        Channel channel;
        RpcResponseHolder rpcResponseHolder;

        RpcInvocationHandler(
                Channel channel, RpcResponseHolder rpcResponseHolder) {
            this.channel = channel;
            this.rpcResponseHolder = rpcResponseHolder;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            EasyRpcMessage requestMsg = new EasyRpcMessage();
            AbstractMessage protoRequest = (AbstractMessage) args[0];
            Method toByteArrayMethod = AbstractMessageLite.class.getMethod("toByteArray");
            byte[] protoRequestBytes = (byte[]) toByteArrayMethod.invoke(protoRequest);
            requestMsg.messageData = protoRequestBytes;
            requestMsg.serviceMethod = method.getName();
            requestMsg.serviceName = proxy.getClass().getInterfaces()[0].getName();

            channel.writeAndFlush(requestMsg);

            synchronized (rpcResponseHolder) {
                if (!rpcResponseHolder.isPresent()) {
                    rpcResponseHolder.wait(5000);
                }
            }
            if (!rpcResponseHolder.isPresent()) {
                throw new IOException("Rpc time out");
            }
            EasyRpcMessage response = rpcResponseHolder.get();

            byte[] protoResponseBytes = response.messageData;
            Class<?> returnType = method.getReturnType();
            Method parseFromMethod = returnType.getMethod("parseFrom", byte[].class);
            AbstractMessage protoResponse = (AbstractMessage) parseFromMethod.invoke(null, protoResponseBytes);
            return protoResponse;
        }
    }

    static class RpcResponseHandler extends SimpleChannelInboundHandler<EasyRpcMessage> {

        RpcResponseHolder rpcResponseHolder;

        RpcResponseHandler(RpcResponseHolder rpcResponseHolder) {
            this.rpcResponseHolder = rpcResponseHolder;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, EasyRpcMessage msg) throws Exception {
            synchronized (this.rpcResponseHolder) {
                rpcResponseHolder.set(msg);
                this.rpcResponseHolder.notify();
            }
        }
    }

    static class RpcResponseHolder {
        EasyRpcMessage rpcResponse;

        RpcResponseHolder() {
            this.rpcResponse = null;
        }

        public void set(EasyRpcMessage rpcResponse) {
            this.rpcResponse = rpcResponse;
        }

        public EasyRpcMessage get() {
            return this.rpcResponse;
        }

        public boolean isPresent() {
            return rpcResponse != null;
        }
    }
}
