package com.zxc.service.com.zxc.serviceimpl;

import com.zxc.rpc.server.RpcServer;

import java.io.IOException;

public class SimpleServiceServer extends RpcServer {

    public SimpleServiceServer() {
        super();
    }

    public static void main(String[] args) {
        SimpleServiceServer server = new SimpleServiceServer();
        server.addService(new CalculatorServiceAction());
        server.addService(new HelloServiceAction());

        try {
            server.start();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
