package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.ServiceProviderSettleDao;
import com.ych.shcm.o2o.model.ServiceProviderSettleDate;
import com.ych.shcm.o2o.model.ServiceProviderSettleDateSummary;
import com.ych.shcm.o2o.model.ServiceProviderSettleDetail;
import com.ych.shcm.o2o.model.ServiceProviderSettleStatus;
import com.ych.shcm.o2o.parameter.QueryServiceProviderSettleParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 服务商结算服务
 * Created by mxp on 2017/7/18.
 */
@Lazy
@Component("shcm.o2o.service.ServiceProviderSettleService")
public class ServiceProviderSettleService {

    @Autowired
    private ServiceProviderSettleDao serviceProviderSettleDao;

    /**
     * 查询服务商结算日数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 服务商结算日数据的分页数据
     */
    public PagedList<ServiceProviderSettleDate> getPagedDateList(QueryServiceProviderSettleParameter parameter) {
        return serviceProviderSettleDao.selectPagedDateList(parameter);
    }

    /**
     * 查询服务商结算明细的分页列表
     *
     * @param parameter
     *         查询参数
     * @return 服务商结算明细的分页列表
     */
    public PagedList<ServiceProviderSettleDetail> getPagedDetailList(QueryServiceProviderSettleParameter parameter) {
        return serviceProviderSettleDao.selectPagedDetailList(parameter);
    }

    /**
     * 查询服务商结算日汇总数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 服务商结算日汇总数据的分页数据
     */
    public PagedList<ServiceProviderSettleDateSummary> getPagedSummaryList(QueryServiceProviderSettleParameter parameter) {
        return serviceProviderSettleDao.selectPagedSummaryList(parameter);
    }

    /**
     * 结算日结算
     *
     * @param id
     *         结算日数据id
     * @return 成功状态
     */
    @Transactional
    public CommonOperationResultWidthData settledDate(BigDecimal id) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        ServiceProviderSettleDate settleDate = serviceProviderSettleDao.selectDateById(id);
        settleDate.setStatus(ServiceProviderSettleStatus.SETTLED);
        if (serviceProviderSettleDao.updateDateStatus(settleDate) > 0) {
            if (serviceProviderSettleDao.checkDateIfSettledAll(settleDate.getSettleDate())) {
                ServiceProviderSettleDateSummary shopSettleDateSummary = serviceProviderSettleDao.selectSummaryBySettleDate(settleDate.getSettleDate());
                shopSettleDateSummary.setStatus(ServiceProviderSettleStatus.SETTLED);
                if (serviceProviderSettleDao.updateSummaryStatus(shopSettleDateSummary) > 0) {
                    ret.setResult(CommonOperationResult.Succeeded);
                } else {
                    throw new RuntimeException();
                }
            }
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;

    }

    /**
     * 结算汇总数据结算
     *
     * @param id
     *         数据id
     * @return 成功状态
     */
    @Transactional
    public CommonOperationResultWidthData settledSummary(BigDecimal id) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        ServiceProviderSettleDateSummary shopSettleDateSummary = serviceProviderSettleDao.selectSummaryById(id);
        shopSettleDateSummary.setStatus(ServiceProviderSettleStatus.SETTLED);
        if (serviceProviderSettleDao.updateSummaryStatus(shopSettleDateSummary) > 0) {
            ServiceProviderSettleDate shopSettleDate = new ServiceProviderSettleDate();
            shopSettleDate.setStatus(ServiceProviderSettleStatus.SETTLED);
            shopSettleDate.setSettleDate(shopSettleDateSummary.getSettleDate());
            serviceProviderSettleDao.updateDateStatus(shopSettleDate);
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

}
