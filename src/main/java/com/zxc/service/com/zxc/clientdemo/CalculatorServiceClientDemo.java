package com.zxc.service.com.zxc.clientdemo;

import com.zxc.nioeasyrpc.proto.CalculatorRequest;
import com.zxc.nioeasyrpc.proto.CalculatorResponse;
import com.zxc.rpc.client.RpcClient;
import com.zxc.service.CalculatorService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class CalculatorServiceClientDemo {

    public static void main(String[] args) {
        RpcClient client = new RpcClient();
        CalculatorService.Async service =
                (CalculatorService.Async) client.getService(CalculatorService.Async.class);
        CalculatorRequest request = CalculatorRequest.newBuilder().setNum1(1).setNum2(2).build();
        request.toByteArray();
        Future<CalculatorResponse> response = service.add(request);
        try {
            System.out.println(response.get().getResult());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            client.disconnect();
        }
    }
}
