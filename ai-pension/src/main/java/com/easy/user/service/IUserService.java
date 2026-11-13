package com.easy.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.easy.common.bean.User;
import com.easy.common.util.Result;

public interface IUserService extends IService<User> {

    Result sendSmsCode(String phone);

    Result register(User user);

    Result login(User user);

    Result refresh(String accessToken, String refreshToken);

    Result logout(String accessToken, String refreshToken);

    Result getProfile();

    Result updateProfile(User user);

    Result changePassword(String oldPassword, String newPassword);

    Result resetPassword(String phone, String newPassword, String smsCode);

}