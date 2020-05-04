package com.zxc.service;

import com.zxc.nioeasyrpc.proto.HelloRequest;
import com.zxc.nioeasyrpc.proto.HelloResponse;

public interface HelloService {
    HelloResponse sayHello(HelloRequest request);
}
