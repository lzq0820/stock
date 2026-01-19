package com.liuzhq.common.feignclients.fallback;

import com.liuzhq.common.feignclients.Hystrix01Client;
import com.liuzhq.common.response.ResultModel;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Hystrix01ClientFallback
 * @Description 开启OpenFeign调用过程中，指定Hystrix的降级方法
 * @Author liuzhq
 * @Date 2023/12/13 21:59
 * @Version 1.0
 */
@Component
public class Hystrix01ClientFallback implements Hystrix01Client {

    @Override
    public ResultModel<String> demo2(Integer id) {
        return ResultModel.error("Hystrix服务调用【/hystrix-openfeign/demo/test1(" + id +")】失败，请稍后重试");
    }
}
