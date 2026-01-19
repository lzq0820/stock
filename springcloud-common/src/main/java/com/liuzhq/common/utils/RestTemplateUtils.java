package com.liuzhq.common.utils;

import com.google.common.collect.Maps;
import com.liuzhq.common.response.ResultModel;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * RestTemplate 通用请求工具类（静态方法版）
 * 封装GET/POST/PUT/DELETE等请求，统一异常处理和参数配置
 * 从Spring容器中获取RestTemplate实例，无需注入，直接调用静态方法
 */
@Slf4j
public class RestTemplateUtils {

    private static final String REQUEST_START_TIME = "request_start_time";

    // 添加监控方法
    private static void recordRequestTime(String method, String url, long duration, int statusCode) {
        log.info("=== HTTP {} 请求监控 === URL: {}, 耗时: {}ms, 状态码: {}",
                method, url, duration, statusCode);
    }

    // ==================== 基础配置 ====================
    /**
     * 从Spring容器获取RestTemplate实例（核心改造）
     */
    private static RestTemplate getRestTemplate() {
        try {
            // 使用通用的SpringContextHolder获取RestTemplate
            return SpringUtils.getApplicationContext().getBean(RestTemplate.class);
        } catch (Exception e) {
            log.error("获取Spring容器中的RestTemplate失败，手动创建默认实例", e);
            return new RestTemplate();
        }
    }

