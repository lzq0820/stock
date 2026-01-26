package com.liuzhq.stock.BaseInfo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liuzhq.common.utils.CollectionUtils;
import com.liuzhq.common.utils.JsonUtils;
import com.liuzhq.common.utils.StockNameStyleUtil;
import com.liuzhq.stock.BaseInfo.client.PythonApiClient;
import com.liuzhq.stock.BaseInfo.dto.StockPoolDto;
import com.liuzhq.stock.BaseInfo.dto.innerClass.RelatedPlate;
import com.liuzhq.stock.BaseInfo.entity.StockPool;
import com.liuzhq.stock.BaseInfo.mapper.StockPoolMapper;
import com.liuzhq.stock.BaseInfo.service.StockPoolService;
import com.liuzhq.stock.BaseInfo.service.SysHolidayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 股票池服务（适配Python FastAPI接口）
 * 优化：从数据库查询节假日，不再实时调用API
 * 新增：支持同步指定日期的股票池数据
 */
@Service
@Slf4j
public class StockPoolServiceImpl extends ServiceImpl<StockPoolMapper, StockPool> implements StockPoolService {

    @Resource
    private PythonApiClient pythonApiClient;
    @Resource
    private SysHolidayService sysHolidayService; // 注入节假日服务

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步指定股票池数据到数据库（默认同步当日数据）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncStockPoolData(String poolKey) throws UnsupportedEncodingException {
        // 调用重载方法，默认同步当日数据
        return syncStockPoolData(poolKey, LocalDate.now());
    }

