package com.liuzhq.common.feignclients;

import com.liuzhq.common.feignclients.fallback.Hystrix01ClientFallback;
import com.liuzhq.common.response.ResultModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Hystrix01Client
 * @Description consul-Hystrix-01包下的Feign客户端
 * @Author liuzhq
 * @Date 2023/12/13 21:13
 * @Version 1.0
 */
@FeignClient(name = "hystrix1", fallback = Hystrix01ClientFallback.class)
public interface Hystrix01Client {

    @GetMapping("/hystrix1/hystrix/fallback/{id}")
    ResultModel<String> demo2(@PathVariable("id") Integer id);
}
