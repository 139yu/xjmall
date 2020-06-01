package com.xj.mall.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.github.pagehelper.PageHelper;
import com.xj.mall.dao.*;
import com.xj.mall.dto.AdminUserDetails;
import com.xj.mall.dto.UmsAdminParam;
import com.xj.mall.dto.UpdateAdminPasswordParam;
import com.xj.mall.mapper.UmsAdminLoginLogMapper;
import com.xj.mall.mapper.UmsAdminMapper;
import com.xj.mall.mapper.UmsAdminPermissionRelationMapper;
import com.xj.mall.mapper.UmsAdminRoleRelationMapper;
import com.xj.mall.model.*;
import com.xj.mall.security.utils.JwtTokenUtil;
import com.xj.mall.service.UmsAdminCacheService;
import com.xj.mall.service.UmsAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UmsAdminServiceImpl implements UmsAdminService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UmsAdminServiceImpl.class);
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UmsAdminMapper adminMapper;
    @Autowired
    private UmsAdminRoleRelationMapper adminRoleRelationMapper;
    @Autowired
    private UmsAdminPermissionRelationMapper adminPermissionRelationMapper;
    @Autowired
    private UmsAdminRoleRelationDao adminRoleRelationDao;
    @Autowired
    private UmsAdminPermissionRelationDao adminPermissionRelationDao;
    @Autowired
    private UmsAdminLoginLogMapper adminLoginLogMapper;
    @Autowired
    private UmsAdminCacheService adminCacheService;

    @Override
    public UmsAdmin getAdminByUsername(String username) {
        UmsAdmin admin = adminCacheService.getAdmin(username);
        if(admin!=null) return  admin;
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(username);
        List<UmsAdmin> adminList = adminMapper.selectByExample(example);
        if (adminList != null && adminList.size() > 0) {
            admin = adminList.get(0);
            adminCacheService.setAdmin(admin);
            return admin;
        }
        return null;
    }

    @Override
    public UmsAdmin register(UmsAdminParam umsAdminParam) {
        //查询是否用相同用户名的用户
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(umsAdminParam.getUsername());
        List<UmsAdmin> umsAdmins = adminMapper.selectByExample(example);
        if (CollectionUtil.isNotEmpty(umsAdmins)) {
            return null;
        }
        UmsAdmin umsAdmin = new UmsAdmin();
        BeanUtils.copyProperties(umsAdminParam, umsAdmin);
        umsAdmin.setCreateTime(new Date());
        umsAdmin.setStatus(1);
        //对密码进行加密
        umsAdmin.setPassword(passwordEncoder.encode(umsAdmin.getPassword()));
        return umsAdmin;
    }

    @Override
    public String login(String username, String password) {
        String token = null;
        //密码需要客户端加密后传递
        try {
            UserDetails userDetails = loadUserByUsername(username);
            if(!passwordEncoder.matches(password,userDetails.getPassword())){
                throw new BadCredentialsException("密码不正确");
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            token = jwtTokenUtil.generateToken(userDetails);
//            updateLoginTimeByUsername(username);
            insertLoginLog(username);
        } catch (AuthenticationException e) {
            LOGGER.warn("登录异常:{}", e.getMessage());
        }
        return token;
    }

    /**
     * 添加登录记录
     * @param username 用户名
     */
    private void insertLoginLog(String username) {
        UmsAdmin admin = getAdminByUsername(username);
        if(admin==null) return;
        UmsAdminLoginLog loginLog = new UmsAdminLoginLog();
        loginLog.setAdminId(admin.getId());
        loginLog.setCreateTime(new Date());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        loginLog.setIp(request.getRemoteAddr());
        adminLoginLogMapper.insert(loginLog);
    }

    @Override
    public String refreshToken(String oldToken) {
        return jwtTokenUtil.refreshHeadToken(oldToken);
    }

    @Override
    public UmsAdmin getItem(Long id) {
        return adminMapper.selectByPrimaryKey(id);
    }

    @Override
    public List<UmsAdmin> list(String keyword, Integer pageSize, Integer pageNum) {
        PageHelper.startPage(pageNum, pageSize);
        UmsAdminExample example = new UmsAdminExample();
        if (!StringUtils.isEmpty(keyword)) {
            UmsAdminExample.Criteria criteria = example.createCriteria();
            criteria.andUsernameLike("%" + keyword + "%");
        }
        return adminMapper.selectByExample(example);
    }

    @Override
    public int update(Long id, UmsAdmin admin) {
        admin.setId(id);
        UmsAdmin temp = adminMapper.selectByPrimaryKey(id);
        if (temp != null) {
            if (temp.getPassword().equals(passwordEncoder.encode(admin.getPassword()))) {
                admin.setPassword(null);
            } else {
                admin.setPassword(passwordEncoder.encode(admin.getPassword()));
            }
        }
        int count = adminMapper.updateByPrimaryKeySelective(admin);
        adminCacheService.delAdmin(id);
        return count;
    }

    @Override
    public int delete(Long id) {
        adminCacheService.delAdmin(id);
        int count = adminMapper.deleteByPrimaryKey(id);
        return count;
    }

    @Override
    public int updateRole(Long adminId, List<Long> roleIds) {
        int count = roleIds == null ? 0 : roleIds.size();
        UmsAdminRoleRelationExample adminRoleRelationExample = new UmsAdminRoleRelationExample();
        adminRoleRelationExample.createCriteria().andAdminIdEqualTo(adminId);
        adminRoleRelationMapper.deleteByExample(adminRoleRelationExample);
        if (CollectionUtil.isNotEmpty(roleIds)) {
            List<UmsAdminRoleRelation> list = new ArrayList<>();
            for (Long roleId : roleIds) {
                UmsAdminRoleRelation adminRoleRelation = new UmsAdminRoleRelation();
                adminRoleRelation.setAdminId(adminId);
                adminRoleRelation.setRoleId(roleId);
                list.add(adminRoleRelation);
            }
            count = adminRoleRelationDao.insertList(adminId,roleIds);
        }

        return count;
    }

    @Override
    public List<UmsRole> getRoleList(Long adminId) {
        return adminRoleRelationDao.getRoleList(adminId);
    }

    @Override
    public List<UmsResource> getResourceList(Long adminId) {
        return null;
    }

    @Override
    public int updatePermission(Long adminId, List<Long> permissionIds) {
        //删除原有权限关系
        UmsAdminPermissionRelationExample relationExample = new UmsAdminPermissionRelationExample();
        relationExample.createCriteria().andAdminIdEqualTo(adminId);
        adminPermissionRelationMapper.deleteByExample(relationExample);
        //获取用户所有角色、权限
        List<UmsPermission> permissionList = adminRoleRelationDao.getPermissionList(adminId);
        List<Long> rolePermissionList = permissionList.stream().map(UmsPermission::getId).collect(Collectors.toList());
        if (!CollectionUtil.isEmpty(permissionIds)) {
            List<UmsAdminPermissionRelation> relationList = new ArrayList<>();
            //筛选出要添加的权限
            List<Long> addPermissions = permissionIds.stream().filter(permissionId -> !rolePermissionList.contains(permissionId)).collect(Collectors.toList());
            //筛选出要添加的权限
            List<Long> removePermissions = rolePermissionList.stream().filter(permissionId -> !permissionIds.contains(permissionId)).collect(Collectors.toList());
            relationList.addAll(convert(adminId,1,addPermissions));
            relationList.addAll(convert(adminId,-1,removePermissions));
            return adminPermissionRelationDao.insertList(relationList);
        }
        return 0;
    }
    private List<UmsAdminPermissionRelation> convert(Long adminId,Integer type,List<Long> permissionIds){
        List<UmsAdminPermissionRelation> relationList = permissionIds.stream().map(permissionId -> {
            UmsAdminPermissionRelation relation = new UmsAdminPermissionRelation();
            relation.setAdminId(adminId);
            relation.setPermissionId(permissionId);
            relation.setType(type);
            return relation;
        }).collect(Collectors.toList());
        return relationList;
    }
    @Override
    public List<UmsPermission> getPermissionList(Long adminId) {
        return adminRoleRelationDao.getPermissionList(adminId);
    }

    @Override
    public int updatePassword(UpdateAdminPasswordParam param) {
        if (StringUtils.isEmpty(param.getNewPassword()) || StringUtils.isEmpty(param.getUsername()) || StringUtils.isEmpty(param.getOldPassword())) {
            return -1;
        }
        UmsAdminExample example = new UmsAdminExample();
        example.createCriteria().andUsernameEqualTo(param.getUsername());
        List<UmsAdmin> umsAdmins = adminMapper.selectByExample(example);
        if (CollectionUtil.isEmpty(umsAdmins)) {
            return -2;
        }
        UmsAdmin umsAdmin = umsAdmins.get(0);
        if (!passwordEncoder.matches(param.getOldPassword(), umsAdmin.getPassword())) {
            return -3;
        }
        umsAdmin.setPassword(passwordEncoder.encode(param.getNewPassword()));
        adminMapper.updateByPrimaryKey(umsAdmin);
        adminCacheService.delAdmin(umsAdmin.getId());
        return 1;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        UmsAdmin admin = getAdminByUsername(username);
        if (admin != null) {
            List<UmsPermission> permissionList = getPermissionList(admin.getId());
            return new AdminUserDetails(admin, permissionList);
        }
        throw new UsernameNotFoundException("用户名或密码错误");
    }
}
