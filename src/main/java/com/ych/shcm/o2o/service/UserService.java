package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.shcm.o2o.dao.CarDao;
import com.ych.shcm.o2o.dao.CarModelDao;
import com.ych.shcm.o2o.dao.OrderDao;
import com.ych.shcm.o2o.dao.OrderStatusHisDao;
import com.ych.shcm.o2o.dao.UserCarDao;
import com.ych.shcm.o2o.dao.UserDao;
import com.ych.shcm.o2o.dao.UserThirdAuthDao;
import com.ych.shcm.o2o.event.OrderStatusChanged;
import com.ych.shcm.o2o.model.AccessChannel;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.CarExpiredMaintenanceInfo;
import com.ych.shcm.o2o.model.CarModel;
import com.ych.shcm.o2o.model.CarUserHistory;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.model.Order;
import com.ych.shcm.o2o.model.OrderStatus;
import com.ych.shcm.o2o.model.OrderStatusHis;
import com.ych.shcm.o2o.model.ThirdAuthType;
import com.ych.shcm.o2o.model.User;
import com.ych.shcm.o2o.model.UserAccessChannel;
import com.ych.shcm.o2o.model.UserCar;
import com.ych.shcm.o2o.model.UserThirdAuth;
import com.ych.shcm.o2o.openinf.GuaranteeRequestPayload;
import com.ych.shcm.o2o.openinf.IRequest;
import com.ych.shcm.o2o.openinf.IResponse;
import com.ych.shcm.o2o.openinf.RequestAction;
import com.ych.shcm.o2o.openinf.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 用户的服务
 * <p>
 * Created by U on 2017/7/18.
 */
