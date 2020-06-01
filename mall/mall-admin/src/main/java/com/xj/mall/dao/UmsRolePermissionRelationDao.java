package com.xj.mall.dao;

import com.xj.mall.model.UmsPermission;
import com.xj.mall.model.UmsRolePermissionRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UmsRolePermissionRelationDao {
    /**
     * 获取权限
     * @param roleId
     * @return
     */
    List<UmsPermission> getPermissionList(@Param("roleId") Long roleId);
    /**
     * 批量插入角色和权限关系
     */
    int insertList(@Param("list")List<UmsRolePermissionRelation> list);
}
