package com.zxc.service;

import com.zxc.nioeasyrpc.proto.CalculatorRequest;
import com.zxc.nioeasyrpc.proto.CalculatorResponse;

public interface CalculatorService {
    CalculatorResponse add(CalculatorRequest request);
}
