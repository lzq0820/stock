package com.liuzhq.common.feignclients;

import com.liuzhq.common.dto.OpenFeignObjectParam;
import com.liuzhq.common.response.ResultModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * ProductClient
 * @Description 商品服务OpenFeign客户端
 * @Author liuzhq
 * @Date 2023/12/11 15:32
 * @Version 1.0
 */
@FeignClient("product")
public interface ProductClient {

    @GetMapping("/product/test/getProduct")
    String get();

    @GetMapping("/product/param/test1")
    String test1(@RequestParam("name") String name,
                 @RequestParam("age") Integer age);

    @PostMapping("/product/param/test2")
    OpenFeignObjectParam test2(@RequestBody OpenFeignObjectParam dto);

    @PostMapping("/product/param/test3")
    OpenFeignObjectParam test3(@RequestPart("dto") OpenFeignObjectParam dto);

    @GetMapping("/product/param/test10/{id}")
    ResultModel<OpenFeignObjectParam> test10(@PathVariable("id") Integer id);

    @PostMapping("/product/param/test11")
    ResultModel<OpenFeignObjectParam> test11(@RequestBody OpenFeignObjectParam dto);

    @GetMapping("/product/timeOut/defaultTimeOut")
    String defaultTimeOut();
}
