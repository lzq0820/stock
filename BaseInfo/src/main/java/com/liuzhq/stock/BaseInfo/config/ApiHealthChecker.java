package com.liuzhq.stock.BaseInfo.config;

import com.liuzhq.stock.BaseInfo.client.PythonApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * API健康检查组件
 */
@Component
@Slf4j
public class ApiHealthChecker {
    
    @Autowired
    private PythonApiClient pythonApiClient;
    
    @Scheduled(fixedDelay = 300000) // 每5分钟检查一次
    public void checkApiHealth() {
        log.info("开始检查Python API服务健康状况");
        
        try {
            // 测试基本连接
            String healthCheckUrl = "http://localhost:8000/";
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckUrl, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Python API服务连接正常");
            } else {
                log.warn("Python API服务连接异常，状态码：{}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Python API服务不可达", e);
        }
    }
    
    @Autowired
    private RestTemplate restTemplate;
}
