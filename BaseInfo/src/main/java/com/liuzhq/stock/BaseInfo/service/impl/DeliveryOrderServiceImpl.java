package com.liuzhq.stock.BaseInfo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.liuzhq.stock.BaseInfo.dto.DeliveryOrderDto;
import com.liuzhq.stock.BaseInfo.entity.DeliveryOrder;
import com.liuzhq.stock.BaseInfo.entity.StockPool;
import com.liuzhq.stock.BaseInfo.mapper.DeliveryOrderMapper;
import com.liuzhq.stock.BaseInfo.mapper.StockPoolMapper;
import com.liuzhq.stock.BaseInfo.service.DeliveryOrderService;
import com.liuzhq.stock.BaseInfo.service.StockPoolService;
import com.liuzhq.stock.BaseInfo.strategy.BrokerDeliveryStrategy;
import com.liuzhq.stock.BaseInfo.strategy.BrokerStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 交割单服务层（集成策略+工厂模式）
 */
@Slf4j
@Service
public class DeliveryOrderServiceImpl extends ServiceImpl<DeliveryOrderMapper, DeliveryOrder> implements DeliveryOrderService {

    @Resource
    private DeliveryOrderMapper deliveryOrderMapper;
    @Resource
    private BrokerStrategyFactory brokerStrategyFactory;

    // ========== 原有分页查询方法（保留） ==========
    @Override
    public PageInfo<DeliveryOrder> queryDeliveryOrderPage(DeliveryOrderDto dto) {
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        String brokerCode = dto.getBrokerCode();
        String accountNo = dto.getAccountNo();
        String securityCode = dto.getSecurityCode();
        String startTime = dto.getStartTime();
        String endTime = dto.getEndTime();

        try (Page<DeliveryOrder> page = PageHelper.startPage(pageNum, pageSize)) {
            List<DeliveryOrder> list = deliveryOrderMapper.queryDeliveryOrder(
                    brokerCode, accountNo, securityCode, startTime, endTime
            );
            return new PageInfo<>(list);
        }
    }

    // ========== 新增：同步指定券商的交割单 ==========
    @Transactional(rollbackFor = Exception.class)
    public void syncDeliveryOrder(DeliveryOrderDto dto) {
        // 1. 通过工厂获取对应券商的策略实例
        BrokerDeliveryStrategy strategy = brokerStrategyFactory.getStrategy(dto.getBrokerCode());

        // 3. 调用策略方法获取交割单（屏蔽不同券商的API差异）
        List<DeliveryOrder> deliveryOrders = strategy.getDeliveryOrder(dto);

        // 4. 入库（此处省略批量插入逻辑，可根据你的Mapper扩展）
        if (!deliveryOrders.isEmpty()) {
            log.info("同步{}券商交割单，共{}条数据", dto.getBrokerCode(), deliveryOrders.size());
            // deliveryOrderMapper.batchInsert(deliveryOrders); // 需自行实现批量插入
        }
    }
}