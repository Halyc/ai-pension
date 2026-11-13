package com.easy.common.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUtil {

    @Autowired
    @Qualifier(value = "redisTemplate")  //和  一起使用可以通过buName方式注入bean

    RedisTemplate redisTemplate;

    public static final String BLACK_LIST_KEY = "blacklist";

    public Long disabledUser(Long userId) {

        return redisTemplate.opsForSet().add(BLACK_LIST_KEY, userId);
    }

    public long ableUser(Long userId) {

        return redisTemplate.opsForSet().remove(BLACK_LIST_KEY, userId);
    }

    public boolean checkUserBlacklist(Long userId){

        return redisTemplate.opsForSet().isMember(BLACK_LIST_KEY,userId);
    }




}
