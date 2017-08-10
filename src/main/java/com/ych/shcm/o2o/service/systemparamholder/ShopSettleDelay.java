package com.ych.shcm.o2o.service.systemparamholder;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 门店结算延迟天数
 *
 * Created by U on 2017/7/17.
 */
@Component(ShopSettleDelay.NAME)
public class ShopSettleDelay extends SystemParameterHolder {

    public static final String NAME = "shcm.o2o.service.systemparamholder.ShopSettleDelay";

    public ShopSettleDelay() {
        setKey("shopSettleDelay");
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
