package com.liuzhq.common.feignclients.usernacos;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author liuzhq
 * @version 1.0
 * @description
 * @date 2025/9/26 19:14
 */
@FeignClient("USERSERVICE")
public interface UserNacosClient {

    @GetMapping("/user/invoke")
    String invokeProduct();

}
