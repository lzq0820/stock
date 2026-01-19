package com.liuzhq.common;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseEntity implements Serializable {

    @TableField(exist = false)
    private Integer pageNum;

    @TableField(exist = false)
    private Integer pageSize;

    @TableField(exist = false)
    private String startTime;

    @TableField(exist = false)
    private String endTime;

}
