package com.easy.admin.controller;

import com.easy.common.annotation.OperLog;
import com.easy.common.service.IFileService;
import com.easy.common.util.Result;
import com.easy.common.util.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/upload")
public class AdminFileController {

    @Autowired
    private IFileService fileService;

    @OperLog(module = "文件管理", operation = "上传图片", operateType = 4)
    @PostMapping("/image")
    public Result uploadImage(
            @RequestHeader(Sys.REQUEST_ACCESSTOKEN_KEY) String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dir", required = false, defaultValue = "covers") String dir) {
        return fileService.uploadImage(file, dir);
    }
}