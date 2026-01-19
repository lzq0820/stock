<!--筛选面板组件（reason多选、搜索框）-->
<template>
  <div class="filter-panel">
    <!-- 隐藏ST股票开关 -->
    <div class="filter-item">
      <el-switch
          v-model="notShowSt"
          active-text="隐藏ST股票"
          inactive-text="显示ST股票"
          @change="handleStSwitch"
      />
    </div>

    <!-- 日期选择器 -->
    <div class="filter-item">
      <el-date-picker
          v-model="tradeDate"
          type="date"
          placeholder="选择日期"
          :disabled-date="disabledDate"
          @change="handleDateChange"
      />
      <div v-if="holidayName" class="holiday-tip">{{ holidayName }}</div>
    </div>

    <!-- 涨停原因多选 -->
    <div class="filter-item">
      <div class="search-wrapper">
        <el-select
            v-model="selectedReasons"
            multiple
            filterable
            remote
            reserve-keyword
            placeholder="选择涨停原因"
            :remote-method="handleRemoteSearch"
            :options="reasonOptions"
            @change="handleReasonChange"
        />
        <el-button
            v-if="selectedReasons.length > 0"
            icon="Close"
            size="small"
            circle
            class="clear-btn"
            @click="clearReasons"
        />
      </div>
    </div>

    <!-- 股票名称搜索 -->
    <div class="filter-item">
      <div class="search-wrapper">
        <el-input
            v-model="stockName"
            placeholder="搜索股票名称"
            @input="handleSearchChange"
        />
        <el-button
            v-if="stockName"
            icon="Close"
            size="small"
            circle
            class="clear-btn"
            @click="clearStockName"
        />
      </div>
    </div>

    <!-- 详情搜索 -->
    <div class="filter-item">
      <div class="search-wrapper">
        <el-input
            v-model="stockReason"
            placeholder="搜索股票详情"
            @input="handleSearchChange"
        />
        <el-button
            v-if="stockReason"
            icon="Close"
            size="small"
            circle
            class="clear-btn"
            @click="clearStockReason"
        />
      </div>
    </div>

    <!-- 清空所有筛选条件按钮 -->
    <div class="filter-item">
      <el-button
          type="warning"
          plain
          @click="clearAllFilters"
      >
        清空所有筛选条件
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElSwitch, ElDatePicker, ElSelect, ElInput, ElButton } from 'element-plus'
import { formatDate, getDisabledDates, getHolidayName } from '@/utils/date'
import { getHolidayList } from '@/api/holiday'

// 定义props
const props = defineProps({
  initTradeDate: {
    type: String,
    default: formatDate(new Date())
  },
  initNotShowSt: {
    type: Number,
    default: 0
  },
  allReasons: {
    type: Array,
    default: []
  }
})

// 定义emit
const emit = defineEmits([
  'stSwitch',
  'dateChange',
  'filterChange'
])

// 响应式数据
const notShowSt = ref(props.initNotShowSt)
const tradeDate = ref(props.initTradeDate)
const selectedReasons = ref([])
const stockName = ref('')
const stockReason = ref('')
const disabledDates = ref([])
const holidayList = ref([])
const holidayName = ref('')
const reasonOptions = ref([])

// 初始化
onMounted(async () => {
  // 获取禁用日期
  disabledDates.value = await getDisabledDates()
  // 获取节假日列表
  const holidayRes = await getHolidayList(new Date().getFullYear())
  holidayList.value = holidayRes.data
  // 更新节假日名称
  holidayName.value = getHolidayName(tradeDate.value, holidayList.value)
  // 初始化原因选项
  reasonOptions.value = props.allReasons.map(reason => ({ label: reason, value: reason }))
})

// 监听allReasons变化
watch(() => props.allReasons, (newVal) => {
  reasonOptions.value = newVal.map(reason => ({ label: reason, value: reason }))
})

// 禁用日期判断
const disabledDate = (date) => {
  const dateStr = formatDate(date)
  return disabledDates.value.includes(dateStr)
}

// 远程搜索原因
const handleRemoteSearch = (query) => {
  if (query) {
    reasonOptions.value = props.allReasons
        .filter(reason => reason.includes(query))
        .map(reason => ({ label: reason, value: reason }))
  } else {
    reasonOptions.value = props.allReasons.map(reason => ({ label: reason, value: reason }))
  }
}

// 清空各个筛选条件
const clearReasons = () => {
  selectedReasons.value = []
  emitFilterChange()
}

const clearStockName = () => {
  stockName.value = ''
  emitFilterChange()
}

const clearStockReason = () => {
  stockReason.value = ''
  emitFilterChange()
}

// 清空所有筛选条件
const clearAllFilters = () => {
  selectedReasons.value = []
  stockName.value = ''
  stockReason.value = ''
  notShowSt.value = 0 // 重置ST股票显示设置
  emitFilterChange()
}

// 事件处理
const handleStSwitch = () => {
  emit('stSwitch', notShowSt.value)
}

const handleDateChange = (val) => {
  const dateStr = formatDate(val)
  holidayName.value = getHolidayName(dateStr, holidayList.value)
  emit('dateChange', dateStr)
}

const handleReasonChange = () => {
  emitFilterChange()
}

const handleSearchChange = () => {
  emitFilterChange()
}

// 触发筛选变化
const emitFilterChange = () => {
  emit('filterChange', {
    reasonList: selectedReasons.value,
    stockName: stockName.value,
    stockReason: stockReason.value
  })
}
</script>

<style scoped>
.filter-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding: 10px 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #e6e6e6;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
  position: relative;
}

.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.clear-btn {
  position: absolute;
  right: 8px;
  z-index: 1;
  transform: translateY(-50%);
  top: 50%;
  background-color: #f5f5f5;
  border: 1px solid #dcdfe6;
}

.holiday-tip {
  font-size: 12px;
  color: #999;
}
</style>
