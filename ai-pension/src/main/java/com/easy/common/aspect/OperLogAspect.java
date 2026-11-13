package com.easy.common.aspect;

import cn.hutool.core.util.StrUtil;
import com.easy.admin.service.IOperLogService;
import com.easy.common.annotation.OperLog;
import com.easy.common.bean.SysOperLog;
import com.easy.common.bean.User;
import com.easy.common.util.CurrentUser;
import com.easy.common.util.SensitiveDataMasker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


  //操作日志切面
  //拦截 @OperLog 注解，自动采集操作信息并异步写入日志表
@Aspect   //声明类为AOP切面
@Component
public class OperLogAspect {

    @Autowired
    IOperLogService operLogService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.easy.common.annotation.OperLog)")  // 切入点表达式 环绕通知：拦截所有标注了@OperLog注解的方法
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperLog operLog = method.getAnnotation(OperLog.class); // 从Method上获取@OperLog注解实例

        HttpServletRequest request = getRequest(); // 获取HTTP请求对象
        User user = CurrentUser.getCurrentUser();  // 获取当前用户
        // 构建日志对象
        SysOperLog log = new SysOperLog();
        log.setModule(operLog.module());
        log.setOperation(operLog.operation());
        log.setOperate_type(operLog.operateType());
        log.setMethod(joinPoint.getTarget().getClass().getName() + "." + method.getName());
        log.setOperate_time(LocalDateTime.now());
        // 填充请求信息
        if (request != null) {
            log.setRequest_url(request.getRequestURI());
            log.setRequest_method(request.getMethod());  //设置HTTP方法
            log.setIp(getClientIp(request));
            String ua = request.getHeader("User-Agent");
            log.setUser_agent(StrUtil.sub(ua, 0, 500));  // 截取前500字符设置UA
        }
        try {
            String params = buildRequestParams(joinPoint, method, request);
            if (StrUtil.isNotBlank(params)) {
                log.setRequest_params(StrUtil.sub(params, 0, 2000));
            }
        } catch (Exception ignored) {
        }
        // 填充用户信息
        if (user != null) {
            log.setUser_id(user.getId());
            log.setUser_name(user.getPhone());
            log.setUser_real_name(user.getReal_name());
            log.setRole(user.getRole());
        }
        // 执行原方法，捕获异常
        Object result;
        try {
            result = joinPoint.proceed();
            log.setStatus(1);
        } catch (Throwable e) {
            log.setStatus(0);
            log.setError_msg(StrUtil.sub(e.getMessage(), 0, 1000));
            throw e; // 继续抛出，不吞异常
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            log.setCost_time(cost);
            operLogService.saveLogAsync(log);
        }
        return result;
    }

    /**
     * 优先记录客户端原始 JSON；文件、查询参数和路径参数记录可用元数据。
     */
    private String buildRequestParams(ProceedingJoinPoint joinPoint, Method method,
                                      HttpServletRequest request) throws Exception {
        if (request == null) {
            return null;
        }

        String jsonBody = getCachedJsonBody(request);
        if (StrUtil.isNotBlank(jsonBody)) {
            return SensitiveDataMasker.maskJson(jsonBody);
        }

        Map<String, Object> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values == null) {
                params.put(key, null);
            } else if (values.length == 1) {
                params.put(key, values[0]);
            } else {
                params.put(key, values);
            }
        });

        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < args.length && i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isAnnotationPresent(RequestHeader.class)
                    || parameter.isAnnotationPresent(RequestBody.class)) {
                continue;
            }
            Object loggable = toLoggableArgument(args[i]);
            if (loggable != null) {
                params.put(resolveParameterName(parameter, i), loggable);
            }
        }

        if (params.isEmpty()) {
            return null;
        }
        String json = objectMapper.writeValueAsString(params);
        return SensitiveDataMasker.maskJson(json);
    }

    private String getCachedJsonBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }
        byte[] cachedBody = wrapper.getContentAsByteArray();
        if (cachedBody.length == 0) {
            return null;
        }
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    private String resolveParameterName(Parameter parameter, int index) {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        if (requestParam != null && StrUtil.isNotBlank(requestParam.value())) {
            return requestParam.value();
        }
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        if (pathVariable != null && StrUtil.isNotBlank(pathVariable.value())) {
            return pathVariable.value();
        }
        return "arg" + index;
    }

    /**
     * 文件对象不能直接 JSON 序列化，只记录不会暴露文件内容的元数据。
     */
    private Object toLoggableArgument(Object argument) {
        if (argument instanceof MultipartFile file) {
            return fileMetadata(file);
        }
        if (argument instanceof MultipartFile[] files) {
            List<Map<String, Object>> metadata = new ArrayList<>();
            for (MultipartFile file : files) {
                metadata.add(fileMetadata(file));
            }
            return metadata;
        }
        return argument;
    }

    private Map<String, Object> fileMetadata(MultipartFile file) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("originalName", file.getOriginalFilename());
        metadata.put("contentType", file.getContentType());
        metadata.put("size", file.getSize());
        metadata.put("empty", file.isEmpty());
        return metadata;
    }

    private HttpServletRequest getRequest() {  // 从当前线程获取 HttpServletRequest
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
    private String getClientIp(HttpServletRequest request) {  // 获取客户端真实 IP
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                // 多级代理时取第一个
                int idx = ip.indexOf(',');
                return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
