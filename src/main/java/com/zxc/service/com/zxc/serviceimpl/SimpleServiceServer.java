package com.zxc.service.com.zxc.serviceimpl;

import com.zxc.rpc.config.ZooKeeperConfig;
import com.zxc.rpc.server.RpcServer;

import java.io.IOException;

public class SimpleServiceServer extends RpcServer {

    public SimpleServiceServer() {
        super();
    }

    public static void main(String[] args) {
        ZooKeeperConfig.ON = false;
        SimpleServiceServer server = new SimpleServiceServer();
        server.addService(new CalculatorServiceActionBase());
        server.addService(new HelloServiceActionBase());

        try {
            server.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
