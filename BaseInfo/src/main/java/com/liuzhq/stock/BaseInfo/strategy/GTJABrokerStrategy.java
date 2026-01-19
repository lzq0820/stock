package com.liuzhq.stock.BaseInfo.strategy;

import com.liuzhq.stock.BaseInfo.dto.DeliveryOrderDto;
import com.liuzhq.stock.BaseInfo.entity.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 国泰君安券商策略实现
 */
@Slf4j
@Component // 交给Spring管理
public class GTJABrokerStrategy implements BrokerDeliveryStrategy {

    @Override
    public String getBrokerCode() {
        return "GTJA"; // 国泰君安编码
    }

    @Override
    public List<DeliveryOrder> getDeliveryOrder(DeliveryOrderDto dto) {
        // 1. 解析通用参数
        String accountNo = dto.getAccountNo();
        String startTime = dto.getStartTime();
        String endTime = dto.getEndTime();
        
        // 2. 调用国泰君安官方API（此处为模拟，真实场景替换为实际API调用）
        log.info("调用国泰君安API，账户：{}，时间范围：{} - {}", accountNo, startTime, endTime);
        // 模拟API返回的原始数据（真实场景需解析券商返回的JSON/XML数据）
        List<DeliveryOrder> rawData = mockGTJAAPIResponse(accountNo, startTime, endTime);
        
        // 3. 转换为统一的DeliveryOrder对象（核心：屏蔽不同券商的字段差异）
        for (DeliveryOrder item : rawData) {
            item.setBrokerCode(getBrokerCode());
            item.setAccountNo(accountNo);
        }
        
        return rawData;
    }

    /**
     * 模拟国泰君安API返回数据
     */
    private List<DeliveryOrder> mockGTJAAPIResponse(String accountNo, String startTime, String endTime) {
        List<DeliveryOrder> data = new ArrayList<>();
        DeliveryOrder order = new DeliveryOrder();
        order.setBrokerCode(getBrokerCode());
        order.setAccountNo(accountNo);
        order.setSecurityCode("600000");
        order.setSecurityName("浦发银行");
        order.setTradeType("卖出");
        order.setTradeTime(new Date());
        order.setTradePrice(new BigDecimal("8.50"));
        order.setTradeQuantity(1000);
        order.setTradeAmount(new BigDecimal("8500.00"));
        order.setCommissionFee(new BigDecimal("4.25"));
        order.setStampTax(new BigDecimal("0.00"));
        order.setTransferFee(new BigDecimal("1.00"));
        order.setTotalFee(new BigDecimal("5.25"));
        data.add(order);
        return data;
    }
}