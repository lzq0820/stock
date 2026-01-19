package com.liuzhq.stock.BaseInfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuzhq.stock.BaseInfo.dto.StockPoolDto;
import com.liuzhq.stock.BaseInfo.entity.StockPool;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface StockPoolMapper extends BaseMapper<StockPool> {
    /**
     * 根据交易日期和股票池类型查询数据
     */
    List<StockPoolDto> selectByTradeDateAndPoolType(@Param("tradeDate") String tradeDate,
                                                    @Param("poolType") String poolType,
                                                    @Param("notShowSt") Integer notShowSt);

    /**
     * 批量插入数据（忽略重复）
     */
    int batchInsertIgnore(@Param("list") List<StockPool> stockPoolList);
}