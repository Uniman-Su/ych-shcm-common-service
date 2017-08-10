package com.ych.shcm.o2o.service;

import com.ych.shcm.o2o.model.AccessChannel;
import com.ych.shcm.o2o.openinf.IRequest;
import com.ych.shcm.o2o.openinf.IResponse;

/**
 * 请求对象的执行器
 */
public interface AccessChannelRequestExecutor {

    /**
     * 执行接入渠道提交的请求
     *
     * @param request
     *         请求对象
     * @param accessChannel
     *         访问渠道
     * @return 返回结果
     */
    IResponse<?> execute(IRequest<?> request, AccessChannel accessChannel);

}
