package com.liuzhq.stock.BaseInfo.strategy;

import com.liuzhq.stock.BaseInfo.dto.DeliveryOrderDto;
import com.liuzhq.stock.BaseInfo.entity.DeliveryOrder;

import java.util.List;
import java.util.Map;

/**
 * 券商交割单获取策略接口（策略模式核心）
 */
public interface BrokerDeliveryStrategy {

    /**
     * 获取券商编码（如GTJA-国泰君安，GF-广发）
     */
    String getBrokerCode();

    /**
     * 调用券商API获取交割单
     * @param dto 通用参数（账户号、开始时间、结束时间等）
     * @return 统一格式的交割单列表
     */
    List<DeliveryOrder> getDeliveryOrder(DeliveryOrderDto dto);
}