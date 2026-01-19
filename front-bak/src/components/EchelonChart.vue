<template>
  <div class="echelon-chart">
    <h2><span class="highlighted-date">{{ titleDate }}</span> {{ titlePrefix }} <span class="stock-count">{{
        stocks.length }}</span> 只股票</h2>
    <div v-for="group in groupedStocks" :key="group.days" class="echelon-group"
         :style="{ background: getGradientBackground(group.days) }">
      <div class="group-header">
        <div class="limit-days" v-if="poolType !== 'broken_zt'">{{ group.days }} 连板</div>
      </div>
      <div class="stocks-grid">
        <!-- 修改：传递clickedReasons数组 -->
        <StockCard v-for="stock in sortedStocks(group.stocks)" :key="stock.id" :stock="stock"
                   :is-highlighted="isHighlighted(stock)" :search-query="searchQuery" :clicked-reasons="clickedReasons"
                   @reason-click="onReasonClick" />
      </div>
    </div>
  </div>
</template>

<script>
import StockCard from './StockCard.vue';
import { sortStocksByChangePercent, calculatePromotionRate } from '../utils/helpers.js';

export default {
  name: 'EchelonChart',
  components: {
    StockCard
  },
  props: {
    stocks: {
      type: Array,
      default: () => []
    },
    poolType: {
      type: String,
      required: true
    },
    searchQuery: {
      type: String,
      default: ''
    },
    clickedReasons: {  // 修改：接收数组
      type: Array,
      default: () => []
    },
    selectedDate: {
      type: String,
      required: true
    }
  },
  computed: {
    titlePrefix() {
      switch (this.poolType) {
        case 'zt':
          return '涨停池';
        case 'dt':
          return '跌停池';
        case 'yesterday_zt':
          return '昨日涨停池';
        case 'super_stock':
          return '强势股池';
        case 'broken_zt':
          return '炸板池';
        default:
          return '股票池';
      }
    },
    titleDate() {
      return this.selectedDate || '今日';
    },
    groupedStocks() {
      const groups = {};

      this.stocks.forEach(stock => {
        let days = stock.limitUpDays || 1;
        if (this.poolType === 'dt') {
          days = stock.limitDownDays || 1;
        } else if (this.poolType === 'yesterday_zt') {
          days = stock.yesterdayLimitUpDays || 1;
        } else {
          days = stock.limitUpDays || 1;
        }
        if (!groups[days]) groups[days] = [];
        groups[days].push(stock);
      });

      Object.keys(groups).forEach(key => {
        groups[key].sort((a, b) => {
          if (this.poolType === 'dt') {
            if (a.changePercent !== b.changePercent) {
              return a.changePercent - b.changePercent;
            }
            return a.stockName.localeCompare(b.stockName);
          } else {
            return b.changePercent - a.changePercent;
          }
        });
      });

      const sortedKeys = Object.keys(groups).map(Number);
      sortedKeys.sort((a, b) => b - a);

      const sortedGroups = sortedKeys.map(key => ({
        days: key,
        stocks: groups[key]
      }));

      return sortedGroups;
    }
  },
  methods: {
    sortedStocks(stocks) {
      if (this.poolType === 'dt') {
        return stocks.sort((a, b) => a.changePercent - b.changePercent);
      } else {
        return stocks.sort((a, b) => b.changePercent - a.changePercent);
      }
    },
    getPromotionRate(days) {
      const tempGroups = {};
      this.groupedStocks.forEach(group => {
        tempGroups[group.days] = group.stocks;
      });
      return calculatePromotionRate(tempGroups, days);
    },
    isHighlighted(stock) {
      // 没有点击关键词时不高亮
      if (!this.clickedReasons || this.clickedReasons.length === 0) return false;

      // 获取当前股票的所有reason关键词
      const stockReasonParts = (stock.reason || '')
          .split('+')
          .map(part => part.trim().toLowerCase())
          .filter(part => part);

      // 也检查stockReason（如果需要）
      const stockReasonFull = (stock.stockReason || '').toLowerCase();

      // 检查是否有任意关键词匹配
      return this.clickedReasons.some(clickedReason => {
        const clickedReasonLower = clickedReason.toLowerCase().trim();
        // 匹配分割后的关键词 或 完整的stockReason
        return stockReasonParts.includes(clickedReasonLower) ||
            stockReasonFull.includes(clickedReasonLower);
      });
    },
    onReasonClick(reasons) {
      this.$emit('reason-click', reasons);  // 传递关键词数组
    },
    getGradientBackground(days) {
      const colors = {
        1: '#ffebee', 2: '#e3f2fd', 3: '#e8f5e9', 4: '#fff8e1', 5: '#f3e5f5',
        6: '#fff3e0', 7: '#e0f7fa', 8: '#fce4ec', 9: '#fffde7', 10: '#ede7f6'
      };
      return colors[days] || colors[10];
    }
  },
  watch: {
    searchQuery(newVal) {
      if (newVal) {
        this.$emit('update:clickedReasons', []);  // 清空关键词数组
      }
    }
  }
}
</script>

<style scoped>
/* 样式保持不变 */
.echelon-chart {
  margin-right: 10px;
  color: #000000;
  width: fit-content;
  max-width: 100%;
  overflow-y: auto;
}

.stock-count {
  color: #ff0000;
  font-weight: bold;
  font-size: 1.2em;
}

.highlighted-date {
  color: #2196f3;
  font-weight: bold;
  font-size: 1.2em;
}

.echelon-group {
  margin-bottom: 20px;
  padding: 10px;
  border-radius: 5px;
  width: fit-content;
}

.group-header {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 10px;
  color: #000000;
}

.promotion-rate {
  margin-left: 20px;
}

.stocks-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.2cm;
  width: fit-content;
}

.stocks-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.2cm;
}

.stocks-container>* {
  margin-bottom: 0.2cm;
}
</style>