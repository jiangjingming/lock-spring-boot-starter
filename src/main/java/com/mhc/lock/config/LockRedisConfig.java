package com.mhc.lock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;



/**
 * 生成redis jedisPoll
 * @author jiangjingming
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LockRedisProperties.class)
public class LockRedisConfig {

    @Autowired
    private LockRedisProperties lockRedisProperties;

    @Bean
    @ConditionalOnMissingBean
    public JedisPool redisPoolFactory() {
        String host = lockRedisProperties.getHost();
        int port = lockRedisProperties.getPort();
        String password = lockRedisProperties.getPassword();
        int maxIdle = lockRedisProperties.getMaxIdle();
        Long maxWaitMillis = lockRedisProperties.getMaxWaitMillis();
        int timeout = lockRedisProperties.getTimeout();

        log.info("lock_JedisPool注入成功！！");
        log.info("lock_redis地址：" + host + ":" + port);
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);

        JedisPool jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password);

        return jedisPool;
    }
}
