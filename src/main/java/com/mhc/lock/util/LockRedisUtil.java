package com.mhc.lock.util;

import com.mhc.lock.service.LockRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.Objects;

/**
 * 分布锁工具
 * Created by jiangjingming on 2018/3/28.
 */
@Slf4j
public class LockRedisUtil {

    private static final Long RELEASE_SUCCESS = 1L;

    private static JedisPool jedisPool = SpringUtil.getBean(JedisPool.class);

    /**
     * 获取分布式锁
     * @param redisKey
     * @param redisValue
     * @param expireTime
     * @return 是否获取锁
     */
    public static boolean tryGetDistributedLock(String redisKey, String redisValue, Integer expireTime) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return LockRedisService.tryGetDistributedLock(jedis, redisKey, redisValue, expireTime);
        } catch (Exception e) {
            log.error("获取分布式锁异常, redisKey= [{}], redisValue = [{}], expireTime = [{}],异常信息 = [{}]", redisKey, redisValue, expireTime, e);
            return false;
        } finally {
            if (Objects.nonNull(jedis)) {
                jedis.close();
            }
        }
    }

    /**
     * 归还分布式锁
     * @param redisKey
     * @param redisValue
     * @return 是否归还锁
     */
    public static boolean releaseDistributedLock(String redisKey, String redisValue) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(redisKey), Collections.singletonList(redisValue));
            if (RELEASE_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("归还分布式锁异常, redisKey= [{}], redisValue = [{}],异常信息 = [{}]", redisKey, redisValue, e);
            return false;
        } finally {
            jedis.close();
        }
    }
}
