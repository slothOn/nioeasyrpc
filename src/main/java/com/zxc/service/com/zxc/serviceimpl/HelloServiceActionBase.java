package com.zxc.service.com.zxc.serviceimpl;

import com.zxc.nioeasyrpc.proto.HelloRequest;
import com.zxc.nioeasyrpc.proto.HelloResponse;
import com.zxc.rpc.server.BaseEasyRpcService;
import com.zxc.rpc.server.EasyRpcServerAnnotation;
import com.zxc.service.HelloService;

public class HelloServiceActionBase extends BaseEasyRpcService implements HelloService {

    @EasyRpcServerAnnotation.ServiceMethod
    @Override
    public HelloResponse sayHello(HelloRequest request) {
        return HelloResponse.newBuilder().setHelloText("Hello World.").build();
    }
}
