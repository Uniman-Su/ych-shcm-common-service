package com.ych.shcm.o2o.service.systemparamholder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;

/**
 * 微信店铺所有者绑定OpenId URL
 *
 * Created by U on 2017/7/26.
 */
@Lazy
@Component(WXShopOwnerBindOpenIdUrl.NAME)
public class WXShopOwnerBindOpenIdUrl extends SystemParameterHolder {

    public static final String NAME = "shcm.o2o.service.systemparamholder.WXShopOwnerBindOpenIdUrl";

    public WXShopOwnerBindOpenIdUrl() {
        setKey("WXShopOwnerBindOpenIdUrl");
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
