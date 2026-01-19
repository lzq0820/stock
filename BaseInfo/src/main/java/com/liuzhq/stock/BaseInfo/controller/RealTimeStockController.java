package com.liuzhq.stock.BaseInfo.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liuzhq.common.response.ResultModel;
import com.liuzhq.stock.BaseInfo.client.PythonApiClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 实时行情数据接口控制器
 */
/**
 * 实时股票行情控制器 - 修复404问题
 */
@RestController
@RequestMapping("/baseInfo/realtime")
@Api(tags = "实时股票行情接口")
@Slf4j
public class RealTimeStockController {

    @Autowired
    private PythonApiClient pythonApiClient;

    /**
     * 获取A股实时行情数据
     */
    @GetMapping("/a/realtime")
    @ApiOperation("获取A股实时行情")
    public ResultModel<JSONObject> getRealTimeData() {
        log.info("调用实时行情接口获取A股实时数据");
        try {
            String response = pythonApiClient.getRealTimeStockData();

            if (StringUtils.isBlank(response)) {
                log.warn("Python实时行情接口返回空响应");
                return ResultModel.success(new JSONObject(), "暂无数据");
            }

            JSONObject result = JSONObject.parseObject(response);
            Integer code = result.getInteger("code");
            if (code == null || code != 200) {
                String msg = result.getString("msg") != null ? result.getString("msg") : "未知错误";
                log.error("Python实时行情接口业务异常：{}", msg);
                return ResultModel.error("获取实时行情数据失败：" + msg);
            }

            return ResultModel.success(result, "获取成功");
        } catch (Exception e) {
            log.error("调用Python实时行情接口失败", e);
            return ResultModel.error("获取实时行情数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取沪深京A股实时行情统计数据
     */
    @GetMapping("/a/statistics")
    @ApiOperation("获取A股实时行情统计数据")
    public ResultModel<JSONObject> getRealTimeStatistics() {
        log.info("调用实时行情统计接口");
        try {
            String response = pythonApiClient.getRealTimeStatistics();

            if (StringUtils.isBlank(response)) {
                log.warn("Python实时行情统计接口返回空响应");
                return ResultModel.success(new JSONObject(), "暂无数据");
            }

            JSONObject result = JSONObject.parseObject(response);
            Integer code = result.getInteger("code");
            if (code == null || code != 200) {
                String msg = result.getString("msg") != null ? result.getString("msg") : "未知错误";
                log.error("Python实时行情统计接口业务异常：{}", msg);
                return ResultModel.error("获取实时行情统计数据失败：" + msg);
            }

            return ResultModel.success(result, "获取成功");
        } catch (Exception e) {
            log.error("调用Python实时行情统计接口失败", e);
            return ResultModel.error("获取实时行情统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 搜索股票
     */
    @GetMapping("/search")
    @ApiOperation("搜索股票")
    public ResultModel<JSONObject> searchStock(
            @ApiParam(value = "搜索关键词", required = true)
            @RequestParam String keyword,
            @ApiParam(value = "返回数量，默认10", required = false)
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("调用股票搜索接口，关键词：{}，限制：{}", keyword, limit);

        try {
            String response = pythonApiClient.searchStock(keyword, limit);

            if (StringUtils.isBlank(response)) {
                log.warn("Python股票搜索接口返回空响应，关键词：{}", keyword);
                return ResultModel.success(new JSONObject(), "未找到匹配的股票");
            }

            JSONObject result = JSONObject.parseObject(response);
            Integer code = result.getInteger("code");
            if (code == null || code != 200) {
                String msg = result.getString("msg") != null ? result.getString("msg") : "未知错误";
                log.error("Python股票搜索接口业务异常：{}", msg);
                return ResultModel.error("搜索股票失败：" + msg);
            }

            return ResultModel.success(result, "搜索成功");
        } catch (Exception e) {
            log.error("调用Python股票搜索接口失败，关键词：{}", keyword, e);
            return ResultModel.error("搜索股票失败：" + e.getMessage());
        }
    }

    /**
     * 获取实时行情中的涨跌幅排行榜
     */
    @GetMapping("/rankings")
    @ApiOperation("获取实时行情涨跌幅排行榜")
    public ResultModel<JSONObject> getRankings(
            @ApiParam(value = "排行榜类型：price(价格), change(涨跌幅), volume(成交量)", required = true)
            @RequestParam String type,
            @ApiParam(value = "返回数量，默认10", required = false)
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("调用实时行情排行榜接口，类型：{}，限制：{}", type, limit);

        try {
            // 调用实时统计接口获取排行榜数据
            String response = pythonApiClient.getRealTimeStatistics();

            if (StringUtils.isBlank(response)) {
                log.warn("Python实时行情统计接口返回空响应");
                return ResultModel.success(new JSONObject(), "暂无排行榜数据");
            }

            JSONObject result = JSONObject.parseObject(response);
            Integer code = result.getInteger("code");
            if (code == null || code != 200) {
                String msg = result.getString("msg") != null ? result.getString("msg") : "未知错误";
                log.error("Python实时行情统计接口业务异常：{}", msg);
                return ResultModel.error("获取排行榜数据失败：" + msg);
            }

            // 从统计结果中提取相应的排行榜数据
            JSONObject statistics = result.getJSONObject("statistics");
            String rankingField = getRankingFieldByType(type);

            if (statistics.containsKey(rankingField)) {
                JSONArray rankingData = statistics.getJSONArray(rankingField);
                if (rankingData != null && !rankingData.isEmpty()) {
                    // 限制返回数量
                    JSONArray limitedData = new JSONArray();
                    int count = Math.min(limit, rankingData.size());
                    for (int i = 0; i < count; i++) {
                        limitedData.add(rankingData.get(i));
                    }

                    JSONObject responseData = new JSONObject();
                    responseData.put("type", type);
                    responseData.put("limit", limit);
                    responseData.put("data", limitedData);
                    responseData.put("total", rankingData.size());

                    return ResultModel.success(responseData, "获取排行榜数据成功");
                } else {
                    return ResultModel.success(new JSONObject(), "暂无该类型的排行榜数据");
                }
            } else {
                return ResultModel.error("不支持的排行榜类型：" + type);
            }
        } catch (Exception e) {
            log.error("获取排行榜数据失败，类型：{}", type, e);
            return ResultModel.error("获取排行榜数据失败：" + e.getMessage());
        }
    }

    private String getRankingFieldByType(String type) {
        switch (type.toLowerCase()) {
            case "price":
                return "top_price";
            case "change":
                return "top_change";
            case "volume":
                return "top_volume";
            default:
                return "top_change"; // 默认按涨跌幅排序
        }
    }
}
