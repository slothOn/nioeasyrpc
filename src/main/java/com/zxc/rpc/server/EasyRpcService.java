package com.zxc.rpc.server;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.InvalidProtocolBufferException;
import com.zxc.rpc.message.EasyRpcMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public abstract class EasyRpcService {
    EasyRpcMessage execute(EasyRpcMessage request)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InvalidProtocolBufferException {
        Optional<Method> methodOptional = Arrays.stream(
                this.getClass().getDeclaredMethods()).filter(m -> m.getName().equals(request.serviceMethod)).findFirst();
        Method method = methodOptional.get();
        Class<?> requestType = method.getParameterTypes()[0];
        Method parseFromMethod = requestType.getMethod("parseFrom", byte[].class);
        AbstractMessage protoRequest = (AbstractMessage) parseFromMethod.invoke(null, request.messageData);

        AbstractMessage protoResponse = (AbstractMessage) method.invoke(this, protoRequest);

        Method toByteArrayMethod = AbstractMessageLite.class.getMethod("toByteArray");
        byte[] protoBytes = (byte[]) toByteArrayMethod.invoke(protoResponse);
        EasyRpcMessage response = new EasyRpcMessage();
        response.serviceName = request.serviceName;
        response.serviceMethod = request.serviceMethod;
        response.messageData = protoBytes;

        return response;
    }

}
