package com.liuzhq.stock.BaseInfo.strategy;

import com.liuzhq.stock.BaseInfo.dto.DeliveryOrderDto;
import com.liuzhq.stock.BaseInfo.entity.DeliveryOrder;

import java.util.List;
import java.util.Map;

// 抽象模板类
public abstract class AbstractBrokerDeliveryStrategy implements BrokerDeliveryStrategy {

    // 模板方法：定义固定流程
    @Override
    public final List<DeliveryOrder> getDeliveryOrder(DeliveryOrderDto dto) {
        // 1. 公共逻辑：参数校验
        validateParams(dto);
        // 2. 子类实现：调用具体券商API
        List<Map<String, Object>> rawData = callBrokerApi(dto);
        // 3. 公共逻辑：数据转换
        return convertToDeliveryOrder(rawData, dto.getAccountNo());
    }

    // 公共方法：参数校验
    private void validateParams(DeliveryOrderDto dto) {
        if (dto.getAccountNo() == null) {
            throw new IllegalArgumentException("账户号不能为空");
        }
    }

    // 抽象方法：子类实现具体API调用
    protected abstract List<Map<String, Object>> callBrokerApi(DeliveryOrderDto dto);

    // 抽象方法：子类实现数据转换（不同券商字段不同）
    protected abstract List<DeliveryOrder> convertToDeliveryOrder(List<Map<String, Object>> rawData, String accountNo);

    // 保留getBrokerCode抽象方法
    @Override
    public abstract String getBrokerCode();
}