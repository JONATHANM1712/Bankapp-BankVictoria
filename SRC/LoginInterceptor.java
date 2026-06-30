package com.example.bankapp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginInterceptor implements HandlerInterceptor {

    private static final int SESSION_TIMEOUT_DETIK = 30 * 60;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userLogin") != null) {
            session.setMaxInactiveInterval(SESSION_TIMEOUT_DETIK);
            return true;
        }

        response.sendRedirect("/login?timeout=true");
        return false;
    }
}
