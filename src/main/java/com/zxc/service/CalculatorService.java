package com.zxc.service;

import com.zxc.nioeasyrpc.proto.CalculatorRequest;
import com.zxc.nioeasyrpc.proto.CalculatorResponse;

import java.util.concurrent.Future;

public interface CalculatorService {
    CalculatorResponse add(CalculatorRequest request);

    interface Async {
        Future<CalculatorResponse> add(CalculatorRequest request);
    }
}
