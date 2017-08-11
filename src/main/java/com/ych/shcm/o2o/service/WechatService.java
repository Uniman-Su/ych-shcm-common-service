package com.ych.shcm.o2o.service;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;
import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.SystemParameterHolder;
import com.ych.core.wechat.mp.UserInfo;
import com.ych.core.wechat.mp.authorization.*;
import com.ych.core.wechat.mp.authorization.dao.UserAccessTokenInfoDao;
import com.ych.shcm.o2o.dao.*;
import com.ych.shcm.o2o.model.*;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.service.systemparamholder.*;
import com.ych.shcm.o2o.wechat.parameter.NavigateInParameter;
import com.ych.shcm.o2o.wechat.parameter.RedirectBackParameter;

/**
 * 微信相关的服务
 * <p>
 * Created by U on 2017/7/17.
 */
@Lazy
@Component("shcm.o2o.service.WechatService")
public class WechatService {

    private Logger logger = LoggerFactory.getLogger(WechatService.class);

    /**
     * 访问渠道相关服务
     */
    @Autowired
    private AccessChannelService accessChannelService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CarDao carDao;

    @Autowired
    private CarModelDao carModelDao;

    @Autowired
    private UserCarDao userCarDao;

    @Autowired
    private UserThirdAuthDao userThirdAuthDao;

    @Autowired
    private UserWechatInfoDao userWechatInfoDao;

    @Autowired
    private UserAccessTokenInfoDao userAccessTokenInfoDao;

    @Autowired
    private ShopDao shopDao;

    @Autowired
    private OperatorDao operatorDao;

    @Autowired
    private OperatorThirdAuthDao operatorThirdAuthDao;

    @Resource(name = WXNavigateInRedirectUrl.NAME)
    private SystemParameterHolder wxNavigateInRedirectUrl;

    @Resource(name = WXMPAppID.NAME)
    private SystemParameterHolder wxMPAppId;

    @Resource(name = WXMPAppSecrect.NAME)
    private SystemParameterHolder wxMPAppSecrect;

    @Resource(name = WXMPAppJWTExpireMin.NAME)
    private SystemParameterHolder wxMPAppJWTExpireMin;

    @Resource(name = WXShopOwnerRedirectUrl.NAME)
    private SystemParameterHolder wxShopOwnerRedirectUrl;

    @Resource(name = WXShopLocationRedirectUrl.NAME)
    private SystemParameterHolder wxShopLocationRedirectUrl;

    @Autowired
    private JWTService jwtService;

    /**
     * 消息源
     */
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private AuthorizationUtils wxAuthorizationUtils;

    /**
     * 签名是否合法
     *
     * @param parameter
     *         导航进入的参数
     * @param securityKey
     *         SecurityKey
     * @return 如果是则返回true
     */
    private boolean isDigestValid(NavigateInParameter parameter, String securityKey) {
        TreeMap<String, String> sortMap = new TreeMap<>();
        sortMap.put("appCode", parameter.getAppCode());
        sortMap.put("userId", parameter.getUserId());
        sortMap.put("vin", parameter.getVin());
        sortMap.put("timestamp", String.valueOf(parameter.getTimestamp().getTime()));
        sortMap.put("securityKey", securityKey);

        StringBuilder stringBuilder = new StringBuilder();
        for (String seg : sortMap.values()) {
            stringBuilder.append(seg);
        }

        return accessChannelService.digest(stringBuilder.toString()).equals(parameter.getDigest());
    }

