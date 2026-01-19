package com.liuzhq.stock.BaseInfo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liuzhq.common.response.ResultModel;
import com.liuzhq.stock.BaseInfo.entity.SysHoliday;
import com.liuzhq.stock.BaseInfo.service.SysHolidayService;
import com.liuzhq.stock.BaseInfo.task.HolidayDataSyncTask;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 节假日数据同步手动触发接口
 */
@RestController
@RequestMapping("/api/holiday/sync")
@Slf4j
@Api(tags = "节假日数据同步接口")
public class HolidaySyncController {

    @Resource
    private HolidayDataSyncTask holidayDataSyncTask;
    @Resource
    private SysHolidayService sysHolidayService;

    /**
     * 手动触发指定年份的节假日数据同步
     *
     * @param year 要同步的年份（不传则同步当前年）
     */
    @GetMapping("/manual")
    @ApiOperation(value = "手动触发节假日数据同步", notes = "可指定年份，不传则同步当前年和下一年")
    public ResultModel<String> manualSync(
            @ApiParam(value = "要同步的年份（如：2026），不传则同步当前年和下一年", example = "2026")
            @RequestParam(required = false) Integer year) {
        try {
            if (year == null) {
                // 不传年份：同步当前年+下一年（和定时任务逻辑一致）
                int currentYear = LocalDate.now().getYear();
                int nextYear = currentYear + 1;

                holidayDataSyncTask.manualSyncHolidayData(currentYear);
                holidayDataSyncTask.manualSyncHolidayData(nextYear);

                return ResultModel.success(String.format("已触发%s年和%s年节假日数据同步", currentYear, nextYear));
            } else {
                // 传了年份：仅同步指定年份
                // 参数校验：年份范围限制（1970-2100）
                if (year < 1970 || year > 2100) {
                    return ResultModel.error("年份参数无效，请输入1970-2100之间的年份");
                }

                holidayDataSyncTask.manualSyncHolidayData(year);
                return ResultModel.success(String.format("已触发%s年节假日数据同步", year));
            }
        } catch (Exception e) {
            log.error("手动触发节假日数据同步失败", e);
            return ResultModel.error("同步失败：" + e.getMessage());
        }
    }

    /**
     * 立即执行一次完整的同步任务（和定时任务逻辑完全一致）
     */
    @GetMapping("/execute")
    @ApiOperation(value = "执行完整同步任务", notes = "同步当前年和下一年，和凌晨定时任务逻辑完全一致")
    public ResultModel<String> executeFullTask() {
        try {
            holidayDataSyncTask.syncHolidayDataTask();
            return ResultModel.success("完整节假日同步任务已执行完成");
        } catch (Exception e) {
            log.error("执行完整同步任务失败", e);
            return ResultModel.error("执行失败：" + e.getMessage());
        }
    }

    /**
     * 查询当年当月的法定节假日
     */
    @GetMapping("/query")
    @ApiOperation(value = "执行完整同步任务", notes = "同步当前年和下一年，和凌晨定时任务逻辑完全一致")
    public ResultModel<List<SysHoliday>> query(
            @ApiParam(value = "年份") @RequestParam(required = false, defaultValue = "") Integer year,
            @ApiParam(value = "月份") @RequestParam(required = false, defaultValue = "") Integer month) {
        try {
            List<SysHoliday> allHolidayList = new ArrayList<>();
            List<Integer> yearList = new ArrayList<>();
            yearList.add(year);
            yearList.add(year - 1);
//            yearList.add(year + 1);
            yearList.forEach(y -> {
                List<SysHoliday> holidayList = sysHolidayService.query(y, month);
                allHolidayList.addAll(holidayList);
            });
            return ResultModel.success(allHolidayList);
        } catch (Exception e) {
            log.error("执行完整同步任务失败", e);
            return ResultModel.error("执行失败：" + e.getMessage());
        }
    }
}