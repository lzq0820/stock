package com.liuzhq.stock.BaseInfo.controller;

import com.github.pagehelper.PageInfo;
import com.liuzhq.common.response.ResultModel;
import com.liuzhq.stock.BaseInfo.dto.DeliveryOrderDto;
import com.liuzhq.stock.BaseInfo.entity.DeliveryOrder;
import com.liuzhq.stock.BaseInfo.service.DeliveryOrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 交割单控制器
 */
@RestController
@RequestMapping("/delivery")
public class DeliveryOrderController {

    @Resource
    private DeliveryOrderService deliveryOrderService;

    /**
     * 分页查询交割单
     */
    @GetMapping("/order/list")
    public ResultModel<PageInfo<DeliveryOrder>> queryDeliveryOrder(@RequestBody DeliveryOrderDto dto) {
        try {
            PageInfo<DeliveryOrder> pageInfo = deliveryOrderService.queryDeliveryOrderPage(dto);
            return ResultModel.success(pageInfo);
        } catch (Exception e) {
            return ResultModel.error("查询交割单失败：" + e.getMessage());
        }
    }

    // 新增：同步指定券商的交割单
    @PostMapping("/order/sync")
    public ResultModel<Void> syncDeliveryOrder(@RequestBody DeliveryOrderDto dto) {
        try {
            deliveryOrderService.syncDeliveryOrder(dto);
            return ResultModel.success(null);
        } catch (Exception e) {
            return ResultModel.error("同步交割单失败：" + e.getMessage());
        }
    }
}