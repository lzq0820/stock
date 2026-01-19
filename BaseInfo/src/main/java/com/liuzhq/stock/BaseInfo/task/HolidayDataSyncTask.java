package com.liuzhq.stock.BaseInfo.task;

import com.liuzhq.stock.BaseInfo.entity.SysHoliday;
import com.liuzhq.stock.BaseInfo.service.SysHolidayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 节假日数据同步定时任务
 * 每天凌晨0点执行，同步当前年和下一年的节假日数据（包含全年周末）
 * 修改点：
 * 1. 同步单年份数据时，先清空该年份所有记录，再重新入库
 * 2. 新增month字段，同步维护日期对应的月份
 */
@Component
@Slf4j
public class HolidayDataSyncTask {

    @Autowired
    private SysHolidayService sysHolidayService;

    /**
     * 每天凌晨0点执行节假日数据同步
     * cron表达式说明：秒 分 时 日 月 周 年（年可选）
     * 0 0 0 * * ?  表示每天凌晨0点0分0秒执行
     * 0 0 0 ? * 7  表示每周六凌晨0点0分0秒执行
     * 0 0 0 1 * ?  每月 1 号凌晨 0 点 0 分 0 秒执行
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional(rollbackFor = Exception.class)
    public void syncHolidayDataTask() {
        log.info("开始执行节假日数据同步定时任务，当前时间：{}", LocalDateTime.now());

        try {
            // 1. 获取当前年份和下一年份（提前同步下一年数据，避免跨年问题）
            int currentYear = LocalDate.now().getYear();
            int nextYear = currentYear + 1;

            // 2. 同步当前年数据（包含周末）
            syncSingleYearHolidayData(currentYear);

            log.info("节假日数据同步定时任务执行完成");
        } catch (Exception e) {
            log.error("节假日数据同步定时任务执行失败", e);
            // 可添加告警逻辑（如发送邮件/短信通知）
            throw new RuntimeException("节假日数据同步定时任务执行异常", e);
        }
    }

    /**
     * 同步单个年份的节假日数据（包含全年周末）
     * 修改点：先清空该年份所有记录，再重新入库；新增month字段赋值
     * @param year 要同步的年份
     */
    private void syncSingleYearHolidayData(int year) {
        try {
            // 新增逻辑：同步前先清空该年份的所有记录（核心修改）
            log.info("第一步：清空{}年已有的所有节假日/周末数据", year);
            sysHolidayService.lambdaUpdate()
                    .eq(SysHoliday::getYear, year)
                    .remove();

            // 2. 调用API同步节假日和调休数据（同步时需给month字段赋值）
            log.info("第二步：同步{}年节假日和调休数据", year);
            sysHolidayService.syncHolidayData(year);

            log.info("{}年节假日+周末数据同步完成（清空重插）", year);

        } catch (Exception e) {
            log.error("同步{}年节假日数据失败", year, e);
            throw new RuntimeException(String.format("同步%d年节假日数据异常", year), e);
        }
    }



    /**
     * 手动触发同步任务（可选，用于紧急更新）
     */
    public void manualSyncHolidayData(Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        log.info("手动触发{}年节假日数据同步（清空重插+新增月份字段）", year);
        syncSingleYearHolidayData(year);
    }

}