package com.xj.mall.dao;

import com.xj.mall.model.UmsPermission;
import com.xj.mall.model.UmsRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 后台用户与角色dao
 */
public interface UmsAdminRoleRelationDao {

    /**
     * 批量插入用户与角色关系
     * @param adminId
     * @param roleIds
     * @return
     */
    int insertList(Long adminId, List<Long> roleIds);

    /**
     * 根据用户id获取角色列表
     * @param adminId
     * @return
     */
    List<UmsRole> getRoleList(@Param("adminId") Long adminId);

    /**
     * 根据用户id获取用户权限
     * @param adminId
     * @return
     */
    List<UmsPermission> getPermissionList(@Param("adminId") Long adminId);
}
