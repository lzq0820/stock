<template>
  <div class="yan-zhen-chart">
    <!-- 梯度列表 -->
    <div v-for="(grade, index) in filteredStockList" :key="index" class="grade-item">
      <!-- 梯度标题 -->
      <div class="grade-header" :style="{ backgroundColor: getGradeColor(grade.limitDays) }">
        <span class="grade-title">{{ grade.title }}</span>
        <span v-if="grade.chance" class="grade-chance">晋级几率：{{ grade.chance }}</span>
      </div>

      <!-- 梯度内股票 -->
      <div class="stock-grid">
        <div
            v-for="(stock, idx) in grade.children"
            :key="idx"
            class="stock-item"
            :class="{ 'stock-item--match': stock.isMatch }"
        >
          <div class="stock-info">
            <div class="stock-code">{{ stock.stockCode }}</div>
            <div
                class="stock-name"
                v-html="stock.stockName"
                :style="{ color: stock.changePercent > 0 ? 'red' : stock.changePercent < 0 ? 'green' : '#333' }"
            ></div>
            <div class="stock-price">价格：{{ stock.price }}</div>
            <div class="stock-change">涨幅：{{ stock.changePercent }}%</div>
            <div class="stock-reason">
              原因：{{ stock.reasonList.join(',') }}
            </div>
            <div class="stock-detail">
              详情：
              <el-tooltip
                  :content="stock.stockReason"
                  placement="top"
                  effect="dark"
              >
                <span class="detail-text">
                  {{ stock.stockReason.length > 10 ? stock.stockReason.slice(0, 10) + '...' : stock.stockReason }}
                </span>
              </el-tooltip>
              <!-- 详情搜索标黄 -->
              <template v-if="stockReason">
                <span v-html="highlightKeyword(stock.stockReason, stockReason)"></span>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, defineProps } from 'vue'
import { ElTooltip } from 'element-plus'
import { filterStocks } from '@/utils/filter'

// 定义props
const props = defineProps({
  stockList: {
    type: Array,
    default: []
  },
  filterParams: {
    type: Object,
    default: () => ({})
  },
  stockReason: {
    type: String,
    default: ''
  }
})

// 响应式数据
const filteredStockList = ref([])

// 监听股票列表和筛选参数变化
watch([() => props.stockList, () => props.filterParams], () => {
  filteredStockList.value = filterStocks(props.stockList, props.filterParams)
}, { immediate: true })

// 获取梯度颜色（浅色区分）
const getGradeColor = (limitDays) => {
  const colorMap = {
    1: '#f0f8fb',
    2: '#f5fafe',
    3: '#eaf6fa',
    4: '#e8f4f8',
    5: '#e3f2fd',
    6: '#e1f5fe',
    default: '#f8f9fa'
  }
  return colorMap[limitDays] || colorMap.default
}

// 高亮详情中的关键词
const highlightKeyword = (text, keyword) => {
  if (!keyword) return text
  const reg = new RegExp(keyword, 'g')
  return text.replace(reg, `<span style="background-color: yellow;">${keyword}</span>`)
}
</script>

<style scoped>
.yan-zhen-chart {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.grade-item {
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  overflow: hidden;
}

.grade-header {
  padding: 10px 15px;
  font-weight: bold;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.grade-title {
  font-size: 16px;
}

.grade-chance {
  font-size: 14px;
  color: #666;
}

.stock-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  padding: 15px;
}

.stock-item {
  padding: 10px;
  border: 1px solid #e6e6e6;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.stock-item--match {
  background-color: #fff9e6;
}

.stock-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
  font-size: 14px;
}

.stock-code {
  color: #666;
  font-size: 12px;
}

.detail-text {
  display: inline-block;
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>