    /**
     * 默认JSON请求头（静态方法）
     */
    private static HttpHeaders getDefaultJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON)); // 替换assertj的Lists，使用JDK原生List
        return headers;
    }

    // ==================== GET请求（带URL参数） ====================
    public static <T> T get(String url, Class<T> responseType) {
        return get(url, Maps.newHashMap(), getDefaultJsonHeaders(), responseType);
    }

    /**
     * GET请求：带URL参数，返回指定类型的对象（静态方法）
     * @param url 基础URL（不含参数）
     * @param params URL参数（key-value）
     * @param responseType 返回值类型（如String.class、BaseInfo.class）
     * @return 泛型结果
     */
    public static <T> T get(String url, Map<String, Object> params, Class<T> responseType) {
        return get(url, params, getDefaultJsonHeaders(), responseType);
    }

    /**
     * GET请求：自定义请求头 + URL参数（静态方法）
     */
    public static <T> T get(String url, Map<String, Object> params, HttpHeaders headers, Class<T> responseType) {
        // 拼接URL参数
        String requestUrl = buildUrlWithParams(url, params);
        long startTime = System.currentTimeMillis(); // 记录开始时间
        log.info("=== RestTemplate GET请求 === URL: {}", requestUrl);

        try {
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<T> response = getRestTemplate().exchange(
                    requestUrl,
                    HttpMethod.GET,
                    requestEntity,
                    responseType
            );

            // 计算耗时
            long duration = System.currentTimeMillis() - startTime;

            // 校验响应状态
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("=== GET请求成功，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("GET", requestUrl, duration, response.getStatusCodeValue());
                return response.getBody();
            } else {
                log.error("=== GET请求失败，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("GET", requestUrl, duration, response.getStatusCodeValue());
                throw new RuntimeException(String.format("GET请求失败，状态码：%d", response.getStatusCodeValue()));
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("=== GET请求异常，状态码: {}，响应体: {}，耗时: {}ms", e.getRawStatusCode(), e.getResponseBodyAsString(), duration);
            recordRequestTime("GET", requestUrl, duration, e.getRawStatusCode());
            throw new RuntimeException(String.format("GET请求异常：%s，详情：%s", e.getMessage(), e.getResponseBodyAsString()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("=== GET请求未知异常，耗时: {}ms ===", duration, e);
            recordRequestTime("GET", requestUrl, duration, 500);
            throw new RuntimeException("GET请求失败：" + e.getMessage());
        }
    }


    // ==================== POST请求（JSON请求体） ====================
    /**
     * POST请求：JSON请求体，返回指定类型的对象（静态方法）
     * @param url 请求URL
     * @param requestBody JSON请求体（POJO/Map）
     * @param responseType 返回值类型
     * @return 泛型结果
     */
    public static <T> T post(String url, Object requestBody, Class<T> responseType) {
        return post(url, requestBody, getDefaultJsonHeaders(), responseType);
    }

    /**
     * POST请求：自定义请求头 + JSON请求体（静态方法）
     */
    public static <T> T post(String url, Object requestBody, HttpHeaders headers, Class<T> responseType) {
        long startTime = System.currentTimeMillis(); // 记录开始时间
        log.info("=== RestTemplate POST请求 === URL: {}, 请求体: {}", url, JsonUtils.toJson(requestBody));

        try {
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<T> response = getRestTemplate().exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    responseType
            );

            long duration = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("=== POST请求成功，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("POST", url, duration, response.getStatusCodeValue());
                return response.getBody();
            } else {
                log.error("=== POST请求失败，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("POST", url, duration, response.getStatusCodeValue());
                throw new RuntimeException(String.format("POST请求失败，状态码：%d", response.getStatusCodeValue()));
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("=== POST请求异常，状态码: {}，响应体: {}，耗时: {}ms", e.getRawStatusCode(), e.getResponseBodyAsString(), duration);
            recordRequestTime("POST", url, duration, e.getRawStatusCode());
            throw new RuntimeException(String.format("POST请求异常：%s，详情：%s", e.getMessage(), e.getResponseBodyAsString()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("=== POST请求未知异常，耗时: {}ms ===", duration, e);
            recordRequestTime("POST", url, duration, 500);
            throw new RuntimeException("POST请求失败：" + e.getMessage());
        }
    }


    /**
     * 重载POST方法：支持自定义请求头（包含apiKey）
     */
    public static <T> T postWithApiKey(String url, Object requestBody, String apiKey, Class<T> responseType) {
        HttpHeaders headers = getDefaultJsonHeaders();
        headers.set("apiKey", apiKey); // 添加接口要求的apiKey请求头
        return post(url, requestBody, headers, responseType);
    }

    // ==================== PUT/DELETE请求（扩展） ====================
    /**
     * PUT请求：JSON请求体（静态方法）
     */
    public static <T> T put(String url, Object requestBody, Class<T> responseType) {
        return put(url, requestBody, getDefaultJsonHeaders(), responseType);
    }

    public static <T> T put(String url, Object requestBody, HttpHeaders headers, Class<T> responseType) {
        long startTime = System.currentTimeMillis(); // 记录开始时间
        log.info("=== RestTemplate PUT请求 === URL: {}, 请求体: {}", url, JsonUtils.toJson(requestBody));

        try {
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<T> response = getRestTemplate().exchange(
                    url,
                    HttpMethod.PUT,
                    requestEntity,
                    responseType
            );

            long duration = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("=== PUT请求成功，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("PUT", url, duration, response.getStatusCodeValue());
                return response.getBody();
            } else {
                log.error("=== PUT请求失败，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("PUT", url, duration, response.getStatusCodeValue());
                throw new RuntimeException(String.format("PUT请求失败，状态码：%d", response.getStatusCodeValue()));
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("=== PUT请求未知异常，耗时: {}ms ===", duration, e);
            recordRequestTime("PUT", url, duration, 500);
            throw new RuntimeException("PUT请求失败：" + e.getMessage());
        }
    }


    /**
     * DELETE请求：带URL参数（静态方法）
     */
    public static <T> T delete(String url, Map<String, Object> params, Class<T> responseType) {
        String requestUrl = buildUrlWithParams(url, params);
        long startTime = System.currentTimeMillis(); // 记录开始时间
        log.info("=== RestTemplate DELETE请求 === URL: {}", requestUrl);

        try {
            ResponseEntity<T> response = getRestTemplate().exchange(
                    requestUrl,
                    HttpMethod.DELETE,
                    new HttpEntity<>(getDefaultJsonHeaders()),
                    responseType
            );

            long duration = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("=== DELETE请求成功，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("DELETE", requestUrl, duration, response.getStatusCodeValue());
                return response.getBody();
            } else {
                log.error("=== DELETE请求失败，响应状态码: {}，耗时: {}ms", response.getStatusCodeValue(), duration);
                recordRequestTime("DELETE", requestUrl, duration, response.getStatusCodeValue());
                throw new RuntimeException(String.format("DELETE请求失败，状态码：%d", response.getStatusCodeValue()));
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("=== DELETE请求未知异常，耗时: {}ms ===", duration, e);
            recordRequestTime("DELETE", url, duration, 500);
            throw new RuntimeException("DELETE请求失败：" + e.getMessage());
        }
    }


    // ==================== 工具方法（私有静态） ====================
    /**
     * 拼接URL和参数（GET/DELETE请求用，私有静态方法）
     */
    private static String buildUrlWithParams(String url, Map<String, Object> params) {
        if (CollectionUtils.isEmpty(params)) {
            return url;
        }

        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromHttpUrl(url);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (StringUtils.hasText(key) && value != null) {
                urlBuilder.queryParam(key, value);
            }
        }
        return urlBuilder.build().toUriString();
    }

    // ==================== 适配你的项目：返回ResultModel的重载方法（静态） ====================
    /**
     * GET请求：返回统一ResultModel格式（静态方法）
     */
    public static <T> ResultModel<T> getForResult(String url, Map<String, Object> params, Class<T> responseType) {
        try {
            T data = get(url, params, responseType);
            return ResultModel.success(data);
        } catch (Exception e) {
            log.error("GET请求失败", e);
            return ResultModel.error(e.getMessage());
        }
    }

    /**
     * POST请求：返回统一ResultModel格式（静态方法）
     */
    public static <T> ResultModel<T> postForResult(String url, Object requestBody, Class<T> responseType) {
        try {
            T data = post(url, requestBody, responseType);
            return ResultModel.success(data);
        } catch (Exception e) {
            log.error("POST请求失败", e);
            return ResultModel.error(e.getMessage());
        }
    }

    /**
     * POST请求：带apiKey请求头，返回统一ResultModel格式
     */
    public static <T> ResultModel<T> postWithApiKeyForResult(String url, Object requestBody, String apiKey, Class<T> responseType) {
        try {
            T data = postWithApiKey(url, requestBody, apiKey, responseType);
            return ResultModel.success(data);
        } catch (Exception e) {
            log.error("POST请求（带apiKey）失败", e);
            return ResultModel.error(e.getMessage());
        }
    }

}