package com.ych.shcm.o2o.service.systemparamholder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;

/**
 * JWT允许时间戳误差的范围(秒)
 *
 * Created by U on 2017/7/18.
 */
@Lazy
@Component(JWTAllowedClockSkewInSeconds.NAME)
public class JWTAllowedClockSkewInSeconds extends SystemParameterHolder {

    public static final String NAME = "shcm.o2o.service.systemparamholder.JWTAllowedClockSkewInSeconds";

    public JWTAllowedClockSkewInSeconds() {
        setKey("JWTAllowedClockSkewInSeconds");
    }

    @Autowired
    @Override
    public void setSystemParameterSvc(SystemParameterService systemParameterSvc) {
        super.setSystemParameterSvc(systemParameterSvc);
    }

    @Value("${system.common.appKey}")
    @Override
    public void setAppKey(String appKey) {
        super.setAppKey(appKey);
    }

}
