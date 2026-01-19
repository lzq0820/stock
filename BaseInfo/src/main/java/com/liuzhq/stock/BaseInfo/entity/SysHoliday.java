package com.liuzhq.stock.BaseInfo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.liuzhq.common.annotation.Description;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Description("法定节假日")
@Data
@TableName("sys_holiday")
public class SysHoliday {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 节假日日期
     */
    @TableField("holiday_date")
    private LocalDate holidayDate;

    /**
     * 节假日名称
     */
    @TableField("holiday_name")
    private String holidayName;

    /**
     * 是否为法定节假日 0-否 1-是
     */
    @TableField("is_holiday")
    private Integer isHoliday;

    /**
     * 是否为调休补班日 0-否 1-是
     */
    @TableField("is_makeup_work")
    private Integer isMakeupWork;

    /**
     * 年份
     */
    @TableField("year")
    private Integer year;

    /**
     * 月份
     */
    @TableField("month")
    private Integer month;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}