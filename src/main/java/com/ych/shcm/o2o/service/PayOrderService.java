package com.ych.shcm.o2o.service;

import com.ych.core.model.BaseWithCommonOperationResult;
import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.SystemParameterHolder;
import com.ych.core.pay.ChannelOperationResult;
import com.ych.core.pay.CreatePayOrderParameter;
import com.ych.core.pay.CreatePayOrderParameterImpl;
import com.ych.core.pay.PaymentStrategy;
import com.ych.shcm.o2o.dao.CarDao;
import com.ych.shcm.o2o.dao.OrderDao;
import com.ych.shcm.o2o.dao.OrderStatusHisDao;
import com.ych.shcm.o2o.dao.PayFeeConfigDao;
import com.ych.shcm.o2o.dao.PayOrderDao;
import com.ych.shcm.o2o.dao.RefundOrderDao;
import com.ych.shcm.o2o.event.OrderStatusChanged;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.Order;
import com.ych.shcm.o2o.model.OrderStatus;
import com.ych.shcm.o2o.model.OrderStatusHis;
import com.ych.shcm.o2o.model.PayChannel;
import com.ych.shcm.o2o.model.PayFeeConfig;
import com.ych.shcm.o2o.model.PayOrder;
import com.ych.shcm.o2o.model.PayOrderOrder;
import com.ych.shcm.o2o.model.PayOrderStatus;
import com.ych.shcm.o2o.model.PayOrderStatusHistory;
import com.ych.shcm.o2o.model.RefundOrder;
import com.ych.shcm.o2o.model.RefundOrderStatus;
import com.ych.shcm.o2o.service.systemparamholder.PayOrderExpires;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 支付单服务
 */
@Lazy
@Component("shcm.o2o.service.PayOrderService")
public class PayOrderService {

    private Logger logger = LoggerFactory.getLogger(PayOrderService.class);

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderStatusHisDao orderStatusHisDao;

    @Autowired
    private PayOrderDao payOrderDao;

    @Autowired
    private PayFeeConfigDao payFeeConfigDao;

    @Autowired
    private RefundOrderDao refundOrderDao;

    @Value("${system.common.serverNo}")
    private String serverNo;

    private AtomicInteger flowNoSeq = new AtomicInteger();

    @Resource(name = "paymentStrategyMediator")
    private PaymentStrategy paymentStrategy;

    @Resource(name = PayOrderExpires.NAME)
    private SystemParameterHolder payOrderExpires;

    @Resource(name = Constants.TRANSACTION_TEMPLATE)
    private TransactionTemplate transactionTemplate;

    @Autowired
    private ApplicationContext context;
    @Autowired
    private CarDao carDao;

    /**
     * @return 生成新的流水号
     */
    private String generateFlowNo() {
        return "P" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmssSSS") + StringUtils.leftPad(serverNo, 3, '0') + StringUtils.leftPad(String.valueOf(Math.abs(flowNoSeq.incrementAndGet() % 10000)), 4, '0');
    }

    /**
     * @return 生成新退款的流水号
     */
    private String generateRefundFlowNo() {
        return "R" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmssSSS") + StringUtils.leftPad(serverNo, 3, '0') + StringUtils.leftPad(String.valueOf(Math.abs(flowNoSeq.incrementAndGet() % 10000)), 4, '0');
    }

    /**
     * @return 生成渠道退款的流水号, 某些支付渠道需要自己生成一个渠道退款流水号
     */
    private String generateChannelRefundFlowNo() {
        return "CR" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmssSSS") + StringUtils.leftPad(serverNo, 3, '0') + StringUtils.leftPad(String.valueOf(Math.abs(flowNoSeq.incrementAndGet() % 1000)), 3, '0');
    }

