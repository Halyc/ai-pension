package com.easy.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * 缓存 JSON 请求体，供操作日志记录客户端实际提交的内容。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JsonRequestBodyCachingFilter extends OncePerRequestFilter {

    private static final int MAX_CACHED_BODY_SIZE = 64 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("application/json")) {
            ContentCachingRequestWrapper wrapper =
                    new ContentCachingRequestWrapper(request, MAX_CACHED_BODY_SIZE);
            filterChain.doFilter(wrapper, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}
