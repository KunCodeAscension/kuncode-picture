package com.kuncode.kuncodepicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan(value = "com.kuncode.kuncodepicturebackend.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class KuncodePictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(KuncodePictureBackendApplication.class, args);
    }

}
