package com.mhc.lock.service;

import com.mhc.lock.annotion.LockRedis;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * aop切面
 * Created by jiangjingming on 2018/1/12.
 */
@Slf4j
@Aspect
@Component
@ConditionalOnBean(JedisPool.class)
public class LockRedisService {

    private static final String LOCK_SUCCESS = "OK";
    private static final String SET_IF_NOT_EXIST = "NX";
    private static final String SET_WITH_EXPIRE_TIME = "PX";

    @Autowired
    private JedisPool jedisPool;

    @Pointcut("@annotation(com.mhc.lock.annotion.LockRedis)")
    public void lockRedis() {

    }

    @Around(value = "lockRedis()")
    public void isGetLockRedis(ProceedingJoinPoint joinPoint) throws InterruptedException {
        Jedis jedis = jedisPool.getResource();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        LockRedis lockRedis = method.getAnnotation(LockRedis.class);
        String redisKey = joinPoint.getTarget().getClass().getName().concat(".").concat(joinPoint.getSignature().getName());
        String redisValue = UUID.randomUUID().toString();
        boolean isGetLockFlag = tryGetDistributedLock(jedis, redisKey, redisValue, lockRedis.expireTime());
        if (isGetLockFlag) {
            try {
                joinPoint.proceed();
            } catch (Throwable throwable) {
                log.error("出错异常，throwable = [{}]", throwable);
            } finally {
                releaseDistributedLock(jedis,redisKey,redisValue,lockRedis);
                return;
            }
        }
        if (!isGetLockFlag) {
            managePolling(lockRedis, jedis, joinPoint);
        }
    }

    /**
     * 尝试获取分布式锁
     * @param jedis Redis客户端
     * @param lockKey 锁
     * @param redisValue 请求标识
     * @param expireTime 超期时间
     * @return 是否获取成功
     */
    public static boolean tryGetDistributedLock(Jedis jedis, String lockKey, String redisValue, int expireTime) {

        String result = jedis.set(lockKey, redisValue, SET_IF_NOT_EXIST, SET_WITH_EXPIRE_TIME, expireTime);

        if (LOCK_SUCCESS.equals(result)) {
            return true;
        }
        return false;

    }

    /**
     * 释放分布式锁
     * @param jedis Redis客户端
     * @param redisKey 锁
     * @param redisValue 请求标识
     */
    public static void releaseDistributedLock(Jedis jedis, String redisKey, String redisValue, LockRedis lockRedis) {
        boolean flag = lockRedis.isDelayReleaseLock();
        try {
            if (!flag) {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                jedis.eval(script, Collections.singletonList(redisKey), Collections.singletonList(redisValue));
            } else {
                jedis.expire(redisKey, lockRedis.leaseTime());
            }
        } catch (Exception e) {
            log.error("释放分布式锁releaseDistributedLock出现异常， e = [{}]", e);
        } finally {
            if (Objects.nonNull(jedis)) {
                jedis.close();
            }
        }

    }

    /**
     * 处理是否需要轮询
     * @param lockRedis
     * @param jedis
     * @param joinPoint
     */
    private void managePolling(LockRedis lockRedis, Jedis jedis, ProceedingJoinPoint joinPoint) throws InterruptedException {
        if (Objects.nonNull(jedis)) {
            jedis.close();
        }
        //是否轮询
        if (lockRedis.isPolling()) {
            int pollingIntervalTime = lockRedis.pollingIntervalTime();
            Thread.sleep(pollingIntervalTime);
            log.warn("Thread.sleep(),pollingIntervalTime==[{}]", pollingIntervalTime);
            isGetLockRedis(joinPoint);
        }
    }

}

