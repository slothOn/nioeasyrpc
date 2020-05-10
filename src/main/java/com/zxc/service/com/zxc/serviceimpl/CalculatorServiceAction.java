package com.zxc.service.com.zxc.serviceimpl;

import com.zxc.nioeasyrpc.proto.CalculatorRequest;
import com.zxc.nioeasyrpc.proto.CalculatorResponse;
import com.zxc.rpc.server.EasyRpcService;
import com.zxc.service.CalculatorService;

public class CalculatorServiceAction extends EasyRpcService implements CalculatorService {

    @Override
    public CalculatorResponse add(CalculatorRequest request) {
        CalculatorResponse.Builder builder = CalculatorResponse.newBuilder();
        int num1 = request.getNum1();
        int num2 = request.getNum2();
        builder.setResult(num1 + num2);
        return builder.build();
    }
}
