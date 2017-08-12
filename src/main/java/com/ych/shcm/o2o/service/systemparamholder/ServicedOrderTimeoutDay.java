package com.ych.shcm.o2o.service.systemparamholder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;

/**
 * 已服务订单的超时天数
 */
@Lazy
@Component(ServicedOrderTimeoutDay.NAME)
public class ServicedOrderTimeoutDay extends SystemParameterHolder {

    public static final String NAME = "shcm.o2o.service.systemparamholder.ServicedOrderTimeoutDay";

    public ServicedOrderTimeoutDay() {
        setKey("ServicedOrderTimeoutDay");
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