    /**
     * 新增：同步指定日期、指定股票池类型的数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncStockPoolData(String poolKey, LocalDate tradeDate) throws UnsupportedEncodingException {
        // 先校验并获取有效交易日
        LocalDate validTradeDate = sysHolidayService.getValidTradeDate(tradeDate);

        // 调用Python API获取指定日期的股票池数据
        List<Map<String, Object>> pythonData = pythonApiClient.getStockPoolData(poolKey, validTradeDate);

        if (pythonData.isEmpty()) {
            log.info("Python API返回{}股票池{}日期无数据", poolKey, validTradeDate.format(DATE_FORMATTER));
            return true;
        }

        List<StockPool> stockPoolList = convertToStockPoolList(pythonData, poolKey);
        int insertCount = getBaseMapper().batchInsertIgnore(stockPoolList);
        log.info("同步{}股票池{}日期数据完成，插入{}条记录", poolKey, validTradeDate.format(DATE_FORMATTER), insertCount);
        return insertCount > 0;
    }

    /**
     * 查询指定日期和股票池类型的数据
     * 核心优化：从数据库查询交易日，性能大幅提升
     * 新增：查询不到数据时自动同步指定日期数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<StockPoolDto> queryByDateAndPoolType(LocalDate tradeDate,
                                                     String poolType,
                                                     Integer notShowSt) throws UnsupportedEncodingException {
        // 1. 校验并获取有效交易日
        Result result = getValidateTradeDateResult(tradeDate);
        log.info("查询 {} 股票池，有效日期：{}", DATE_FORMATTER.format(tradeDate), result.format);
        List<StockPoolDto> stockPoolList = getBaseMapper().selectByTradeDateAndPoolType(result.format, poolType, notShowSt);

        // 4. 新增逻辑：查询不到数据时，自动同步指定日期的股票池数据
        if (CollectionUtils.isEmpty(stockPoolList)) {
            if ("lbjj".equals(poolType)) {
                return stockPoolList;
            }
            log.info("{}股票池{}日期本地无数据，开始从Python API同步", poolType, result.format);
            // 同步指定日期、指定类型的股票池数据
            boolean syncSuccess = syncStockPoolData(poolType, result.validTradeDate);
            if (syncSuccess) {
                // 同步成功后重新查询
                stockPoolList = getBaseMapper().selectByTradeDateAndPoolType(result.format, poolType, notShowSt);
                log.info("同步后重新查询到{}条{}股票池 {} 日期数据", stockPoolList.size(), poolType, result.format);
            } else {
                log.warn("{}股票池 {} 日期数据同步失败", poolType, result.format);
            }
        }
        if ("dt".equals(poolType)) {
            stockPoolList = stockPoolList.stream()
                    .sorted(Comparator.comparing(StockPool::getChangePercent)
                            .thenComparing(StockPool::getPrice)
                            .thenComparing(StockPool::getStockCode))
                    .collect(Collectors.toList());
        } else {
            stockPoolList = stockPoolList.stream()
                    .sorted(Comparator.comparing(StockPool::getChangePercent).reversed()
                            .thenComparing(StockPool::getPrice)
                            .thenComparing(StockPool::getStockCode))
                    .collect(Collectors.toList());
        }

        Map<Integer, List<StockPoolDto>> limitDaysGroupMap = stockPoolList.stream().map(stock -> {
            stock.buildStockNameAndReason();
            StockPoolDto child = new StockPoolDto();
            BeanUtils.copyProperties(stock, child);
            return child;
        }).collect(Collectors.groupingBy(StockPoolDto::getLimitDays));

        List<StockPoolDto> rootList = new ArrayList<>();
        limitDaysGroupMap.forEach((limitDays, childrenList) -> {
            StockPoolDto stockPoolDto = new StockPoolDto();
            stockPoolDto.setTitle(limitDays + " 连板");
            stockPoolDto.setChildren(childrenList.stream().sorted((o1, o2) -> {
                if ("dt".equals(poolType)) {
                    return o2.getChangePercent().compareTo(o1.getChangePercent());
                }
                return o1.getChangePercent().compareTo(o2.getChangePercent());
            }).collect(Collectors.toList()));
            stockPoolDto.setLimitDays(limitDays);

            rootList.add(stockPoolDto);
        });

        return rootList.stream().sorted(Comparator.comparing(StockPoolDto::getLimitDays).reversed()).collect(Collectors.toList());
    }

    private Result getValidateTradeDateResult(LocalDate tradeDate) {
        LocalDate validTradeDate = sysHolidayService.getValidTradeDate(tradeDate);

        // 2. 如果日期有调整，打印日志提示
        if (!validTradeDate.equals(tradeDate)) {
            log.info("查询日期{}为非交易日，自动调整为最近交易日{}",
                    tradeDate.format(DATE_FORMATTER),
                    validTradeDate.format(DATE_FORMATTER));
        }

        // 3. 查询有效交易日的数据
        String format = DATE_FORMATTER.format(validTradeDate);
        return new Result(validTradeDate, format);
    }

    private static class Result {
        public final LocalDate validTradeDate;
        public final String format;

        public Result(LocalDate validTradeDate, String format) {
            this.validTradeDate = validTradeDate;
            this.format = format;
        }
    }

    /**
     * 同步所有股票池数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncAllStockPoolData() {
        String[] poolKeys = {"zt", "dt", "yesterday_zt", "broken_zt", "super_stock"};
        boolean allSuccess = true;
        for (String poolKey : poolKeys) {
            try {
                syncStockPoolData(poolKey);
            } catch (Exception e) {
                log.error("同步{}股票池数据失败", poolKey, e);
                allSuccess = false;
            }
        }
        return allSuccess;
    }

    // ========== 以下原有方法保持不变 ==========
    private List<StockPool> convertToStockPoolList(List<Map<String, Object>> pythonData, String poolKey) {
        // 原有逻辑不变，此处省略（保持你原来的实现）
        List<StockPool> result = new ArrayList<>();
        for (Map<String, Object> data : pythonData) {
            StockPool stockPool = new StockPool();

            String tradeDateStr = (String) data.get("trade_date");
            stockPool.setTradeDate(tradeDateStr != null ? LocalDate.parse(tradeDateStr, DATE_FORMATTER) : LocalDate.now());

            stockPool.setStockCode((String) data.get("stock_code"));
            stockPool.setStockName((String) data.get("stock_name"));
            stockPool.setStockType(convertToInteger(data.get("stock_type")));
            stockPool.setPrice(convertToBigDecimal(data.get("price")));
            stockPool.setChangePercent(convertToBigDecimal(data.get("change_percent")));
            stockPool.setTurnoverRatio(convertToBigDecimal(data.get("turnover_ratio")));
            stockPool.setCirculationMarketCap(convertToBigDecimal(data.get("circulation_market_cap")));
            stockPool.setTotalMarketCap(convertToBigDecimal(data.get("total_market_cap")));
            stockPool.setIssuePrice(convertToBigDecimal(data.get("issue_price")));
            stockPool.setListedDate(convertToLocalDateTime(data.get("listed_date")));
            stockPool.setPoolType(poolKey);
            stockPool.setBuyLockRatio(convertToBigDecimal(data.get("buy_lock_ratio")));
            stockPool.setSellLockRatio(convertToBigDecimal(data.get("sell_lock_ratio")));
            stockPool.setCurrentLockAmount(convertToBigDecimal(data.get("current_lock_amount")));
            stockPool.setMaxLockAmount(convertToBigDecimal(data.get("max_lock_amount")));
            stockPool.setStockReason((String) data.get("stock_reason"));
            stockPool.setRelatedPlates((String) data.get("related_plates"));
            stockPool.setCreateTime(LocalDateTime.now());
            stockPool.setUpdateTime(LocalDateTime.now());

            switch (poolKey) {
                case "zt":
                    stockPool.setLimitDays(convertToInteger(data.get("limit_up_days")));
                    stockPool.setBreakLimitTimes(convertToInteger(data.get("break_limit_up_times")));
                    stockPool.setFirstLimitUpTime(convertToLocalDateTime(data.get("first_limit_up_time")));
                    stockPool.setLastLimitUpTime(convertToLocalDateTime(data.get("last_limit_up_time")));
                    break;
                case "dt":
                    stockPool.setLimitDays(convertToInteger(data.get("limit_down_days")));
                    stockPool.setBreakLimitTimes(convertToInteger(data.get("break_limit_down_times")));
                    stockPool.setFirstLimitDownTime(convertToLocalDateTime(data.get("first_limit_down_time")));
                    stockPool.setLastLimitDownTime(convertToLocalDateTime(data.get("last_limit_down_time")));
                    break;
                case "yesterday_zt":
                    stockPool.setLimitDays(convertToInteger(data.get("yesterday_limit_up_days")));
                    stockPool.setYesterdayBreakLimitUpTimes(convertToInteger(data.get("yesterday_break_limit_up_times")));
                    stockPool.setYesterdayFirstLimitUpTime(convertToLocalDateTime(data.get("yesterday_first_limit_up_time")));
                    stockPool.setYesterdayLastLimitUpTime(convertToLocalDateTime(data.get("yesterday_last_limit_up_time")));
                    break;
                case "broken_zt":
                    stockPool.setLimitDays(1);
                    stockPool.setBreakLimitTimes(convertToInteger(data.get("break_limit_up_times")));
                    stockPool.setFirstLimitUpTime(convertToLocalDateTime(data.get("first_limit_up_time")));
                    stockPool.setLastBreakLimitUpTime(convertToLocalDateTime(data.get("last_break_limit_up_time")));
                    break;
                case "super_stock":
                    stockPool.setLimitDays(convertToInteger(data.get("limit_up_days")));
                    stockPool.setMDaysNBoards((String) data.get("m_days_n_boards"));
                    stockPool.setFirstLimitUpTime(convertToLocalDateTime(data.get("first_limit_up_time")));
                    stockPool.setLastLimitUpTime(convertToLocalDateTime(data.get("last_limit_up_time")));
                    break;
            }

            result.add(stockPool);
        }
        return result;
    }

    private Integer convertToInteger(Object value) {
        if (value == null || "".equals(value)) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("转换Integer失败：{}", value, e);
                return 0;
            }
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        return 0;
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null || "".equals(value)) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value).setScale(2, BigDecimal.ROUND_HALF_UP);
            } catch (NumberFormatException e) {
                log.warn("转换BigDecimal失败：{}", value, e);
                return BigDecimal.ZERO;
            }
        }
        if (value instanceof Double) {
            return BigDecimal.valueOf((Double) value).setScale(2, BigDecimal.ROUND_HALF_UP);
        }
        if (value instanceof Integer) {
            return BigDecimal.valueOf((Integer) value).setScale(2);
        }
        if (value instanceof Long) {
            return BigDecimal.valueOf((Long) value).setScale(2);
        }
        return BigDecimal.ZERO;
    }

    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null || "".equals(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse((String) value, DATETIME_FORMATTER);
        } catch (Exception e) {
            log.error("日期转换失败：{}", value, e);
            return null;
        }
    }

    @Override
    public List<StockPoolDto> lbjjStockPool(LocalDate tradeDate, Integer notShowSt) {
        Result result = getValidateTradeDateResult(tradeDate);

        List<StockPool> stockPoolList = getBaseMapper().selectList(new LambdaQueryWrapper<StockPool>()
                .eq(StockPool::getTradeDate, result.format)
                .ne(StockPool::getPoolType, "super_stock")
                .notLike(notShowSt != null && notShowSt == 1, StockPool::getStockName, "ST")
        );

        if (CollectionUtils.isEmpty(stockPoolList)) {
            syncAllStockPoolData();
            stockPoolList = getBaseMapper().selectList(new LambdaQueryWrapper<StockPool>()
                    .eq(StockPool::getTradeDate, result.format)
                    .ne(StockPool::getPoolType, "super_stock")
                    .notLike(notShowSt != null && notShowSt == 1, StockPool::getStockReason, "ST")
            );
        }

        Map<String, Map<Integer, List<StockPool>>> poolGroupMap = stockPoolList.stream()
                .peek(StockPool::buildStockNameAndReason)
                .collect(Collectors.groupingBy(
                        StockPool::getPoolType,
                        Collectors.groupingBy(
                                StockPool::getLimitDays,
                                Collectors.toList()
                        )
                ));
        Map<Integer, List<StockPool>> ztMap = Optional.ofNullable(poolGroupMap.get("zt")).orElse(Collections.emptyMap());
        Map<Integer, List<StockPool>> dtMap = Optional.ofNullable(poolGroupMap.get("dt")).orElse(Collections.emptyMap());
        Map<Integer, List<StockPool>> yesterdayZtMap = Optional.ofNullable(poolGroupMap.get("yesterday_zt")).orElse(Collections.emptyMap());
        Map<Integer, List<StockPool>> brokenZtMap = Optional.ofNullable(poolGroupMap.get("broken_zt")).orElse(Collections.emptyMap());

        // 3. 炸板池提取「股票代码→股票对象」的Map（解决flatMap和toMap问题）
        Map<String, StockPool> ztStockMap = ztMap.values().stream()
                // 修正1：flatMap需要传入流转换函数，将List<StockPool>转为Stream<StockPool>
                .flatMap(List::stream)
                // 修正2：toMap的key=股票代码，value=股票对象；处理重复代码（保留第一个/最后一个）
                .collect(Collectors.toMap(
                        StockPool::getStockCode,  // key：股票代码
                        stock -> stock,           // value：股票对象
                        (existing, replacement) -> existing, // 重复key时保留原有值（避免报错）
                        LinkedHashMap::new        // 保持排序顺序（与原流一致）
                ));
        Map<String, StockPool> dtStockMap = dtMap.values().stream()
                // 修正1：flatMap需要传入流转换函数，将List<StockPool>转为Stream<StockPool>
                .flatMap(List::stream)
                // 修正2：toMap的key=股票代码，value=股票对象；处理重复代码（保留第一个/最后一个）
                .collect(Collectors.toMap(
                        StockPool::getStockCode,  // key：股票代码
                        stock -> stock,           // value：股票对象
                        (existing, replacement) -> existing, // 重复key时保留原有值（避免报错）
                        LinkedHashMap::new        // 保持排序顺序（与原流一致）
                ));
        Map<String, StockPool> brokenStockZtMap = brokenZtMap.values().stream()
                // 修正1：flatMap需要传入流转换函数，将List<StockPool>转为Stream<StockPool>
                .flatMap(List::stream)
                // 修正2：toMap的key=股票代码，value=股票对象；处理重复代码（保留第一个/最后一个）
                .collect(Collectors.toMap(
                        StockPool::getStockCode,  // key：股票代码
                        stock -> stock,           // value：股票对象
                        (existing, replacement) -> existing, // 重复key时保留原有值（避免报错）
                        LinkedHashMap::new        // 保持排序顺序（与原流一致）
                ));

        List<StockPoolDto> resultList = new ArrayList<>();
        StockPoolDto firstLimitUpStockDto = new StockPoolDto();
        firstLimitUpStockDto.setTitle("首板");
        firstLimitUpStockDto.setLimitDays(1);
        firstLimitUpStockDto.setChildren(new ArrayList<>());

        yesterdayZtMap.forEach((ztTimes, yesterdayZtList) -> {
            StockPoolDto stockPoolDto = new StockPoolDto();
            stockPoolDto.setTitle(ztTimes + " 进 " + (ztTimes + 1));
            stockPoolDto.setLimitDays(ztTimes + 1);

            int yesterdayZtSize = yesterdayZtList.size();
            Map<String, StockPool> todayZtStockMap = ztMap.getOrDefault(ztTimes + 1, new ArrayList<>()).stream().collect(Collectors.toMap(StockPool::getStockCode, Function.identity()));
            stockPoolDto.setChance(new BigDecimal(todayZtStockMap.size() * 100.0 / yesterdayZtSize).setScale(2, RoundingMode.HALF_UP).doubleValue() + "%");

            stockPoolDto.setChildren(yesterdayZtList.stream().map(stock -> {
                StockPoolDto child = new StockPoolDto();
                BeanUtils.copyProperties(stock, child);
                return child;
            }).collect(Collectors.toList()));

            stockPoolDto.getChildren().forEach(item -> {
                String stockCode = item.getStockCode();
                StockPool todayZtStock = ztStockMap.get(stockCode);
                if (todayZtStock != null) {
                    BeanUtils.copyProperties(todayZtStock, item);
                    item.setStockName(StockNameStyleUtil.addStyledTag(item.getStockName(), "成"));
                    item.setJjType(0);
                } else if (dtStockMap.containsKey(stockCode)) {
                    item.setStockName(StockNameStyleUtil.addStyledTag(item.getStockName(), "跌"));
                    item.setJjType(1);
                } else if (brokenStockZtMap.containsKey(stockCode)) {
                    item.setStockName(StockNameStyleUtil.addStyledTag(item.getStockName(), "炸"));
                    item.setJjType(2);
                } else {
                    // todo 查询当前涨幅
                    item.setStockName(StockNameStyleUtil.addStyledTag(item.getStockName(), "败"));
                    item.setJjType(3);
                }
            });
            stockPoolDto.setChildren(stockPoolDto.getChildren().stream()
                    .sorted(Comparator.comparingInt(StockPoolDto::getJjType)
                            .thenComparing(StockPoolDto::getChangePercent)
                    ).collect(Collectors.toList()));

            resultList.add(stockPoolDto);
        });

        firstLimitUpStockDto.getChildren().addAll(ztStockMap.values().stream().map(item -> {
                    StockPoolDto stockPoolDto = new StockPoolDto();
                    BeanUtils.copyProperties(item, stockPoolDto);
                    if (item.getPoolType().equals("zt")) {
                        stockPoolDto.setStockName(StockNameStyleUtil.addStyledTag(stockPoolDto.getStockName(), "成"));
                        stockPoolDto.setJjType(0);
                    } else {
                        stockPoolDto.setStockName(StockNameStyleUtil.addStyledTag(stockPoolDto.getStockName(), "炸"));
                        stockPoolDto.setJjType(1);
                    }
                    return stockPoolDto;
                }).sorted(Comparator.comparingInt(StockPoolDto::getJjType)
                        .thenComparing(StockPoolDto::getChangePercent))
                .collect(Collectors.toList()));
        resultList.add(firstLimitUpStockDto);

        return resultList.stream().sorted(Comparator.comparingInt(StockPoolDto::getLimitDays).reversed()).collect(Collectors.toList());
    }

}