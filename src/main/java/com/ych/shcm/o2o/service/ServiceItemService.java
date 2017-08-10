package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.ServiceItemDao;
import com.ych.shcm.o2o.model.ServiceItem;
import com.ych.shcm.o2o.model.ServiceStatus;
import com.ych.shcm.o2o.parameter.QueryServiceItemListParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.math.BigDecimal;

/**
 * 服务项目服务
 */
@Lazy
@Component("shcm.o2o.service.ServiceItemService")
public class ServiceItemService {

    @Autowired
    private ServiceItemDao serviceItemDao;

    /**
     * 根据ID取得服务项目详情
     *
     * @param id
     *         服务项目ID
     * @return 服务项目
     */
    public ServiceItem getServiceItemDetail(BigDecimal id) {
        return serviceItemDao.selectById(id);
    }

    /**
     * 修改服务项目数据
     *
     * @param serviceItem
     *         服务项目数据
     * @return 结果
     */
    public CommonOperationResultWidthData modify(ServiceItem serviceItem) {
        ServiceItem old = serviceItemDao.selectById(serviceItem.getId());
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        try {
            Assert.notNull(serviceItem, "服务项目不能为空");
            Assert.notNull(serviceItem.getComment(), "服务项目备注不能为空");
            Assert.notNull(serviceItem.getIconPath(), "服务项目图标不能为空");
            Assert.notNull(serviceItem.getBrokerage(), "服务项目佣金不能为空");
            Assert.notNull(serviceItem.getType(), "服务项目类型不能为空");
            Assert.notNull(serviceItem.getPrice(), "服务项目价格不能为空");
            Assert.notNull(serviceItem.getName(), "服务项目名不能为空");
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        ;
        old.setComment(serviceItem.getComment());
        old.setIconPath(serviceItem.getIconPath());
        old.setName(serviceItem.getName());
        old.setBrokerage(serviceItem.getBrokerage());
        old.setPrice(serviceItem.getPrice());
        old.setType(serviceItem.getType());
        if (serviceItemDao.update(old) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

    /**
     * 修改服务项目状态
     *
     * @param serviceItemId
     *         服务项目id
     * @param serviceItemId
     *         服务项目状态
     * @return 结果
     */
    public CommonOperationResultWidthData changeStatus(BigDecimal serviceItemId, ServiceStatus status) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        ServiceItem old = serviceItemDao.selectById(serviceItemId);
        try {
            Assert.notNull(serviceItemId, "服务项目ID不能为空");
            Assert.notNull(status, "服务项目状态不能为空");
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        old.setStatus(status);
        if (serviceItemDao.update(old) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

    /**
     * 新增服务项目
     *
     * @param serviceItem
     *         服务项目数据
     * @return 结果
     */

    public CommonOperationResultWidthData add(ServiceItem serviceItem) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        try {
            Assert.notNull(serviceItem, "服务项目不能为空");
            Assert.notNull(serviceItem.getComment(), "服务项目备注不能为空");
            Assert.notNull(serviceItem.getIconPath(), "服务项目图标不能为空");
            Assert.notNull(serviceItem.getBrokerage(), "服务项目佣金不能为空");
            Assert.notNull(serviceItem.getType(), "服务项目类型不能为空");
            Assert.notNull(serviceItem.getPrice(), "服务项目价格不能为空");
            Assert.notNull(serviceItem.getName(), "服务项目名不能为空");
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        serviceItem.setStatus(ServiceStatus.ENABLED);
        if (serviceItemDao.insert(serviceItem) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;

    }

    /**
     * 删除服务项目
     *
     * @param id
     *         服务项目id
     * @return 结果
     */

    public CommonOperationResultWidthData remove(BigDecimal id) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        if (serviceItemDao.delete(id) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

    /**
     * 取得服务项目列表
     *
     * @param parameter
     *         查询参数
     * @return 查询结果
     */
    public PagedList<ServiceItem> getServiceItemList(QueryServiceItemListParameter parameter) {
        return serviceItemDao.selectServiceItemList(parameter);

    }
}
