package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.ServiceItemDao;
import com.ych.shcm.o2o.model.ServiceItem;
import com.ych.shcm.o2o.model.ServiceStatus;
import com.ych.shcm.o2o.parameter.QueryServiceItemListParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * 服务项目服务
 */
@Lazy
@Component("shcm.o2o.service.ServiceItemService")
public class ServiceItemService {

    @Autowired
    private ServiceItemDao serviceItemDao;
    @Autowired
    private MessageSource messageSource;

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
            Assert.notNull(serviceItem, messageSource.getMessage("service.validate.serviceItem.required", null, Locale.getDefault()));
            Assert.hasLength(serviceItem.getComment(), messageSource.getMessage("service.validate.serviceItem.comment.required", null, Locale.getDefault()));
            Assert.hasLength(serviceItem.getIconPath(), messageSource.getMessage("service.validate.serviceItem.icon.required", null, Locale.getDefault()));
            Assert.notNull(serviceItem.getBrokerage(), messageSource.getMessage("service.validate.serviceItem.brokerage.required", null, Locale.getDefault()));
            Assert.notNull(serviceItem.getType(), messageSource.getMessage("service.validate.serviceItem.type.required", null, Locale.getDefault()));
            Assert.notNull(serviceItem.getPrice(), messageSource.getMessage("service.validate.serviceItem.price.required", null, Locale.getDefault()));
            Assert.hasLength(serviceItem.getName(), messageSource.getMessage("service.validate.serviceItem.name.required", null, Locale.getDefault()));
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
            Assert.notNull(serviceItemId, messageSource.getMessage("service.validate.serviceItem.id.required", null, Locale.getDefault()));
            Assert.notNull(status, messageSource.getMessage("service.validate.serviceItem.status.required", null, Locale.getDefault()));
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
            Assert.notNull(serviceItem, messageSource.getMessage("service.validate.serviceItem.required", null, Locale.getDefault()));
            Assert.hasLength(serviceItem.getComment(), messageSource.getMessage("service.validate.ServiceProvider.addr.required", null, Locale.getDefault()));
            Assert.hasLength(serviceItem.getIconPath(), messageSource.getMessage("service.validate.serviceItem.icon.required", null, Locale.getDefault()));
            Assert.notNull(serviceItem.getBrokerage(), messageSource.getMessage("service.validate.serviceItem.brokerage.required", null, Locale.getDefault()));
            Assert.notNull(serviceItem.getType(), messageSource.getMessage("service.validate.serviceItem.type.required", null, Locale.getDefault()));
            Assert.notNull(serviceItem.getPrice(), messageSource.getMessage("service.validate.serviceItem.price.required", null, Locale.getDefault()));
            Assert.hasLength(serviceItem.getName(), messageSource.getMessage("service.validate.serviceItem.name.required", null, Locale.getDefault()));
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
