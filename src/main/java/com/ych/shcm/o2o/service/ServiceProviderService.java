package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.shcm.o2o.dao.ServiceProviderDao;
import com.ych.shcm.o2o.model.ServiceProvider;
import com.ych.shcm.o2o.model.ServiceProviderBusinessArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 服务商服务类
 * Created by mxp on 2017/7/18.
 */
@Lazy
@Component("shcm.o2o.service.ServiceProviderService")
public class ServiceProviderService {

    @Autowired
    private ServiceProviderDao serviceProviderDao;
    @Autowired
    private MessageSource messageSource;

    /**
     * 新增服务商
     *
     * @param serviceProvider
     *         服务商信息
     * @return 成功数据
     */
    public CommonOperationResultWidthData add(ServiceProvider serviceProvider) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        if (!checkServiceProviderInfo(serviceProvider, ret)) {
            return ret;
        }
        if (serviceProviderDao.insert(serviceProvider) < 0) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription(messageSource.getMessage("system.common.operationFailed", null, Locale.getDefault()));
            return ret;
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 修改服务商信息
     *
     * @param serviceProvider
     *         服务商信息
     * @return 更新的行数
     */
    public CommonOperationResultWidthData modify(ServiceProvider serviceProvider) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        if (!checkServiceProviderInfo(serviceProvider, ret)) {
            return ret;
        }
        ServiceProvider old = serviceProviderDao.selectById(serviceProvider.getId());
        old.setAddress(serviceProvider.getAddress());
        old.setAreaId(serviceProvider.getAreaId());
        old.setName(serviceProvider.getName());
        if (serviceProviderDao.update(serviceProvider) < 0) {
            ret.setResult(CommonOperationResult.Failed);
            ret.setDescription(messageSource.getMessage("system.common.operationFailed", null, Locale.getDefault()));
            return ret;
        }
        ret.setResult(CommonOperationResult.Succeeded);
        return ret;
    }

    /**
     * 检查服务商相关字段
     *
     * @param serviceProvider
     * @param ret
     * @return
     */
    private boolean checkServiceProviderInfo(ServiceProvider serviceProvider, CommonOperationResultWidthData ret) {
        try {
            Assert.notNull(serviceProvider, messageSource.getMessage("service.validate.servicePack.required", null, Locale.getDefault()));
            Assert.notNull(serviceProvider.getAddress(), messageSource.getMessage("service.validate.servicePack.addr.required", null, Locale.getDefault()));
            Assert.notNull(serviceProvider.getName(), messageSource.getMessage("service.validate.servicePack.name.required", null, Locale.getDefault()));
            Assert.notNull(serviceProvider.getAreaId(), messageSource.getMessage("service.validate.ServiceProvider.area.required", null, Locale.getDefault()));
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 根据ID取得服务商信息
     *
     * @param id
     *         ID
     * @return 服务商信息
     */
    public ServiceProvider getById(BigDecimal id) {
        return serviceProviderDao.selectById(id);
    }

    /**
     * 取得指定业务地区的服务商
     *
     * @param areaId
     *         地区ID
     * @return 服务商
     */
    public ServiceProvider selectByBusinessAreaId(String areaId) {

        return serviceProviderDao.selectByBusinessAreaId(areaId);
    }

    /**
     * 新增服务商的业务地区
     *
     * @param serviceProviderId
     *         服务商ID
     * @param businessAreas
     *         业务地区列表
     * @return 操作结果
     */

    public CommonOperationResultWidthData insertBusinessArea(BigDecimal serviceProviderId, Set<String> businessAreas) {
        Assert.notNull(serviceProviderId, messageSource.getMessage("service.validate.ServiceProvider.id.required", null, Locale.getDefault()));
        Assert.notEmpty(businessAreas, messageSource.getMessage("service.validate.ServiceProvider.businessAreas.required", null, Locale.getDefault()));
        List<ServiceProviderBusinessArea> list = new ArrayList<>();
        for (String areaId : businessAreas) {
            ServiceProviderBusinessArea area = new ServiceProviderBusinessArea();
            area.setAreaId(areaId);
            area.setServiceProviderId(serviceProviderId);
            list.add(area);
        }
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        if (serviceProviderDao.insertBusinessArea(list) == list.size()) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.PartialSucceeded);
        }
        return ret;
    }

    /**
     * 改变服务商的业务地区
     *
     * @param serviceProviderId
     *         服务商ID
     * @param newBusinessAreas
     *         业务地区列表
     * @return 操作结果
     */

    public CommonOperationResultWidthData modifyBusinessArea(BigDecimal serviceProviderId, Set<String> newBusinessAreas) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        Assert.notNull(serviceProviderId, messageSource.getMessage("service.validate.ServiceProvider.id.required", null, Locale.getDefault()));
        serviceProviderDao.deleteBusinessArea(serviceProviderId);
        if (CollectionUtils.isEmpty(newBusinessAreas)) {
            return ret;
        }
        List<ServiceProviderBusinessArea> list = new ArrayList<>();
        for (String areaId : newBusinessAreas) {
            ServiceProviderBusinessArea area = new ServiceProviderBusinessArea();
            area.setAreaId(areaId);
            area.setServiceProviderId(serviceProviderId);
            list.add(area);
        }

        if (serviceProviderDao.insertBusinessArea(list) == list.size()) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.PartialSucceeded);
        }
        return ret;
    }

}
