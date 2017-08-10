package com.ych.shcm.o2o.service.systemparamholder;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 服务商分成比例
 */
@Component(ServiceProviderIncomesRate.NAME)
public class ServiceProviderIncomesRate extends SystemParameterHolder {

    public static final String NAME = "shcm.o2o.service.systemparamholder.ServiceProviderIncomesRate";

    public ServiceProviderIncomesRate() {
        setKey("ServiceProviderIncomesRate");
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
