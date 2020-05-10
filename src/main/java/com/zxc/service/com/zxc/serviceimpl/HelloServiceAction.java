package com.zxc.service.com.zxc.serviceimpl;

import com.zxc.nioeasyrpc.proto.HelloRequest;
import com.zxc.nioeasyrpc.proto.HelloResponse;
import com.zxc.rpc.server.EasyRpcService;
import com.zxc.service.HelloService;

public class HelloServiceAction extends EasyRpcService implements HelloService {
    @Override
    public HelloResponse sayHello(HelloRequest request) {
        return HelloResponse.newBuilder().setHelloText("Hello World.").build();
    }
}
