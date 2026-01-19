package com.liuzhq.common.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * 股票名称样式工具类
 * 给标记（成/跌/炸/败）添加HTML样式，供前端v-html渲染
 */
public class StockNameStyleUtil {

    // 样式常量（可根据需求调整）
    private static final String SUCCESS_STYLE = "<span style='color: red; font-weight: bold;'>%s</span>";    // 成：红+加粗
    private static final String FALL_STYLE = "<span style='color: green; font-weight: bold;'>%s</span>";  // 跌：绿+加粗
    private static final String BROKEN_STYLE = "<span style='color: orange; font-weight: bold;'>%s</span>"; // 炸：黄/橙+加粗
    private static final String FAIL_STYLE = "%s"; // 败：无样式

    /**
     * 给股票名称添加带样式的标记
     * @param stockName 原始股票名称（如：梦天家居）
     * @param tag 标记（成/跌/炸/败）
     * @return 带HTML样式的股票名称（如：梦天家居<span style='color:red;font-weight:bold;'>(成)</span>）
     */
    public static String addStyledTag(String stockName, String tag) {
        if (StringUtils.isBlank(stockName)) {
            return "";
        }
        if (StringUtils.isBlank(tag)) {
            return stockName;
        }

        // 统一标记格式（加括号）
        String tagWithBracket = "(" + tag + ")";
        String styledTag;

        // 根据标记类型选择样式
        switch (tag) {
            case "成":
                styledTag = String.format(SUCCESS_STYLE, tagWithBracket);
                break;
            case "跌":
                styledTag = String.format(FALL_STYLE, tagWithBracket);
                break;
            case "炸":
                styledTag = String.format(BROKEN_STYLE, tagWithBracket);
                break;
            case "败":
                styledTag = String.format(FAIL_STYLE, tagWithBracket);
                break;
            default:
                // 未知标记：无样式
                styledTag = tagWithBracket;
                break;
        }

        return stockName + styledTag;
    }
}