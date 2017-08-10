package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.SystemParameterHolder;
import com.ych.core.wechat.mp.authorization.UserAccessTokenInfo;
import com.ych.shcm.o2o.model.Constants;
import com.ych.shcm.o2o.service.systemparamholder.JWTAllowedClockSkewInSeconds;
import com.ych.shcm.o2o.service.systemparamholder.WXMPAppJWTExpireMin;
import com.ych.shcm.o2o.service.systemparamholder.WebConsoleJWTExpireMin;
import com.ych.shcm.o2o.wechat.parameter.RedirectBackParameter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.ReservedClaimNames;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * JWT的服务
 * <p>
 * Created by U on 2017/7/18.
 */
@Lazy
@Component("shcm.o2o.service.JWTService")
public class JWTService {

    private static final Logger logger = LoggerFactory.getLogger(JWTService.class);

    public static final String JWT_OPEN_ID = "openId";

    public static final String JWT_CAR_ID = "carId";

    public static final String JWT_ACCESS_CHANNEL_ID = "accessChannelId";

    public static final String JWT_SHOP_ID = "shopId";

    @Resource(name = WXMPAppJWTExpireMin.NAME)
    private SystemParameterHolder wxMPAppJWTExpireMin;

    @Resource(name = WebConsoleJWTExpireMin.NAME)
    private SystemParameterHolder wcJWTExpireMin;

    @Resource(name = JWTAllowedClockSkewInSeconds.NAME)
    private SystemParameterHolder jwtAllowedClockSkewInSeconds;

    @Autowired
    private MessageSource messageSource;

    /**
     * 生成微信用户开放平台用户的JWT
     *
     * @param parameter
     *         回调参数
     * @param userAccessTokenInfo
     *         用户AccessToken信息
     * @return JWT
     */
    public String generateWechatJWT(RedirectBackParameter parameter, UserAccessTokenInfo userAccessTokenInfo, BigDecimal defaultCarId) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(Constants.WECHAT_MP_ISSUER);
        claims.setAudience(String.valueOf(parameter.getUserId()));
        claims.setExpirationTimeMinutesInTheFuture(wxMPAppJWTExpireMin.getIneterValue());
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject(Constants.WECHAT_MP_USER_JWT);

