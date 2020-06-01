package com.zxc.rpc.server;

import com.zxc.rpc.config.ZooKeeperConfig;
import com.zxc.rpc.message.Spliter;
import com.zxc.rpc.message.RpcMessageConverter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class RpcServer {
    int port = 8080;

    Map<String, EasyRpcService> map;
    CuratorFramework zkClient;

    protected RpcServer() {
        map = new HashMap<>();
    }

    public void addService(EasyRpcService service) {
        Class<? extends EasyRpcService> serviceClass = service.getClass();
        for (Method method : serviceClass.getMethods()) {
            map.put(serviceClass.getInterfaces()[0].getName() + ":" + method.getName(), service);
        }
    }

    private void registerServices() throws UnknownHostException {
        zkClient = CuratorFrameworkFactory.newClient(
                ZooKeeperConfig.ZK_CONN_ADDR, new ExponentialBackoffRetry(1000, 3));
        String rpcServerAddress = InetAddress.getLocalHost().getHostAddress() + ":" + port;
        String serviceProviderPath = "/nioeasyrpc/providers/";
        zkClient.start();
        for (Map.Entry<String, EasyRpcService> entry : map.entrySet()) {
            try {
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                        .inBackground()
                        .forPath(serviceProviderPath + entry.getKey() + "/_", rpcServerAddress.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws IOException {
        // find a random port that's available
        RpcMessageHandler rpcMessageHandler = new RpcMessageHandler(map);
        NioEventLoopGroup boostGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        final ServerBootstrap serverBootstrap = new ServerBootstrap();

        serverBootstrap.group(boostGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(RpcMessageConverter.INSTANCE);
                        ch.pipeline().addLast(rpcMessageHandler);
                    }
                });

        ChannelFuture future = serverBootstrap.bind(port);
        try {
            future.await();
            if (future.isSuccess()) {
                System.out.println("rpc server has been bond to :" + port);
            } else {
                throw new IOException("fail to bind rpc server to :" + port);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        registerServices();
    }
}
