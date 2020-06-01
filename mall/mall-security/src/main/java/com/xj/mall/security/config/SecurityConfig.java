package com.xj.mall.security.config;

import com.xj.mall.security.component.JwtAuthenticationTokenFilter;
import com.xj.mall.security.component.RestAuthenticationEntryPoint;
import com.xj.mall.security.component.RestfulAccessDeniedHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry = httpSecurity.authorizeRequests();
        //不需要保护的资源允许访问
        for (String url : ignoreUrlsConfig().getUrls()) {
            registry.antMatchers(url).permitAll();
        }
        //允许跨域的OPTIONS请求
        registry.antMatchers(HttpMethod.OPTIONS).permitAll();
        //其他请求需要身份认证
        registry.and()
                .authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)//永远不创建httpSession
                //自定义权限去处理
                .and()
                .exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler())
                .authenticationEntryPoint(restAuthenticationEntryPoint())
                // 自定义权限拦截器JWT过滤器
                .and()
                .addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);

    }
    @Bean
    public JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter(){
         return new JwtAuthenticationTokenFilter();
    }
    @Bean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint(){
        return new RestAuthenticationEntryPoint();
    }
    @Bean
    public RestfulAccessDeniedHandler restfulAccessDeniedHandler(){
        return new RestfulAccessDeniedHandler();
    }
    @Bean
    public IgnoreUrlsConfig ignoreUrlsConfig() {
        return new IgnoreUrlsConfig();
    }
}
