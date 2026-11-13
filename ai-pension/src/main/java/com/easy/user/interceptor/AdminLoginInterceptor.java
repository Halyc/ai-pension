package com.easy.user.interceptor;

import com.easy.common.bean.User;
import com.easy.common.exception.ServiceException;
import com.easy.common.mapper.membermapper.IUserMapper;
import com.easy.common.util.Sys;
import com.easy.common.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
//管理后台登录拦截器。拦截所有需要管理员权限的接口
@Component
public class AdminLoginInterceptor implements HandlerInterceptor {

    @Autowired
    IUserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String accessToken = request.getHeader(Sys.REQUEST_ACCESSTOKEN_KEY);
        // 1. 验证 Token 是否有效
        if (accessToken == null || !TokenUtil.verifyAccessToken(accessToken)) {
            throw new ServiceException(401, "认证失效");
        }
        // 2. 验证 Token 类型必须是管理员
        Object userType = TokenUtil.getPayload(accessToken, TokenUtil.USER_TYPE_KEY);
        if (userType == null || !TokenUtil.USER_TYPE_ADMIN.equals(userType)) {
            throw new ServiceException(403, "无权访问管理后台");
        }
        // 3. 查询用户并验证权限
        Object userid = TokenUtil.getPayload(accessToken, TokenUtil.USER_KEY);
        Long id = Long.parseLong(userid.toString());
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException(401, "账号不存在");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new ServiceException(403, "不是管理员账号");
        }
        if (Sys.USER_STATUS_DISABLED.equals(user.getStatus())) {
            throw new ServiceException(403, "账号已被禁用");
        }
        return true;
    }
}