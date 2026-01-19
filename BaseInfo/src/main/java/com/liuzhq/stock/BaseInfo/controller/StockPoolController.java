package com.liuzhq.stock.BaseInfo.controller;

import com.liuzhq.common.response.ResultModel;
import com.liuzhq.stock.BaseInfo.client.PythonApiClient;
import com.liuzhq.stock.BaseInfo.dto.StockPoolDto;
import com.liuzhq.stock.BaseInfo.service.StockPoolService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 股票池数据接口 - 修复404问题并使用新API客户端
 * 适配JDK 8版本
 */
@RestController
@RequestMapping("/baseInfo/stockPool")
@Api(tags = "股票池数据接口")
@Slf4j
public class StockPoolController {

    @Autowired
    private StockPoolService stockPoolService;

    @Autowired
    private PythonApiClient pythonApiClient;

    @ApiOperation("同步指定股票池数据")
    @PostMapping("/sync/{poolKey}")
    public ResultModel<String> syncStockPool(
            @ApiParam(value = "股票池类型（zt=涨停池, dt=跌停池, yesterday_zt=昨日涨停, broken_zt=炸板池, super_stock=强势股池）", required = true)
            @PathVariable String poolKey,
            @ApiParam(value = "交易日期，格式yyyy-MM-dd，默认当天", required = false)
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate) {
        log.info("开始同步股票池数据，类型：{}，日期：{}", poolKey, tradeDate);

        try {
            boolean success = StringUtils.isEmpty(tradeDate.toString()) ?
                    stockPoolService.syncStockPoolData(poolKey) :
                    stockPoolService.syncStockPoolData(poolKey, tradeDate);

            if (success) {
                return ResultModel.success("股票池数据同步成功");
            } else {
                return ResultModel.error("股票池数据同步失败");
            }
        } catch (Exception e) {
            log.error("同步股票池数据失败，类型：{}", poolKey, e);
            return ResultModel.error("同步股票池数据失败：" + e.getMessage());
        }
    }

    /**
     * 查询指定日期的股票池数据
     */
    @GetMapping("/query")
    @ApiOperation("查询指定日期的股票池数据")
    public ResultModel<List<StockPoolDto>> queryStockPool(
            @ApiParam(value = "交易日期，格式yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate,
            @ApiParam(value = "股票池类型", required = true)
            @RequestParam String poolType,
            @ApiParam(value = "是否显示ST股票（0=显示，1=不显示，默认1）", required = false)
            @RequestParam(defaultValue = "1") Integer notShowSt) {
        log.info("查询股票池数据，日期：{}，类型：{}，是否显示ST：{}", tradeDate, poolType, notShowSt);

        try {
            List<StockPoolDto> result = stockPoolService.queryByDateAndPoolType(tradeDate, poolType, notShowSt);
            return ResultModel.success(result, "查询成功");
        } catch (Exception e) {
            log.error("查询股票池数据失败", e);
            return ResultModel.error("查询股票池数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取连板晋级梯度股票池
     */
    @GetMapping("/lbjj")
    @ApiOperation("获取连板晋级梯度股票池")
    public ResultModel<List<StockPoolDto>> lbjjStockPool(
            @ApiParam(value = "交易日期，格式yyyy-MM-dd", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate tradeDate,
            @ApiParam(value = "是否显示ST股票（0=显示，1=不显示，默认1）", required = false)
            @RequestParam(defaultValue = "1") Integer notShowSt) {
        log.info("获取连板晋级梯度股票池，日期：{}，是否显示ST：{}", tradeDate, notShowSt);

        try {
            List<StockPoolDto> result = stockPoolService.lbjjStockPool(tradeDate, notShowSt);
            return ResultModel.success(result, "获取成功");
        } catch (Exception e) {
            log.error("获取连板晋级梯度股票池失败", e);
            return ResultModel.error("获取连板晋级梯度股票池失败：" + e.getMessage());
        }
    }

    /**
     * 同步所有股票池数据
     */
    @PostMapping("/syncAll")
    @ApiOperation("同步所有股票池数据")
    public ResultModel<String> syncAllStockPool() {
        log.info("开始同步所有股票池数据");

        try {
            boolean success = stockPoolService.syncAllStockPoolData();
            if (success) {
                return ResultModel.success("所有股票池数据同步成功");
            } else {
                return ResultModel.error("所有股票池数据同步失败");
            }
        } catch (Exception e) {
            log.error("同步所有股票池数据失败", e);
            return ResultModel.error("同步所有股票池数据失败：" + e.getMessage());
        }
    }
}
