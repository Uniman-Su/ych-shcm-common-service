package com.ych.shcm.o2o.service.systemparamholder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.service.SystemParameterService;

/**
 * 车辆可选第二个服务包的月份数
 *
 * Created by U on 2017/9/11.
 */
@Component(SelectableSecondSPMonth.NAME)
public class SelectableSecondSPMonth extends SystemParameterHolder {

    public static final String NAME = "shcm.o2o.service.systemparamholder.SelectableSecondSPMonth";

    public SelectableSecondSPMonth() {
        setKey("selectableSecondSPMonth");
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
