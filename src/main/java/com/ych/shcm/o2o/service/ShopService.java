package com.ych.shcm.o2o.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ych.core.model.BaseWithCommonOperationResult;
import com.ych.core.model.CommonOperationResult;
import com.ych.core.model.PagedList;
import com.ych.shcm.o2o.dao.ShopCarBrandDao;
import com.ych.shcm.o2o.dao.ShopDao;
import com.ych.shcm.o2o.model.CarBrand;
import com.ych.shcm.o2o.model.Shop;
import com.ych.shcm.o2o.model.ShopCarBrand;
import com.ych.shcm.o2o.model.ShopImage;
import com.ych.shcm.o2o.parameter.QueryShopParameter;

/**
 * Created by mxp on 2017/7/17.
 */
@Lazy
@Component("shcm.o2o.service.ShopService")
public class ShopService {

    @Autowired
    private ShopDao shopDao;

    @Autowired
    private ShopCarBrandDao shopCarBrandDao;

    @Autowired
    private MessageSource messageSource;

    /**
     * 获取店铺的分页列表
     *
     * @param parameter
     *         查询参数
     * @return 返回的列表
     */
    public PagedList<Shop> getPagedList(QueryShopParameter parameter) {

        return shopDao.selectPagedList(parameter);
    }

    /**
     * 新增店铺
     *
     * @param shop
     *         店铺
     * @return 成功状态
     */
    @Transactional
    public boolean addShop(Shop shop) {
        if (shopDao.existsConflict(shop)) {
            shopDao.insert(shop);
            for (ShopImage shopImage : shop.getImages()) {
                shopDao.insertImage(shopImage);
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * 修改店铺
     *
     * @param shop
     *         店铺
     * @return 成功状态
     */
    @Transactional
    public boolean modifyShop(Shop shop) {
        Shop old = shopDao.selectById(shop.getId());
        List<ShopImage> oldImages = shopDao.selectImagesByShopId(shop.getId());
        if (old != null) {
            old.setName(shop.getName());
            old.setAddress(shop.getAddress());
            old.setAreaId(shop.getAreaId());
            old.setAverageScore(shop.getAverageScore());
            old.setCoordinateType(shop.getCoordinateType());
            old.setDesc(shop.getDesc());
            old.setDistance(shop.getDistance());
            old.setEvaluationCount(shop.getEvaluationCount());
            old.setLatitude(shop.getLatitude());
            old.setLongitude(shop.getLongitude());
            old.setImagePath(shop.getImagePath());
            old.setModifierId(shop.getModifierId());
            old.setModifyTime(new Date());
            old.setPersonToContact(shop.getPersonToContact());
            old.setPhone(shop.getPhone());
            old.setStatus(shop.getStatus());
            old.setSoneUserId(shop.getSoneUserId());
            shopDao.update(shop);
            for (ShopImage shopImage : shop.getImages()) {
                if (shopDao.existsConflictImages(shopImage)) {
                    shopDao.updateImage(shopImage);
                } else {
                    shopDao.insertImage(shopImage);
                }
            }
            for (ShopImage image : oldImages) {
                boolean deleteFlag = true;
                for (ShopImage shopImage : shop.getImages()) {
                    if (image.getId().compareTo(shopImage.getId()) == 0) {
                        deleteFlag = false;
                    }
                }
                if (deleteFlag) {
                    shopDao.deleteImageById(image);
                }
            }
        } else {
            return false;
        }
        return true;
    }

    /**
     * 修改店铺的坐标
     *
     * @param name
     *         店铺名称
     * @param longitude
     *         经度
     * @param latitude
     *         纬度
     * @param modifierId
     *         修改者ID
     * @return 操作结果
     */
    public BaseWithCommonOperationResult modifyLocation(String name, double longitude, double latitude, BigDecimal modifierId) {
        Shop shop = shopDao.selectDetailByName(name);

        if (shop == null) {
            return new BaseWithCommonOperationResult(CommonOperationResult.NotExists, messageSource.getMessage("shop.name.notExists", null, Locale.getDefault()));
        }

        shop.setLongitude(longitude);
        shop.setLatitude(latitude);
        shop.setModifierId(modifierId);
        shopDao.update(shop);

        return new BaseWithCommonOperationResult(CommonOperationResult.Succeeded);
    }

    /**
     * 取得店铺详情
     *
     * @param id
     *         店铺id
     * @return 店铺详情
     */
    public Shop getShopById(BigDecimal id) {

        Shop shop = shopDao.selectDetailById(id);
        shop.setImages(shopDao.selectImagesByShopId(id));

        return shop;
    }

    /**
     * 新增店铺车型品牌
     *
     * @param carBrands
     *         品牌列表
     * @return 成功状态
     */
    public boolean addShopCarBrand(List<ShopCarBrand> carBrands) {
        return shopCarBrandDao.insert(carBrands) > 0;
    }

    /**
     * 删除店铺车型品牌
     *
     * @param id
     *         id列表
     * @return 成功状态
     */
    public boolean removeShopCarBrand(BigDecimal... id) {
        return shopCarBrandDao.delete(id) > 0;
    }

    /**
     * 查询店铺车型品牌
     *
     * @param shopId
     *         门店ID
     * @return 品牌列表
     */
    public List<CarBrand> removeShopCarBrand(BigDecimal shopId) {
        return shopCarBrandDao.selectByShopId(shopId);
    }

    /**
     * 根据用户ID取得用户绑定的店铺信息
     *
     * @param userId
     *         用户ID
     * @return 绑定的店铺信息列表
     */
    public List<Shop> getByUserId(BigDecimal userId) {

        return shopDao.selectByUserId(userId);
    }
}
