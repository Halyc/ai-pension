package com.easy.common.config;

import com.easy.user.interceptor.AdminLoginInterceptor;
import com.easy.user.interceptor.CurrentUserInterceptor;
import com.easy.user.interceptor.UserLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration  // 声明此类为Spring MVC配置类
public class MVCConfig implements WebMvcConfigurer {  //注册并配置拦截器

    @Autowired
    UserLoginInterceptor userLoginInterceptor;
    @Autowired
    CurrentUserInterceptor currentUserInterceptor;
    @Autowired
    AdminLoginInterceptor adminLoginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userLoginInterceptor).addPathPatterns("/api/member/**");
        registry.addInterceptor(currentUserInterceptor).addPathPatterns("/api/member/**");
        registry.addInterceptor(adminLoginInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout"
                );
    }
}