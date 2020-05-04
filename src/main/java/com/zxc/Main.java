package com.zxc;

public class Main {
    public static void main(String[] args) {
        String serviceName = "com.zxc.service.CalculatorService";
        byte[] s1 = serviceName.getBytes();
        System.out.println(s1.length);
        System.out.println(new String(s1));
    }
}
