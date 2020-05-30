package com.zxc.service.com.zxc.clientdemo;

import com.google.common.util.concurrent.MoreExecutors;
import com.zxc.nioeasyrpc.proto.HelloRequest;
import com.zxc.nioeasyrpc.proto.HelloResponse;
import com.zxc.rpc.client.RpcClient;
import com.zxc.service.HelloService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HelloServiceClientDemo {
    public static void main(String[] args) {
        RpcClient client = new RpcClient();
        HelloService.Async service = (HelloService.Async) client.getService(HelloService.Async.class);
        HelloRequest request = HelloRequest.getDefaultInstance();
        request.toByteArray();
        Future<HelloResponse> response = service.sayHello(request);
        MoreExecutors.directExecutor().execute(() -> {
            try {
                System.out.println(response.get().getHelloText());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        });
        client.disconnect();
    }
}
