package com.easy.common.util;

import com.easy.common.bean.User;

//利用线程获取当前用户,在同一请求线程内共享当前登录用户信息

public class CurrentUser {

    //当前用户
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    //设置当前用户
    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    //获取当前用户  @return
    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    //清除当前用户
    public static void clearCurrentUser() {
        CURRENT_USER.remove();
    }

    //获取当前用户 ID   @return
    public static Long getCurrentUserID() {
        User user = CURRENT_USER.get();
        return user != null ? user.getId() : null;
    }
}