@Lazy
@Component("shcm.o2o.service.UserService")
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserCarDao userCarDao;

    @Autowired
    private CarModelDao carModelDao;

    @Autowired
    private CarDao carDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderStatusHisDao orderStatusHisDao;

    @Autowired
    private UserThirdAuthDao userThirdAuthDao;

    @Autowired
    private AccessChannelService accessChannelService;

    /**
     * 事务模板
     */
    @Resource(name = Constants.TRANSACTION_TEMPLATE)
    private TransactionTemplate transactionTempalte;

    @Autowired
    private ApplicationContext context;
    @Autowired
    private MessageSource messageSource;

    @PostConstruct
    public void init() {
        accessChannelService.registerRequestExecutor(RequestAction.Guarantee, new ValidateAndAddCarAndUserInnerClass());
    }

    /**
     * 根据ID获取用户信息
     *
     * @param id
     *         ID
     * @return 用户信息
     */
    public User getUserById(BigDecimal id) {
        return userDao.selectById(id);
    }

    /**
     * 取得用户车辆列表
     *
     * @param userId
     *         取得参数
     * @return 分页数据
     */
    public List<CarModel> getCarsOfUser(BigDecimal userId) {

        return carDao.selectCarsOfUser(userId);
    }

    /**
     * 根据第三方ID查询第三方认证信息
     *
     * @param thirdAuthType
     *         第三方类型
     * @param thirdId
     *         第三方ID
     * @return 第三方认证信息
     */
    public UserThirdAuth getThirdAuthByByThirdId(ThirdAuthType thirdAuthType, String thirdId) {
        return userThirdAuthDao.selectByThirdId(thirdAuthType, thirdId);
    }

    /**
     * 验证和处理车辆和用户新增
     */
    public class ValidateAndAddCarAndUserInnerClass implements AccessChannelRequestExecutor {

        /**
         * 执行接入渠道提交的请求
         *
         * @param request
         *         请求对象
         * @param accessChannel
         *         访问渠道
         * @return 返回结果
         */
        @Override
        public IResponse<?> execute(final IRequest<?> request, final AccessChannel accessChannel) {
            final GuaranteeRequestPayload payload = (GuaranteeRequestPayload) request.getPayload();
            Response response = new Response();
            try {
                Assert.notNull(payload, messageSource.getMessage("accessChannel.payload.required", null, Locale.getDefault()));
                Assert.notNull(payload.getId(), messageSource.getMessage("accessChannel.userId.required", null, Locale.getDefault()));
                Assert.hasLength(payload.getName(), messageSource.getMessage("accessChannel.userName.required", null, Locale.getDefault()));
                Assert.hasLength(payload.getPhone(), messageSource.getMessage("accessChannel.userPhone.required", null, Locale.getDefault()));
                Assert.hasLength(payload.getVin(), messageSource.getMessage("accessChannel.vin.required", null, Locale.getDefault()));
                Assert.notNull(payload.getModelId(), messageSource.getMessage("accessChannel.car.modelId.required", null, Locale.getDefault()));
                Assert.notNull(payload.getEffectiveTime(), messageSource.getMessage("accessChannel.effectiveTime.required", null, Locale.getDefault()));
                Assert.notNull(payload.getExpires(), messageSource.getMessage("accessChannel.expires.required", null, Locale.getDefault()));
                Assert.notNull(accessChannel, messageSource.getMessage("accessChannel.required", null, Locale.getDefault()));
            } catch (IllegalArgumentException e) {
                response.setResult(CommonOperationResult.Failed.name());
                response.setResultMsg(e.getMessage());
                return response;
            }

            if (carModelDao.selectById(new BigDecimal(payload.getModelId()), false) == null) {
                response.setResult(CommonOperationResult.NotExists.name());
                response.setResultMsg(context.getMessage("carModel.notExists", null, Locale.getDefault()));
                return response;
            }

            payload.setVin(payload.getVin().toUpperCase());

            return transactionTempalte.execute(new TransactionCallback<Response>() {

                @Override
                public Response doInTransaction(TransactionStatus status) {
                    Response responseIn = new Response();

                    try {
                        UserAccessChannel userAccessChannel = userDao.selectAccessChannelByChannelId(accessChannel.getId(), payload.getId());
                        User user;

                        if (userAccessChannel == null) {
                            user = userDao.selectByPhone(payload.getPhone());

                            if (user != null) {
                                UserAccessChannel existedUserAccessChannel = userDao.selectAccessChannelByUserChannel(user.getId(), accessChannel.getId());
                                if (existedUserAccessChannel == null) {
                                    userAccessChannel = new UserAccessChannel();
                                    userAccessChannel.setUserId(user.getId());
                                    userAccessChannel.setAccessChannelId(accessChannel.getId());
                                    userAccessChannel.setUserIdOfAccessChannel(payload.getId());
                                    userDao.insertAccessChannel(userAccessChannel);
                                } else {
                                    responseIn.setResult(CommonOperationResult.Existed.name());
                                    responseIn.setResultMsg(context.getMessage("accessChannel.userPhone.bound", null, Locale.getDefault()));
                                    return responseIn;
                                }
                            }

                            user = new User();
                            user.setName(payload.getName());
                            user.setPhone(payload.getPhone());
                            user.setModifierId(BigDecimal.ZERO);
                            userDao.insert(user);

                            userAccessChannel = new UserAccessChannel();
                            userAccessChannel.setUserId(user.getId());
                            userAccessChannel.setAccessChannelId(accessChannel.getId());
                            userAccessChannel.setUserIdOfAccessChannel(payload.getId());
                            userDao.insertAccessChannel(userAccessChannel);
                        }

                        Car car = carDao.selectByVin(payload.getVin());
                        UserCar userCar;

                        if (car == null) {
                            car = new Car();
                            car.setModelId(new BigDecimal(payload.getModelId()));
                            car.setVin(payload.getVin());
                            car.setEffectTime(payload.getEffectiveTime());
                            car.setExpires(payload.getExpires());
                            carDao.insert(car);

                            userCar = new UserCar();
                            userCar.setUserId(userAccessChannel.getUserId());
                            userCar.setCarId(car.getId());
                            userCarDao.insertUserCar(userCar);
                        } else {
                            if (!car.getModelId().equals(new BigDecimal(payload.getModelId()))) {
                                responseIn.setResult(CommonOperationResult.IllegalArguments.name());
                                responseIn.setResultMsg(context.getMessage("accessChannel.vin.existed", null, Locale.getDefault()));
                                return responseIn;
                            }

                            BigDecimal oldFirstOrderId;
                            OrderStatus oldFirstOrderStatus;

                            if (car.getEffectTime().compareTo(payload.getEffectiveTime()) > 0) {
                                CarExpiredMaintenanceInfo expiredMaintenanceInfo = new CarExpiredMaintenanceInfo();
                                BeanUtils.copyProperties(car, expiredMaintenanceInfo);
                                expiredMaintenanceInfo.setId(null);
                                expiredMaintenanceInfo.setCarId(car.getId());
                                carDao.insertExpiredMaintenanceInfo(expiredMaintenanceInfo);

                                oldFirstOrderId = car.getFirstOrderId();
                                oldFirstOrderStatus = car.getFirstOrderStatus();

                                car.setEffectTime(payload.getEffectiveTime());
                                car.setExpires(payload.getExpires());
                                car.setFirstOrderId(null);
                                car.setFirstMaintenanceMoney(null);
                                car.setFirstMaintenanceTime(null);
                                car.setFirstOrderStatus(null);
                                carDao.update(car);
                            } else {
                                oldFirstOrderId = car.getFirstOrderId();
                                oldFirstOrderStatus = car.getFirstOrderStatus();
                            }

                            userCar = userCarDao.selectUserCarByCarId(car.getId());

                            if (!userCar.getUserId().equals(userAccessChannel.getUserId())) {
                                userCarDao.deleteUserCarById(userCar.getId());

                                if (oldFirstOrderStatus == OrderStatus.UNPAYED || oldFirstOrderStatus == OrderStatus.PAYED) {
                                    Order order = orderDao.selectById(oldFirstOrderId);
                                    Order oldOrder = ObjectUtils.clone(order);

                                    OrderStatus oldStatus = order.getStatus();
                                    OrderStatus newStatus = oldStatus == OrderStatus.UNPAYED ? OrderStatus.CANCELED : OrderStatus.INVALID;

                                    order.setStatus(newStatus);
                                    order.setModifierId(BigDecimal.ZERO);
                                    orderDao.update(order);

                                    OrderStatusHis orderStatusHis = new OrderStatusHis();
                                    orderStatusHis.setOrderId(order.getId());
                                    orderStatusHis.setOldStatus(oldStatus);
                                    orderStatusHis.setStatus(newStatus);
                                    orderStatusHis.setModifierId(BigDecimal.ZERO);
                                    orderStatusHisDao.insert(orderStatusHis);

                                    context.publishEvent(new OrderStatusChanged(oldOrder, order));

                                    if (car.getFirstOrderId() != null) {
                                        car.setFirstOrderStatus(null);
                                        car.setFirstOrderId(null);
                                        car.setFirstMaintenanceMoney(null);
                                        car.setFirstMaintenanceTime(null);
                                        carDao.update(car);
                                    }
                                }

                                UserCar newUserCar = new UserCar();
                                newUserCar.setUserId(userAccessChannel.getUserId());
                                newUserCar.setCarId(car.getId());
                                userCarDao.insertUserCar(newUserCar);

                                CarUserHistory carUserHistory = new CarUserHistory();
                                carUserHistory.setCarId(car.getId());
                                carUserHistory.setOldUserId(userCar.getUserId());
                                carUserHistory.setUserId(userAccessChannel.getUserId());
                                carUserHistory.setChangeTime(new Date());
                                userCarDao.insertUserHistory(carUserHistory);
                            }
                        }
                    } catch (RuntimeException e) {
                        status.setRollbackOnly();
                        logger.error("Handle access channel post maintenance info failed", e);
                        responseIn.setResultMsg(context.getMessage("system.common.operationFailed", null, Locale.getDefault()));
                        responseIn.setResult(CommonOperationResult.Failed.name());
                        return responseIn;
                    }

                    responseIn.setResult(CommonOperationResult.Succeeded.name());
                    responseIn.setResultMsg(context.getMessage("system.common.success", null, Locale.getDefault()));
                    return responseIn;
                }
            });
        }
    }
}
