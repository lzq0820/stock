package com.liuzhq.common.feignclients.productnacos;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * @author liuzhq
 * @version 1.0
 * @description
 * @date 2025/9/26 19:14
 */
@FeignClient("PRODUCTSERVICE")
public interface ProductNacosClient {
}
