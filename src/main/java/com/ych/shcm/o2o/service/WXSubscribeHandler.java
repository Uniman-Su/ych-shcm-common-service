package com.ych.shcm.o2o.service;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Locale;
import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.ych.core.wechat.mp.MsgType;
import com.ych.core.wechat.mp.UserInfo;
import com.ych.core.wechat.mp.WXMPUtils;
import com.ych.core.wechat.mp.pushmsg.*;
import com.ych.shcm.o2o.dao.UserWechatInfoDao;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.UserWechatInfo;

/**
 * 维系关注消息处理器
 */
@Lazy
@Component("shcm.o2o.service.WXSubscribeHandler")
public class WXSubscribeHandler implements PushEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(WXSubscribeHandler.class);

    @Autowired
    private UserWechatInfoDao userWechatInfoDao;

    @Autowired
    private WXMPUtils wxMPUtils;

    @Resource(name = Constants.TASK_EXECUTOR)
    private TaskExecutor taskExecutor;

    @Override
    public boolean isSupport(MsgType msgType) {
        return msgType == MsgType.event;
    }

    @Override
    public boolean isSupport(EventType eventType) {
        return EventType.subscribe == eventType || EventType.unsubscribe == eventType;
    }

    @Override
    public IPushMessageResponse<?> handle(final IPushMessage pushMessage) {
        final IPushEvent event = (IPushEvent) pushMessage;

        taskExecutor.execute(new Runnable() {

            @Override
            public void run() {
                UserWechatInfo userWechatInfo = userWechatInfoDao.selectByOpenId(event.getSender());

                if (userWechatInfo != null) {
                    if (userWechatInfo.isSubscribe() && event.getEventType() == EventType.unsubscribe) {
                        userWechatInfo.setSubscribe(false);
                        userWechatInfo.setSubscribeTime(null);
                        userWechatInfoDao.update(userWechatInfo);
                    } else if (!userWechatInfo.isSubscribe() && event.getEventType() == EventType.subscribe) {
                        UserInfo userInfo = wxMPUtils.getUserInfo(event.getSender(), Locale.SIMPLIFIED_CHINESE);
                        try {
                            BeanUtils.copyProperties(userWechatInfo, userInfo);
                            userWechatInfoDao.update(userWechatInfo);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            logger.error("Copy properties failed", e);
                        }

                    }
                } else if (event.getEventType() == EventType.subscribe) {
                    UserInfo userInfo = wxMPUtils.getUserInfo(event.getSender(), Locale.SIMPLIFIED_CHINESE);
                    userWechatInfo = new UserWechatInfo();

                    try {
                        BeanUtils.copyProperties(userWechatInfo, userInfo);
                        userWechatInfoDao.insert(userWechatInfo);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.error("Copy properties failed", e);
                    }
                }
            }

        });

        return IPushMessageResponse.DEFAULT_RESPONSE;
    }
}
