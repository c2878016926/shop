package com.example.shop.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求日志拦截器
 * 记录所有 HTTP 请求的方法、路径、状态码和耗时
 * 体现生产级系统的可观测性设计
 */
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME);
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : -1;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        // 静态资源不记录
        if (uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/img/")) {
            return;
        }

        if (status >= 400) {
            log.warn("{} {} -> {} ({}ms)", method, uri, status, elapsed);
        } else {
            log.info("{} {} -> {} ({}ms)", method, uri, status, elapsed);
        }
    }
}
