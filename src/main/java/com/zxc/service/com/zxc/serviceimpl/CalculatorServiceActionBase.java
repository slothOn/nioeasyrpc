package com.zxc.service.com.zxc.serviceimpl;

import com.zxc.nioeasyrpc.proto.CalculatorRequest;
import com.zxc.nioeasyrpc.proto.CalculatorResponse;
import com.zxc.rpc.server.BaseEasyRpcService;
import com.zxc.rpc.server.EasyRpcServerAnnotation;
import com.zxc.service.CalculatorService;

public class CalculatorServiceActionBase extends BaseEasyRpcService implements CalculatorService {

    @EasyRpcServerAnnotation.ServiceMethod
    @Override
    public CalculatorResponse add(CalculatorRequest request) {
        CalculatorResponse.Builder builder = CalculatorResponse.newBuilder();
        int num1 = request.getNum1();
        int num2 = request.getNum2();
        builder.setResult(num1 + num2);
        return builder.build();
    }
}