    private CommonOperationResultWidthData<ChannelOperationResult> clearPayOrder(BigDecimal orderId) {
        PayOrder payOrder = payOrderDao.selectUnpayedByOrderId(orderId);

        if (payOrder == null) {
            CommonOperationResultWidthData<ChannelOperationResult> ret = new CommonOperationResultWidthData<>();
            ret.setResult(CommonOperationResult.NotExists);
            return ret;
        }

        CommonOperationResultWidthData<ChannelOperationResult> ret = paymentStrategy.cancelPayOrder(payOrder.getPayChannel().name(), payOrder.getFlowNo(), payOrder.getPayChannelFlowNo(), null);

        if (ret.getResult() == CommonOperationResult.Succeeded) {
            PayOrderStatus oldPayOrderStatus = payOrder.getStatus();
            payOrder.setStatus(PayOrderStatus.CANCELED);
            payOrder.setResultCode(ret.getData().getErrorCode());
            payOrderDao.update(payOrder);

            PayOrderStatusHistory payOrderStatusHis = new PayOrderStatusHistory();
            payOrderStatusHis.setPayOrderId(payOrder.getId());
            payOrderStatusHis.setOldStatus(oldPayOrderStatus);
            payOrderStatusHis.setStatus(payOrder.getStatus());
            payOrderStatusHis.setChangeTime(Calendar.getInstance().getTime());
            payOrderDao.insertStatusHistory(payOrderStatusHis);
        }

        return ret;
    }

    private List<PayOrderOrder> fillChannelFee(PayOrder payOrder, Order... orders) {
        PayFeeConfig payFeeConfig = payFeeConfigDao.selectByPayChannel(payOrder.getPayChannel());

        payOrder.setChannelFeeRefundable(payFeeConfig.isRefundable());
        BigDecimal totalFee, orderFee;

        if (payFeeConfig.isFixed()) {
            totalFee = payFeeConfig.getFixedFee();
        } else {
            totalFee = payOrder.getPrice().multiply(payFeeConfig.getFeeRate()).divide(BigDecimal.valueOf(100), new MathContext(2));

            if (payFeeConfig.getBeginFee() != null && payFeeConfig.getBeginFee().compareTo(totalFee) > 0) {
                totalFee = payFeeConfig.getBeginFee();
            } else if (payFeeConfig.getFeeLimit() != null && payFeeConfig.getFeeLimit().compareTo(totalFee) < 0) {
                totalFee = payFeeConfig.getFeeLimit();
            }
        }
        payOrder.setChannelFee(totalFee);

        BigDecimal restFee = totalFee;
        List<PayOrderOrder> ret = new ArrayList<>();

        for (Order order : orders) {
            PayOrderOrder payOrderOrder = new PayOrderOrder();
            payOrderOrder.setOrderId(order.getId());

            orderFee = order.getMoney().divide(payOrder.getPrice()).multiply(totalFee, new MathContext(2));
            payOrderOrder.setChannelFee(orderFee);

            restFee = restFee.subtract(orderFee);
            ret.add(payOrderOrder);
        }

        if (restFee.compareTo(BigDecimal.ZERO) > 0) {
            PayOrderOrder last = ret.get(ret.size() - 1);
            last.setChannelFee(last.getChannelFee().add(restFee));
        }

        return ret;
    }

