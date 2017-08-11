package com.ych.shcm.o2o.service;

import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.CommonOperationResultWidthData;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.CarModelDao;
import com.ych.shcm.o2o.dao.ServicePackDao;
import com.ych.shcm.o2o.dao.ServicePackItemDao;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.CarModel;
import com.ych.shcm.o2o.model.ServiceItem;
import com.ych.shcm.o2o.model.ServiceItemType;
import com.ych.shcm.o2o.model.ServicePack;
import com.ych.shcm.o2o.model.ServiceStatus;
import com.ych.shcm.o2o.parameter.QueryServicePackListParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 服务包服务
 */
@Lazy
@Component("shcm.o2o.service.ServicePackService")
public class ServicePackService {

    @Autowired
    private ServicePackDao servicePackDao;

    @Autowired
    private ServicePackItemDao servicePackItemDao;
    @Autowired
    private CarModelDao carModelDao;
    @Autowired
    private MessageSource messageSource;

    /**
     * 根据ID取得服务包详情
     *
     * @param id
     *         服务包ID
     * @return 服务包
     */
    public ServicePack getServicePackDetail(BigDecimal id) {
        return servicePackDao.selectById(id);
    }

    /**
     * 修改服务包数据
     *
     * @param servicePack
     *         服务包数据
     * @return 结果
     */
    public CommonOperationResultWidthData modify(ServicePack servicePack) {
        ServicePack old = servicePackDao.selectById(servicePack.getId());
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        try {
            Assert.notNull(servicePack, messageSource.getMessage("service.validate.servicePack.required", null, Locale.getDefault()));
            Assert.notNull(servicePack.getComment(), messageSource.getMessage("service.validate.servicePack.comment.required", null, Locale.getDefault()));
            Assert.notNull(servicePack.getIconPath(), messageSource.getMessage("service.validate.servicePack.icon.required", null, Locale.getDefault()));
            Assert.notNull(servicePack.getName(), messageSource.getMessage("service.validate.servicePack.name.required", null, Locale.getDefault()));
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        ;
        old.setComment(servicePack.getComment());
        old.setIconPath(servicePack.getIconPath());
        old.setName(servicePack.getName());
        if (servicePackDao.update(old) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

    /**
     * 修改服务包状态
     *
     * @param servicePackId
     *         服务包id
     * @param servicePackId
     *         服务包状态
     * @return 结果
     */
    public CommonOperationResultWidthData changeStatus(BigDecimal servicePackId, ServiceStatus status) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        ServicePack old = servicePackDao.selectById(servicePackId);
        try {
            Assert.notNull(servicePackId, messageSource.getMessage("service.validate.servicePack.id.required", null, Locale.getDefault()));
            Assert.notNull(status, messageSource.getMessage("service.validate.servicePack.status.required", null, Locale.getDefault()));
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        old.setStatus(status);
        if (servicePackDao.update(old) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

    /**
     * 新增服务包
     *
     * @param servicePack
     *         服务包数据
     * @return 结果
     */

    public CommonOperationResultWidthData add(ServicePack servicePack) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        try {
            Assert.notNull(servicePack, messageSource.getMessage("service.validate.servicePack.required", null, Locale.getDefault()));
            Assert.notNull(servicePack.getComment(), messageSource.getMessage("service.validate.servicePack.comment.required", null, Locale.getDefault()));
            Assert.notNull(servicePack.getIconPath(), messageSource.getMessage("service.validate.servicePack.icon.required", null, Locale.getDefault()));
            Assert.notNull(servicePack.getName(), messageSource.getMessage("service.validate.servicePack.name.required", null, Locale.getDefault()));
        } catch (IllegalArgumentException e) {
            ret.setResult(CommonOperationResult.IllegalArguments);
            ret.setDescription(e.getMessage());
            return ret;
        }
        servicePack.setStatus(ServiceStatus.ENABLED);
        if (servicePackDao.insert(servicePack) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;

    }

    /**
     * 删除服务包
     *
     * @param id
     *         服务包id
     * @return 结果
     */

    public CommonOperationResultWidthData remove(BigDecimal id) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();

        if (servicePackDao.delete(id) > 0) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

    /**
     * 取得服务包列表
     *
     * @param parameter
     *         查询参数
     * @return 查询结果
     */
    public PagedList<ServicePack> getServicePackList(QueryServicePackListParameter parameter) {
        return servicePackDao.selectServicePackList(parameter);

    }

    /**
     * 选择服务包项目
     *
     * @param packId
     * @param itemIds
     * @return
     */
    public CommonOperationResultWidthData addServicePackAndItemRelationShip(BigDecimal packId, Set<BigDecimal> itemIds) {
        CommonOperationResultWidthData ret = new CommonOperationResultWidthData();
        int count = servicePackItemDao.insertList(packId, itemIds);
        if (count == itemIds.size()) {
            ret.setResult(CommonOperationResult.Succeeded);
        } else if (count > 0) {
            ret.setResult(CommonOperationResult.PartialSucceeded);
        } else {
            ret.setResult(CommonOperationResult.Failed);
        }
        return ret;
    }

    /**
     * 取得服务包价格
     *
     * @param items
     * @param car
     * @return
     */
    public BigDecimal getPriceOfPack(List<ServiceItem> items, Car car) {
        BigDecimal packPrice = BigDecimal.ZERO;
        for (ServiceItem theItem : items) {
            if (theItem.getType() == ServiceItemType.NORMAL) {
                packPrice = packPrice.add(theItem.getPrice());
            } else {
                CarModel carModel = carModelDao.selectById(car.getModelId(), false);
                int num = carModel.getEngineOilCapacity().divide(BigDecimal.ONE, 0, BigDecimal.ROUND_UP).intValue();
                if (num < 4) {
                    num = 4;
                }
                packPrice = packPrice.add(theItem.getPrice().multiply(new BigDecimal(num)));
            }
        }
        return packPrice;
    }
}
