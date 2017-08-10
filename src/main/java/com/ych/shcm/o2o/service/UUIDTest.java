package com.ych.shcm.o2o.service;

import java.util.UUID;

public class UUIDTest {

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString().replaceAll("\\-", "").toUpperCase());
    }

}
