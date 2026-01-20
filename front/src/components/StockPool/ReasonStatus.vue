<template>
  <div class="reason-stats">
    <div class="stats-header">
      <h3>涨停原因统计</h3>
      <el-button
          v-if="selectedReasons.length > 0"
          @click="clearSelectedReasons"
          size="small"
          type="warning"
          plain
      >
        清除已选原因
      </el-button>
    </div>

    <div class="stats-container reason-tag"
         v-for="stat in sortedStats"
         :key="stat.reason"
         :class="{ 'selected': isSelected(stat.reason) }"
         @click="toggleReasonSelection(stat.reason)">
        <span class="reason-text">{{ stat.reason }}</span>
        <span class="count">{{ stat.count }}</span>
      </div>
  </div>
</template>

<script setup>
import { computed, defineProps, defineEmits } from 'vue'

const props = defineProps({
  stockList: {
    type: Array,
    default: () => []
  },
  filterParams: {
    type: Object,
    default: () => ({})
  },
  selectedReasons: {
    type: Array,
    default: () => []
  },
  filteredStockList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['reason-toggle'])

// 计算各涨停原因出现次数
const reasonStats = computed(() => {
  const stats = {}

  // 使用 props.stockList 来计算统计信息
  props.stockList.forEach(grade => {
    grade.children?.forEach(stock => {
      stock.reasonList?.forEach(reason => {
        if (stats[reason]) {
          stats[reason]++
        } else {
          stats[reason] = 1
        }
      })
    })
  })

  return Object.entries(stats).map(([reason, count]) => ({
    reason,
    count
  }))
})

// 按数量降序排列
const sortedStats = computed(() => {
  return [...reasonStats.value].sort((a, b) => b.count - a.count)
})

// 判断原因是否被选中
const isSelected = (reason) => {
  return props.selectedReasons.includes(reason)
}

// 切换原因选择状态
const toggleReasonSelection = (reason) => {
  emit('reason-toggle', reason)
}

// 清除已选原因
const clearSelectedReasons = () => {
  emit('clear-selected-reasons')
}
</script>

<style scoped>
.reason-stats {
  padding: 15px;
  border: 1px solid #e6e6e6;
  border-radius: 8px;
  margin: 10px 0;
  background-color: #fafafa;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.stats-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.stats-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reason-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 12px;
}

.reason-tag:hover {
  background-color: #eef7ff;
  border-color: #409eff;
}

.reason-tag.selected {
  background-color: #e6f7ff;
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.reason-tag .reason-text {
  margin-right: 6px;
  color: #333;
}

.reason-tag .count {
  background-color: #409eff;
  color: white;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: bold;
}
</style>
