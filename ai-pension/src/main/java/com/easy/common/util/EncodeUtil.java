package com.easy.common.util;

import org.springframework.util.DigestUtils;
/**
 * 密码加密工具类
 *  密码加密与验证工具类。
 *  采用MD5+随机盐+随机迭代次数的加密策略，
 *  将盐和迭代次数嵌入密文中，
 *  验证时从密文中提取参数重新加密比对
 */
public class EncodeUtil {
    public static void main(String[] args) {
        String str = "123123";
        String encode = EncodeUtil.encode(str);
        System.out.println(encode);
        System.out.println(EncodeUtil.check(str,encode));
    }

    private static final int COUNT_INDEX =3;
    private static final int SALT_INDEX =6;

    /**
     * 注册时密码加密方法
     * @param src 需要加密的原文
     * @return
     */
    public static String encode(String src) {
        char[] arr = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char char_count = arr[(int) (Math.random() * arr.length)];
        char salt = arr[(int) (Math.random() * arr.length)];
        String result = action(src,char_count,salt);
        return result;
    }

    /**
     * 验证密码
     * @param src 需要验证的原文
     * @param encode 加密后的密文
     * @return
     */
    public static boolean check(String src,String encode){
        char char_count = encode.charAt(COUNT_INDEX);
        char salt = encode.charAt(SALT_INDEX);
        String result = action(src,char_count,salt);
        System.out.println(result);
        return encode.equals(result);
    }

    /**
     * 加密方法
     * @param src 需要加密的方法
     * @param char_count 加密次数
     * @param salt 随机盐
     * @return
     */
    private static String action(String src,char char_count,char salt){
        String result =src+salt;  //原文与盐值拼接
        for(int i=0;i<char_count;i++){
            result = DigestUtils.md5DigestAsHex(result.getBytes());
        }
        String strA=result.substring(0,COUNT_INDEX);
        String strB=result.substring(COUNT_INDEX,SALT_INDEX-1);
        String strC=result.substring(SALT_INDEX-1);
        System.out.println(result);
        result = strA+char_count+strB+salt+strC;
        return result;
    }
}