        claims.setClaim(JWT_OPEN_ID, String.valueOf(userAccessTokenInfo.getOpenId()));
        claims.setClaim(JWT_ACCESS_CHANNEL_ID, String.valueOf(parameter.getAccessChannelId()));
        if (parameter.getCarId() != null) {
            claims.setClaim(JWT_CAR_ID, String.valueOf(parameter.getCarId()));
        } else if (defaultCarId != null) {
            claims.setClaim(JWT_CAR_ID, String.valueOf(defaultCarId));
        }

        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(claims.toJson());
        jws.setKey(Constants.JWT_KEY);
        jws.setKeyIdHeaderValue(Constants.JWT_KEY_ID);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);

        try {
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            // actually not happen
            logger.error("Generate JWT failed", e);
            return null;
        }
    }

    /**
     * 生成微信用户开放平台用户的JWT
     *
     * @param userId
     *         用户ID
     * @param openId
     *         OpenId
     * @param inputClaims
     *         需要设置的申明信息
     * @return JWT
     */
    public String generateWechatJWT(BigDecimal userId, String openId, Map<String, String> inputClaims) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(Constants.WECHAT_MP_ISSUER);
        claims.setAudience(String.valueOf(userId));
        claims.setExpirationTimeMinutesInTheFuture(wxMPAppJWTExpireMin.getIneterValue());
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject(Constants.WECHAT_MP_USER_JWT);
        claims.setClaim(JWT_OPEN_ID, openId);

        if (MapUtils.isNotEmpty(inputClaims)) {
            for (Map.Entry<String, String> claimEntry : inputClaims.entrySet()) {
                claims.setClaim(claimEntry.getKey(), claimEntry.getValue());
            }
        }

        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(claims.toJson());
        jws.setKey(Constants.JWT_KEY);
        jws.setKeyIdHeaderValue(Constants.JWT_KEY_ID);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);

        try {
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            // actually not happen
            logger.error("Generate JWT failed", e);
            return null;
        }
    }

    /**
     * 生成Web控制台的JWT
     *
     * @param operatorId
     *         用户ID
     * @param inputClaims
     *         需要设置的申明信息
     * @return JWT
     */
    public String generateWebConsoleJWT(BigDecimal operatorId, Map<String, String> inputClaims) {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(Constants.WEB_CONSOLE_OPERATOR_ISSUER);
        claims.setAudience(String.valueOf(operatorId));
        claims.setExpirationTimeMinutesInTheFuture(wcJWTExpireMin.getIneterValue());
        claims.setGeneratedJwtId();
        claims.setIssuedAtToNow();
        claims.setNotBeforeMinutesInThePast(2);
        claims.setSubject(Constants.WEB_CONSOLE_OPERATOR_JWT);

        if (MapUtils.isNotEmpty(inputClaims)) {
            for (Map.Entry<String, String> claimEntry : inputClaims.entrySet()) {
                claims.setClaim(claimEntry.getKey(), claimEntry.getValue());
            }
        }

        JsonWebSignature jws = new JsonWebSignature();

        jws.setPayload(claims.toJson());
        jws.setKey(Constants.JWT_KEY);
        jws.setKeyIdHeaderValue(Constants.JWT_KEY_ID);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);

        try {
            return jws.getCompactSerialization();
        } catch (JoseException e) {
            // actually not happen
            logger.error("Generate JWT failed", e);
            return null;
        }
    }

    /**
     * 对JWT进行鉴权
     *
     * @param token
     *         Token
     * @param issuer
     *         指定发行者,如果为null则不指定发行者
     * @return 鉴权通过返回true
     */
    public AuthorizeResult authorize(String token, String... issuer) {
        JwtConsumerBuilder jwtConsumerBuilder = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(jwtAllowedClockSkewInSeconds.getIneterValue())
                .setRequireSubject()
                .setRequireJwtId()
                .setSkipDefaultAudienceValidation()
                .setVerificationKey(Constants.JWT_KEY)
                .setJwsAlgorithmConstraints(new AlgorithmConstraints(AlgorithmConstraints.ConstraintType.WHITELIST,
                        AlgorithmIdentifiers.HMAC_SHA256));

        if (issuer.length == 0) {
            jwtConsumerBuilder.setExpectedAudience(true, null);
        } else {
            jwtConsumerBuilder.setExpectedAudience(issuer);
        }

        JwtConsumer jwtConsumer = jwtConsumerBuilder.build();

        JwtClaims jwtClaims;

        try {
            jwtClaims = jwtConsumer.processToClaims(token);
        } catch (InvalidJwtException e) {
            logger.error("Decode JWT failed", e);
            return new AuthorizeResult(false);
        }

        logger.debug("Authorize JWT Claims {}", jwtClaims);

        try {
            if (CollectionUtils.isEmpty(jwtClaims.getAudience())) {
                logger.debug("Audience is absent");
                return new AuthorizeResult(false);
            }

            if (Constants.WECHAT_MP_ISSUER.equals(jwtClaims.getIssuer()) && Constants.WECHAT_MP_USER_JWT.equals(jwtClaims.getSubject())) {
                return new WXMPAuthorizeResult(true, jwtClaims);
            } else if (Constants.WEB_CONSOLE_OPERATOR_ISSUER.equals(jwtClaims.getIssuer()) && Constants.WEB_CONSOLE_OPERATOR_JWT.equals(jwtClaims.getSubject())) {
                return new AuthorizeResult(true, jwtClaims);
            } else {
                logger.warn("Illegal JWT issuer[{}] and subject[{}]", jwtClaims.getIssuer(), jwtClaims.getSubject());
            }
        } catch (MalformedClaimException e) {
            logger.error("JWTClaims MalformedClaimException", e);
            return new AuthorizeResult(false);
        }

        return new AuthorizeResult(false);
    }

    /**
     * 刷新JWT
     *
     * @param token
     *         旧的JWT
     * @return 如果操作成功会返回新的JWT
     */
    public CommonOperationResultWidthData<String> refreshToken(String token) {
        return refreshToken(token, null);
    }

    /**
     * 刷新JWT
     *
     * @param token
     *         旧的JWT
     * @param replaces
     *         需要替换的Payload参数
     * @return 如果操作成功会返回新的JWT
     */
    public CommonOperationResultWidthData<String> refreshToken(String token, Map<String, String> replaces) {
        CommonOperationResultWidthData<String> ret = new CommonOperationResultWidthData<>();
        ret.setResult(CommonOperationResult.Failed);

        AuthorizeResult authorizeResult = authorize(token, Constants.WECHAT_MP_ISSUER, Constants.WEB_CONSOLE_OPERATOR_ISSUER);

        if (authorizeResult.isSuccess()) {
            JwtClaims claims = new JwtClaims();
            claims.setIssuer(authorizeResult.getIssuer());
            claims.setAudience(String.valueOf(authorizeResult.getAudienceId()));
            claims.setExpirationTimeMinutesInTheFuture(Constants.WECHAT_MP_ISSUER.equals(authorizeResult.getIssuer()) ? wxMPAppJWTExpireMin.getIneterValue() : wcJWTExpireMin.getIneterValue());
            claims.setJwtId(authorizeResult.getJwtId());
            claims.setIssuedAtToNow();
            claims.setNotBeforeMinutesInThePast(2);
            claims.setSubject(authorizeResult.getSubject());

            for (Map.Entry<String, Object> entry : authorizeResult.getClaimsMap().entrySet()) {
                claims.setClaim(entry.getKey(), entry.getValue());
            }

            if (MapUtils.isNotEmpty(replaces)) {
                for (Map.Entry<String, String> replaceEntry : replaces.entrySet()) {
                    claims.setClaim(replaceEntry.getKey(), replaceEntry.getValue());
                }
            }

            JsonWebSignature jws = new JsonWebSignature();

            jws.setPayload(claims.toJson());
            jws.setKey(Constants.JWT_KEY);
            jws.setKeyIdHeaderValue(Constants.JWT_KEY_ID);
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);

            try {
                ret.setData(jws.getCompactSerialization());
                ret.setResult(CommonOperationResult.Succeeded);
            } catch (JoseException e) {
                logger.error("Generate JWT failed", e);
            }
        } else {
            ret.setDescription(messageSource.getMessage("jwt.validate.illegal", null, Locale.getDefault()));
        }

        return ret;
    }

    /**
     * 鉴权结果
     */
    public class AuthorizeResult {

        /**
         * 成功
         */
        private boolean success;

        /**
         * JWT签发者
         */
        private String issuer;

        /**
         * JWT的ID
         */
        private String jwtId;

        /**
         * 收听者的ID
         */
        private BigDecimal audienceId;

        /**
         * 订阅主题
         */
        private String subject;

        /**
         * OpenID
         */
        private String openId;

        /**
         * 声明字段映射
         */
        private Map<String, Object> claimsMap;

        AuthorizeResult(boolean success) {
            this.success = success;
        }

        AuthorizeResult(boolean success, JwtClaims claims) throws MalformedClaimException {
            this.success = success;

            issuer = claims.getIssuer();
            jwtId = claims.getJwtId();
            audienceId = new BigDecimal(claims.getAudience().get(0));
            subject = claims.getSubject();

            openId = (String) claims.getClaimValue(JWT_OPEN_ID);
            claimsMap = claims.getClaimsMap(ReservedClaimNames.INITIAL_REGISTERED_CLAIM_NAMES);
        }

        /**
         * @return OpenID
         */
        public String getOpenId() {
            return openId;
        }

        /**
         * @param openId
         *         OpenID
         */
        public void setOpenId(String openId) {
            this.openId = openId;
        }

        /**
         * @return 成功
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * @param success
         *         成功
         */
        public void setSuccess(boolean success) {
            this.success = success;
        }

        /**
         * @return JWT签发者
         */
        public String getIssuer() {
            return issuer;
        }

        /**
         * @param issuer
         *         JWT签发者
         */
        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        /**
         * @return JWT的ID
         */
        public String getJwtId() {
            return jwtId;
        }

        /**
         * @param jwtId
         *         JWT的ID
         */
        public void setJwtId(String jwtId) {
            this.jwtId = jwtId;
        }

        /**
         * @return 收听者的ID
         */
        public BigDecimal getAudienceId() {
            return audienceId;
        }

        /**
         * @param audienceId
         *         收听者的ID
         */
        public void setAudienceId(BigDecimal audienceId) {
            this.audienceId = audienceId;
        }

        /**
         * @return 订阅主题
         */
        public String getSubject() {
            return subject;
        }

        /**
         * @param subject
         *         订阅主题
         */
        public void setSubject(String subject) {
            this.subject = subject;
        }

        /**
         * @return 声明字段映射
         */
        public Map<String, Object> getClaimsMap() {
            return claimsMap;
        }

        /**
         * @param claimsMap
         *         声明字段映射
         */
        public void setClaimsMap(Map<String, Object> claimsMap) {
            this.claimsMap = claimsMap;
        }
    }

    /**
     * 微信使用的授权结果
     */
    public class WXMPAuthorizeResult extends AuthorizeResult {

        /**
         * 车辆ID
         */
        private BigDecimal carId;

        /**
         * 访问渠道ID
         */
        private BigDecimal accessChannelId = BigDecimal.ZERO;

        /**
         * 门店ID
         */
        private BigDecimal shopId;

        WXMPAuthorizeResult(boolean success, JwtClaims claims) throws MalformedClaimException {
            super(success, claims);

            String carIdStr = (String) claims.getClaimValue(JWT_CAR_ID);
            if (carIdStr != null) {
                carId = new BigDecimal(carIdStr);
            }

            String accessChannelIdStr = (String) claims.getClaimValue(JWT_ACCESS_CHANNEL_ID);
            if (accessChannelIdStr != null) {
                accessChannelId = new BigDecimal(accessChannelIdStr);
            }

            String shopIdStr = (String) claims.getClaimValue(JWT_SHOP_ID);
            if (shopIdStr != null) {
                shopId = new BigDecimal(shopIdStr);
            }
        }

        /**
         * @return 车辆ID
         */
        public BigDecimal getCarId() {
            return carId;
        }

        /**
         * @param carId
         *         车辆ID
         */
        public void setCarId(BigDecimal carId) {
            this.carId = carId;
        }

        /**
         * @return 访问渠道ID
         */
        public BigDecimal getAccessChannelId() {
            return accessChannelId;
        }

        /**
         * @param accessChannelId
         *         访问渠道ID
         */
        public void setAccessChannelId(BigDecimal accessChannelId) {
            this.accessChannelId = accessChannelId;
        }

        /**
         * @return 门店ID
         */
        public BigDecimal getShopId() {
            return shopId;
        }

        /**
         * @param shopId
         *         门店ID
         */
        public void setShopId(BigDecimal shopId) {
            this.shopId = shopId;
        }
    }

}
