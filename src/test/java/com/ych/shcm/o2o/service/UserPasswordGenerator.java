package com.ych.shcm.o2o.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import com.ych.shcm.o2o.model.Constants;

public class UserPasswordGenerator {

    private static String randomPassword() {
        return RandomStringUtils.randomAlphanumeric(8);
    }

    private static String signPassword(String password) {
        return DigestUtils.sha256Hex(password + Constants.USER_PASSWORD_SECURITY_KEY);
    }

    public static void main(String[] args) {
//        String[][] userNames = {{"花溪腾达汽车修理店", "hxtdqcxld"},{"飞奔俐汽车修理厂", "fblqcxlc"},{"安信捷新添大道店", "axjxtddd"},{"安信捷沙冲南路店", "axjscnld"},{"安信捷云谭北路与下麦站交叉口店", "axjytblyxmzjckd"},{"高远汽车快修中心", "gyqckxzx"},{"中策空间", "zckj"},{"乌当启宏汽车养护中心", "wdqhqcyhzx"},{"中汽美途", "zqmt"},{"星悦奔宝圣泉店", "xybbsqd"},{"星悦奔宝小河店", "xybbxhd"},{"星悦奔宝金阳店", "xybbjyd"}};
//
//        for (String[] entry : userNames) {
//            String password = randomPassword();
//            String signed = signPassword(password);
//            System.out.println(entry[0] + "," + entry[1] + "," + password + "," + signed);
//        }

        System.out.println(signPassword("888888"));
    }

}
