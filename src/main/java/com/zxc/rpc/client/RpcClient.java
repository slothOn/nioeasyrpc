package com.zxc.rpc.client;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.AbstractMessageLite;
import com.zxc.rpc.message.Spliter;
import com.zxc.rpc.message.EasyRpcMessage;
import com.zxc.rpc.message.RpcMessageConverter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.concurrent.Callable;

// bind to an RPC service
public class RpcClient {

    private RpcClientContext rpcClientContext;

    public RpcClient() {}

    private void findService() {

    }

    private void connect() {
        // return ip:port

        this.rpcClientContext = new RpcClientContext();
        this.rpcClientContext.setRpcResponseHolder(new RpcResponseHolder());

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        this.rpcClientContext.setWorkerGroup(workerGroup);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(RpcMessageConverter.INSTANCE);
                        ch.pipeline().addLast(new RpcResponseHandler(rpcClientContext));
                    }
                });

        ChannelFuture future = bootstrap.connect("localhost", 8080);
        try {
            future.await();
            if (future.isSuccess()) {
                Channel channel = future.channel();
                this.rpcClientContext.setChannel(channel);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        rpcClientContext.channel.close();
        rpcClientContext.workerGroup.shutdownGracefully();
    }

    public Object getService(Class<?> serviceClass) {
        this.connect();
        InvocationHandler handler = new RpcInvocationHandler(rpcClientContext);
        return Proxy.newProxyInstance(
                serviceClass.getClassLoader(), new Class[] {serviceClass}, handler);
    }

    static class RpcInvocationHandler implements InvocationHandler {
        RpcClientContext rpcClientContext;

        RpcInvocationHandler(
                RpcClientContext rpcClientContext) {
            this.rpcClientContext = rpcClientContext;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            EasyRpcMessage requestMsg = new EasyRpcMessage();
            AbstractMessage protoRequest = (AbstractMessage) args[0];
            Method toByteArrayMethod = AbstractMessageLite.class.getMethod("toByteArray");
            byte[] protoRequestBytes = (byte[]) toByteArrayMethod.invoke(protoRequest);
            requestMsg.messageData = protoRequestBytes;
            requestMsg.serviceMethod = method.getName();
            String interfaceName = proxy.getClass().getInterfaces()[0].getName();
            // remove "$Async"
            requestMsg.serviceName = interfaceName.substring(0, interfaceName.length() - 6);

            return rpcClientContext.workerGroup.submit(new Callable<AbstractMessage>() {

                @Override
                public AbstractMessage call() throws Exception {
                    rpcClientContext.channel.writeAndFlush(requestMsg);
                    RpcResponseHolder rpcResponseHolder = rpcClientContext.rpcResponseHolder;

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
                    ParameterizedType returnType = (ParameterizedType) method.getGenericReturnType();
                    Method parseFromMethod =
                            ((Class)returnType.getActualTypeArguments()[0]).getMethod("parseFrom", byte[].class);
                    AbstractMessage protoResponse =
                            (AbstractMessage) parseFromMethod.invoke(null, protoResponseBytes);
                    return protoResponse;
                }
            });
        }
    }

    static class RpcResponseHandler extends SimpleChannelInboundHandler<EasyRpcMessage> {

        RpcClientContext rpcClientContext;

        RpcResponseHandler(RpcClientContext rpcClientContext) {
            this.rpcClientContext = rpcClientContext;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, EasyRpcMessage msg) throws Exception {
            synchronized (rpcClientContext.rpcResponseHolder) {
                rpcClientContext.rpcResponseHolder.set(msg);
                this.rpcClientContext.rpcResponseHolder.notify();
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

    static class RpcClientContext {
        public Channel channel;
        public RpcResponseHolder rpcResponseHolder;
        public NioEventLoopGroup workerGroup;

        public RpcClientContext() {}

        public Channel getChannel() {
            return channel;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        public RpcResponseHolder getRpcResponseHolder() {
            return rpcResponseHolder;
        }

        public void setRpcResponseHolder(RpcResponseHolder rpcResponseHolder) {
            this.rpcResponseHolder = rpcResponseHolder;
        }

        public NioEventLoopGroup getWorkerGroup() {
            return workerGroup;
        }

        public void setWorkerGroup(NioEventLoopGroup workerGroup) {
            this.workerGroup = workerGroup;
        }
    }
}
