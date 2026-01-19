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
 * 广发证券策略实现
 */
@Slf4j
@Component
public class GFBrokerStrategy implements BrokerDeliveryStrategy {

    @Override
    public String getBrokerCode() {
        return "GF"; // 广发证券编码
    }

    @Override
    public List<DeliveryOrder> getDeliveryOrder(DeliveryOrderDto dto) {
        // 1. 解析参数
        String accountNo = dto.getAccountNo();
        String startTime = dto.getStartTime();
        String endTime = dto.getEndTime();
        
        // 2. 调用广发证券API（模拟）
        log.info("调用广发证券API，账户：{}，时间范围：{} - {}", accountNo, startTime, endTime);
        List<DeliveryOrder> rawData = mockGFAPIResponse(accountNo, startTime, endTime);
        
        // 3. 转换为统一的DeliveryOrder（注意：广发的字段名和国泰君安不同，需适配）
        for (DeliveryOrder item : rawData) {
            item.setBrokerCode(getBrokerCode());
            item.setAccountNo(accountNo);
        }
        
        return rawData;
    }

    /**
     * 模拟广发证券API返回数据
     */
    private List<DeliveryOrder> mockGFAPIResponse(String accountNo, String startTime, String endTime) {
        List<DeliveryOrder> data = new ArrayList<>();
        DeliveryOrder order = new DeliveryOrder();
        order.setBrokerCode(getBrokerCode());
        order.setAccountNo(accountNo);
        order.setSecurityCode("000001");
        order.setSecurityName("平安银行");
        order.setTradeType("卖出");
        order.setTradeTime(new Date());
        order.setTradePrice(new BigDecimal("12.80"));
        order.setTradeQuantity(500);
        order.setTradeAmount(new BigDecimal("6400.00"));
        order.setCommissionFee(new BigDecimal("3.20"));
        order.setStampTax(new BigDecimal("6.40"));
        order.setTransferFee(new BigDecimal("0.64"));
        order.setTotalFee(new BigDecimal("10.24"));
        data.add(order);
        return data;
    }
}