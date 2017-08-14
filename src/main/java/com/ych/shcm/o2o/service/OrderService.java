package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.core.model.SystemParameterHolder;
import com.ych.shcm.o2o.dao.CarDao;
import com.ych.shcm.o2o.dao.CarModelDao;
import com.ych.shcm.o2o.dao.OrderAppointmentDao;
import com.ych.shcm.o2o.dao.OrderBillDao;
import com.ych.shcm.o2o.dao.OrderDao;
import com.ych.shcm.o2o.dao.OrderEvaluationDao;
import com.ych.shcm.o2o.dao.OrderServicePackDao;
import com.ych.shcm.o2o.dao.OrderServicePackItemDao;
import com.ych.shcm.o2o.dao.OrderStatusHisDao;
import com.ych.shcm.o2o.dao.PayOrderDao;
import com.ych.shcm.o2o.dao.ServiceItemDao;
import com.ych.shcm.o2o.dao.ServiceProviderDao;
import com.ych.shcm.o2o.dao.ServiceProviderSettleDao;
import com.ych.shcm.o2o.dao.ShopDao;
import com.ych.shcm.o2o.dao.ShopSettleDao;
import com.ych.shcm.o2o.dao.UserCarDao;
import com.ych.shcm.o2o.event.OrderStatusChanged;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.CarModel;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.Order;
import com.ych.shcm.o2o.model.OrderAppointment;
import com.ych.shcm.o2o.model.OrderBill;
import com.ych.shcm.o2o.model.OrderEvaluation;
import com.ych.shcm.o2o.model.OrderServicePack;
import com.ych.shcm.o2o.model.OrderServicePackItem;
import com.ych.shcm.o2o.model.OrderStatus;
import com.ych.shcm.o2o.model.OrderStatusCount;
import com.ych.shcm.o2o.model.OrderStatusHis;
import com.ych.shcm.o2o.model.PayOrder;
import com.ych.shcm.o2o.model.ServiceItem;
import com.ych.shcm.o2o.model.ServiceItemType;
import com.ych.shcm.o2o.model.ServicePack;
import com.ych.shcm.o2o.model.ServiceProvider;
import com.ych.shcm.o2o.model.ServiceProviderSettleDate;
import com.ych.shcm.o2o.model.ServiceProviderSettleDateSummary;
import com.ych.shcm.o2o.model.ServiceProviderSettleDetail;
import com.ych.shcm.o2o.model.ServiceProviderSettleStatus;
import com.ych.shcm.o2o.model.Shop;
import com.ych.shcm.o2o.model.ShopSettleDate;
import com.ych.shcm.o2o.model.ShopSettleDateSummary;
import com.ych.shcm.o2o.model.ShopSettleDetail;
import com.ych.shcm.o2o.model.ShopSettleStatus;
import com.ych.shcm.o2o.model.UserCar;
import com.ych.shcm.o2o.parameter.QueryOrderAppointmentListParameter;
import com.ych.shcm.o2o.parameter.QueryOrderListParameter;
import com.ych.shcm.o2o.service.systemparamholder.ServiceProviderIncomesRate;
import com.ych.shcm.o2o.service.systemparamholder.ServiceProviderSettleDelay;
import com.ych.shcm.o2o.service.systemparamholder.ServicedOrderTimeoutDay;
import com.ych.shcm.o2o.service.systemparamholder.ShopSettleDelay;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单服务
 * Created by mxp on 2017/7/15.
 */
@Lazy
@Component("shcm.o2o.service.OrderService")
public class OrderService {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderAppointmentDao orderAppointmentDao;

    @Autowired
    private OrderServicePackDao orderServicePackDao;

    @Autowired
    private OrderServicePackItemDao orderServicePackItemDao;

    @Autowired
    private OrderStatusHisDao orderStatusHisDao;

    @Autowired
    private OrderEvaluationDao orderEvaluationDao;

    @Autowired
    private ShopSettleDelay shopSettleDelay;

    @Autowired
    private ServiceProviderSettleDelay serviceProviderSettleDelay;

    @Resource(name = ServiceProviderIncomesRate.NAME)
    private SystemParameterHolder serviceProviderIncomesRate;

    @Autowired
    private ShopSettleDao shopSettleDao;

    @Autowired
    private ServiceProviderSettleDao serviceProviderSettleDao;

    @Autowired
    private PayOrderDao payOrderDao;

    @Autowired
    private ServiceItemDao serviceItemDao;

    @Autowired
    private CarModelDao carModelDao;

    @Autowired
    private CarService carService;

    @Autowired
    private CarDao carDao;

    @Value("${system.common.serverNo}")
    private String serverNo;

