package com.liuzhq.stock.BaseInfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuzhq.stock.BaseInfo.entity.DeliveryOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 交割单Mapper
 */
@Mapper
public interface DeliveryOrderMapper extends BaseMapper<DeliveryOrder> {
    /**
     * 多条件查询交割单
     */
    List<DeliveryOrder> queryDeliveryOrder(
            @Param("brokerCode") String brokerCode,
            @Param("accountNo") String accountNo,
            @Param("securityCode") String securityCode,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime
    );
}