    /**
     * 根据订单ID创建支付单
     *
     * @param parameter
     *         创建参数
     * @param extendedParamters
     *         扩展参数
     * @return 操作结果
     */
    public CommonOperationResultWidthData<ChannelOperationResult> createPayOrder(CreatePayOrderParameter parameter, Map<String, Object> extendedParamters) {
        CommonOperationResultWidthData<ChannelOperationResult> ret = new CommonOperationResultWidthData<>();
        Order order = orderDao.selectByNo(parameter.getFlowNo());

        if (order == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(context.getMessage("order.orderNo.notExists", null, Locale.getDefault()));
            return ret;
        }

        if (order.getStatus() != OrderStatus.UNPAYED) {
            ret.setResult(CommonOperationResult.IllegalStatus);
            ret.setDescription(context.getMessage("order.status.illegal", null, Locale.getDefault()));
            return ret;
        }

        CommonOperationResultWidthData<ChannelOperationResult> clearResult = clearPayOrder(order.getId());
        if (clearResult.getResult() == CommonOperationResult.NotExists || clearResult.getResult() == CommonOperationResult.Succeeded) {
            CreatePayOrderParameterImpl createParameter = new CreatePayOrderParameterImpl();
            createParameter.setChannelName(parameter.getChannelName());
            createParameter.setFlowNo(generateFlowNo());
            createParameter.setOrderSubject(parameter.getOrderSubject());
            createParameter.setOrderDesc(parameter.getOrderDesc());
            createParameter.setTotalMoney(order.getMoney());
            createParameter.setTimeout(payOrderExpires.getLongValue());

            ret = paymentStrategy.createPayOrder(createParameter, extendedParamters);

            if (ret.getResult() == CommonOperationResult.Succeeded) {
                final PayOrder payOrder = new PayOrder();
                payOrder.setFlowNo(createParameter.getFlowNo());
                payOrder.setPayChannel(PayChannel.valueOf(createParameter.getChannelName()));
                payOrder.setPayChannelFlowNo(StringUtils.trimToEmpty(ret.getData().getChannelFlowNo()));
                payOrder.setStatus(PayOrderStatus.UNPAYED);
                payOrder.setPrice(order.getMoney());
                payOrder.setPrepayId(ret.getData().getPrepayChannelFlowNo());

                final List<PayOrderOrder> payOrderOrders = fillChannelFee(payOrder, order);

                try {
                    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            payOrderDao.insert(payOrder);

                            for (PayOrderOrder payOrderOrder : payOrderOrders) {
                                payOrderOrder.setPayOrderId(payOrder.getId());
                            }

                            payOrderDao.insertOrder(payOrderOrders);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Insert PayOrder failed", e);
                    ret = new CommonOperationResultWidthData<>();
                    ret.setResult(CommonOperationResult.Failed);
                    return ret;
                }
            }

            return ret;
        } else {
            return clearResult;
        }
    }

