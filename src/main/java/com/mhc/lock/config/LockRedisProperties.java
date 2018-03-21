package com.mhc.lock.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 读取redis配置属性
 * @author jiangjingming
 */
@Data
@ConfigurationProperties(prefix = "spring.redis")
public class LockRedisProperties {
    private String host;
    private int port;
    private String password;
    private int timeout;
    private int maxIdle;
    private long maxWaitMillis;
}
