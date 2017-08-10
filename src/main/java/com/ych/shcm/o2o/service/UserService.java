package com.ych.shcm.o2o.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import com.ych.core.model.CommonOperationResult;
import com.ych.shcm.o2o.dao.*;
import com.ych.shcm.o2o.model.*;
import com.ych.shcm.o2o.openinf.*;

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
    private UserThirdAuthDao userThirdAuthDao;

    @Autowired
    private AccessChannelService accessChannelService;

    /**
     * 事务模板
     */
    @Resource(name = Constants.TRANSACTION_TEMPLATE)
    private TransactionTemplate transactionTempalte;

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
                Assert.notNull(payload, "请求对象不能为空");
                Assert.notNull(payload.getId(), "用户在自身平台的id不能为空");
                Assert.notNull(payload.getName(), "用户的姓名不能为空");
                Assert.notNull(payload.getPhone(), "用户电话不能为空");
                Assert.notNull(payload.getVin(), "车辆的VIN码不能为空");
                Assert.notNull(payload.getModelId(), "车型不能为空");
                Assert.notNull(payload.getEffectiveTime(), "生效时间不能为空");
                Assert.notNull(payload.getExpires(), "过期时间不能为空");
                Assert.notNull(accessChannel, "访问渠道ID不能为空");
            } catch (IllegalArgumentException e) {
                response.setResult(CommonOperationResult.Failed.name());
                response.setResultMsg(e.getMessage());
                return response;
            }

            if (carModelDao.selectById(new BigDecimal(payload.getModelId()), false) == null) {
                response.setResult(CommonOperationResult.NotExists.name());
                response.setResultMsg(messageSource.getMessage("carModel.notExists", null, Locale.getDefault()));
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
                                    responseIn.setResultMsg(messageSource.getMessage("accessChannel.userPhone.bound", null, Locale.getDefault()));
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
                                responseIn.setResultMsg(messageSource.getMessage("accessChannel.vin.existed", null, Locale.getDefault()));
                                return responseIn;
                            }

                            if (car.getEffectTime().compareTo(payload.getEffectiveTime()) > 0) {
                                CarExpiredMaintenanceInfo expiredMaintenanceInfo = new CarExpiredMaintenanceInfo();
                                BeanUtils.copyProperties(car, expiredMaintenanceInfo);
                                expiredMaintenanceInfo.setId(null);
                                expiredMaintenanceInfo.setCarId(car.getId());
                                carDao.insertExpiredMaintenanceInfo(expiredMaintenanceInfo);

                                car.setEffectTime(payload.getEffectiveTime());
                                car.setExpires(payload.getExpires());
                                car.setFirstMaintenanceMoney(null);
                                car.setFirstMaintenanceTime(null);
                                car.setFirstOrderStatus(null);
                                carDao.update(car);
                            }

                            userCar = userCarDao.selectUserCarByCarId(car.getId());

                            if (!userCar.getUserId().equals(userAccessChannel.getUserId())) {
                                userCarDao.deleteUserCarById(userCar.getId());

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
                        responseIn.setResultMsg(messageSource.getMessage("system.common.operationFailed", null, Locale.getDefault()));
                        responseIn.setResult(CommonOperationResult.Failed.name());
                        return responseIn;
                    }

                    responseIn.setResult(CommonOperationResult.Succeeded.name());
                    responseIn.setResultMsg(messageSource.getMessage("system.common.success", null, Locale.getDefault()));
                    return responseIn;
                }
            });
        }
    }
}
