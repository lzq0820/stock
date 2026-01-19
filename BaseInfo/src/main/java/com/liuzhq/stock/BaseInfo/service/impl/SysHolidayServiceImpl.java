package com.liuzhq.stock.BaseInfo.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liuzhq.common.enums.CacheKey;
import com.liuzhq.common.utils.CacheUtils;
import com.liuzhq.common.utils.CollectionUtils;
import com.liuzhq.stock.BaseInfo.entity.SysHoliday;
import com.liuzhq.stock.BaseInfo.mapper.SysHolidayMapper;
import com.liuzhq.stock.BaseInfo.service.SysHolidayService;
import com.liuzhq.stock.BaseInfo.task.HolidayDataSyncTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.*;
import java.security.cert.X509Certificate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SysHolidayServiceImpl extends ServiceImpl<SysHolidayMapper, SysHoliday> implements SysHolidayService {

    private static final String HOLIDAY_API_URL = "https://timor.tech/api/holiday/year/";
    private static final RestTemplate REST_TEMPLATE = createSpring528CompatibleRestTemplate();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private HolidayDataSyncTask holidayDataSyncTask;

    /**
     * 创建适配Spring 5.2.8.RELEASE的RestTemplate
     */
    private static RestTemplate createSpring528CompatibleRestTemplate() {
        // 禁用SSL证书验证
        disableGlobalSslVerification();

        // 创建RequestFactory，设置超时时间
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
                HttpURLConnection connection = super.openConnection(url, proxy);
                connection.setConnectTimeout(30000); // 30秒连接超时
                connection.setReadTimeout(60000);    // 60秒读取超时
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                return connection;
            }
        };

        // 配置DNS缓存
        configureDnsCache();

        return new RestTemplate(factory);
    }

    /**
     * 全局禁用SSL证书验证
     */
    private static void disableGlobalSslVerification() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new java.security.SecureRandom());

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            log.info("全局SSL证书验证已禁用（生产环境建议配置合法证书）");
        } catch (Exception e) {
            log.warn("禁用SSL验证失败：{}", e.getMessage());
        }
    }

    /**
     * 配置DNS缓存
     */
    private static void configureDnsCache() {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "10");

        try {
            InetAddress[] addresses = InetAddress.getAllByName("timor.tech");
            log.info("DNS解析timor.tech结果：");
            for (InetAddress addr : addresses) {
                log.info("  - {}", addr.getHostAddress());
            }
        } catch (UnknownHostException e) {
            log.error("DNS解析timor.tech失败：{}", e.getMessage());
        }
    }

    /**
     * 同步指定年份的节假日数据
     * 核心逻辑：仅当API返回有效节假日数据时，才生成周六/周日数据；无节假日则直接结束
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncHolidayData(Integer year) {
        log.info("开始同步{}年节假日数据", year);
        String apiUrl = HOLIDAY_API_URL + year;
        String response;

        // 1. 调用API（仅调用一次，无重试）
        try {
            testConnection(apiUrl);
            response = REST_TEMPLATE.getForObject(apiUrl, String.class);
        } catch (Exception e) {
            log.error("调用API失败，无法同步{}年节假日数据", year, e);
            return false;
        }

        // 2. 校验API返回结果
        if (response == null || response.isEmpty()) {
            log.error("获取{}年节假日数据失败：API返回空，不处理周六/周日", year);
            return false;
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(response);
            // 3. 校验API返回码
            if (rootNode.get("code").asInt() != 0) {
                String errorMsg = rootNode.has("msg") ? rootNode.get("msg").asText("未知错误") : "未知错误";
                log.error("获取{}年节假日数据失败：{}，不处理周六/周日", year, errorMsg);
                return false;
            }

            // 4. 解析API返回的节假日数据
            Map<String, SysHoliday> holidayMap = new HashMap<>();
            JsonNode holidaysNode = rootNode.get("holiday");
            if (holidaysNode != null && holidaysNode.isObject()) {
                holidaysNode.fields().forEachRemaining(entry -> {
                    try {
                        JsonNode holidayNode = entry.getValue();
                        String fullDateStr = holidayNode.get("date").asText("");
                        if (fullDateStr.isEmpty()) {
                            log.warn("日期{}的date字段为空，跳过", entry.getKey());
                            return;
                        }

                        LocalDate holidayDate = LocalDate.parse(fullDateStr, FULL_DATE_FORMATTER);
                        if (holidayDate.getYear() != year) {
                            return;
                        }

                        SysHoliday holiday = new SysHoliday();
                        holiday.setHolidayDate(holidayDate);
                        holiday.setHolidayName(holidayNode.get("name").asText(""));
                        holiday.setIsHoliday(holidayNode.get("holiday").asBoolean() ? 1 : 0);
                        holiday.setIsMakeupWork("补班".equals(holidayNode.has("type") ? holidayNode.get("type").asText("") : "") ? 1 : 0);
                        holiday.setYear(year);
                        holiday.setMonth(holidayDate.getMonthValue());

                        // 存入Map，日期字符串作为key
                        holidayMap.put(fullDateStr, holiday);

                    } catch (Exception e) {
                        log.error("解析节假日数据失败：{}", entry.getKey(), e);
                    }
                });
            }

            // 8. 批量插入/更新
            log.info("{} 年有 {} 天节假日", year, holidayMap.size());

            if (!holidayMap.isEmpty()) {
                Set<DayOfWeek> weekendDays = new HashSet<>();
                weekendDays.add(DayOfWeek.SATURDAY);
                weekendDays.add(DayOfWeek.SUNDAY);
                LocalDate currentDate = LocalDate.of(year, 1, 1);
                LocalDate endOfYear = LocalDate.of(year + 1, 1, 1);
                while (currentDate.isBefore(endOfYear)) {
                    DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
                    String format = currentDate.format(FULL_DATE_FORMATTER);
                    if (!weekendDays.contains(dayOfWeek) || holidayMap.containsKey(format)) {
                        currentDate = currentDate.plusDays(1);
                        continue;
                    }
                    SysHoliday sysHoliday = new SysHoliday();
                    sysHoliday.setHolidayDate(currentDate);
                    sysHoliday.setHolidayName(dayOfWeek == DayOfWeek.SATURDAY ? "周六" : "周日");
                    sysHoliday.setIsHoliday(1);
                    sysHoliday.setIsMakeupWork(0);
                    sysHoliday.setYear(year);
                    sysHoliday.setMonth(currentDate.getMonthValue());
                    sysHoliday.setUpdateTime(LocalDateTime.now());
                    sysHoliday.setCreateTime(LocalDateTime.now());
                    holidayMap.put(format, sysHoliday);

                    currentDate = currentDate.plusDays(1);
                }

                baseMapper.batchInsertOrUpdate(new ArrayList<>(holidayMap.values()));
                log.info("同步{}年节假日数据完成，共处理{}条记录（节假日+周末）", year, holidayMap.size());
                return true;
            } else {
                log.warn("{}年节假日数据解析结果为空，无数据可同步", year);
                return false;
            }
        } catch (Exception e) {
            log.error("同步{}年节假日数据异常", year, e);
            return false;
        }
    }

    /**
     * 解析API返回的节假日数据，存入Map（key：yyyy-MM-dd）
     */
    private Map<String, SysHoliday> parseApiHolidayData(JsonNode rootNode, Integer year) {
        Map<String, SysHoliday> holidayMap = new HashMap<>();

        // 解析主节假日数据
        JsonNode holidaysNode = rootNode.get("holiday");
        if (holidaysNode != null && holidaysNode.isObject()) {
            holidaysNode.fields().forEachRemaining(entry -> {
                try {
                    JsonNode holidayNode = entry.getValue();
                    String fullDateStr = holidayNode.get("date").asText("");
                    if (fullDateStr.isEmpty()) {
                        log.warn("日期{}的date字段为空，跳过", entry.getKey());
                        return;
                    }

                    LocalDate holidayDate = LocalDate.parse(fullDateStr, FULL_DATE_FORMATTER);
                    if (holidayDate.getYear() != year) {
                        return;
                    }

                    SysHoliday holiday = new SysHoliday();
                    holiday.setHolidayDate(holidayDate);
                    holiday.setHolidayName(holidayNode.get("name").asText(""));
                    holiday.setIsHoliday(holidayNode.get("holiday").asBoolean() ? 1 : 0);
                    holiday.setIsMakeupWork("补班".equals(holidayNode.has("type") ? holidayNode.get("type").asText("") : "") ? 1 : 0);
                    holiday.setYear(year);
                    holiday.setMonth(holidayDate.getMonthValue());

                    // 存入Map，日期字符串作为key
                    holidayMap.put(fullDateStr, holiday);
                    log.debug("解析到{}年节假日：{} {}", fullDateStr, holiday.getHolidayName());

                } catch (Exception e) {
                    log.error("解析节假日数据失败：{}", entry.getKey(), e);
                }
            });
        }

        // 解析调休补班数据
        JsonNode typeNode = rootNode.get("type");
        if (typeNode != null && typeNode.isObject()) {
            typeNode.fields().forEachRemaining(entry -> {
                try {
                    String dateStr = entry.getKey();
                    JsonNode typeInfo = entry.getValue();

                    if ("补班".equals(typeInfo.get("name").asText(""))) {
                        String fullDateStr = dateStr.contains("-") ? dateStr : year + "-" + dateStr;
                        LocalDate holidayDate = LocalDate.parse(fullDateStr, FULL_DATE_FORMATTER);
                        if (holidayDate.getYear() != year) {
                            return;
                        }

                        SysHoliday makeupWork = new SysHoliday();
                        makeupWork.setHolidayDate(holidayDate);
                        makeupWork.setHolidayName("调休补班");
                        makeupWork.setIsHoliday(0);
                        makeupWork.setIsMakeupWork(1);
                        makeupWork.setYear(year);
                        makeupWork.setMonth(holidayDate.getMonthValue());

                        // 补班数据覆盖原有记录（若存在）
                        holidayMap.put(fullDateStr, makeupWork);
                        log.debug("解析到{}年补班日：{}", year, fullDateStr);
                    }
                } catch (Exception e) {
                    log.error("解析调休补班数据失败：{}", entry.getKey(), e);
                }
            });
        }

        return holidayMap;
    }

    /**
     * 批量生成全年周六/周日（仅当有节假日数据时调用）
     * 逻辑：找到当年第一个周六，之后每次+7天（周六），周日=周六+1天
     */
    private void generateWeekendData(Map<String, SysHoliday> holidayMap, Integer year) {
        log.info("开始生成{}年周六/周日数据（因API返回有效节假日数据）, 节假日数量：{}", year, holidayMap.size());
        if (holidayMap.isEmpty()) {
            return;
        }

        LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        // 步骤1：先处理1月1日到第一个周六之间的周末（避免漏算年初的周日/周六）
        LocalDate firstSaturday = firstDayOfYear.with(TemporalAdjusters.firstInMonth(DayOfWeek.SATURDAY));
        LocalDate currentDate = firstDayOfYear;
        // 遍历1月1日到第一个周六前一天，补全这段时间的周末
        while (currentDate.isBefore(firstSaturday)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                String weekendName = dayOfWeek == DayOfWeek.SATURDAY ? "周六" : "周日";
                processWeekendDay(holidayMap, currentDate, year, weekendName);
            }
            currentDate = currentDate.plusDays(1);
        }

        // 步骤2：从第一个周六开始，按周递增遍历（每次+7天），确保后续周末无遗漏
        LocalDate currentSaturday = firstSaturday;
        while (!currentSaturday.isAfter(endOfYear)) {
            LocalDate currentSunday = currentSaturday.plusDays(1); // 周六+1天=周日

            // 处理周六
            processWeekendDay(holidayMap, currentSaturday, year, "周六");
            // 处理周日（若未超过当年）
            if (!currentSunday.isAfter(endOfYear)) {
                processWeekendDay(holidayMap, currentSunday, year, "周日");
            }

            // 下一个周六（+7天）
            currentSaturday = currentSaturday.plusWeeks(1);
        }

        log.info("{}年周六/周日数据生成完成，共{}条（含节假日覆盖）", year, holidayMap.size());
    }

    /**
     * 处理单个周末日期：若未被节假日覆盖则添加，已覆盖则保留节假日名称
     */
    private void processWeekendDay(Map<String, SysHoliday> holidayMap, LocalDate date, Integer year, String weekendName) {
        String dateStr = FULL_DATE_FORMATTER.format(date);

        // 若该日期已存在节假日记录（API返回），则跳过（保留节假日名称）
        if (holidayMap.containsKey(dateStr)) {
            log.debug("{} 是{}且属于节假日，保留节假日名称：{}", dateStr, weekendName, holidayMap.get(dateStr).getHolidayName());
            return;
        }

        // 新增周末记录
        SysHoliday weekend = new SysHoliday();
        weekend.setHolidayDate(date);
        weekend.setYear(year);
        weekend.setMonth(date.getMonthValue());
        weekend.setIsHoliday(0); // 周末不是法定节假日
        weekend.setIsMakeupWork(0); // 周末不是补班日
        weekend.setHolidayName(weekendName);

        holidayMap.put(dateStr, weekend);
        log.debug("新增{}年{}：{}", year, weekendName, dateStr);
    }

    /**
     * 测试API连接性
     */
    private void testConnection(String apiUrl) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("timor.tech", 443), 10000);
            socket.close();
            log.info("成功连接到 timor.tech:443");
        } catch (Exception e) {
            log.error("连接到timor.tech:443失败：{}", e.getMessage());
        }
    }

    @Override
    public SysHoliday getHolidayByDate(LocalDate date) {
        try {
            return baseMapper.selectByHolidayDate(date);
        } catch (Exception e) {
            log.error("查询{}的节假日信息失败", date, e);
            return null;
        }
    }

    @Override
    public boolean isTradeDay(LocalDate date) {
        if (date == null) {
            log.warn("判断交易日失败：日期为空");
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        SysHoliday holiday = getHolidayByDate(date);

        if (holiday != null) {
            if (holiday.getIsHoliday() == 1) {
                log.debug("{} 是法定节假日，非交易日", date);
                return false;
            }
            if (holiday.getIsMakeupWork() == 1) {
                log.debug("{} 是调休补班日，属于交易日", date);
                return true;
            }
        }

        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        boolean isTradeDay = !isWeekend;

        log.debug("{} 星期{}，{}交易日", date, dayOfWeek.getValue(), isTradeDay ? "是" : "非");
        return isTradeDay;
    }

    /**
     * 获取有效交易日
     */
    public LocalDate getValidTradeDate(LocalDate targetDate) {
        int year = targetDate.getYear();
        List<String> currentHolidayList = query(year, null).stream()
                .map(item -> FULL_DATE_FORMATTER.format(item.getHolidayDate()))
                .collect(Collectors.toList());
        List<String> preYearHolidayList = query(year - 1, null).stream()
                .map(item -> FULL_DATE_FORMATTER.format(item.getHolidayDate()))
                .collect(Collectors.toList());

        LocalDate currentDate = targetDate;
        int maxTryCount = 60;
        int tryCount = 0;

        while (tryCount < maxTryCount) {
            String currentDateStr = FULL_DATE_FORMATTER.format(currentDate);
            if (!currentHolidayList.contains(currentDateStr) && !preYearHolidayList.contains(currentDateStr)) {
                return currentDate;
            }
            currentDate = currentDate.minusDays(1);
            tryCount++;
        }

        log.warn("查找最近交易日失败（已尝试{}天），返回日期：{}", maxTryCount, currentDate);
        return currentDate;
    }

    @Override
    public List<SysHoliday> query(Integer year, Integer month) {
        List<SysHoliday> holidayList;
        String holidayListJson = CacheUtils.get(CacheKey.HOLIDAY.getKey() + year);

        if (StringUtils.isBlank(holidayListJson)) {
            holidayList = getBaseMapper().selectList(new LambdaQueryWrapper<SysHoliday>().eq(SysHoliday::getYear, year));
            if (CollectionUtils.isEmpty(holidayList)) {
                holidayDataSyncTask.manualSyncHolidayData(year);
                holidayList = getBaseMapper().selectList(new LambdaQueryWrapper<SysHoliday>()
                        .eq(year != null, SysHoliday::getYear, year)
                        .eq(month != null, SysHoliday::getMonth, month)
                );
            }
            CacheUtils.put(CacheKey.HOLIDAY.getKey() + year, JSON.toJSONString(holidayList));
        } else {
            holidayList = JSON.parseArray(holidayListJson, SysHoliday.class);
        }

        if (CollectionUtils.isEmpty(holidayList)) {
            holidayDataSyncTask.manualSyncHolidayData(year);
        }

        return holidayList.stream()
                .filter(item -> year != null && Objects.equals(item.getYear(), year) && (month == null || Objects.equals(item.getMonth(), month)))
                .collect(Collectors.toList());
    }
}