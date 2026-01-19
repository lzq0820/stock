package com.liuzhq.stock.BaseInfo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.liuzhq.common.utils.JsonUtils;
import com.liuzhq.stock.BaseInfo.dto.innerClass.RelatedPlate;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 股票池数据实体
 */
@Data
@TableName("t_stock_pool")
public class StockPool {
    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 交易日期（yyyy-MM-dd）
     */
    private LocalDate tradeDate;

    /**
     * 股票代码（如603986.SS）
     */
    private String stockCode;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 股票类型
     */
    private Integer stockType;

    /**
     * 最新价（元）
     */
    private BigDecimal price;

    /**
     * 涨跌幅（%）
     */
    private BigDecimal changePercent;

    /**
     * 换手率（%）
     */
    private BigDecimal turnoverRatio;

    /**
     * 流通市值（亿元）
     */
    private BigDecimal circulationMarketCap;

    /**
     * 总市值（亿元）
     */
    private BigDecimal totalMarketCap;

    /**
     * 发行价（元）
     */
    private BigDecimal issuePrice;

    /**
     * 上市日期
     */
    private LocalDateTime listedDate;

    /**
     * 股票池类型（zt=涨停池, dt=跌停池, yesterday_zt=昨日涨停, broken_zt=炸板池, super_stock=强势股池）
     */
    private String poolType;

    /**
     * 买盘封单比（%）
     */
    private BigDecimal buyLockRatio;

    /**
     * 卖盘封单比（%）
     */
    private BigDecimal sellLockRatio;

    /**
     * 当前封单金额（元）
     */
    private BigDecimal currentLockAmount;

    /**
     * 最高封单金额（元）
     */
    private BigDecimal maxLockAmount;

    /**
     * 涨停天数（涨停池/强势股池）
     */
    private Integer limitDays;

    /**
     * 开板次数（涨停池/跌停池）
     */
    private Integer breakLimitTimes;

    /**
     * 首次封板时间（涨停池/炸板池/强势股池）
     */
    private LocalDateTime firstLimitUpTime;

    /**
     * 最后封板时间（涨停池/强势股池）
     */
    private LocalDateTime lastLimitUpTime;

    /**
     * 首次封跌停时间（跌停池）
     */
    private LocalDateTime firstLimitDownTime;

    /**
     * 最后封跌停时间（跌停池）
     */
    private LocalDateTime lastLimitDownTime;

    /**
     * 昨日开板次数（昨日涨停池）
     */
    private Integer yesterdayBreakLimitUpTimes;

    /**
     * 昨日首次封板时间（昨日涨停池）
     */
    private LocalDateTime yesterdayFirstLimitUpTime;

    /**
     * 昨日最后封板时间（昨日涨停池）
     */
    private LocalDateTime yesterdayLastLimitUpTime;

    /**
     * 最后炸板时间（炸板池）
     */
    private LocalDateTime lastBreakLimitUpTime;

    /**
     * 几天几板（强势股池）
     */
    private String mDaysNBoards;

    /**
     * 股票上涨/下跌原因
     */
    private String stockReason;

    /**
     * 相关板块（JSON字符串）
     */
    private String relatedPlates;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    @ApiModelProperty("涨停/跌停原因")
    @TableField(exist = false)
    private String reason;

    public void buildStockNameAndReason() {
        String relatedPlates = StringUtils.defaultIfBlank(getRelatedPlates(), "[]");
        List<RelatedPlate> relatedPlateList = JsonUtils.parseArray(relatedPlates, RelatedPlate.class);
        List<String> relatedPlateNameList = relatedPlateList.stream().map(RelatedPlate::getPlateName).collect(Collectors.toList());
        List<String> relatedPlateReasonList = relatedPlateList.stream().map(RelatedPlate::getPlateReason).collect(Collectors.toList());
        setRelatedPlates(getStockReason());
        setReason(StringUtils.join(relatedPlateNameList, "+"));
        setStockName(getStockName() + "[" + getPrice() + (StringUtils.isNotBlank(getReason()) ? ("," + getReason()) : "") + "]");
        setStockReason(StringUtils.defaultIfBlank(StringUtils.join(relatedPlateReasonList, "\n"), null));
    }
}