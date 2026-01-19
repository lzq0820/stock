package com.example.springcloudalibaba01env;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class SpringCloudAlibaba01EnvApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudAlibaba01EnvApplication.class, args);
        log.info("nacos 客户端启动成功...");
    }

}
