package com.liuzhq.stock.BaseInfo.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API配置管理器 - 动态加载和管理Python API配置
 */
@Component
@Slf4j
public class ApiConfigManager {

    private volatile Map<String, ApiConfig> apiConfigs = new ConcurrentHashMap<>();
    private volatile Map<String, String> providerBaseUrls = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadApiConfigsFromPython();
        loadProviderBaseUrls();
    }

    /**
     * 从Python脚本目录加载API配置
     */
    private void loadApiConfigsFromPython() {
        try {
            String configPath = System.getProperty("user.dir") + "/src/main/resources/stock_api_service/api_config.json";
            File configFile = new File(configPath);
            if (configFile.exists()) {
                String configContent = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
                // 解析JSON配置
                JSONObject jsonObject = JSONObject.parseObject(configContent);
                JSONArray apisArray = jsonObject.getJSONArray("apis");

                for (int i = 0; i < apisArray.size(); i++) {
                    JSONObject apiObj = apisArray.getJSONObject(i);
                    ApiConfig config = new ApiConfig();
                    config.setName(apiObj.getString("name"));
                    config.setMethod(apiObj.getString("method"));
                    config.setPath(apiObj.getString("path"));
                    config.setDescription(apiObj.getString("description"));
                    config.setProvider(apiObj.getString("provider"));
                    config.setCategory(apiObj.getString("category"));
                    config.setTimeout(apiObj.getIntValue("timeout"));
                    config.setEnabled(apiObj.getBooleanValue("enabled"));

                    apiConfigs.put(config.getName(), config);
                }
                log.info("成功加载 {} 个API配置", apisArray.size());
            } else {
                // 如果JSON文件不存在，创建默认配置
                initializeDefaultConfigs();
            }
        } catch (Exception e) {
            log.error("加载API配置失败", e);
            // 发生错误时使用默认配置
            initializeDefaultConfigs();
        }
    }

    /**
     * 初始化默认配置（当JSON配置文件不存在时使用）
     */
    private void initializeDefaultConfigs() {
        log.info("使用默认API配置");

        // 东方财富API
        initializeDefaultEastMoneyConfigs();

        // 选股宝API
        initializeDefaultXuanGuBaoConfigs();

        log.warn("使用默认配置，建议创建 api_config.json 文件以获得更好的灵活性");
    }

    private void initializeDefaultEastMoneyConfigs() {
        apiConfigs.put("eastmoney_realtime", createApiConfig(
                "eastmoney_realtime", "GET", "/api/eastmoney/api/stock/a/realtime",
                "东方财富实时行情", "eastmoney", "realtime", 30000, true
        ));
        apiConfigs.put("eastmoney_statistics", createApiConfig(
                "eastmoney_statistics", "GET", "/api/eastmoney/api/stock/a/realtime/statistics",
                "东方财富实时行情统计", "eastmoney", "statistics", 30000, true
        ));
        apiConfigs.put("eastmoney_search", createApiConfig(
                "eastmoney_search", "GET", "/api/eastmoney/api/stock/search",
                "东方财富股票搜索", "eastmoney", "search", 30000, true
        ));
    }

    private void initializeDefaultXuanGuBaoConfigs() {
        apiConfigs.put("xuangubao_zt_pool", createApiConfig(
                "xuangubao_zt_pool", "GET", "/api/xuangubao/stock/pool/zt",
                "选股宝涨停池", "xuangubao", "stock_pool", 30000, true
        ));
        apiConfigs.put("xuangubao_dt_pool", createApiConfig(
                "xuangubao_dt_pool", "GET", "/api/xuangubao/stock/pool/dt",
                "选股宝跌停池", "xuangubao", "stock_pool", 30000, true
        ));
        apiConfigs.put("xuangubao_yesterday_zt_pool", createApiConfig(
                "xuangubao_yesterday_zt_pool", "GET", "/api/xuangubao/stock/pool/yesterday_zt",
                "选股宝昨日涨停池", "xuangubao", "stock_pool", 30000, true
        ));
        apiConfigs.put("xuangubao_broken_zt_pool", createApiConfig(
                "xuangubao_broken_zt_pool", "GET", "/api/xuangubao/stock/pool/broken_zt",
                "选股宝炸板池", "xuangubao", "stock_pool", 30000, true
        ));
        apiConfigs.put("xuangubao_super_stock_pool", createApiConfig(
                "xuangubao_super_stock_pool", "GET", "/api/xuangubao/stock/pool/super_stock",
                "选股宝强势股池", "xuangubao", "stock_pool", 30000, true
        ));
    }

    private ApiConfig createApiConfig(String name, String method, String path, String description,
                                      String provider, String category, int timeout, boolean enabled) {
        ApiConfig config = new ApiConfig();
        config.setName(name);
        config.setMethod(method);
        config.setPath(path);
        config.setDescription(description);
        config.setProvider(provider);
        config.setCategory(category);
        config.setTimeout(timeout);
        config.setEnabled(enabled);
        return config;
    }

    private void loadProviderBaseUrls() {
        providerBaseUrls.put("eastmoney", "http://localhost:8000");
        providerBaseUrls.put("xuangubao", "http://localhost:8000");
    }

    /**
     * 获取API配置
     */
    public ApiConfig getApiConfig(String apiName) {
        return apiConfigs.get(apiName);
    }

    /**
     * 获取完整的API URL
     */
    public String getFullUrl(String apiName) {
        ApiConfig config = getApiConfig(apiName);
        if (config == null) {
            throw new IllegalArgumentException("找不到API配置: " + apiName);
        }
        String baseUrl = providerBaseUrls.get(config.getProvider());
        // 如果路径已经包含provider前缀，则不需要重复添加
        String fullPath = config.getPath();
        if (!fullPath.startsWith("/")) {
            fullPath = "/" + fullPath;
        }
        return baseUrl + fullPath;
    }

    /**
     * 刷新配置
     */
    public void refreshConfigs() {
        apiConfigs.clear();
        loadApiConfigsFromPython();
    }
}


