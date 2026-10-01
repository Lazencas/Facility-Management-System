package com.kgt.facility_access_management.common.interceptor;

import com.kgt.facility_access_management.user.domain.User;
import com.kgt.facility_access_management.user.domain.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("=== AdminCheckInterceptor 실행 ===");
        System.out.println("URI = " + request.getRequestURI());

        HttpSession session = request.getSession(false);

        //세션이 없으면
        if (session == null) {
            System.out.println("session 없음");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        User loginUser = (User) session.getAttribute("LOGIN_USER");

        //세션에 유저가 없으면
        if (loginUser == null) {
            System.out.println("LOGIN_USER 없음");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        //권한이 관리자가 아닐시에 튕겨냄
        if (loginUser.getRole() != UserRole.ADMIN) {
            System.out.println(">>> USER라서 403 차단");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("관리자 권한이 필요합니다.");
            return false;
        }
        System.out.println(">>> ADMIN 통과");
        return true;

    }
}