    /**
     * 退款
     *
     * @param orderNo
     *         订单号
     * @return 退款结果
     */
    public CommonOperationResultWidthData<ChannelOperationResult> refund(String orderNo, Map<String, Object> extendedParamters) {
        CommonOperationResultWidthData<ChannelOperationResult> ret = new CommonOperationResultWidthData<>();
        Order order = orderDao.selectByNo(orderNo);

        if (order == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(context.getMessage("order.orderNo.notExists", null, Locale.getDefault()));
            return ret;
        }

        if (order.getStatus() != OrderStatus.PAYED) {
            ret.setResult(CommonOperationResult.IllegalStatus);
            ret.setDescription(context.getMessage("system.common.illegalStatus", null, Locale.getDefault()));
            return ret;
        }

        PayOrder payOrder = payOrderDao.selectPayedByOrderId(order.getId());
        String channelRefundNo = paymentStrategy.isGenerateRefundNo(payOrder.getPayChannel().name()) ? generateChannelRefundFlowNo() : null;
        final String refundFlowNo = generateRefundFlowNo();
        ret = paymentStrategy.refund(payOrder.getPayChannel().name(), payOrder.getFlowNo(), refundFlowNo, payOrder.getPayChannelFlowNo(), channelRefundNo, extendedParamters);

        if (ret.getResult() == CommonOperationResult.Succeeded) {
            final RefundOrder refundOrder = new RefundOrder();
            refundOrder.setOrderId(order.getId());
            refundOrder.setFlowNo(refundFlowNo);
            refundOrder.setPayChannel(payOrder.getPayChannel());
            refundOrder.setPayChannelFlowNo(ret.getData().getChannelFlowNo());
            refundOrder.setRefundMoney(order.getMoney());
            refundOrder.setStatus(RefundOrderStatus.APPLY_SUCCESS);

            try {
                transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                    @Override
                    protected void doInTransactionWithoutResult(TransactionStatus status) {
                        refundOrderDao.insert(refundOrder);

                        Order order = orderDao.selectById(refundOrder.getOrderId());
                        Order oldOrder = ObjectUtils.clone(order);
                        order.setStatus(OrderStatus.REFUNDED);
                        order.setModifierId(BigDecimal.ZERO);
                        orderDao.update(order);
                        firstMaintenaceChangeCarInfo(order);
                        OrderStatusHis orderStatusHis = new OrderStatusHis();
                        orderStatusHis.setOrderId(order.getId());
                        orderStatusHis.setOldStatus(oldOrder.getStatus());
                        orderStatusHis.setStatus(OrderStatus.REFUNDED);
                        orderStatusHisDao.insert(orderStatusHis);

                        context.publishEvent(new OrderStatusChanged(oldOrder, order));
                    }
                });
            } catch (Exception e) {
                logger.error("Insert order status history failed", e);
            }
        }

        return ret;
    }

    /**
     * 支付结果通知
     *
     * @param flowNo
     *         流水号
     * @param channelFlowNo
     *         渠道流水号
     * @param payTime
     *         支付时间
     * @param payResult
     *         支付是否成功
     * @param errorCode
     *         渠道返回的错误码
     * @param errorDesc
     *         渠道返回的错误描述
     * @return 操作结果
     */
    @Transactional(Constants.TRANSACTION_MANAGER)
    public BaseWithCommonOperationResult payNotified(String flowNo, String channelFlowNo, Date payTime, boolean payResult, String errorCode, String errorDesc) {
        BaseWithCommonOperationResult ret = new BaseWithCommonOperationResult();
        PayOrder payOrder = payOrderDao.selectByFlowNo(flowNo);

        // 规避重复通知的情况
        if ((payResult && payOrder.getStatus() == PayOrderStatus.PAYED) ||
                (!payResult && payOrder.getStatus() == PayOrderStatus.FAILED)) {
            ret.setResult(CommonOperationResult.Succeeded);
            return ret;
        } else if (payResult && payOrder.getStatus() != PayOrderStatus.UNPAYED) {
            ret.setResult(CommonOperationResult.IllegalStatus);
            return ret;
        }

        Date currentTime = Calendar.getInstance().getTime();

        PayOrderStatus oldPayOrderStatus = payOrder.getStatus();
        payOrder.setStatus(payResult ? PayOrderStatus.PAYED : PayOrderStatus.FAILED);
        payOrder.setPayChannelFlowNo(channelFlowNo);
        payOrder.setResultCode(errorCode);
        payOrder.setResultMsg(errorDesc);
        payOrderDao.update(payOrder);

        PayOrderStatusHistory payOrderStatusHis = new PayOrderStatusHistory();
        payOrderStatusHis.setPayOrderId(payOrder.getId());
        payOrderStatusHis.setOldStatus(oldPayOrderStatus);
        payOrderStatusHis.setStatus(payOrder.getStatus());
        payOrderStatusHis.setChangeTime(payTime != null ? payTime : currentTime);
        payOrderDao.insertStatusHistory(payOrderStatusHis);

        if (payResult) {
            for (PayOrderOrder payOrderOrder : payOrderDao.selectOrderByPayOrderId(payOrder.getId())) {
                Order order = orderDao.selectById(payOrderOrder.getOrderId());
                Order oldOrder = ObjectUtils.clone(order);
                order.setStatus(OrderStatus.PAYED);
                order.setModifierId(BigDecimal.ZERO);
                orderDao.update(order);
                firstMaintenaceChangeCarInfo(order);
                OrderStatusHis orderStatusHis = new OrderStatusHis();
                orderStatusHis.setOrderId(order.getId());
                orderStatusHis.setOldStatus(oldOrder.getStatus());
                orderStatusHis.setStatus(OrderStatus.PAYED);
                orderStatusHis.setModifyTime(payTime);
                orderStatusHisDao.insert(orderStatusHis);

                context.publishEvent(new OrderStatusChanged(oldOrder, order));
            }
        }

        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 根据订单ID查找支付的支付单
     *
     * @param orderId
     *         订单ID
     * @return 支付单据
     */
    public PayOrder getPayedByOrderId(BigDecimal orderId) {
        return payOrderDao.selectPayedByOrderId(orderId);
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
            if (order.getStatus() == OrderStatus.SERVICED) {
                car.setFirstMaintenanceTime(new Date());
            }
            carDao.update(car);
        }
    }
}