    private AtomicInteger flowNoSeq = new AtomicInteger();

    @Autowired
    private OrderBillDao orderBillDao;

    @Autowired
    private ShopDao shopDao;
    
    @Autowired
    private ApplicationContext appCtx;
    
    @Autowired
    private ServiceProviderDao serviceProviderDao;

    @Autowired
    private UserCarDao userCarDao;
    
    @Resource(name = ServicedOrderTimeoutDay.NAME)
    private SystemParameterHolder servicedOrderTimeoutDay;

    /**
     * 通过ID取得订单详情
     *
     * @param id
     *         订单ID
     * @return 订单实体
     */
    public Order getById(BigDecimal id) {
        return orderDao.selectById(id);
    }

    /**
     * 通过订单号取得订单详情
     *
     * @param orderNo
     *         订单号
     * @return 订单实体
     */
    public Order getByNo(String orderNo) {
        return orderDao.selectByNo(orderNo);
    }

    /**
     * 生成新订单
     *
     * @param order
     *         订单实体
     * @return 差入行
     */
    @Transactional
    public CommonOperationResultWidthData<Map<String, Object>> createOrder(Order order) {
        CommonOperationResultWidthData<Map<String, Object>> ret = new CommonOperationResultWidthData();
        Map<String, Object> map = new HashMap<>();
        Car car = carDao.selectById(order.getCarId());

        UserCar userCar = userCarDao.selectUserCarByUserIdAndCarId(order.getUserId(), order.getCarId());

        String orderNo = generateOrderNo();
        order.setOrderNo(orderNo);
        List<ServicePack> servicePacks = carService.getServicePackOfCar(car.getModelId());

        try {
            Assert.notNull(order.getCarId(), appCtx.getMessage("car.id.required", null, Locale.getDefault()));
            Assert.notEmpty(order.getOrderServicePacks(), appCtx.getMessage("servicePack.required", null, Locale.getDefault()));
            Assert.notNull(car, appCtx.getMessage("car.id.notExists", null, Locale.getDefault()));
            Assert.notNull(order.getUserId(), appCtx.getMessage("user.id.required", null, Locale.getDefault()));
            Assert.notEmpty(servicePacks, appCtx.getMessage("carModel.no.service.use", null, Locale.getDefault()));
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        boolean isFirstMaintenance = false;
        if (car.getFirstOrderId() == null) {
            isFirstMaintenance = true;
        } else {
            if (car.getFirstOrderStatus() == OrderStatus.UNPAYED || car.getFirstOrderStatus() == OrderStatus.PAYED) {
                ret.setResult(CommonOperationResult.IllegalOperation);
                ret.setDescription(appCtx.getMessage("car.firstMaintenance.notDone", null, Locale.getDefault()));
                return ret;
            }
        }

        List<OrderServicePack> forInsertPack = new ArrayList<>();
        List<OrderServicePackItem> forInsertItem = new ArrayList<>();
        BigDecimal orderPrice = BigDecimal.ZERO;

        for (OrderServicePack pack : order.getOrderServicePacks()) {
            ServicePack servicePack = null;
            if (isFirstMaintenance) {
                if (pack.getServicePackId().compareTo(servicePacks.get(0).getId()) != 0) {
                    ret.setResult(CommonOperationResult.IllegalArguments);
                    ret.setDescription(appCtx.getMessage("car.firstMaintenance.required", null, Locale.getDefault()));
                    return ret;
                }
                servicePack = servicePacks.get(0);
            } else {
                for (ServicePack servicePackIn : servicePacks) {
                    if (pack.getServicePackId().compareTo(servicePackIn.getId()) == 0) {
                        servicePack = servicePackIn;
                    }
                }
            }
            if (servicePack == null) {
                ret.setResult(CommonOperationResult.IllegalArguments);
                ret.setDescription(appCtx.getMessage("servicePack.id.notExists", null, Locale.getDefault()));
                return ret;
            }
            OrderServicePack orderServicePack = new OrderServicePack();
            orderServicePack.setName(servicePack.getName());
            orderServicePack.setIconPath(servicePack.getIconPath());

            orderServicePack.setServicePackId(servicePack.getId());
            forInsertPack.add(orderServicePack);
            BigDecimal packPrice = BigDecimal.ZERO;
            List<ServiceItem> items = serviceItemDao.selectServiceItemsOfPack(servicePack.getId());
            for (ServiceItem theItem : items) {
                OrderServicePackItem orderServicePackItem = new OrderServicePackItem();
                forInsertItem.add(orderServicePackItem);
                orderServicePackItem.setIconPath(theItem.getIconPath());
                orderServicePackItem.setName(theItem.getName());
                orderServicePackItem.setServiceItemId(theItem.getId());
                orderServicePackItem.setPrice(theItem.getPrice());
                orderServicePackItem.setServiceItemId(theItem.getId());
                if (theItem.getType() == ServiceItemType.NORMAL) {
                    packPrice = packPrice.add(theItem.getPrice());
                    orderServicePackItem.setNum(1);
                } else {
                    CarModel carModel = carModelDao.selectById(car.getModelId(), false);
                    int num = carModel.getEngineOilCapacity().divide(BigDecimal.ONE, 0, BigDecimal.ROUND_UP).intValue();
                    if (num < 4) {
                        num = 4;
                    }
                    orderServicePackItem.setNum(num);
                    packPrice = packPrice.add(theItem.getPrice().multiply(new BigDecimal(num)));
                }
            }
            orderPrice = orderPrice.add(packPrice);
            orderServicePack.setPrice(packPrice);
            orderServicePack.setOrderServicePackItems(forInsertItem);
        }
        order.setStatus(OrderStatus.UNPAYED);
        order.setMoney(orderPrice);
        orderDao.insert(order);
        if (isFirstMaintenance) {
            car.setFirstOrderId(order.getId());
            car.setFirstMaintenanceMoney(order.getMoney());
            car.setFirstOrderStatus(OrderStatus.UNPAYED);
            carDao.update(car);
        }
        for (OrderServicePack orderServicePack : forInsertPack) {
            orderServicePack.setOrderId(order.getId());
            orderServicePackDao.insert(orderServicePack);
            for (OrderServicePackItem orderServicePackItem : orderServicePack.getOrderServicePackItems()) {
                orderServicePackItem.setOrderServicePackId(orderServicePack.getId());
                orderServicePackItemDao.insert(orderServicePackItem);
            }
        }
        map.put("orderId", order.getId());
        map.put("orderNo", order.getOrderNo());
        ret.setData(map);
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 取消订单
     *
     * @param orderId
     *         订单ID
     */
    @Transactional
    public CommonOperationResultWidthData cancelOrder(BigDecimal orderId) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData<>();
        Order old = orderDao.selectById(orderId);
        Order oldEventEntity = ObjectUtils.clone(old);
        if (old == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(appCtx.getMessage("order.id.notExists", null, Locale.getDefault()));
            return ret;
        }
        OrderStatus oldStatus = old.getStatus();
        if (oldStatus == OrderStatus.UNPAYED) {
            old.setStatus(OrderStatus.CANCELED);
            orderDao.update(old);

            OrderStatusHis orderStatusHis = new OrderStatusHis();
            orderStatusHis.setOrderId(old.getId());
            orderStatusHis.setOldStatus(oldStatus);
            orderStatusHis.setStatus(OrderStatus.CANCELED);
            orderStatusHis.setModifierId(old.getModifierId());
            orderStatusHisDao.insert(orderStatusHis);
        } else {
            ret.setResult(CommonOperationResult.Failed);
            return ret;
        }
        ret.setResult(CommonOperationResult.Succeeded);

        appCtx.publishEvent(new OrderStatusChanged(oldEventEntity, old));

        return ret;
    }

    /**
     * 确认服务
     *
     * @param orderId
     *         订单ID
     */
    @Transactional
    public CommonOperationResultWidthData confirmService(BigDecimal orderId) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData<>();
        Order old = orderDao.selectById(orderId);
        Order oldEventEntity = ObjectUtils.clone(old);
        if (old == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(appCtx.getMessage("order.id.notExists", null, Locale.getDefault()));
            return ret;
        }
        OrderStatus oldStatus = old.getStatus();
        if (oldStatus == OrderStatus.SERVICED) {
            old.setStatus(OrderStatus.CONFIRMED);
            orderDao.update(old);

            OrderStatusHis orderStatusHis = new OrderStatusHis();
            orderStatusHis.setOrderId(old.getId());
            orderStatusHis.setOldStatus(oldStatus);
            orderStatusHis.setStatus(OrderStatus.CONFIRMED);
            orderStatusHis.setModifierId(old.getModifierId());
            orderStatusHisDao.insert(orderStatusHis);
        } else {
            ret.setResult(CommonOperationResult.Failed);
            return ret;
        }
        ret.setResult(CommonOperationResult.Succeeded);

        appCtx.publishEvent(new OrderStatusChanged(oldEventEntity, old));

        return ret;
    }

