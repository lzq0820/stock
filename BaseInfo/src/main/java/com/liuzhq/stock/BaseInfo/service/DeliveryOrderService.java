package com.liuzhq.stock.BaseInfo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.pagehelper.PageInfo;
import com.liuzhq.stock.BaseInfo.dto.DeliveryOrderDto;
import com.liuzhq.stock.BaseInfo.entity.DeliveryOrder;

public interface DeliveryOrderService extends IService<DeliveryOrder> {

    PageInfo<DeliveryOrder> queryDeliveryOrderPage(DeliveryOrderDto dto);

    void syncDeliveryOrder(DeliveryOrderDto dto);

}
