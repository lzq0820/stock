package com.liuzhq.stock.BaseInfo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liuzhq.stock.BaseInfo.entity.SysHoliday;
import java.time.LocalDate;
import java.util.List;

public interface SysHolidayService extends IService<SysHoliday> {

    /**
     * 同步指定年份的节假日数据到数据库
     */
    boolean syncHolidayData(Integer year);

    /**
     * 根据日期查询节假日信息
     */
    SysHoliday getHolidayByDate(LocalDate date);

    /**
     * 判断指定日期是否为交易日
     */
    boolean isTradeDay(LocalDate date);

    LocalDate getValidTradeDate(LocalDate targetDate);

    List<SysHoliday> query(Integer year, Integer month);
}