    /**
     * 对订单进行服务操作
     *
     * @param orderId
     *         订单ID
     * @param mileage
     *         车辆里程
     */
    @Transactional(Constants.TRANSACTION_MANAGER)
    public CommonOperationResultWidthData serviceTheOrder(BigDecimal orderId, long mileage, BigDecimal shopId) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData<>();
        Order old = orderDao.selectById(orderId);
        Order oldEventEntity = ObjectUtils.clone(old);
        if (old == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(appCtx.getMessage("order.id.notExists", null, Locale.getDefault()));
            return ret;
        }
        OrderStatus oldStatus = old.getStatus();
        if (oldStatus == OrderStatus.PAYED) {
            old.setStatus(OrderStatus.SERVICED);
            old.setShopId(shopId);
            ServiceProvider serviceProvider = serviceProviderDao.selectByBusinessAreaId(shopDao.selectById(shopId).getAreaId());
            old.setServiceProviderId(serviceProvider.getId());
            old.setMileage(mileage);
            if (orderDao.update(old) <= 0) {
                ret.setResult(CommonOperationResult.Failed);
                ret.setDescription(appCtx.getMessage("order.statusChange", null, Locale.getDefault()));
            }
            OrderStatusHis orderStatusHis = new OrderStatusHis();
            orderStatusHis.setOrderId(old.getId());
            orderStatusHis.setOldStatus(oldStatus);
            orderStatusHis.setStatus(OrderStatus.SERVICED);
            orderStatusHis.setModifierId(old.getModifierId());
            orderStatusHisDao.insert(orderStatusHis);
            addSettleMoney(old);
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription(appCtx.getMessage("order.status.illegal", null, Locale.getDefault()));
            return ret;

        }

