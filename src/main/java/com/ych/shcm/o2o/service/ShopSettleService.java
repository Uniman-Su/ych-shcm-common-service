package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.ShopSettleDao;
import com.ych.shcm.o2o.model.ShopSettleDate;
import com.ych.shcm.o2o.model.ShopSettleDateSummary;
import com.ych.shcm.o2o.model.ShopSettleDetail;
import com.ych.shcm.o2o.model.ShopSettleStatus;
import com.ych.shcm.o2o.parameter.QueryShopSettleParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 门店结算服务
 * Created by mxp on 2017/7/18.
 */
@Lazy
@Component("shcm.o2o.service.ShopSettleService")
public class ShopSettleService {

    @Autowired
    private ShopSettleDao shopSettleDao;

    /**
     * 查询店铺结算日数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 店铺结算日数据的分页数据
     */
    public PagedList<ShopSettleDate> getPagedDateList(QueryShopSettleParameter parameter) {
        return shopSettleDao.selectPagedDateList(parameter);
    }

    /**
     * 查询店铺结算明细的分页列表
     *
     * @param parameter
     *         查询参数
     * @return 店铺结算明细的分页列表
     */
    public PagedList<ShopSettleDetail> getPagedDetailList(QueryShopSettleParameter parameter) {
        return shopSettleDao.selectPagedDetailList(parameter);
    }

    /**
     * 查询店铺结算日汇总数据的分页数据
     *
     * @param parameter
     *         查询参数
     * @return 店铺结算日汇总数据的分页数据
     */
    public PagedList<ShopSettleDateSummary> getPagedSummaryList(QueryShopSettleParameter parameter) {
        return shopSettleDao.selectPagedSummaryList(parameter);
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
        ShopSettleDate settleDate = shopSettleDao.selectDateById(id);
        settleDate.setStatus(ShopSettleStatus.SETTLED);
        if (shopSettleDao.updateDateStatus(settleDate) > 0) {
            if (shopSettleDao.checkDateIfSettledAll(settleDate.getSettleDate())) {
                ShopSettleDateSummary shopSettleDateSummary = shopSettleDao.selectSummaryBySettleDate(settleDate.getSettleDate());
                shopSettleDateSummary.setStatus(ShopSettleStatus.SETTLED);
                if (shopSettleDao.updateSummaryStatus(shopSettleDateSummary) > 0) {
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

        ShopSettleDateSummary shopSettleDateSummary = shopSettleDao.selectSummaryById(id);
        shopSettleDateSummary.setStatus(ShopSettleStatus.SETTLED);
        if (shopSettleDao.updateSummaryStatus(shopSettleDateSummary) > 0) {
            ShopSettleDate shopSettleDate = new ShopSettleDate();
            shopSettleDate.setStatus(ShopSettleStatus.SETTLED);
            shopSettleDate.setSettleDate(shopSettleDateSummary.getSettleDate());
            shopSettleDao.updateDateStatus(shopSettleDate);
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

}
