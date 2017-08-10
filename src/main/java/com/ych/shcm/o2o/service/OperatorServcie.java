package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.shcm.o2o.dao.OperatorDao;
import com.ych.shcm.o2o.dao.OperatorThirdAuthDao;
import com.ych.shcm.o2o.model.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * 操作员的服务
 * <p>
 * Created by U on 2017/7/18.
 */
@Lazy
@Component("shcm.o2o.service.OperatorServcie")
public class OperatorServcie {

    private static final Logger logger = LoggerFactory.getLogger(OperatorServcie.class);

    @Autowired
    private OperatorDao operatorDao;

    @Autowired
    private OperatorThirdAuthDao operatorThirdAuthDao;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private JWTService jwtService;

    /**
     * 根据ID查找管理员信息
     *
     * @param id
     *         ID
     * @return 管理员信息
     */
    public Operator getById(BigDecimal id) {
        return operatorDao.selectById(id);
    }

    /**
     * 对密码进行签名
     *
     * @param password
     *         密码
     * @return 签名的结果
     */
    static protected String signPassword(String password) {
        return DigestUtils.sha256Hex(password + Constants.WEB_CONSOLE_OPERATOR_PASSWORD_SECURITY_KEY);
    }

    /**
     * 执行登录操作
     *
     * @param userName
     *         用户名
     * @param password
     *         密码
     * @return 操作结果
     */
    public CommonOperationResultWidthData<String> login(String userName, String password) {
        CommonOperationResultWidthData<String> ret = new CommonOperationResultWidthData<>();

        Operator operator = operatorDao.selectByUsername(userName);
        if (operator == null) {
            ret.setResult(CommonOperationResult.NotExists);
            ret.setDescription(messageSource.getMessage("console.operator.notExists", null, Locale.getDefault()));
            return ret;
        }
        if (!operator.getPassword().equals(signPassword(password))) {
            ret.setResult(CommonOperationResult.IllegalPassword);
            ret.setDescription(messageSource.getMessage("system.common.illegalPassword", null, Locale.getDefault()));
            return ret;
        }

        ret.setData(jwtService.generateWebConsoleJWT(operator.getId(), null));
        ret.setResult(CommonOperationResult.Succeeded);

        return ret;
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
    public OperatorThirdAuth getThirdAuthByByThirdId(ThirdAuthType thirdAuthType, String thirdId) {
        return operatorThirdAuthDao.selectByThirdId(thirdAuthType, thirdId);
    }

}
