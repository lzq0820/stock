package com.liuzhq.stock.BaseInfo;

import com.liuzhq.common.config.SwaggerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@Slf4j
// 扫描指定包，但排除 SwaggerConfig 类
@ComponentScan(
        basePackages = {"com.liuzhq.stock.BaseInfo", "com.liuzhq.common"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SwaggerConfig.class
        )
)
public class BaseInfoApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(BaseInfoApplication.class, args);
        String serverPort = context.getEnvironment().getProperty("server.port");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path");
        log.info("基础信息服务启动成功 http://localhost:{}{}", serverPort, contextPath);
    }
}