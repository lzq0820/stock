package com.liuzhq.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;

/**
 * 访问：
 * 第三方swagger界面：http://localhost:${server.port}/${spring.application.name}/doc.html
 * swagger界面：http://localhost:${server.port}/${spring.application.name}/swagger-ui/index.html
 */

@Configuration
public class SwaggerConfig implements WebMvcConfigurer {
    /**
     * 用于读取配置文件 application.properties 中 swagger 属性是否开启
     */
    @Value("${swagger.enabled}")
    Boolean swaggerEnabled;

    /**
     * 配置文件未定义时，赋予默认值
     */
    @Value("${swagger.title:配置文件未定义}")
    private String swaggerTitle;

    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.OAS_30)
                .apiInfo(apiInfo())
                // 是否开启swagger
                .enable(swaggerEnabled)
                // 选择那些路径和api会生成document
                .select()
                /**
                 * 过滤条件，扫描指定路径条件下的文件
                 */
                // 扫描带 StartSwaggerScan 注解的controller，为了开发用，选择关闭这个功能
//                .apis(RequestHandlerSelectors.withClassAnnotation(StartSwaggerScan.class))
                // 对根下所有路径进行监控
                /**
                 * RequestHandlerSelectors.any()：扫描所有接口
                 * RequestHandlerSelectors.none()：不扫描任何接口
                 */
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        /*作者信息*/
        Contact contact = new Contact("江南好", "", "1641140182@qq.com");
        return new ApiInfo(
                swaggerTitle,
                "Spring Boot 集成 Swagger3 测试接口文档",
                "v1.0",
                "",
                contact,
                "Apache 2.0",
                "http://www.apache.org/licenses/LICENSE-2.0",
                new ArrayList()
        );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        // swagger第三方ui配置：swagger-bootstrap-ui
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
    }
}