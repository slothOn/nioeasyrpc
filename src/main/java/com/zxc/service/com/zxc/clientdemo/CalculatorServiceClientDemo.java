package com.zxc.service.com.zxc.clientdemo;

import com.zxc.nioeasyrpc.proto.CalculatorRequest;
import com.zxc.nioeasyrpc.proto.CalculatorResponse;
import com.zxc.rpc.client.RpcClient;
import com.zxc.service.CalculatorService;


public class CalculatorServiceClientDemo {

    public static void main(String[] args) {
        RpcClient client = new RpcClient();
        CalculatorService service = (CalculatorService) client.getService(CalculatorService.class);
        CalculatorRequest request = CalculatorRequest.newBuilder().setNum1(1).setNum2(2).build();
        request.toByteArray();
        CalculatorResponse response = service.add(request);
        System.out.println(response.getResult());
    }
}
