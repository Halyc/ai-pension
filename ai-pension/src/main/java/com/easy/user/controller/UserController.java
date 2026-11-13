package com.easy.user.controller;

import com.easy.common.annotation.OperLog;
import com.easy.common.bean.User;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import com.easy.user.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    IUserService userService;

    @OperLog(module = "会员账号", operation = "会员注册", operateType = 1)
    @PostMapping("/auth/register")
    public Result register(@RequestBody User user) {
        return userService.register(user);
    }

    @OperLog(module = "会员账号", operation = "发送短信验证码", operateType = 1)
    @PostMapping("/auth/sms/code")
    public Result sendSmsCode(@RequestBody Map<String, String> body) {
        return userService.sendSmsCode(body.get("phone"));
    }

    @OperLog(module = "会员账号", operation = "会员登录", operateType = 4)
    @PostMapping("/auth/login")
    public Result login(@RequestBody User user) {
        return userService.login(user);
    }

    @PostMapping("/auth/refresh")
    public Result refresh(@RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accesstoken, @RequestHeader(Sys.REQUEST_REFRESHTOKEN_KEY) String refreshtoken) {
        return userService.refresh(accesstoken, refreshtoken);
    }

    @OperLog(module = "会员账号", operation = "会员登出", operateType = 4)
    @PostMapping("/auth/logout")
    public Result logout(@RequestHeader("Authorization") String accesstoken, String refreshtoken) {
        return userService.logout(accesstoken, refreshtoken);
    }

    @GetMapping("/member/profile")
    public Result getProfile(){   //获取当前用户资料
        return userService.getProfile();
    }

    @OperLog(module = "会员账号", operation = "修改个人资料", operateType = 2)
    @PutMapping("/member/profile")
    public Result updateProfile(@RequestBody User user){   //更新个人资料
        return userService.updateProfile(user);
    }

    @OperLog(module = "会员账号", operation = "修改密码", operateType = 2)
    @PostMapping("/member/profile/change-password")
    public Result changePassword(@RequestBody Map<String,String> body){
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        return userService.changePassword(oldPassword,newPassword);
    }

    @OperLog(module = "会员账号", operation = "重置密码", operateType = 2)
    @PostMapping("/auth/reset-password")
    public Result resetPassword(@RequestBody Map<String,String> body){
        String phone = body.get("phone");
        String newPassword = body.get("newPassword");
        String smsCode = body.get("smsCode");
        return userService.resetPassword(phone, newPassword, smsCode);
    }

}