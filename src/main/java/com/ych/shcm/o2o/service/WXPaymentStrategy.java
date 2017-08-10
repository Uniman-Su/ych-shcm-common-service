package com.ych.shcm.o2o.service;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ych.core.model.SystemParameterHolder;
import com.ych.core.wechat.pay.WXPayUtils;
import com.ych.shcm.o2o.model.PayChannel;
import com.ych.shcm.o2o.service.systemparamholder.WXMPAppID;
import com.ych.shcm.o2o.service.systemparamholder.WXPayMerchantId;

/**
 * 微信的支付策略
 */
@Lazy
@Component("shcm.o2o.service.WXPaymentStrategy")
public class WXPaymentStrategy extends com.ych.core.wechat.pay.WXPaymentStrategy {

    private String[] supportChannelNames = new String[]{PayChannel.WXPAY.name()};

    @Autowired
    @Override
    public void setWxPayUtils(WXPayUtils wxPayUtils) {
        super.setWxPayUtils(wxPayUtils);
    }

    @Resource(name = WXMPAppID.NAME)
    @Override
    public void setWxAppId(SystemParameterHolder wxAppId) {
        super.setWxAppId(wxAppId);
    }

    @Resource(name = WXPayMerchantId.NAME)
    @Override
    public void setWxPayMerchantId(SystemParameterHolder wxPayMerchantId) {
        super.setWxPayMerchantId(wxPayMerchantId);
    }

    @Override
    public String[] getSupportChannelNames() {
        String[] ret = new String[supportChannelNames.length];
        System.arraycopy(supportChannelNames, 0, ret, 0, supportChannelNames.length);
        return ret;
    }

}