    /**
     * 验证导航进入的参数合法性
     *
     * @param parameter
     *         导航进入的参数
     * @return 验证结果
     */
    private ValidateNavigateInResult validateNavigateInParameter(NavigateInParameter parameter) {
        ValidateNavigateInResult ret = new ValidateNavigateInResult();
        CommonOperationResultWidthData<String> result = new CommonOperationResultWidthData<>();
        ret.setResult(result);

        if (!accessChannelService.isTimestampValid(parameter.getTimestamp().getTime())) {
            result.setResult(CommonOperationResult.IllegalAccess);
            result.setDescription(messageSource.getMessage("accessChannel.timestamp.illegal", null, Locale.getDefault()));
            return ret;
        }

        AccessChannel accessChannel = accessChannelService.getAccessChannelDao().selectByCode(parameter.getAppCode());

        if (accessChannel == null) {
            result.setResult(CommonOperationResult.NotExists);
            result.setDescription(messageSource.getMessage("accessChannel.appCode.illegal", null, Locale.getDefault()));
            return ret;
        }

        if (!isDigestValid(parameter, accessChannel.getSecurityKey())) {
            result.setResult(CommonOperationResult.IllegalArguments);
            result.setDescription(messageSource.getMessage("accessChannel.digest.illegal", null, Locale.getDefault()));
            return ret;
        }

        UserAccessChannel userAccessChannel = userDao.selectAccessChannelByChannelId(accessChannel.getId(), parameter.getUserId());
        if (userAccessChannel == null) {
            result.setResult(CommonOperationResult.IllegalArguments);
            result.setDescription(messageSource.getMessage("accessChannel.userId.illegal", null, Locale.getDefault()));
            return ret;
        }
        ret.setUserId(userAccessChannel.getUserId());
        ret.setAccessChannelId(accessChannel.getId());

        if (StringUtils.isNotEmpty(parameter.getVin())) {
            parameter.setVin(parameter.getVin().toUpperCase());
            Car car = carDao.selectByVin(parameter.getVin());
            if (car == null) {
                result.setResult(CommonOperationResult.IllegalArguments);
                result.setDescription(messageSource.getMessage("accessChannel.vin.illegal", null, Locale.getDefault()));
                return ret;
            }


            UserCar userCar = userCarDao.selectUserCarByUserIdAndCarId(userAccessChannel.getUserId(), car.getId());
            if (userCar == null) {
                result.setResult(CommonOperationResult.IllegalArguments);
                result.setDescription(messageSource.getMessage("accessChannel.vin.wrongUser", null, Locale.getDefault()));
                return ret;
            }

            CarModel carModel = carModelDao.selectById(car.getModelId(), false);
            if (carModel.getEngineOilCapacity().compareTo(BigDecimal.ZERO) == 0) {
                result.setResult(CommonOperationResult.IllegalArguments);
                result.setDescription(messageSource.getMessage("carModel.enginOil.capacity.notExisted", null, Locale.getDefault()));
                return ret;
            }

            ret.setCarId(car.getId());
        }

        result.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 验证微信公众号导航进入的参数
     *
     * @param parameter
     *         输入参数
     * @return 如果验证成功会附带返回微信用户授权页的URL, 否则会返回错误描述
     */
    public CommonOperationResultWidthData<String> navigateIn(NavigateInParameter parameter) {
        logger.info("Wechat navigate in:{}", parameter);

        CommonOperationResultWidthData<String> ret = new CommonOperationResultWidthData<>();

        ValidateNavigateInResult validateInputResult = validateNavigateInParameter(parameter);
        if (validateInputResult.getResult().getResult() != CommonOperationResult.Succeeded) {
            logger.info("Result:{}", ret);
            return validateInputResult.getResult();
        }

        String redirectUrl = MessageFormat.format(wxNavigateInRedirectUrl.getStringValue(), validateInputResult.getUserId(), validateInputResult.getCarId(), validateInputResult.getAccessChannelId());
        ret.setData(wxAuthorizationUtils.getAuthorizationUrl(wxMPAppId.getStringValue(), redirectUrl, AuthorizationScope.snsapi_base, null));
        ret.setResult(CommonOperationResult.Succeeded);

        logger.info("Result:{}", ret);

        return ret;
    }

    /**
     * 执行微信的回调操作
     *
     * @param parameter
     *         回调参数
     * @return 操作结果, 成功时将附带用户的JWT
     */
    public CommonOperationResultWidthData<String> redirectBack(RedirectBackParameter parameter) {
        CommonOperationResultWidthData<String> ret = new CommonOperationResultWidthData<>();

        UserAccessTokenInfo userAccessTokenInfo = wxAuthorizationUtils.getUserAccessToken(wxMPAppId.getStringValue(), wxMPAppSecrect.getStringValue(), parameter.getCode());
        if (userAccessTokenInfo == null) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription(messageSource.getMessage("wechat.mp.user.accessToken.draw.failed", null, Locale.getDefault()));
            return ret;
        }

        UserAccessTokenInfo oldUserAccessTokenInfo = userAccessTokenInfoDao.selectByOpenId(userAccessTokenInfo.getOpenId());
        if (oldUserAccessTokenInfo == null) {
            userAccessTokenInfoDao.insert(userAccessTokenInfo);
        } else {
            userAccessTokenInfoDao.update(userAccessTokenInfo);
        }

        UserThirdAuth userThirdAuth = userThirdAuthDao.selectByThirdId(ThirdAuthType.WECHAT_OPENID, userAccessTokenInfo.getOpenId());
        if (userThirdAuth == null) {
            userThirdAuth = userThirdAuthDao.selectByThirdType(parameter.getUserId(), ThirdAuthType.WECHAT_OPENID);
            if (userThirdAuth != null) {
                ret.setResult(CommonOperationResult.Existed);
                ret.setDescription(messageSource.getMessage("wechat.mp.user.openId.userId.existed", null, Locale.getDefault()));
                return ret;
            }

            userThirdAuth = new UserThirdAuth();
            userThirdAuth.setUserId(parameter.getUserId());
            userThirdAuth.setType(ThirdAuthType.WECHAT_OPENID);
            userThirdAuth.setThirdId(userAccessTokenInfo.getOpenId());
            userThirdAuthDao.insert(userThirdAuth);
        }

        BigDecimal defaultCarId = null;
        if (parameter.getCarId() == null) {
            UserCar userCar = userCarDao.selectOneByUserId(parameter.getUserId());
            if (userCar != null) {
                defaultCarId = userCar.getId();
            }
        }

        ret.setResult(CommonOperationResult.Succeeded);
        ret.setData(jwtService.generateWechatJWT(parameter, userAccessTokenInfo, defaultCarId));

        return ret;
    }

