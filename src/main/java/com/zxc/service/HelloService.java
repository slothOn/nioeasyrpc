package com.zxc.service;

import com.zxc.nioeasyrpc.proto.HelloRequest;
import com.zxc.nioeasyrpc.proto.HelloResponse;

import java.util.concurrent.Future;

public interface HelloService {
    HelloResponse sayHello(HelloRequest request);

    interface Async {
        Future<HelloResponse> sayHello(HelloRequest request);
    }
}
