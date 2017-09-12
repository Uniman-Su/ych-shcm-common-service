package com.ych.shcm.o2o.service;

import java.text.MessageFormat;
import java.util.TreeMap;

import org.apache.commons.codec.digest.DigestUtils;

public class ChannelGuideInTest {

    public static void main(String[] args) {
        String url = "http://inf.kuaicarkeep.com/wxmp/logon/guidance?appCode={0}&userId={1}&vin={2}&timestamp={3}&digest={4}";

        String appCode = "chehangjia";
        String userId = "94";
        String vin = "LSVCE6A41CN070212";
        String timestamp = String.valueOf("1502706733468");
        String securityKey = "chehangjiaSecurityKey";

        TreeMap<String, String> sortMap = new TreeMap<>();
        sortMap.put("appCode", appCode);
        sortMap.put("userId", userId);
        sortMap.put("vin", vin);
        sortMap.put("timestamp", timestamp);
        sortMap.put("securityKey", securityKey);

        StringBuilder stringBuilder = new StringBuilder();
        for (String seg : sortMap.values()) {
            stringBuilder.append(seg);
        }

        String digest = DigestUtils.sha256Hex(stringBuilder.toString()).toUpperCase();

        System.out.println(MessageFormat.format(url, appCode, userId, vin, timestamp, digest));
    }

}
