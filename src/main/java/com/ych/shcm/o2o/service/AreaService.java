package com.ych.shcm.o2o.service;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.ych.shcm.o2o.dao.AreaDao;
import com.ych.shcm.o2o.model.Area;

/**
 * 地区服务
 * <p>
 * Created by U on 2017/7/5.
 */
@Lazy
@Component("shcm.o2o.service.AreaService")
public class AreaService {

    /**
     * 地区Dao
     */
    @Autowired
    private AreaDao areaDao;

    /**
     * 根据地区ID查询地区信息
     *
     * @param id
     *         地区ID
     * @return 地区信息
     */
    public Area getAreaById(String id) {
        return areaDao.selectById(id);
    }

    /**
     * 根据父节点查询地区列表
     *
     * @param parentId
     *         父节点ID
     * @return 制定地区的子节点列表
     */
    public List<Area> getChildren(String parentId) {
        return areaDao.selectByParentId(parentId);
    }

    /**
     * 查询从最顶层祖先到指定节点的列表
     *
     * @param id
     *         地区ID
     * @return 最顶层祖先到指定节点的列表
     */
    public List<Area> getFromAncestors(String id) {
        return areaDao.selectFromAncestors(id);
    }

    /**
     * 查询从最顶层祖先到指定节点的ID列表
     *
     * @param id
     *         地区ID
     * @return 最顶层祖先到指定节点的ID列表
     */
    public List<String> getFromAncestorIds(String id) {
        return areaDao.selectFromAncestorIds(id);
    }

    /**
     * 查询指定节点并包含其所有的子孙节点
     *
     * @param id
     *         地区ID
     * @return 指定节点并包含其所有的子孙节点
     */
    public List<Area> getToDescendants(String id) {
        return areaDao.selectToDescendants(id);
    }

    /**
     * 查询指定节点并包含其所有的子孙节点的ID
     *
     * @param id
     *         地区ID
     * @return 指定节点并包含其所有的子孙节点的ID
     */
    public List<String> getToDescendantIds(String id) {
        return areaDao.selectToDescendantIds(id);
    }

    /**
     * 获取地区从顶级节点连接而下的完整名称
     *
     * @param id
     *         地区ID
     * @return 地区从顶级节点连接而下的完整名称
     */
    public String getFullName(String id) {
        return StringUtils.join(areaDao.selectFromAncestors(id), null);
    }

    /**
     * 获取地区从顶级节点连接而下再加上一个详细地址的完整名称
     *
     * @param id
     *         地区ID
     * @param address
     *         详细地址
     * @return 地区从顶级节点连接而下再加上一个详细地址的完整名称
     */
    public String getFullAddress(String id, String address) {
        return StringUtils.join(getFullName(id), address);
    }

}
