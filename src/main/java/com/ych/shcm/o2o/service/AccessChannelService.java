package com.ych.shcm.o2o.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.ych.core.fasterxml.jackson.MapperUtils;
import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.SystemParameterHolder;
import com.ych.core.spring.web.client.Jackson2JsonOnlyResponseExtractor;
import com.ych.shcm.o2o.dao.AccessChannelDao;
import com.ych.shcm.o2o.dao.CarDao;
import com.ych.shcm.o2o.dao.OrderStatusHisDao;
import com.ych.shcm.o2o.dao.UserDao;
import com.ych.shcm.o2o.event.OrderStatusChanged;
import com.ych.shcm.o2o.model.AccessChannel;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.Order;
import com.ych.shcm.o2o.model.OrderStatus;
import com.ych.shcm.o2o.model.UserAccessChannel;
import com.ych.shcm.o2o.openinf.INotify;
import com.ych.shcm.o2o.openinf.IRequest;
import com.ych.shcm.o2o.openinf.IResponse;
import com.ych.shcm.o2o.openinf.Notify;
import com.ych.shcm.o2o.openinf.NotifyMsgType;
import com.ych.shcm.o2o.openinf.NotifyResponse;
import com.ych.shcm.o2o.openinf.OrderEventNotifyPayload;
import com.ych.shcm.o2o.openinf.RequestAction;
import com.ych.shcm.o2o.openinf.Response;
import com.ych.shcm.o2o.service.systemparamholder.AccessChannelTimestampLeeway;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 访问渠道的服务
 * <p>
 * Created by U on 2017/7/17.
 */
@Lazy
@Component("shcm.o2o.service.AccessChannelService")
public class AccessChannelService {

    private Logger logger = LoggerFactory.getLogger(AccessChannelService.class);

    /**
     * 访问渠道的Dao
     */
    @Autowired
    private AccessChannelDao accessChannelDao;

    @Autowired
    private OrderStatusHisDao orderStatusHisDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CarDao carDao;

    /**
     * 时间戳误差
     */
    @Resource(name = AccessChannelTimestampLeeway.NAME)
    private SystemParameterHolder timestampLeeway;

    @Autowired
    private MessageSource messageSource;

    @Resource(name = Constants.TASK_EXECUTOR)
    private TaskExecutor taskExecutor;

    /**
     * 请求命令类型到执行器的映射
     */
    private Map<RequestAction, AccessChannelRequestExecutor> executorMap = new HashMap<>();

    @Resource(name = Constants.DEFAULT_REST_TEMPLATE)
    private RestTemplate restTemplate;

    /**
     * @return 访问渠道的Dao
     */
    AccessChannelDao getAccessChannelDao() {
        return accessChannelDao;
    }

    /**
     * 根据ID查找访问渠道信息
     *
     * @param id
     *         ID
     * @return 访问渠道信息
     */
    public AccessChannel getById(BigDecimal id) {
        return accessChannelDao.selectById(id);
    }

    /**
     * 对内容做签名处理
     *
     * @param content
     *         内容的签名
     * @return 签名后的16进制字符串
     */
    public String digest(String content) {
        return DigestUtils.sha256Hex(content).toUpperCase();
    }

    /**
     * 时间戳是否在有效范围
     *
     * @param timestamp
     *         时间戳
     * @return 如果时间戳有效则返回
     */
    public boolean isTimestampValid(long timestamp) {
        long correction = timestampLeeway.getLongValue();
        long currentMills = System.currentTimeMillis();
        return currentMills - correction <= timestamp && timestamp <= currentMills + correction;
    }

    /**
     * 注册请求执行器
     *
     * @param action
     *         执行命令
     * @param executor
     *         执行器
     */
    public void registerRequestExecutor(RequestAction action, AccessChannelRequestExecutor executor) {
        executorMap.put(action, executor);
    }

    /**
     * 分发第三方接口的请求
     *
     * @param appCode
     *         应用编码
     * @param requestBody
     *         请求报文
     * @param digest
     *         签名
     * @param timestamp
     *         时间戳
     * @return 响应对象
     */
    public IResponse<?> dispatchRequest(String appCode, String requestBody, String digest, long timestamp) {
        if (!isTimestampValid(timestamp)) {
            return new Response<>(CommonOperationResult.IllegalArguments.name(), messageSource.getMessage("accessChannel.timestamp.illegal", null, Locale.getDefault()));
        }

        AccessChannel accessChannel = accessChannelDao.selectByCode(appCode);
        if (accessChannel == null) {
            return new Response<>(CommonOperationResult.IllegalArguments.name(), messageSource.getMessage("accessChannel.appCode.illegal", null, Locale.getDefault()));
        }

        if (!digest(appCode + requestBody + accessChannel.getSecurityKey() + timestamp).equals(digest)) {
            return new Response<>(CommonOperationResult.IllegalArguments.name(), messageSource.getMessage("accessChannel.digest.illegal", null, Locale.getDefault()));
        }

        JsonNode jsonNode;
        RequestAction action;
        IRequest<?> request;

        try {
            jsonNode = MapperUtils.MAPPER.get().readTree(requestBody);
            action = RequestAction.valueOf(jsonNode.get("action").asText());
            request = MapperUtils.MAPPER.get().readValue(requestBody, action.getRequestTypeReference());
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Read JSON failed", e);
            return new Response<>(CommonOperationResult.IllegalArguments.name(), messageSource.getMessage("json.decode.failed", null, Locale.getDefault()));
        }

        return executorMap.get(action).execute(request, accessChannel);
    }

