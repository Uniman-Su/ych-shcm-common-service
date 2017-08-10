package com.ych.shcm.o2o.service.systemparamholder;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 经销商结算延迟天数
 */
@Component(ServiceProviderSettleDelay.NAME)
public class ServiceProviderSettleDelay extends SystemParameterHolder {

    public static final String NAME = "shcm.o2o.service.systemparamholder.ServiceProviderSettleDelay";

    public ServiceProviderSettleDelay() {
        setKey("serviceProviderSettleDelay");
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
