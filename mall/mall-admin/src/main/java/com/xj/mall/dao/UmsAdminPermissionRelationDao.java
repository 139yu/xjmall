package com.xj.mall.dao;


import com.xj.mall.model.UmsAdminPermissionRelation;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UmsAdminPermissionRelationDao {

    int insertList(@Param("list") List<UmsAdminPermissionRelation> list);
}
