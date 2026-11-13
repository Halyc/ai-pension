package com.easy.user.interceptor;

import cn.hutool.core.convert.NumberWithFormat;
import com.easy.common.bean.User;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.Sys;
import com.easy.common.util.TokenUtil;
import com.easy.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CurrentUserInterceptor implements HandlerInterceptor {  //根据请求头里的 Token 查出当前登录用户

    @Autowired
    IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(Sys.REQUEST_ACCESSTOKEN_KEY);
        Object userid = TokenUtil.getPayload(token, TokenUtil.USER_KEY);
        User user = null;
        if (userid instanceof NumberWithFormat id) {
            Long long_id = Long.parseLong(id.toString());
            user = userService.getById(long_id);
        }
        CurrentUser.setCurrentUser(user);
        return true;
    }
}