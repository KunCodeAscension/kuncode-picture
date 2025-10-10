package com.kuncode.kuncodepicturebackend.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 创建配置
        Config config = new Config();
        // 单节点模式，这里替换为你的Redis地址
        config.useSingleServer()
              .setAddress("redis://127.0.0.1:6379")
              .setDatabase(0)
              .setPassword("123456");
        return Redisson.create(config);
    }
}