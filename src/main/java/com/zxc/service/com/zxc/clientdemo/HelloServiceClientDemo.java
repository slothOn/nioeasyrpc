package com.zxc.service.com.zxc.clientdemo;

import com.zxc.nioeasyrpc.proto.HelloRequest;
import com.zxc.nioeasyrpc.proto.HelloResponse;
import com.zxc.rpc.client.RpcClient;
import com.zxc.service.HelloService;

public class HelloServiceClientDemo {
    public static void main(String[] args) {
        RpcClient client = new RpcClient();
        HelloService service = (HelloService) client.getService(HelloService.class);
        HelloRequest request = HelloRequest.getDefaultInstance();
        request.toByteArray();
        HelloResponse response = service.sayHello(request);
        System.out.println(response.getHelloText());
    }
}
