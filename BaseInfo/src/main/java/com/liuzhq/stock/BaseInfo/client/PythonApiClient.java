package com.liuzhq.stock.BaseInfo.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liuzhq.stock.BaseInfo.config.ApiConfig;
import com.liuzhq.stock.BaseInfo.config.ApiConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Python API客户端（完全适配Python FastAPI服务）
 * 核心修改：支持传递日期参数调用统计接口
 */
/**
 * 重构后的Python API客户端 - 使用配置化管理
 */
@Slf4j
@Component
public class PythonApiClient {

    @Autowired
    @Qualifier("pythonApiRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ApiConfigManager apiConfigManager;

    /**
     * 通用API调用方法
     */
    public String callApi(String apiName, Map<String, Object> params) throws UnsupportedEncodingException {
        ApiConfig config = apiConfigManager.getApiConfig(apiName);
        if (config == null || !config.isEnabled()) {
            throw new IllegalArgumentException("API不存在或已禁用: " + apiName);
        }

        String fullUrl = buildUrlWithParams(apiConfigManager.getFullUrl(apiName), params);

        try {
            log.debug("调用Python API: {} {}", config.getMethod(), fullUrl);

            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.valueOf(config.getMethod()),
                    null,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("API调用失败，状态码: {}, URL: {}", response.getStatusCode(), fullUrl);
                throw new RuntimeException("API调用失败，状态码: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.error("API调用404错误: {}", fullUrl, e);
            throw new RuntimeException("API接口不存在，请检查Python服务是否正常运行: " + fullUrl + "。可能的原因是API路径配置错误或Python服务未启动。");
        } catch (Exception e) {
            log.error("调用Python API失败: {}", fullUrl, e);
            throw new RuntimeException("调用Python API失败: " + e.getMessage());
        }
    }

    /**
     * 构建带参数的URL
     */
    private String buildUrlWithParams(String baseUrl, Map<String, Object> params) throws UnsupportedEncodingException {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("?");

        boolean first = true;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            urlBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(String.valueOf(entry.getValue()), "UTF-8"));
            first = false;
        }

        return urlBuilder.toString();
    }

    /**
     * 获取股票池数据
     */
    public List<Map<String, Object>> getStockPoolData(String poolKey, LocalDate tradeDate) throws UnsupportedEncodingException {
        // 根据poolKey确定对应的API名称
        String apiName = getApiNameForPoolKey(poolKey);

        Map<String, Object> params = new HashMap<>();
        if (tradeDate != null) {
            params.put("trade_date", tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        }

        String response = callApi(apiName, params);

        try {
            JSONObject result = JSON.parseObject(response);
            Integer code = result.getInteger("code");
            if (code != null && code == 200) {
                JSONArray data = result.getJSONArray("data");
                if (data != null && !data.isEmpty()) {
                    List<Map<String, Object>> resultData = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++) {
                        if (data.get(i) instanceof JSONObject) {
                            JSONObject obj = (JSONObject) data.get(i);
                            resultData.add(obj.getInnerMap());
                        }
                    }
                    return resultData;
                }
            } else {
                log.error("获取股票池数据失败，错误码: {}, 消息: {}", code, result.getString("msg"));
                throw new RuntimeException("获取股票池数据失败: " + result.getString("msg"));
            }
        } catch (Exception e) {
            log.error("解析股票池数据失败", e);
            throw new RuntimeException("解析股票池数据失败: " + e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 根据股票池类型获取对应的API名称
     */
    private String getApiNameForPoolKey(String poolKey) {
        switch (poolKey) {
            case "zt":
                return "xuangubao_zt_pool";
            case "dt":
                return "xuangubao_dt_pool";
            case "yesterday_zt":
                return "xuangubao_yesterday_zt_pool";
            case "broken_zt":
                return "xuangubao_broken_zt_pool";
            case "super_stock":
                return "xuangubao_super_stock_pool";
            default:
                throw new IllegalArgumentException("不支持的股票池类型: " + poolKey);
        }
    }

    /**
     * 获取实时行情数据
     */
    public String getRealTimeStockData() throws UnsupportedEncodingException {
        return callApi("eastmoney_realtime", Collections.emptyMap());
    }

    /**
     * 获取实时行情统计
     */
    public String getRealTimeStatistics() throws UnsupportedEncodingException {
        return callApi("eastmoney_statistics", Collections.emptyMap());
    }

    /**
     * 搜索股票
     */
    public String searchStock(String keyword, Integer limit) throws UnsupportedEncodingException {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        if (limit != null) {
            params.put("limit", limit);
        }

        return callApi("eastmoney_search", params);
    }
}

