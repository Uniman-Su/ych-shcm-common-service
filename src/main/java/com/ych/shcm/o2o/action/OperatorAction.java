package com.ych.shcm.o2o.action;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ych.shcm.o2o.model.Operator;
import com.ych.shcm.o2o.model.User;
import com.ych.shcm.o2o.service.OperatorServcie;

/**
 * 操作员相关的Action
 * <p>
 * Created by U on 2017/7/18.
 */
@Component(OperatorAction.NAME)
public class OperatorAction extends BaseAction {

    public static final String NAME = "shcm.o2o.action.OperatorAction";

    private static final String OPERATOR_REQUEST_ATTR_NAME = "Operator";

    /**
     * 管理员服务
     */
    @Autowired
    protected OperatorServcie operatorServcie;

    /**
     * @return 管理员信息
     */
    protected Operator getOperator() {
        Operator operator = (Operator) getRequest().getAttribute(OPERATOR_REQUEST_ATTR_NAME);
        if (operator == null) {
            operator = operatorServcie.getById(getAuthorizeResult().getAudienceId());
            getRequest().setAttribute(OPERATOR_REQUEST_ATTR_NAME, operator);
        }
        return operator;
    }

}
