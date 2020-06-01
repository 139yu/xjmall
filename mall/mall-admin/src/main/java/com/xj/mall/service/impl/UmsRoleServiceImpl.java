package com.xj.mall.service.impl;

import com.github.pagehelper.PageHelper;
import com.xj.mall.dao.UmsRoleDao;
import com.xj.mall.dao.UmsRolePermissionRelationDao;
import com.xj.mall.mapper.UmsRoleMapper;
import com.xj.mall.mapper.UmsRoleMenuRelationMapper;
import com.xj.mall.mapper.UmsRolePermissionRelationMapper;
import com.xj.mall.model.*;
import com.xj.mall.service.UmsRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class UmsRoleServiceImpl implements UmsRoleService {

    @Autowired
    private UmsRoleMapper roleMapper;
    @Autowired
    private UmsRolePermissionRelationDao rolePermissionRelationdao;
    @Autowired
    private UmsRolePermissionRelationMapper rolePermissionRelationMapper;
    @Autowired
    private UmsRoleMenuRelationMapper roleMenuRelationMapper;

    @Autowired
    private UmsRoleDao roleDao;

    @Override
    public int create(UmsRole role) {
        role.setCreateTime(new Date());
        role.setAdminCount(0);
        role.setSort(0);
        return roleMapper.insert(role);
    }

    @Override
    public int update(Long id, UmsRole role) {
        role.setId(id);
        return roleMapper.updateByPrimaryKeySelective(role);
    }

    @Override
    public int delete(List<Long> ids) {
        UmsRoleExample example = new UmsRoleExample();
        example.createCriteria().andIdIn(ids);
        int count = roleMapper.deleteByExample(example);
        return count;
    }

    @Override
    public List<UmsPermission> getPermissionList(Long roleId) {
        return rolePermissionRelationdao.getPermissionList(roleId);
    }

    @Override
    public int updatePermission(Long roleId, List<Long> permissionIds) {
        //删除原有关系
        UmsRolePermissionRelationExample relationExample = new UmsRolePermissionRelationExample();
        relationExample.createCriteria().andRoleIdEqualTo(roleId);
        rolePermissionRelationMapper.deleteByExample(relationExample);
        //批量插入关系
        List<UmsRolePermissionRelation> relationList = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            UmsRolePermissionRelation relation = new UmsRolePermissionRelation();
            relation.setPermissionId(permissionId);
            relation.setRoleId(roleId);
            relationList.add(relation);
        }
        return rolePermissionRelationdao.insertList(relationList);
    }

    @Override
    public List<UmsRole> list() {
        return roleMapper.selectByExample(new UmsRoleExample());
    }

    @Override
    public List<UmsRole> list(String keyword, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        UmsRoleExample example = new UmsRoleExample();
        if (!StringUtils.isEmpty(keyword)) {
            example.createCriteria().andNameLike("%" + keyword + "%");
        }
        return roleMapper.selectByExample(example);
    }

    @Override
    public List<UmsMenu> getMenuList(Long adminId) {
        return roleDao.getMenuList(adminId);
    }

    @Override
    public List<UmsMenu> listMenu(Long roleId) {
        return roleDao.getMenuListByRoleId(roleId);
    }

    @Override
    public List<UmsResource> listResource(Long roleId) {
        return null;
    }

    @Override
    public int allocMenu(Long roleId, List<Long> menuIds) {
        //先删除原有关系
        UmsRoleMenuRelationExample example=new UmsRoleMenuRelationExample();
        example.createCriteria().andRoleIdEqualTo(roleId);
        roleMenuRelationMapper.deleteByExample(example);
        //批量插入新关系
        for (Long menuId : menuIds) {
            UmsRoleMenuRelation relation = new UmsRoleMenuRelation();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuRelationMapper.insert(relation);
        }
        return menuIds.size();
    }

    @Override
    public int allocResource(Long roleId, List<Long> resourceIds) {
        return 0;
    }
}
