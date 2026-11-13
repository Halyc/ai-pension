package com.easy.common.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.easy.common.config.OssConfig;
import com.easy.common.exception.ServiceException;
import com.easy.common.service.IFileService;
import com.easy.common.util.Result;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传服务：上传到阿里云 OSS，返回可直接访问的公网 URL
 */
@Slf4j
@Service
public class FileServiceImpl implements IFileService {

    @Autowired
    private OssConfig ossConfig;

    private volatile OSS ossClient;

    private static final long MAX_SIZE = 10 * 1024 * 1024;
    private static final String[] ALLOWED_EXT = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
    /** 体检报告目录额外允许 PDF */
    private static final String[] REPORT_EXT = {"jpg", "jpeg", "png", "gif", "bmp", "webp", "pdf"};
    private static final String REPORT_DIR = "medical-reports";
    private static final String AVATAR_DIR = "avatars/";

    @Override
    public Result uploadImage(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(400, "请选择要上传的文件");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException(400, "文件大小不能超过 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        String subDirKey = (subDir == null || subDir.isBlank()) ? "" : subDir.trim();
        boolean reportUpload = REPORT_DIR.equals(subDirKey);
        String[] allowedExt = reportUpload ? REPORT_EXT : ALLOWED_EXT;

        boolean allowed = false;
        for (String e : allowedExt) {
            if (e.equals(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new ServiceException(400, reportUpload
                    ? "仅支持 pdf/jpg/jpeg/png/gif/bmp/webp 格式"
                    : "仅支持 jpg/jpeg/png/gif/bmp/webp 格式");
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String dir = reportUpload ? REPORT_DIR + "/" : (subDirKey.isEmpty() ? "" : subDirKey + "/");
        String objectName = dir + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "/" + fileName;

        try (InputStream in = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(resolveContentType(file.getContentType(), ext));
            getOssClient().putObject(ossConfig.getBucket(), objectName, in, metadata);
        } catch (OSSException | ClientException e) {
            throw new ServiceException(500, "图片上传失败：" + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException(500, "图片读取失败：" + e.getMessage());
        }

        String url = buildUrl(objectName);

        Map<String, Object> data = new HashMap<>();
        data.put("url", url);
        data.put("fileName", fileName);
        data.put("originalName", originalFilename);
        data.put("size", file.getSize());

        return Result.success("上传成功", data);
    }

    @Override
    public boolean deleteAvatarByUrl(String avatarUrl) {
        String objectName = resolveAvatarObjectName(avatarUrl);
        if (objectName == null) {
            return false;
        }

        try {
            getOssClient().deleteObject(ossConfig.getBucket(), objectName);
            return true;
        } catch (OSSException | ClientException e) {
            log.warn("删除旧头像失败，object={}，原因={}", objectName, e.getMessage());
            return false;
        } catch (RuntimeException e) {
            log.warn("删除旧头像失败，object={}，原因={}", objectName, e.getMessage());
            return false;
        }
    }

    /**
     * 仅解析并返回本系统 OSS 头像目录中的对象名。
     * 支持默认 OSS 域名和配置的 public-prefix（如 CDN 域名）。
     */
    private String resolveAvatarObjectName(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return null;
        }

        try {
            URI avatarUri = URI.create(avatarUrl.trim());
            String path = avatarUri.getPath();
            if (avatarUri.getHost() == null || !StringUtils.hasText(path)) {
                return null;
            }

            String objectName;
            String publicPrefix = ossConfig.getPublicPrefix();
            if (StringUtils.hasText(publicPrefix)) {
                URI prefixUri = URI.create(publicPrefix.trim());
                if (!sameAuthority(avatarUri, prefixUri)) {
                    return null;
                }
                String prefixPath = normalizePath(prefixUri.getPath());
                String avatarPath = normalizePath(path);
                if (prefixPath.isEmpty()) {
                    objectName = avatarPath;
                } else if (avatarPath.startsWith(prefixPath + "/")) {
                    objectName = avatarPath.substring(prefixPath.length() + 1);
                } else {
                    return null;
                }
            } else {
                String endpointHost = getEndpointHost();
                String bucket = ossConfig.getBucket();
                if (!StringUtils.hasText(endpointHost) || !StringUtils.hasText(bucket)) {
                    return null;
                }
                String expectedHost = bucket + "." + endpointHost;
                if (!expectedHost.equalsIgnoreCase(avatarUri.getHost())) {
                    return null;
                }
                objectName = normalizePath(path);
            }

            return objectName.startsWith(AVATAR_DIR) && objectName.length() > AVATAR_DIR.length()
                    ? objectName
                    : null;
        } catch (IllegalArgumentException e) {
            log.debug("忽略无法解析的头像地址：{}", avatarUrl);
            return null;
        }
    }

    private boolean sameAuthority(URI first, URI second) {
        return second.getHost() != null
                && second.getHost().equalsIgnoreCase(first.getHost())
                && effectivePort(second) == effectivePort(first);
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        return "http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443;
    }

    private String getEndpointHost() {
        String endpoint = ossConfig.getEndpoint();
        if (!StringUtils.hasText(endpoint)) {
            return null;
        }
        URI endpointUri = URI.create(endpoint.trim().contains("://")
                ? endpoint.trim()
                : "https://" + endpoint.trim());
        return endpointUri.getHost();
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "";
        }
        String normalized = path;
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 懒加载 OSS 客户端，避免未配置 OSS 时影响应用启动
     */
    private OSS getOssClient() {
        if (ossClient == null) {
            synchronized (this) {
                if (ossClient == null) {
                    String endpoint = ossConfig.getEndpoint();
                    String accessKey = ossConfig.getAccessKey();
                    String secretKey = ossConfig.getSecretKey();
                    String bucket = ossConfig.getBucket();
                    if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(accessKey)
                            || !StringUtils.hasText(secretKey) || !StringUtils.hasText(bucket)) {
                        throw new ServiceException(500,
                                "未配置阿里云 OSS，请设置 OSS_ENDPOINT/OSS_ACCESS_KEY/OSS_SECRET_KEY/OSS_BUCKET 环境变量");
                    }
                    ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
                }
            }
        }
        return ossClient;
    }

    /**
     * 浏览器未提供 Content-Type 时按扩展名兜底，保证 OSS 上的 PDF/图片可以直接预览
     */
    private String resolveContentType(String contentType, String ext) {
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        if ("pdf".equals(ext)) {
            return "application/pdf";
        }
        return "image/" + ("jpg".equals(ext) ? "jpeg" : ext);
    }

    /**
     * 优先使用配置的访问前缀（如 CDN 域名），未配置时按 OSS 默认域名拼接
     */
    private String buildUrl(String objectName) {
        String publicPrefix = ossConfig.getPublicPrefix();
        if (StringUtils.hasText(publicPrefix)) {
            return publicPrefix.endsWith("/") ? publicPrefix + objectName : publicPrefix + "/" + objectName;
        }
        String host = ossConfig.getEndpoint().replaceFirst("^https?://", "");
        return "https://" + ossConfig.getBucket() + "." + host + "/" + objectName;
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
