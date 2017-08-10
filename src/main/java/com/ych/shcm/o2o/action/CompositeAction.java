package com.ych.shcm.o2o.action;

import javax.annotation.Resource;

import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.Operator;
import com.ych.shcm.o2o.model.Shop;
import com.ych.shcm.o2o.model.User;

/**
 * 复合Action,提供UserAction和OperatorAction的所有能力
 */
public class CompositeAction extends BaseAction {

    @Resource(name = UserAction.NAME)
    private UserAction userAction;

    @Resource(name = OperatorAction.NAME)
    private OperatorAction operatorAction;

    public User getUser() {
        return userAction.getUser();
    }

    public Car getCar() {
        return userAction.getCar();
    }

    public Shop getShop() {
        return userAction.getShop();
    }

    public Operator getOperator() {
        return operatorAction.getOperator();
    }

}
