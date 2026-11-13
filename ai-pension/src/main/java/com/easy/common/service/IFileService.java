package com.easy.common.service;

import com.easy.common.util.Result;
import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
    Result uploadImage(MultipartFile file, String subDir);  //上传图片，参数为上传文件对象和子目录名，返回统一结果

    /**
     * 删除 OSS 中由本系统上传的头像。
     * 非本 Bucket 的头像地址会被忽略，避免误删外部资源。
     *
     * @return OSS 删除请求是否成功
     */
    boolean deleteAvatarByUrl(String avatarUrl);
}