    /**
     * 简单回调仅传入授权Code的情况
     *
     * @param code
     *         微信授权Code
     * @return 成功的情况下携带OpenId返回
     */
    public CommonOperationResultWidthData<String> simpleRedirectBack(String code) {
        CommonOperationResultWidthData<String> ret = new CommonOperationResultWidthData<>();

        UserAccessTokenInfo userAccessTokenInfo = wxAuthorizationUtils.getUserAccessToken(wxMPAppId.getStringValue(), wxMPAppSecrect.getStringValue(), code);
        if (userAccessTokenInfo == null) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription(messageSource.getMessage("wechat.mp.user.accessToken.draw.failed", null, Locale.getDefault()));
            return ret;
        }

        UserAccessTokenInfo oldUserAccessTokenInfo = userAccessTokenInfoDao.selectByOpenId(userAccessTokenInfo.getOpenId());
        if (oldUserAccessTokenInfo == null) {
            userAccessTokenInfoDao.insert(userAccessTokenInfo);
        } else {
            userAccessTokenInfoDao.update(userAccessTokenInfo);
        }

        if (userAccessTokenInfo.getScope() == AuthorizationScope.snsapi_userinfo) {
            UserWechatInfo userWechatInfo = userWechatInfoDao.selectByOpenId(userAccessTokenInfo.getOpenId());

            if (userWechatInfo == null) {
                AuthorizationUserInfo userInfoResponse = wxAuthorizationUtils.getUserInf(userAccessTokenInfo.getAccessToken(), userAccessTokenInfo.getOpenId(), Locale.SIMPLIFIED_CHINESE);
                userWechatInfo = new UserWechatInfo();

                try {
                    BeanUtils.copyProperties(userWechatInfo, userInfoResponse);
                    userWechatInfoDao.insert(userWechatInfo);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    // actually not happen
                    logger.error("Copy properties failed", e);
                }
            }
        }

        ret.setResult(CommonOperationResult.Succeeded);
        ret.setData(userAccessTokenInfo.getOpenId());

