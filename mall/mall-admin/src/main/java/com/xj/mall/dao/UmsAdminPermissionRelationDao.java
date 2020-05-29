package com.xj.mall.dao;


import com.xj.mall.model.UmsAdminPermissionRelation;
import org.apache.ibatis.annotations.Param;

public interface UmsAdminPermissionRelationDao {

int insertList(@Param("list") UmsAdminPermissionRelation list);
}
