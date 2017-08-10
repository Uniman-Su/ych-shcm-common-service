package com.ych.shcm.o2o.service;

import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.CarBrandDao;
import com.ych.shcm.o2o.dao.CarDao;
import com.ych.shcm.o2o.dao.CarFactoryDao;
import com.ych.shcm.o2o.dao.CarModelDao;
import com.ych.shcm.o2o.dao.CarSeriesDao;
import com.ych.shcm.o2o.dao.CarSeriesGroupDao;
import com.ych.shcm.o2o.dao.UserCarDao;
import com.ych.shcm.o2o.model.Car;
import com.ych.shcm.o2o.model.CarExpiredMaintenanceInfo;
import com.ych.shcm.o2o.model.CarModel;
import com.ych.shcm.o2o.model.CarSeriesGroup;
import com.ych.shcm.o2o.model.ServicePack;
import com.ych.shcm.o2o.model.UserCar;
import com.ych.shcm.o2o.parameter.QueryCarExpiredMaintenanceParameter;
import com.ych.shcm.o2o.parameter.QueryCarParameter;
import com.ych.shcm.o2o.parameter.QueryUserCarParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 车辆服务
 */
@Lazy
@Component("shcm.o2o.service.CarService")
public class CarService {

    @Autowired
    private CarDao carDao;

    @Autowired
    private CarModelDao carModelDao;

    @Autowired
    private CarSeriesGroupDao carSeriesGroupDao;

    @Autowired
    private UserCarDao userCarDao;

    @Autowired
    private CarSeriesDao carSeriesDao;

    @Autowired
    private CarBrandDao carBrandDao;

    @Autowired
    private CarFactoryDao carFactoryDao;

    /**
     * 新增车辆信息
     *
     * @param car
     *         车辆信息
     * @return 插入的行数
     */
    public void addCar(Car car) {
        carDao.insert(car);
    }

    /**
     * 改变车辆信息
     *
     * @param car
     *         车辆信息
     * @return 更新的行数
     */
    public void modifyCar(Car car) {
        carDao.update(car);
    }

    /**
     * 根据ID取得车辆信息
     *
     * @param id
     *         车辆ID
     * @return 车辆信息
     */
    public Car getCarById(BigDecimal id) {
        return carDao.selectById(id);
    }

    /**
     * 根据VIN码取得车辆信息
     *
     * @param vin
     *         VIN码
     * @return 车辆信息
     */
    public Car getCarByVin(String vin) {
        return carDao.selectByVin(vin);
    }

    /**
     * 取得车辆列表
     *
     * @param parameter
     *         取得参数
     * @return 分页数据
     */
    public PagedList<Car> getCarList(QueryCarParameter parameter) {
        PagedList<Car> ret = carDao.selectPagedList(parameter);
        for (Car car : ret.getList()) {
            car.setCarModel(carModelDao.selectById(car.getModelId(), false));
            car.getCarModel().setSeries(carSeriesDao.selectById(car.getCarModel().getSeriesId(), false));
            car.getCarModel().getSeries().setCarBrand(carBrandDao.selectById(car.getCarModel().getSeries().getBrandId(), false));
            car.getCarModel().getSeries().setCarFactory(carFactoryDao.selectById(car.getCarModel().getSeries().getFactoryId(), false));
        }
        return ret;
    }

    /**
     * 增加车辆维护信息
     *
     * @param expiredMaintenanceInfo
     *         车辆维护信息
     * @return 插入的行数
     */
    public void addExpiredMaintenanceInfo(CarExpiredMaintenanceInfo expiredMaintenanceInfo) {
        carDao.insertExpiredMaintenanceInfo(expiredMaintenanceInfo);

    }

    /**
     * 根据ID取得车辆过期维护信息
     *
     * @param id
     *         ID
     * @return 车辆过期维护信息
     */
    public CarExpiredMaintenanceInfo getExpiredMaintenanceInfoById(BigDecimal id) {
        return carDao.selectExpiredMaintenanceInfoById(id);
    }

    /**
     * 根据车辆ID和生效时间车辆过期维护信息
     *
     * @param carId
     *         车辆ID
     * @param effectTime
     *         生效时间
     * @return 车辆过期维护信息
     */
    public CarExpiredMaintenanceInfo getExpiredMaintenanceInfoByCarIdAndEffectTime(BigDecimal carId, Date effectTime) {
        return carDao.selectExpiredMaintenanceInfoByCarIdAndEffectTime(carId, effectTime);
    }

    /**
     * 取得车辆过期维护信息列表
     *
     * @param parameter
     *         取得参数
     * @return 分页数据
     */
    public PagedList<QueryCarExpiredMaintenanceParameter> getPagedExpiredMaintenanceInfoList(QueryCarExpiredMaintenanceParameter parameter) {
        return carDao.selectPagedExpiredMaintenanceInfoList(parameter);
    }

    /**
     * 查询车型可用服务包
     *
     * @param carModelId
     *         车型id
     * @return 服务包列表
     */
    public List<ServicePack> getServicePackOfCar(BigDecimal carModelId) {

        CarModel carModel = carModelDao.selectById(carModelId, false);
        CarSeriesGroup carSeriesGroup = carSeriesGroupDao.selectCarSeriesGroupBySeriesId(carModel.getSeriesId());

        return carSeriesGroupDao.selectServicePackByGroupId(carSeriesGroup.getId());

    }

    /**
     * 查询分页的用户车辆列表
     *
     * @param userId
     *         用户id
     * @return 用户车辆
     */
    public List<UserCar> getUserCarList(BigDecimal userId) {
        QueryUserCarParameter parameter = new QueryUserCarParameter();
        parameter.setUserId(userId);
        parameter.setPageIndex(0);
        parameter.setPageSize(Integer.MAX_VALUE);
        return userCarDao.selectPagedUserCarList(parameter).getList();
    }

}
