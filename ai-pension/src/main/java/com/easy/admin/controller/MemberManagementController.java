package com.easy.admin.controller;

import com.easy.admin.service.IMemberManagementService;
import com.easy.common.annotation.OperLog;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/members")
public class MemberManagementController {

    @Autowired
    private IMemberManagementService memberManagementService;

    @GetMapping("/list")
    public Result getMemberList(@RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
                                @RequestParam(required = false) Integer page,
                                @RequestParam(required = false) Integer size,
                                @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) String status) {  //	分页查询会员列表，支持按手机号、姓名、状态筛选
        return memberManagementService.getMemberList(page, size, phone, name, status);
    }


    @GetMapping("/detail/{userId}")
    public Result getMemberDetail(   //根据用户ID查询会员详细信息
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long userId) {
        return memberManagementService.getMemberDetail(userId);
    }

    @OperLog(module = "会员管理", operation = "启用会员", operateType = 4)
    @PostMapping("/enable/{userId}")
    public Result enableMember(   //启用被禁用的会员账号
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long userId) {
        return memberManagementService.enableMember(userId);
    }

    @OperLog(module = "会员管理", operation = "禁用会员", operateType = 4)
    @PostMapping("/disable/{userId}")
    public Result disableMember(    //禁用会员账号
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long userId) {
        return memberManagementService.disableMember(userId);
    }

    @GetMapping("/levels")
    public Result getMemberLevels(  //获取系统所有会员等级配置
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken) {
        return memberManagementService.getMemberLevels();
    }

    @OperLog(module = "会员管理", operation = "修改会员等级", operateType = 2)
    @PostMapping("/{userId}/level")
    public Result updateMemberLevel(    //管理员手动修改某会员的等级
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String levelCode = body.get("levelCode");
        if (levelCode == null || levelCode.trim().isEmpty()) {
            return Result.error(400, "会员等级代码不能为空");
        }
        return memberManagementService.updateMemberLevel(userId, levelCode.trim());
    }

    @OperLog(module = "会员管理", operation = "修改会员积分", operateType = 2)
    @PostMapping("/{userId}/points")
    public Result updateMemberPoints(   //	管理员手动修改会员的积分
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long userId,
            @RequestBody Map<String, Integer> body) {
        Integer points = body.get("points");
        if (points == null) {
            return Result.error(400, "积分数值不能为空");
        }
        return memberManagementService.updateMemberPoints(userId, points);
    }

    @OperLog(module = "会员管理", operation = "重置会员密码", operateType = 2)
    @PostMapping("/{userId}/reset-password")
    public Result resetMemberPassword(  //将会员密码重置为默认密码 123456
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long userId) {
        return memberManagementService.resetMemberPassword(userId);
    }

    @OperLog(module = "用户管理", operation = "新增用户", operateType = 1)
    @PostMapping("/add")
    public Result addMember(    //新增会员账号
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestBody Map<String, Object> body) {
        String phone = (String) body.get("phone");
        String password = (String) body.get("password");
        String role = (String) body.get("role");
        Integer points = null;
        Object pointsObj = body.get("points");
        if (pointsObj != null) {
            if (pointsObj instanceof Number) {
                points = ((Number) pointsObj).intValue();
            } else {
                try {
                    points = Integer.parseInt(pointsObj.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return memberManagementService.addMember(phone, password, role, points);
    }

    @OperLog(module = "用户管理", operation = "删除用户", operateType = 5)
    @DeleteMapping("/delete/{userId}")
    public Result deleteMember( //删除会员账号
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @PathVariable Long userId) {
        return memberManagementService.deleteMember(userId);
    }

}