        appCtx.publishEvent(new OrderStatusChanged(oldEventEntity, old));

        return ret;
    }

    /**
     * 生成订单号
     *
     * @return
     */
    private String generateOrderNo() {
        return "O" + DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmssSSS") + StringUtils.leftPad(serverNo, 3, '0') + StringUtils.leftPad(String.valueOf(Math.abs(flowNoSeq.incrementAndGet() % 10000)), 4, '0');
    }

    /**
     * 分页获取订单列表
     *
     * @param parameter
     *         取得条件
     * @return 分页列表
     */
    public PagedList<Order> getOrderPageList(QueryOrderListParameter parameter) {
        return orderDao.selectOrderList(parameter);
    }

    /**
     * 新增预约
     *
     * @param orderAppointment
     *         预约实体
     * @return 结果
     */
    public CommonOperationResultWidthData addAppointment(OrderAppointment orderAppointment) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        try {
            Assert.notNull(orderAppointment, appCtx.getMessage("order.appointment.required", null, Locale.getDefault()));
            Assert.notNull(orderAppointment.getOrderId(), appCtx.getMessage("order.id.required", null, Locale.getDefault()));
            Assert.notNull(orderAppointment.getAppointedTime(), appCtx.getMessage("order.appointment.time.required", null, Locale.getDefault()));
            Assert.hasLength(orderAppointment.getPhone(), appCtx.getMessage("order.appointment.phone.required", null, Locale.getDefault()));
            Assert.notNull(orderAppointment.getShopId(), appCtx.getMessage("shop.id.required", null, Locale.getDefault()));
            Assert.hasLength(orderAppointment.getPtc(), appCtx.getMessage("order.appointment.ptc.required", null, Locale.getDefault()));
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }

        if (orderAppointmentDao.insert(orderAppointment) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;

    }

    /**
     * 取得门店的预约列表
     *
     * @param parameter
     *         查询参数
     * @return 订单预约列表
     */
    public PagedList<OrderAppointment> getAppointmentList(QueryOrderAppointmentListParameter parameter) {
        return orderAppointmentDao.selectOrderAppointmentList(parameter);

    }

    /**
     * 根据ID取得订单预约
     *
     * @param id
     *         订单预约ID
     * @return 订单预约
     */
    public OrderAppointment getOrderAppointmentById(BigDecimal id) {
        return orderAppointmentDao.selectById(id);
    }

    /**
     * 订单评价
     *
     * @param orderEvaluation
     */
    @Transactional
    public CommonOperationResultWidthData evaluateOrder(OrderEvaluation orderEvaluation) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData<>();

        try {
            Assert.notNull(orderEvaluation, appCtx.getMessage("order.evaluation.required", null, Locale.getDefault()));
            Assert.notNull(orderEvaluation.getOrderId(), appCtx.getMessage("order.id.required", null, Locale.getDefault()));
            Assert.notNull(orderEvaluation.getSkill(), appCtx.getMessage("order.evaluation.skill.required", null, Locale.getDefault()));
            Assert.notNull(orderEvaluation.getAttitude(), appCtx.getMessage("order.evaluation.attitude.required", null, Locale.getDefault()));
            Assert.notNull(orderEvaluation.getEfficiency(), appCtx.getMessage("order.evaluation.efficiency.required", null, Locale.getDefault()));
            Assert.notNull(orderEvaluation.getEnvironment(), appCtx.getMessage("order.evaluation.environment.required", null, Locale.getDefault()));
            Assert.notNull(orderEvaluation.getOverallEvaluation(), appCtx.getMessage("order.evaluation.overall.required", null, Locale.getDefault()));

        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        Order old = orderDao.selectById(orderEvaluation.getOrderId());
        if (old == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(appCtx.getMessage("order.id.notExists", null, Locale.getDefault()));
            return ret;
        }
        Order oldEventEntity = ObjectUtils.clone(old);
        OrderStatus oldStatus = old.getStatus();
        if (oldStatus == OrderStatus.CONFIRMED) {

            old.setStatus(OrderStatus.EVALUATED);

            if (orderDao.update(old) <= 0) {
                ret.setResult(CommonOperationResult.Failed);
                ret.setDescription(appCtx.getMessage("system.common.operationFailed", null, Locale.getDefault()));
                return ret;
            }
            OrderStatusHis orderStatusHis = new OrderStatusHis();
            orderStatusHis.setOrderId(old.getId());
            orderStatusHis.setOldStatus(oldStatus);
            orderStatusHis.setStatus(OrderStatus.EVALUATED);
            orderStatusHis.setModifierId(old.getModifierId());
            if (orderStatusHisDao.insert(orderStatusHis) <= 0) {
                throw new RuntimeException(appCtx.getMessage("system.common.operationFailed", null, Locale.getDefault()));
            }
        } else {
            ret.setResult(CommonOperationResult.IllegalOperation);
            ret.setDescription(appCtx.getMessage("order.statusChange", null, Locale.getDefault()));
            return ret;
        }
        if (orderEvaluationDao.insert(orderEvaluation) < 0) {
            throw new RuntimeException(appCtx.getMessage("system.common.operationFailed", null, Locale.getDefault()));
        }

        Order order = orderDao.selectById(orderEvaluation.getOrderId());
        Shop shop = shopDao.selectDetailById(order.getShopId());
        int count = shop.getEvaluationCount();
        shop.setEvaluationCount(count + 1);
        shop.setAverageScore((shop.getAverageScore() * count + orderEvaluation.getAverage().doubleValue()) / (count + 1));

        if (shopDao.update(shop) <= 0) {
            throw new RuntimeException(appCtx.getMessage("system.common.operationFailed", null, Locale.getDefault()));
        }
        ret.setResult(CommonOperationResult.Succeeded);

        appCtx.publishEvent(new OrderStatusChanged(oldEventEntity, old));

        return ret;
    }

    /**
     * 增加结算金额
     */
    private void addSettleMoney(Order order) {
        BigDecimal spIncomesRate = serviceProviderIncomesRate.getDecimalValue();

        PayOrder payOrder = payOrderDao.selectPayedByOrderId(order.getId());

        BigDecimal channelFee = payOrderDao.selectOrderByOrderId(payOrder.getId(), order.getId()).getChannelFee();
        BigDecimal incomes = order.getMoney().subtract(channelFee);
        BigDecimal brokerage = calculatedBrokerage(order);

        BigDecimal shopMoney = order.getMoney().subtract(brokerage);
        BigDecimal shopChannelFee = shopMoney.multiply(channelFee).divide(order.getMoney(), new MathContext(2, RoundingMode.UP));
        BigDecimal shopIncomes = shopMoney.subtract(shopChannelFee);

        BigDecimal spAndPlatformChannelFee = channelFee.subtract(shopChannelFee);
        BigDecimal spBrokerage = brokerage.multiply(spIncomesRate).divide(BigDecimal.valueOf(100), new MathContext(2, RoundingMode.DOWN));
        BigDecimal spChannelFee = spBrokerage.multiply(spAndPlatformChannelFee).divide(brokerage, new MathContext(2, RoundingMode.UP));
        BigDecimal spIncomes = spBrokerage.subtract(spChannelFee);

        BigDecimal platformChannelFee = channelFee.subtract(shopChannelFee).subtract(spChannelFee);
        BigDecimal platformIncomes = incomes.subtract(shopIncomes).subtract(spIncomes);

        // 更新店铺结算数据
        int shopSettleDelayVar = shopSettleDelay.getIneterValue();
        Calendar shopSettleCalendar = Calendar.getInstance();
        shopSettleCalendar.add(Calendar.DATE, shopSettleDelayVar);
        Date shopSettleDay = shopSettleCalendar.getTime();

        ShopSettleDate shopSettleDate = new ShopSettleDate();
        shopSettleDate.setShopId(order.getShopId());
        shopSettleDate.setSettleDate(shopSettleDay);
        shopSettleDate.setChannelFee(channelFee);
        shopSettleDate.setOrderPrice(order.getMoney());
        shopSettleDate.setShopIncomes(shopIncomes);
        shopSettleDate.setShopChannelFee(shopChannelFee);
        shopSettleDate.setServiceProviderIncomes(spIncomes);
        shopSettleDate.setServiceProviderChannelFee(spChannelFee);
        shopSettleDate.setPlatformIncomes(platformIncomes);
        shopSettleDate.setPlatformChannelFee(platformChannelFee);
        shopSettleDate.setServiceProviderId(order.getServiceProviderId());
        shopSettleDate.setStatus(ShopSettleStatus.UNSETTLED);
        ShopSettleDate existsDate = shopSettleDao.selectDateByShopIdAndSettleDate(shopSettleDate.getShopId(), shopSettleDay);
        if (existsDate == null) {
            shopSettleDao.insertDate(shopSettleDate);
        } else {
            shopSettleDao.addDateMoney(shopSettleDate);
        }

        ShopSettleDateSummary shopSettleDateSummary = new ShopSettleDateSummary();
        shopSettleDateSummary.setSettleDate(shopSettleDay);
        shopSettleDateSummary.setChannelFee(channelFee);
        shopSettleDateSummary.setOrderPrice(order.getMoney());
        shopSettleDateSummary.setShopIncomes(shopIncomes);
        shopSettleDateSummary.setShopChannelFee(shopChannelFee);
        shopSettleDateSummary.setServiceProviderIncomes(spIncomes);
        shopSettleDateSummary.setServiceProviderChannelFee(spChannelFee);
        shopSettleDateSummary.setPlatformIncomes(platformIncomes);
        shopSettleDateSummary.setPlatformChannelFee(platformChannelFee);
        shopSettleDateSummary.setStatus(ShopSettleStatus.UNSETTLED);
        ShopSettleDateSummary existsSummary = shopSettleDao.selectSummaryBySettleDate(shopSettleDay);
        if (existsSummary == null) {
            shopSettleDao.insertSummary(shopSettleDateSummary);
        } else {
            shopSettleDao.addSummaryMoney(shopSettleDateSummary);
        }

        ShopSettleDetail shopSettleDetail = new ShopSettleDetail();
        shopSettleDetail.setOrderId(order.getId());
        shopSettleDetail.setPayOrderId(payOrder.getId());
        shopSettleDetail.setShopId(order.getShopId());
        shopSettleDetail.setSettleDate(shopSettleDay);
        shopSettleDetail.setChannelFee(channelFee);
        shopSettleDetail.setOrderPrice(order.getMoney());
        shopSettleDetail.setShopIncomes(shopIncomes);
        shopSettleDetail.setShopChannelFee(shopChannelFee);
        shopSettleDetail.setServiceProviderIncomes(spIncomes);
        shopSettleDetail.setServiceProviderChannelFee(spChannelFee);
        shopSettleDetail.setPlatformIncomes(platformIncomes);
        shopSettleDetail.setPlatformChannelFee(platformChannelFee);
        shopSettleDetail.setServiceProviderId(order.getServiceProviderId());
        shopSettleDao.insertDetail(shopSettleDetail);

        // 更新服务商结算数据
        int serviceProviderSettleDelayVar = serviceProviderSettleDelay.getIneterValue();
        Calendar spSettleCalendar = Calendar.getInstance();
        spSettleCalendar.add(Calendar.DATE, serviceProviderSettleDelayVar);
        Date spSettleDay = spSettleCalendar.getTime();

        ServiceProviderSettleDate spSettleDate = new ServiceProviderSettleDate();
        spSettleDate.setSettleDate(shopSettleDay);
        spSettleDate.setChannelFee(channelFee);
        spSettleDate.setOrderPrice(order.getMoney());
        spSettleDate.setShopIncomes(shopIncomes);
        spSettleDate.setShopChannelFee(shopChannelFee);
        spSettleDate.setServiceProviderIncomes(spIncomes);
        spSettleDate.setServiceProviderChannelFee(spChannelFee);
        spSettleDate.setPlatformIncomes(platformIncomes);
        spSettleDate.setPlatformChannelFee(platformChannelFee);
        spSettleDate.setServiceProviderId(order.getServiceProviderId());
        spSettleDate.setStatus(ServiceProviderSettleStatus.UNSETTLED);
        ServiceProviderSettleDate existsSpDate = serviceProviderSettleDao.selectDateByServiceProviderIdAndSettleDate(order.getServiceProviderId(), spSettleDay);
        if (existsSpDate == null) {
            serviceProviderSettleDao.insertDate(spSettleDate);
        } else {
            serviceProviderSettleDao.addDateMoney(spSettleDate);
        }

        ServiceProviderSettleDateSummary spSettleDateSummary = new ServiceProviderSettleDateSummary();
        spSettleDateSummary.setSettleDate(shopSettleDay);
        spSettleDateSummary.setChannelFee(channelFee);
        spSettleDateSummary.setOrderPrice(order.getMoney());
        spSettleDateSummary.setShopIncomes(shopIncomes);
        spSettleDateSummary.setShopChannelFee(shopChannelFee);
        spSettleDateSummary.setServiceProviderIncomes(spIncomes);
        spSettleDateSummary.setServiceProviderChannelFee(spChannelFee);
        spSettleDateSummary.setPlatformIncomes(platformIncomes);
        spSettleDateSummary.setPlatformChannelFee(platformChannelFee);
        spSettleDateSummary.setStatus(ServiceProviderSettleStatus.UNSETTLED);
        ServiceProviderSettleDateSummary existsSpSummary = serviceProviderSettleDao.selectSummaryBySettleDate(spSettleDay);
        if (existsSpSummary == null) {
            serviceProviderSettleDao.insertSummary(spSettleDateSummary);
        } else {
            serviceProviderSettleDao.addSummaryMoney(spSettleDateSummary);
        }

        ServiceProviderSettleDetail spSettleDetail = new ServiceProviderSettleDetail();
        spSettleDetail.setOrderId(order.getId());
        spSettleDetail.setPayOrderId(payOrder.getId());
        spSettleDetail.setShopId(order.getShopId());
        spSettleDetail.setSettleDate(shopSettleDay);
        spSettleDetail.setChannelFee(channelFee);
        spSettleDetail.setOrderPrice(order.getMoney());
        spSettleDetail.setShopIncomes(shopIncomes);
        spSettleDetail.setShopChannelFee(shopChannelFee);
        spSettleDetail.setServiceProviderIncomes(spIncomes);
        spSettleDetail.setServiceProviderChannelFee(spChannelFee);
        spSettleDetail.setPlatformIncomes(platformIncomes);
        spSettleDetail.setPlatformChannelFee(platformChannelFee);
        spSettleDetail.setServiceProviderId(order.getServiceProviderId());
        serviceProviderSettleDao.insertDetail(spSettleDetail);
    }

    /**
     * 计算佣金
     *
     * @param order
     * @return
     */
    private BigDecimal calculatedBrokerage(Order order) {
        BigDecimal incomes = BigDecimal.ZERO;
        for (OrderServicePack orderServicePack : orderServicePackDao.selectByOrderId(order.getId(), true)) {
            for (OrderServicePackItem orderServicePackItem : orderServicePack.getOrderServicePackItems()) {

                ServiceItem item = serviceItemDao.selectById(orderServicePackItem.getServiceItemId());
                incomes = incomes.add(item.getBrokerage().multiply(BigDecimal.valueOf(orderServicePackItem.getNum())));
            }
        }
        return incomes;
    }

    /**
     * 取得订单分组数量 用户ID可为空
     *
     * @param userId
     *         用户id
     * @return 数量列表（根据OrderStatus依次排列）
     */
    public List<OrderStatusCount> getOrderCountGroupByType(BigDecimal userId) {

        return orderDao.selectOrderCountGroupByType(userId);
    }

    /**
     * 根据ID取得订单发票
     *
     * @param orderId
     *         订单ID
     * @return 订单发票
     */
    public OrderBill getOrderBillByOrderId(BigDecimal orderId) {
        return orderBillDao.selectByOrderId(orderId);
    }

    /**
     * 根据订单ID取得订单评价
     *
     * @param orderId
     *         订单ID
     * @return 订单评价
     */
    public OrderEvaluation getOrderEvaluationByOrderId(BigDecimal orderId) {
        return orderEvaluationDao.selectByOrderId(orderId);

    }

    /**
     * 取得订单下的服务
     *
     * @param orderId
     *         订单Id
     * @param needItems
     *         是否需要填充orderServiceItem
     * @return 服务项目
     */
    public List<OrderServicePack> getPackByOrderId(BigDecimal orderId, boolean needItems) {
        return orderServicePackDao.selectByOrderId(orderId, needItems);

    }

    /**
     * 新增订单发票申请
     *
     * @param orderBill
     *         订单发票
     * @return 订单发票
     */
    public CommonOperationResultWidthData addOrderBill(OrderBill orderBill) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        try {
            Assert.notNull(orderBill, appCtx.getMessage("order.bill.required", null, Locale.getDefault()));
            Assert.notNull(orderBill.getOrderId(), appCtx.getMessage("order.id.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getBank(), appCtx.getMessage("company.bank.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getBankAccount(), appCtx.getMessage("company.bankAccount.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getCompany(), appCtx.getMessage("company.name.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getCompanyAddr(), appCtx.getMessage("company.address.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getDeliverAddr(), appCtx.getMessage("order.bill.deliverAddr.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getTaxNo(), appCtx.getMessage("order.bill.taxNo.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getPtc(), appCtx.getMessage("order.bill.ptc.required", null, Locale.getDefault()));
            Assert.hasLength(orderBill.getPhone(), appCtx.getMessage("company.phone.required", null, Locale.getDefault()));

        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        OrderBill orderBill1 = orderBillDao.selectByOrderId(orderBill.getOrderId());
        if (orderBill1 != null) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription(appCtx.getMessage("order.bill.applied", null, Locale.getDefault()));
            return ret;
        }
        if (orderBillDao.insert(orderBill) <= 0) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription(appCtx.getMessage("system.common.operationFailed", null, Locale.getDefault()));
            return ret;
        } else {
            ret.setResult(CommonOperationResult.Succeeded);
            return ret;
        }
    }

    /**
     * 查询评价列表
     *
     * @param parameter
     *         查询参数
     * @return 订单评价
     */
    public PagedList<OrderEvaluation> getOrderEvaluationList(QueryOrderAppointmentListParameter parameter) {
        return orderEvaluationDao.selectOrderEvaluationList(parameter);
    }

    /**
     * 订单状态变更时要判断订单的车辆首次保养状态单据的状态变更
     *
     * @param event 订单状态变更事件
     */
    @EventListener(condition = "#event.original == true")
    public void onStatusChanged(OrderStatusChanged event) {
        Order order = event.getNewEntity();
        Car car = carDao.selectById(order.getCarId());

        if (car.getFirstOrderId().equals(order.getId())) {
            switch(order.getStatus()) {
                case CANCELED:
                case REFUNDED:
                case REFUNDED_OFF_LINE:
                case INVALID:
                    car.setFirstOrderId(null);
                    car.setFirstMaintenanceTime(null);
                    car.setFirstMaintenanceMoney(null);
                    car.setFirstOrderStatus(null);
                    break;

                case SERVICED:
                    car.setFirstOrderStatus(OrderStatus.SERVICED);
                    car.setFirstMaintenanceTime(orderStatusHisDao.selectLatest(order.getId(), OrderStatus.SERVICED).getModifyTime());
                    break;

                default:
                    car.setFirstOrderStatus(order.getStatus());
            }

            carDao.update(car);
        }

    }

    /**
     * 清理超期的已服务订单，将其状态变更为已确认.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional(Constants.TRANSACTION_MANAGER)
    public void clearOutdatedServicedOrder(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 999);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.add(Calendar.DATE, -servicedOrderTimeoutDay.getIneterValue());
        
        QueryOrderListParameter parameter = new QueryOrderListParameter();
        parameter.setStatus(Collections.singletonList(OrderStatus.SERVICED));
        parameter.setEndTime(calendar.getTime());
        parameter.setPageSize(1000);
        parameter.setPageIndex(0);
        
        for (PagedList<Order> pagedList = orderDao.selectOrderList(parameter); pagedList.getTotal() > 0; pagedList = orderDao.selectOrderList(parameter)) {
            if (CollectionUtils.isNotEmpty(pagedList.getList())) {
                for (Order order : pagedList.getList()) {
                    Order oldOrder = ObjectUtils.clone(order);

                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setModifierId(BigDecimal.ZERO);
                    
                    if (orderDao.update(order) > 0) {
                        OrderStatusHis orderStatusHis = new OrderStatusHis();
                        orderStatusHis.setOrderId(order.getId());
                        orderStatusHis.setOldStatus(OrderStatus.SERVICED);
                        orderStatusHis.setStatus(OrderStatus.CONFIRMED);
                        orderStatusHis.setModifierId(BigDecimal.ZERO);
                        orderStatusHisDao.insert(orderStatusHis);
                                
                        appCtx.publishEvent(new OrderStatusChanged(oldOrder, order));
                    }
                }
            }
            
            if (pagedList.getTotal() < 1000) {
                break;
            }
        }
    }



}
