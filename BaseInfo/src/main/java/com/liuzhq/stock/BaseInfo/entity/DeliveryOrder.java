package com.liuzhq.stock.BaseInfo.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.liuzhq.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 交割单实体类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_delivery_order")
public class DeliveryOrder extends BaseEntity {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 券商编码 */
    private String brokerCode;
    /** 证券账户号 */
    private String accountNo;
    /** 证券代码 */
    private String securityCode;
    /** 证券名称 */
    private String securityName;
    /** 交易类型（买入/卖出） */
    private String tradeType;
    /** 成交时间 */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date tradeTime;
    /** 成交价格 */
    private BigDecimal tradePrice;
    /** 成交数量 */
    private Integer tradeQuantity;
    /** 成交金额 */
    private BigDecimal tradeAmount;
    /** 佣金 */
    private BigDecimal commissionFee;
    /** 印花税 */
    private BigDecimal stampTax;
    /** 过户费 */
    private BigDecimal transferFee;
    /** 总费用 */
    private BigDecimal totalFee;
}