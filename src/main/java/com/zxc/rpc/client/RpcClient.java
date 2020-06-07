package com.zxc.rpc.client;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.AbstractMessageLite;
import com.zxc.rpc.config.ZooKeeperConfig;
import com.zxc.rpc.message.Spliter;
import com.zxc.rpc.message.EasyRpcMessage;
import com.zxc.rpc.message.RpcMessageConverter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.lang.reflect.*;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

// bind to an RPC service
public class RpcClient {

    private RpcClientContext rpcClientContext;
    private CuratorFramework zkClient;
    private NioEventLoopGroup workerGroup;
    private Random random;

    ConcurrentHashMap<String, List<String>> serviceMapCache;

    public RpcClient() {
        serviceMapCache = new ConcurrentHashMap<>();
        random = new Random(System.currentTimeMillis());
        workerGroup = new NioEventLoopGroup();
        zkClient = CuratorFrameworkFactory.newClient(
                ZooKeeperConfig.ZK_CONN_ADDR, new ExponentialBackoffRetry(1000, 3));
        zkClient.start();
    }

    public void addServices(List<Class<?>> servicesList) {
        for (Class<?> serviceClz : servicesList) {
            String serviceName = serviceClz.getName();
        }
    }

    private void initiateServiceMap() {

    }

    private String findService(String serviceName) {
        if (!ZooKeeperConfig.ON) {
            return "localhost:8080";
        }
        if (!serviceMapCache.contains(serviceName)) {
            String servicePath = "/nioeasyrpc/providers/" + serviceName;
            try {
                Stat stat = zkClient.checkExists().forPath(servicePath);
                if (null != stat) {
                    List<String> children = zkClient.getChildren().forPath(servicePath);
                    serviceMapCache.put(servicePath, children);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<String> providers = serviceMapCache.get(serviceName);
        String providerId = providers.get(random.nextInt(providers.size()));
        try {
            byte[] data = zkClient.getData().forPath("_" + providerId);
            return String.valueOf(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void connect(Class<?> serviceClass) {
        String serviceAddr = findService(serviceClass.getName());
        String serviceHost = serviceAddr.split(":")[0];
        int servicePort = Integer.valueOf(serviceAddr.split(":")[1]);

        this.rpcClientContext = new RpcClientContext();
        this.rpcClientContext.setRpcResponseHolder(new RpcResponseHolder());

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

        ChannelFuture future = bootstrap.connect(serviceHost, servicePort);
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
        this.connect(serviceClass);
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
