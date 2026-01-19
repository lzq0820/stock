package com.liuzhq.stock.BaseInfo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.liuzhq.common.utils.JsonUtils;
import com.liuzhq.stock.BaseInfo.dto.innerClass.RelatedPlate;
import com.liuzhq.stock.BaseInfo.entity.StockPool;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 股票池数据实体
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockPoolDto extends StockPool {

    @ApiModelProperty("交易日期")
    private LocalDate tradeDate;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("晋级几率")
    private String chance;

    @ApiModelProperty("股票数据")
    private List<StockPoolDto> children;

    @ApiModelProperty("0-成，1-炸，2-败，3-跌")
    private Integer jjType;

}