package com.ych.shcm.o2o.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.*;
import com.ych.shcm.o2o.model.*;
import com.ych.shcm.o2o.parameter.QueryCarBrandListParameter;
import com.ych.shcm.o2o.parameter.QueryCarSeriesListParameter;

/**
 * 车型服务
 * 
 * @author U
 *
 */
@Lazy
@Component("shcm.o2o.service.CarModelService")
public class CarModelService {

	/**
	 * 品牌Dao
	 */
	@Autowired
	private CarBrandDao brandDao;

	/**
	 * 制造厂Dao
	 */
	@Autowired
	private CarFactoryDao factoryDao;

	/**
	 * 车系Dao
	 */
	@Autowired
	private CarSeriesDao seriesDao;

	/**
	 * 车系年份Dao
	 */
	@Autowired
	private CarSeriesYearDao seriesYearDao;

	/**
	 * 车型Dao
	 */
	@Autowired
	private CarModelDao modelDao;

	/**
	 * 根据ID获取车型品牌
	 * 
	 * @param id
	 *            ID
	 * @return 车型品牌
	 */
	public CarBrand getBrandById(BigDecimal id) {
		return brandDao.selectById(id, false);
	}

	/**
	 * @return 获取所有的可用车型品牌并按首字母排序
	 */
	public List<CarBrand> getBrandList() {
		return brandDao.selectList();
	}

	/**
	 * 查询车型品牌的分页列表
	 * 
	 * @param parameter
	 *            查询参数
	 * @return 分页列表
	 */
	public PagedList<CarBrand> queryBrandPagedList(QueryCarBrandListParameter parameter) {
		return brandDao.selectPagedList(parameter);
	}

	/**
	 * 根据品牌ID获取可用的车型制造厂家,并根据首字母排序
	 * 
	 * @param brandId
	 *            品牌ID
	 * @return 车型制造厂家列表
	 */
	public List<CarFactory> getByBrandId(BigDecimal brandId) {
		return factoryDao.selectByBrandId(brandId);
	}

	/**
	 * 根据品牌和制造厂查询可用的车系列表,两个参数不能都为null.<br>
	 * 车系列表按照首字母排序
	 * 
	 * @param brandId
	 *            品牌ID
	 * @param factoryId
	 *            制造厂ID
	 * @return 车系列表
	 */
	public List<CarSeries> getSeriesList(BigDecimal brandId, BigDecimal factoryId) {
		return seriesDao.selectList(brandId, factoryId);
	}

	/**
	 * 分页查询车系
	 * 
	 * @param parameter
	 *            查询参数
	 * @return 分页列表数据
	 */
	public PagedList<CarSeries> querySeriesPagedList(QueryCarSeriesListParameter parameter) {
		return seriesDao.selectPagedList(parameter);
	}

	/**
	 * 根据ID查询车系
	 * 
	 * @param id
	 *            ID
	 * @return 车系
	 */
	public CarSeries getSeriesById(BigDecimal id) {
		return seriesDao.selectById(id, false);
	}

	/**
	 * 根据车系ID查询可用的年份列表,并且按照年份升序排列.
	 * 
	 * @param seriesId
	 *            车系ID
	 * @return 车系年份列表
	 */
	public List<CarSeriesYear> getYearsBySeriesId(BigDecimal seriesId) {
		return seriesYearDao.selectBySeriesId(seriesId);
	}

	/**
	 * 根据车系ID和年份查询可用的车型列表并按照车型排序排列
	 * 
	 * @param seriesId
	 *            车系ID
	 * @param year
	 *            年份
	 * @return 车型列表
	 */
	public List<CarModel> getModelsBySeriesYear(BigDecimal seriesId, int year) {
		return modelDao.selectBySeriesYear(seriesId, year, false);
	}

	/**
	 * 根据Id获取车型信息
	 * 
	 * @param id
	 *            ID
	 * @return 车型信息
	 */
	public CarModel getModelById(BigDecimal id) {
		return modelDao.selectById(id, false);
	}


    /**
     * 根据Id获取品牌工厂
     *
     * @param id
     *            ID
     * @return 车型信息
     */
    public CarFactory getFactoryById(BigDecimal id) {
        return factoryDao.selectById(id, false);
    }

    /**
     * 根据车系ID查询年代列表
     *
     * @param seriesId
     *         车系ID
     * @return 车系年份数据
     */
    public List<CarSeriesYear> getSeriesYearList(BigDecimal seriesId) {
        return seriesYearDao.selectBySeriesId(seriesId);
    }

    /**
     * 查询车型，所有车系与年份
     *
     * @param seriesId
     *         车系ID
     * @param yearId
     *         年份ID
     * @return 车型数据
     */
    public List<CarModel> getModelList(BigDecimal seriesId, BigDecimal yearId) {
        CarSeriesYear year = seriesYearDao.selectById(yearId, false);
        if (year == null) {
            return Collections.EMPTY_LIST;
        }
        return modelDao.selectBySeriesYear(seriesId, year.getYear(), false);
    }

}
