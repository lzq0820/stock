package com.liuzhq.stock.BaseInfo.response;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 股票K线响应实体
 * 对应infoway.io接口的返回格式
 */
@Data
public class StockKlineResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码（200成功）
     */
    private Integer ret;

    /**
     * 响应信息
     */
    private String msg;

    /**
     * 追踪ID
     */
    private String traceId;

    /**
     * K线数据列表
     */
    private List<StockKlineData> data;

    /**
     * K线数据详情
     */
    @Data
    public static class StockKlineData implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 标的代码
         */
        private String s;

        /**
         * K线列表
         */
        private List<KlineItem> respList;
    }

    /**
     * 单根K线信息
     */
    @Data
    public static class KlineItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 成交时间（秒时间戳）
         */
        private String t;

        /**
         * 最高价
         */
        private String h;

        /**
         * 开盘价
         */
        private String o;

        /**
         * 最低价
         */
        private String l;

        /**
         * 收盘价
         */
        private String c;

        /**
         * 成交量
         */
        private String v;

        /**
         * 成交额
         */
        private String vw;

        /**
         * 涨跌幅
         */
        private String pc;

        /**
         * 涨跌额
         */
        private String pca;
    }
}