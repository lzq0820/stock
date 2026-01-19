<template>
  <div class="stock-card" :class="{
    highlighted: isHighlighted
  }" @click="onReasonClick">
    <div class="stock-info">
      <span class="stock-name" v-html="highlightedStockName"></span>
      <span class="change-percent" :class="{
        'red-text': stock.changePercent > 0,
        'green-text': stock.changePercent < 0,
        'black-text': stock.changePercent === 0
      }">
        {{ stock.changePercent > 0 ? '+' : '' }}{{ stock.changePercent }}%
      </span>
      <span class="boards">{{ stock.mDaysNBoards || '' }}</span>
    </div>
    <div class="reason" :data-reason="fullReason" v-html="highlightedReason">
    </div>
  </div>
</template>

<script>
import { truncateText, copyToClipboard } from '../utils/helpers.js';

export default {
  name: 'StockCard',
  props: {
    stock: {
      type: Object,
      required: true
    },
    clickedReasons: {  // 修改：接收关键词数组
      type: Array,
      default: () => []
    },
    searchQuery: {
      type: String,
      default: ''
    }
  },
  emits: ['reason-click'],
  computed: {
    isHighlighted() {
      // 没有点击关键词时不高亮
      if (!this.clickedReasons || this.clickedReasons.length === 0) return false;

      // 获取当前股票的所有reason关键词
      const stockReasonParts = (this.stock.reason || '')
          .split('+')
          .map(part => part.trim().toLowerCase())
          .filter(part => part);

      // 也检查stockReason（完整文本）
      const stockReasonFull = (this.stock.stockReason || '').toLowerCase();

      // 检查是否有任意关键词匹配
      return this.clickedReasons.some(clickedReason => {
        const clickedReasonLower = clickedReason.toLowerCase().trim();
        // 匹配分割后的关键词 或 完整的stockReason
        return stockReasonParts.includes(clickedReasonLower) ||
            stockReasonFull.includes(clickedReasonLower);
      });
    },
    highlightedStockName() {
      return (this.stock.stockName || '').padEnd(6);
    },
    highlightedReason() {
      if (!this.stock.stockReason || !this.stock.stockReason.trim()) {
        return '';
      }
      return truncateText(this.stock.stockReason, 14);
    },
    fullReason() {
      return this.stock.stockReason || '';
    }
  },
  methods: {
    onReasonClick() {
      if (!this.stock.reason) return;
      // 分割reason并发射所有非空关键词数组
      const reasonParts = (this.stock.reason || '')
          .split('+')
          .map(part => part.trim())
          .filter(part => part);
      this.$emit('reason-click', reasonParts);  // 发射数组
    },
  }
}
</script>

<style scoped>
/* 样式保持不变 */
.stock-card {
  border: 1px solid #ccc;
  padding: 5px;
  margin: 2px;
  background-color: #f9f9f9 !important;
  display: inline-block;
  width: 200px;
  font-size: 12px;
  line-height: 1.2;
  transition: background-color 0.2s, border-color 0.2s, font-weight 0.2s;
}

.highlighted {
  background-color: #e0f7fa !important;
  font-weight: bold;
}

.stock-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reason {
  margin-top: 2px;
  cursor: pointer;
  position: relative;
}

.reason:hover::after {
  content: attr(data-reason);
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  background: #333;
  color: white;
  padding: 5px;
  border-radius: 3px;
  white-space: normal;
  max-width: 250px;
  z-index: 100;
}

button {
  font-size: 10px;
  padding: 2px 4px;
}

.change-percent.red-text {
  color: #ff0000 !important;
  font-weight: bold;
}

.change-percent.green-text {
  color: #00b300 !important;
  font-weight: bold;
}

.change-percent.black-text {
  color: #000000 !important;
}

.change-percent {
  font-size: 12px;
  min-width: 40px;
  text-align: right;
}
</style>