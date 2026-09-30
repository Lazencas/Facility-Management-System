package com.kgt.facility_access_management.common.config;

import com.kgt.facility_access_management.common.interceptor.AdminCheckInterceptor;
import com.kgt.facility_access_management.common.interceptor.LoginCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //로그인 관련 인터셉터 (인증)
        registry.addInterceptor(new LoginCheckInterceptor()).
                addPathPatterns(
                        "/logout",
                        "/facilities/**",
                        "/access-requests/**",
                        "/admin/**"
                );

        //권한 관련 인터셉터 (인가)
        registry.addInterceptor(new AdminCheckInterceptor()).
                addPathPatterns(
                        "/admin/**"
                );
    }




}