    private <T> T postForNotify(AccessChannel accessChannel, INotify<?> notify, Class<T> responseType) {
        logger.info("Notify to access channel {}, url:{}, notification:{}", accessChannel.getCode(), accessChannel.getNotifyUrl(), notify);

        if (StringUtils.isEmpty(accessChannel.getNotifyUrl())) {
            logger.info("Url is not configured, abort");
            return null;
        }

        final String requestBody;
        try {
            requestBody = MapperUtils.MAPPER.get().writeValueAsString(notify);
        } catch (JsonProcessingException e) {
            logger.error("Transform Notify {} to JSON failed", notify, e);
            return null;
        }

        long timestamp = System.currentTimeMillis();
        String digest = digest(accessChannel.getCode() + requestBody + accessChannel.getSecurityKey() + timestamp);

        Map<String, Object> uriVars = new HashMap<>();
        uriVars.put("digest", digest);
        uriVars.put("timestamp", timestamp);

        T ret;

        try {
            ret = restTemplate.execute(accessChannel.getNotifyUrl() + "?digest={digest}&timestamp={timestamp}", HttpMethod.POST, new RequestCallback() {
                @Override
                public void doWithRequest(ClientHttpRequest request) throws IOException {
                    request.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
                    Writer writer = new OutputStreamWriter(request.getBody(), "UTF-8");
                    writer.write(requestBody);
                    writer.flush();
                }
            }, Jackson2JsonOnlyResponseExtractor.getInstance(responseType), uriVars);
        } catch (Exception e) {
            logger.error("Execute notify request failed", e);
            return null;
        }

        logger.info("Result:{}", ret);

        return ret;
    }

    /**
     * @param event
     *         订单变更事件
     */
    @EventListener(condition = "#event.original == true")
    public void onOrderStatusChange(final OrderStatusChanged event) {
        final OrderEventNotifyPayload payload = new OrderEventNotifyPayload();
        try {
            firstMaintenaceChangeCarInfo(event.getNewEntity());
        } catch (RuntimeException e) {
            logger.error("更新车辆首保失败");
        }
        switch (event.getNewEntity().getStatus()) {
            case PAYED:
                payload.setPayTime(orderStatusHisDao.selectLatest(event.getNewEntity().getId(), OrderStatus.PAYED).getModifyTime());
                break;
            case SERVICED:
                payload.setServiceTime(orderStatusHisDao.selectLatest(event.getNewEntity().getId(), OrderStatus.SERVICED).getModifyTime());
                break;

            case EVALUATED:
                payload.setEvaluateTime(orderStatusHisDao.selectLatest(event.getNewEntity().getId(), OrderStatus.EVALUATED).getModifyTime());
                break;

            case CONFIRMED:
                payload.setConfirmTime(orderStatusHisDao.selectLatest(event.getNewEntity().getId(), OrderStatus.CONFIRMED).getModifyTime());
                break;

            case REFUNDED:
                payload.setRefundTime(orderStatusHisDao.selectLatest(event.getNewEntity().getId(), OrderStatus.REFUNDED).getModifyTime());
                break;
            default:
                return;

        }

        payload.setStatus(event.getNewEntity().getStatus());
        payload.setOrderNo(event.getNewEntity().getOrderNo());
        payload.setPrice(event.getNewEntity().getMoney());
        payload.setVin(carDao.selectById(event.getNewEntity().getCarId()).getVin());

        final Notify<OrderEventNotifyPayload> notify = new Notify<>();
        notify.setMsgType(NotifyMsgType.OrderEvent);
        notify.setPayload(payload);

        final Runnable task = new Runnable() {

            @Override
            public void run() {
                List<UserAccessChannel> userAccessChannels = userDao.selectAccessChannelUserChannelList(event.getNewEntity().getUserId());
                for (UserAccessChannel userAccessChannel : userAccessChannels) {
                    payload.setUserId(userAccessChannel.getUserIdOfAccessChannel());

                    AccessChannel accessChannel = accessChannelDao.selectById(userAccessChannel.getAccessChannelId());
                    postForNotify(accessChannel, notify, NotifyResponse.class);
                }
            }

        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {

                @Override
                public void afterCommit() {
                    taskExecutor.execute(task);
                }
            });

        } else {
            taskExecutor.execute(task);
        }
    }

    /**
     * 首保后改变车辆信息
     *
     * @param order
     */
    private void firstMaintenaceChangeCarInfo(Order order) {
        Car car = carDao.selectById(order.getCarId());
        if (order.getId().compareTo(car.getFirstOrderId()) == 0) {
            if (order.getStatus() == OrderStatus.CANCELED || order.getStatus() == OrderStatus.REFUNDED || order.getStatus() == OrderStatus.REFUNDED_OFF_LINE) {
                car.setFirstOrderId(null);
                car.setFirstMaintenanceTime(null);
                car.setFirstMaintenanceMoney(null);
                car.setFirstOrderStatus(null);
            } else {
                car.setFirstOrderStatus(order.getStatus());
            }
            if(order.getStatus() == OrderStatus.SERVICED){
                car.setFirstMaintenanceTime(new Date());
            }
            carDao.update(car);
        }
    }

}
