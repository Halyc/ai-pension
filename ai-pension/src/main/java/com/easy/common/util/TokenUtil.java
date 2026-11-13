package com.easy.common.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;
import java.util.UUID;

//JWT Token工具类，实现双Token机制
public class TokenUtil {
    /**
     * Http请求头中Token的key
     */
    public static final String HTTP_HEADER_TOKEN_NAME = "Authorization";
    /**
     * Http请求头中RefreshToken的key
     */
    public static final String HTTP_HEADER_REFRESH_TOKEN_NAME = "refreshtoken";
    /**
     * Token签名
     * AccessToken和RefreshToken的签名一致
     */
    public static final String TOKEN_SECRET = "easy-644aa7c9-69c7-4840-bf7b-5bd6c71197b0";
    /**
     * 乜咯Token中都负载了用户的id
     * Token中负载用户的id的key
     */
    public static final String USER_KEY = "userid";
    /**
     * Token认证对象的类型
     */
    public static final String USER_TYPE_KEY = "usertype";
    /**
     * Token的角色类型 admin
     */
    public static final String USER_TYPE_ADMIN = "admin";
    /**
     * Token的角色类型 user
     */
    public static final String USER_TYPE_USER = "user";
    /**
     * Token类型
     */
    public static final String TYPE_KEY = "type";
    /**
     * AccessToken  类型的值
     */
    public static final String TYPE_ACCESS = "access";
    /**
     * RefreshToken  类型的值
     */
    public static final String TYPE_REFRESH = "refresh";
    /**
     * AccessToken和RefreshToken的关联字段
     */
    public static final String ASSOCIATION_KEY = "association";
    /**
     * 访问令牌过期时间 4小时
     * 刷新令牌过期时间 7天
     */
    private static final Long ACCESS_TOKEN_EXPIRE_TIME = 4 * 60 * 60 * 1000L;
    private static final Long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L;
    private static final Map<String, Long> TOKEN_BLACK_LIST = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        TokenPair tokenPair1 = getTokenPair(1L, USER_TYPE_USER);
        System.out.println(tokenPair1.getAccessToken());
        System.out.println(tokenPair1.getRefreshToken());
        TokenPair tokenPair2 = getTokenPair(2L, USER_TYPE_USER);
        System.out.println(tokenPair2.getAccessToken());
        System.out.println(tokenPair2.getRefreshToken());
    }

    /**
     * 签发Token 获取Token对
     *
     * @param userId
     * @return TokenPair Token对 {accessToken,refreshToken}
     */
    public static TokenPair getTokenPair(Long userId, String userType) {  //签发双token
        String accosication = UUID.randomUUID().toString();
        String accessToken = getToken(userId, TYPE_ACCESS, ACCESS_TOKEN_EXPIRE_TIME, accosication, userType);
        String refreshToken = getToken(userId, TYPE_REFRESH, REFRESH_TOKEN_EXPIRE_TIME, accosication, userType);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 通过参数生成Token字符串
     *
     * @param userId
     * @param type
     * @param expireTime
     * @param association
     * @param userType
     * @return
     */
    private static String getToken(Long userId, String type, long expireTime, String association, String userType) {
        JWT jwt = JWT.create();
        jwt.setPayload(USER_KEY, userId);
        jwt.setPayload(TYPE_KEY, type);
        jwt.setPayload(ASSOCIATION_KEY, association);
        jwt.setPayload(USER_TYPE_KEY, userType);
        jwt.setExpiresAt(new Date(System.currentTimeMillis() + expireTime));
        String token = jwt.setKey(TOKEN_SECRET.getBytes()).sign();
        return token;
    }

    /**
     * 获取Token 弃用
     * @param token
     * @return
     */
//    public static String getToken(Integer userId) {
//        Map<String, Object> map = new HashMap<>();
//        map.put(USER_KEY, userId);
//        return JWTUtil.createToken(map, TOKEN_SECRET.getBytes());
//    }

    /**
     * 验证Token的登录类型 user/admin
     *
     * @param token
     * @param loginType
     * @return
     */
    public static boolean verifyTokenLoginType(String token, String loginType) {
        Object tokenLoginType = getPayload(token, USER_TYPE_KEY);
        return tokenLoginType.equals(loginType);
    }

    /**
     * 验证Token 1.签名 2.是否还有效
     *
     * @param token
     * @return
     */
    public static boolean verifyAccessToken(String token) {
        if (token == null) {
            return false;
        }
        try {
            JWTValidator jwt = JWTValidator.of(token);
            try {
                jwt.validateDate(new Date());
            } catch (Exception e) {
                return false;
            }
            return JWTUtil.verify(token, TOKEN_SECRET.getBytes());
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * 验证RefreshToken
     *
     * @param refreshToken
     * @param accessToken
     * @return
     */
    public static boolean verifyRefreshToken(String refreshToken, String accessToken) {
        if (refreshToken == null || accessToken == null) {
            return false;
        }
        try {
            JWTValidator accessJwt = JWTValidator.of(accessToken);
            JWTValidator refreshJwt = JWTValidator.of(refreshToken);
            refreshJwt.validateDate(new Date());
        } catch (Exception e) {
            return false;
        }
        try {
            Object accessAssociation = getPayload(accessToken, ASSOCIATION_KEY);
            Object refreshAssociation = getPayload(refreshToken, ASSOCIATION_KEY);

            if (accessAssociation == null || refreshAssociation == null) {
                return false;
            }

            return accessAssociation.equals(refreshAssociation) &&
                    JWTUtil.verify(refreshToken, TOKEN_SECRET.getBytes());
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 获取Token中的字段
     *
     * @param token
     * @param key
     * @return
     */
    public static Object getPayload(String token, String key) {
        JWT jwt = JWTUtil.parseToken(token);
        Object object = jwt.getPayload(key);
        return object;
    }

    /**
     * 将Token加入黑名单，有效期至token自身过期时间
     */
    public static void addToBlackList(String token) {
        if (token == null) return;
        try {
            JWT jwt = JWTUtil.parseToken(token);
            Object exp = jwt.getPayload("exp");
            long expireMillis;
            if (exp instanceof Number) {
                expireMillis = ((Number) exp).longValue() * 1000;
            } else {
                expireMillis = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME;
            }
            cleanExpiredBlackList();
            TOKEN_BLACK_LIST.put(token, expireMillis);
        } catch (Exception e) {
            TOKEN_BLACK_LIST.put(token, System.currentTimeMillis() + REFRESH_TOKEN_EXPIRE_TIME);
        }
    }

    /**
     * 判断token是否在黑名单中
     */
    public static boolean isBlackListed(String token) {
        if (token == null) return false;
        Long expire = TOKEN_BLACK_LIST.get(token);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            TOKEN_BLACK_LIST.remove(token);
            return false;
        }
        return true;
    }

    /**
     * 清理已过期的黑名单项
     */
    private static void cleanExpiredBlackList() {
        long now = System.currentTimeMillis();
        TOKEN_BLACK_LIST.entrySet().removeIf(e -> now > e.getValue());
    }

    /**
     * Token对类型
     */
    @Getter
    @AllArgsConstructor
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
    }

}
