package com.zxc.rpc;

import io.netty.buffer.ByteBuf;

public class MessageUtil {

    public static int MAGIC_NUMBER = 0x87654321;
    private static byte VERSION = 0;

    public static void encodeRpcMessage(ByteBuf byteBuf, EasyRpcMessage request) {
        // 4 bytes
        byteBuf.writeInt(MAGIC_NUMBER);
        byte[] serviceNameBytes = request.serviceName.getBytes();
        byte[] serviceMethodBytes = request.serviceMethod.getBytes();
        // 1 byte
        byteBuf.writeByte(VERSION);
        // 1 byte
        byteBuf.writeByte(serviceNameBytes.length);
        // 1 byte
        byteBuf.writeByte(serviceMethodBytes.length);

        int msgLength = request.messageData.length + serviceNameBytes.length + serviceMethodBytes.length;
        // 4 bytes
        byteBuf.writeInt(msgLength);

        byteBuf.writeBytes(serviceNameBytes);
        byteBuf.writeBytes(serviceMethodBytes);
        byteBuf.writeBytes(request.messageData);
    }

    public static EasyRpcMessage decodeRpcMessage(ByteBuf byteBuf) {
        byteBuf.skipBytes(4);
        byteBuf.skipBytes(1);


        byte serveNameLength = byteBuf.readByte();
        byte serviceMethodLength = byteBuf.readByte();
        int dataLength = byteBuf.readInt();

        EasyRpcMessage message = new EasyRpcMessage();

        byte[] serviceNameBytes = new byte[serveNameLength];
        byteBuf.readBytes(serviceNameBytes);
        String serviceName = new String(serviceNameBytes);

        byte[] serviceMethodBytes = new byte[serviceMethodLength];
        byteBuf.readBytes(serviceMethodBytes);
        String serviceMethod = new String(serviceMethodBytes);

        byte[] data = new byte[dataLength - serveNameLength - serviceMethodLength];
        byteBuf.readBytes(data);

        message.serviceName = serviceName;
        message.serviceMethod = serviceMethod;
        message.messageData = data;
        return message;
    }

}
