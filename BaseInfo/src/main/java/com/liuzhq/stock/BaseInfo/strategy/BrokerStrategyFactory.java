package com.liuzhq.stock.BaseInfo.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 券商策略工厂（工厂模式）
 */
@Component
public class BrokerStrategyFactory {

    // 缓存所有券商策略实例（key：券商编码，value：对应的策略类）
    private final Map<String, BrokerDeliveryStrategy> strategyMap = new HashMap<>();

    @Autowired
    public BrokerStrategyFactory(List<BrokerDeliveryStrategy> strategyList) {
        for (BrokerDeliveryStrategy strategy : strategyList) {
            strategyMap.put(strategy.getBrokerCode(), strategy);
        }
    }

    /**
     * 根据券商编码获取对应的策略实例
     * @param brokerCode 券商编码（GTJA/GF/HT）
     * @return 对应的策略实例
     * @throws IllegalArgumentException 无对应券商策略时抛出异常
     */
    public BrokerDeliveryStrategy getStrategy(String brokerCode) {
        BrokerDeliveryStrategy strategy = strategyMap.get(brokerCode);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的券商编码：" + brokerCode);
        }
        return strategy;
    }
}