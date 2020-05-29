package com.xj.mall.service.impl;

import com.xj.mall.common.api.CommonResult;
import com.xj.mall.security.service.RedisService;
import com.xj.mall.service.UmsMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Random;

@Service
public class UmsMemberServiceImpl implements UmsMemberService {

    @Autowired
    private RedisService redisService;
    @Value("${redis.key.prefix.authCode}")
    private String REDIS_KEY_PREFIX_AUTH_CODE;
    @Value("${redis.key.expire.authCode}")
    private Long AUTH_CODE_EXPIRE_SECONDS;

    //生成验证码
    @Override
    public CommonResult generateAuthCode(String telephone) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i=0;i<6;i++){
            sb.append(random.nextInt(10));
        }
        //验证码绑定手机号存储到redis中
        redisService.set(REDIS_KEY_PREFIX_AUTH_CODE + telephone,sb.toString());
        //设置有效时间
        redisService.expire(REDIS_KEY_PREFIX_AUTH_CODE + telephone,AUTH_CODE_EXPIRE_SECONDS);
        return CommonResult.success(sb.toString());
    }

    //对输入的验证码进行验证
    @Override
    public CommonResult verifyAuthCode(String telephone, String authCode) {

        if(StringUtils.isEmpty(authCode)) {
            return CommonResult.failed("请输入验证码");
        }

        String realAuthCode = (String) redisService.get(REDIS_KEY_PREFIX_AUTH_CODE + telephone);
        boolean result = authCode.equals(realAuthCode);
        if (result) {
            return CommonResult.success(null, "验证码校验成功");
        } else {
            return CommonResult.failed("验证码不正确");
        }
    }
}
