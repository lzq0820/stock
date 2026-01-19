package com.liuzhq.stock.BaseInfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liuzhq.stock.BaseInfo.entity.SysHoliday;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SysHolidayMapper extends BaseMapper<SysHoliday> {

    /**
     * 根据日期查询节假日信息
     */
    SysHoliday selectByHolidayDate(@Param("holidayDate") LocalDate holidayDate);

    /**
     * 根据年份查询节假日列表
     */
    List<SysHoliday> selectByYear(@Param("year") Integer year);

    /**
     * 批量插入或更新节假日数据
     */
    int batchInsertOrUpdate(@Param("list") List<SysHoliday> list);
}