        return ret;
    }

    /**
     * 获取店铺所有者入口授权URL
     *
     * @param scope
     *         授权空间
     * @return 店铺所有者入口授权URL
     */
    public String getShopOwenerEntranceAuthUrl(AuthorizationScope scope) {
        return wxAuthorizationUtils.getAuthorizationUrl(wxMPAppId.getStringValue(), wxShopOwnerRedirectUrl.getStringValue(), scope, null);
    }

    /**
     * @return 店铺所有者入口授权URL
     */
    public String getShopOwenerEntranceAuthUrl() {
        return getShopOwenerEntranceAuthUrl(AuthorizationScope.snsapi_base);
    }

    /**
     * 获取店铺坐标更新入口授权URL
     *
     * @param scope
     *         授权空间
     * @return 店铺坐标更新入口授权URL
     */
    public String getShopLocationEntranceAuthUrl(AuthorizationScope scope) {
        return wxAuthorizationUtils.getAuthorizationUrl(wxMPAppId.getStringValue(), wxShopLocationRedirectUrl.getStringValue(), scope, null);
    }

    /**
     * 用户
     *
     * @param openId
     *         OpenId
     * @return 用户信息
     */
    public UserInfo getUserInfo(String openId) {
        return userWechatInfoDao.selectByOpenId(openId);
    }

    /**
     * 通过用户名和密码进行登录
     *
     * @param userName
     *         用户名
     * @param password
     *         密码
     * @param openId
     *         OpenID
     * @return 成功时附带返回JWT
     */
    @Transactional(transactionManager = Constants.TRANSACTION_MANAGER)
    public CommonOperationResultWidthData<String> loginByUserNameAndPassword(String userName, String password, String openId) {
        CommonOperationResultWidthData<String> ret = new CommonOperationResultWidthData<>();

        User user = userDao.selectByUserName(userName);
        if (user == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(messageSource.getMessage("user.userName.notExists", null, Locale.getDefault()));
            return ret;
        }

        if (!user.getPassword().equals(DigestUtils.sha256Hex(password + Constants.USER_PASSWORD_SECURITY_KEY))) {
            ret.setResult(CommonOperationResult.IllegalPassword);
            ret.setDescription(messageSource.getMessage("system.common.illegalPassword", null, Locale.getDefault()));
            return ret;
        }

        UserWechatInfo userWechatInfo = userWechatInfoDao.selectByOpenId(openId);
        if (userWechatInfo == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(messageSource.getMessage("wechat.mp.user.info.openId.notExists", null, Locale.getDefault()));
            return ret;
        }

        UserThirdAuth userThirdAuth = userThirdAuthDao.selectByThirdId(ThirdAuthType.WECHAT_OPENID, openId);

        if (userThirdAuth != null && !userThirdAuth.getUserId().equals(user.getId())) {
            ret.setResult(CommonOperationResult.IllegalOperation);
            ret.setDescription(messageSource.getMessage("wechat.mp.user.info.openId.binded", null, Locale.getDefault()));
            return ret;
        }

        if (userThirdAuth == null) {
            userThirdAuth = new UserThirdAuth();
            userThirdAuth.setUserId(user.getId());
            userThirdAuth.setType(ThirdAuthType.WECHAT_OPENID);
            userThirdAuth.setThirdId(openId);
            userThirdAuthDao.insert(userThirdAuth);
        }

        ret.setResult(CommonOperationResult.Succeeded);

        HashMap<String, String> claims = new HashMap<>();
        claims.put(JWTService.JWT_SHOP_ID, String.valueOf(shopDao.selectByUserId(user.getId()).get(0).getId()));

        ret.setData(jwtService.generateWechatJWT(user.getId(), openId, claims));

        return ret;
    }

    /**
     * 操作员通过微信登录
     *
     * @param userName
     *         用户名
     * @param password
     *         密码
     * @param openId
     *         OpenId
     * @return 带JWT的操作结果
     */
    public CommonOperationResultWidthData<String> operatorLoginByUserNameAndPassword(String userName, String password, String openId) {
        CommonOperationResultWidthData<String> ret = new CommonOperationResultWidthData<>();
        Operator operator = operatorDao.selectByUsername(userName);

        if (operator == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(messageSource.getMessage("console.operator.notExists", null, Locale.getDefault()));
            return ret;
        }

        if (!operator.getPassword().equals(OperatorServcie.signPassword(password))) {
            ret.setResult(CommonOperationResult.IllegalPassword);
            ret.setDescription(messageSource.getMessage("system.common.illegalPassword", null, Locale.getDefault()));
            return ret;
        }

        OperatorThirdAuth operatorThirdAuth = operatorThirdAuthDao.selectByThirdId(ThirdAuthType.WECHAT_OPENID, openId);

        if (operatorThirdAuth != null) {
            if (!operatorThirdAuth.getOperatorId().equals(operator.getId())) {
                ret.setResult(CommonOperationResult.IllegalOperation);
                ret.setDescription(messageSource.getMessage("wechat.mp.user.info.openId.binded", null, Locale.getDefault()));
                return ret;
            }
        } else {
            operatorThirdAuth = operatorThirdAuthDao.selectByThirdType(operator.getId(), ThirdAuthType.WECHAT_OPENID);

            if (operatorThirdAuth != null) {
                ret.setResult(CommonOperationResult.IllegalOperation);
                ret.setDescription(messageSource.getMessage("operator.wechat.opendId.binded", null, Locale.getDefault()));
                return ret;
            }

            operatorThirdAuth = new OperatorThirdAuth();
            operatorThirdAuth.setOperatorId(operator.getId());
            operatorThirdAuth.setType(ThirdAuthType.WECHAT_OPENID);
            operatorThirdAuth.setThirdId(openId);
            operatorThirdAuthDao.insert(operatorThirdAuth);
        }

        HashMap<String, String> claims = new HashMap<>();
        claims.put(JWTService.JWT_OPEN_ID, openId);

        ret.setData(jwtService.generateWebConsoleJWT(operator.getId(), claims));
        ret.setResult(CommonOperationResult.Succeeded);

        return ret;
    }

    /**
     * 验证导航进入的参数结果
     */
    private static class ValidateNavigateInResult {

        private CommonOperationResultWidthData<String> result;

        private BigDecimal userId;

        private BigDecimal carId;

        private BigDecimal accessChannelId;

        public CommonOperationResultWidthData<String> getResult() {
            return result;
        }

        public void setResult(CommonOperationResultWidthData<String> result) {
            this.result = result;
        }

        public BigDecimal getUserId() {
            return userId;
        }

        public void setUserId(BigDecimal userId) {
            this.userId = userId;
        }

        public BigDecimal getCarId() {
            return carId;
        }

        public void setCarId(BigDecimal carId) {
            this.carId = carId;
        }

        public BigDecimal getAccessChannelId() {
            return accessChannelId;
        }

        public void setAccessChannelId(BigDecimal accessChannelId) {
            this.accessChannelId = accessChannelId;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }

    }

}
