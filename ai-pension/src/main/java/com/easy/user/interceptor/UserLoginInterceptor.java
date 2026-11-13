package com.easy.user.interceptor;

import cn.hutool.core.convert.NumberWithFormat;
import com.easy.common.bean.User;
import com.easy.common.exception.ServiceException;
import com.easy.common.util.Sys;
import com.easy.common.util.TokenUtil;
import com.easy.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 验证用户登录状态是否是有效�?
 *
 */
@Component
public class UserLoginInterceptor implements HandlerInterceptor {
    @Autowired
    IUserService userService;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //获取token
        String accessToken=request.getHeader(Sys.REQUEST_ACCESSTOKEN_KEY);
        //检测token
        if(accessToken==null|| !TokenUtil.verifyAccessToken(accessToken)){
            throw new ServiceException(401,"认证失效");
        }
        Object user_type= TokenUtil.getPayload(accessToken,TokenUtil.USER_TYPE_KEY);
        if(user_type==null||(!user_type.equals(TokenUtil.USER_TYPE_USER)&&!user_type.equals(TokenUtil.USER_TYPE_ADMIN))){
            throw new ServiceException(403,"无权访问");
        }
        Object userid=TokenUtil.getPayload(accessToken,TokenUtil.USER_KEY);
        System.out.println(userid.getClass());
        User  user_data=null;
        if(userid instanceof NumberWithFormat id){
            Long long_id=Long.parseLong(id.toString());
            user_data=userService.getById(long_id);
        }
        System.out.println(user_data);
        if(user_data==null||Sys.USER_STATUS_DISABLED.equals(user_data.getStatus())){
            throw new ServiceException(403,"用户已经被禁");
        }
        return true;
    }
}