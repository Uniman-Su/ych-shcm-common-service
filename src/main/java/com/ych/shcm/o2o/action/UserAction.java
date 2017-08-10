package com.ych.shcm.o2o.action;

import com.ych.shcm.o2o.model.*;
import com.ych.shcm.o2o.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 用户侧的Action
 * <p>
 * Created by U on 2017/7/18.
 */
@Component(UserAction.NAME)
public class UserAction extends BaseAction {

    public static final String NAME = "shcm.o2o.action.UserAction";

    /**
     * 用户的属性名称
     */
    private static final String USER_REQUEST_ATTR_NAME = "User";

    /**
     * 选择的车辆的属性名称
     */
    private static final String SELECTED_CAR_REQUEST_ATTR_NAME = "SelectedCar";

    /**
     * 选择的店铺属性名称
     */
    private static final String SELECTED_SHOP_REQUEST_ATTR_NAME = "SelectedShop";

    @Autowired
    private UserService userService;

    @Autowired
    private CarService carService;

    @Autowired
    private CarModelService carModelService;

    @Autowired
    private UploadService uploadService;

    @Autowired
    private ShopService shopService;

    /**
     * @return 用户信息
     */
    protected User getUser() {
        User user = (User) getRequest().getAttribute(USER_REQUEST_ATTR_NAME);
        if (user == null) {
            user = userService.getUserById(getAuthorizeResult().getAudienceId());
            getRequest().setAttribute(USER_REQUEST_ATTR_NAME, user);
        }
        return user;
    }

    /**
     * @return 车辆信息
     */
    protected Car getCar() {
        Car car = (Car) getRequest().getAttribute(SELECTED_CAR_REQUEST_ATTR_NAME);
        if (car == null) {
            BigDecimal carId = getAuthorizeResult().getCarId();
            if (carId != null) {
                car = carService.getCarById(carId);

                CarModel carModel = carModelService.getModelById(car.getModelId());
                CarSeries carSeries = carModelService.getSeriesById(carModel.getSeriesId());
                CarFactory carFactory = carModelService.getFactoryById(carSeries.getFactoryId());
                CarBrand carBrand = carModelService.getBrandById(carSeries.getBrandId());

                carModel.setSeries(carSeries);
                carSeries.setCarFactory(carFactory);
                carSeries.setCarBrand(carBrand);
                car.setCarModel(carModel);
                carBrand.setLogoPath(uploadService.getFileUrl(carBrand.getLogoPath()));

                getRequest().setAttribute(SELECTED_CAR_REQUEST_ATTR_NAME, car);
            }
        }
        return car;
    }

    /**
     *
     * @return 门店信息
     */
    protected Shop getShop() {
        Shop shop = (Shop) getRequest().getAttribute(SELECTED_SHOP_REQUEST_ATTR_NAME);

        if (shop == null) {
            BigDecimal shopId = getAuthorizeResult().getShopId();
            if (shopId != null) {
                shop = shopService.getShopById(shopId);
                getRequest().setAttribute(SELECTED_SHOP_REQUEST_ATTR_NAME, shop);
            }
        }

        return shop;
    }

    @Override
    protected JWTService.WXMPAuthorizeResult getAuthorizeResult() {
        return (JWTService.WXMPAuthorizeResult) super.getAuthorizeResult();
    }
}
