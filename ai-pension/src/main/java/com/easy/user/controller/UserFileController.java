package com.easy.user.controller;

import com.easy.common.annotation.OperLog;
import com.easy.common.service.IFileService;
import com.easy.common.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/member/upload")
public class UserFileController {

    @Autowired
    private IFileService fileService;

    @OperLog(module = "会员账号", operation = "上传头像", operateType = 4)
    @PostMapping("/avatar")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        return fileService.uploadImage(file, "avatars");
    }
}