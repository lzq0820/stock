<template>
  <div class="stock-pool">
    <!-- 标签切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="连板晋级" name="lbjj"></el-tab-pane>
      <el-tab-pane label="涨停池" name="zt"></el-tab-pane>
      <el-tab-pane label="跌停池" name="dt"></el-tab-pane>
      <el-tab-pane label="强势股池" name="super_stock"></el-tab-pane>
      <el-tab-pane label="炸板池" name="broken_zt"></el-tab-pane>
    </el-tabs>

    <!-- 筛选面板 -->
    <FilterPanel
        :init-trade-date="tradeDate"
        :init-not-show-st="notShowSt"
        :all-reasons="allReasons"
        @st-switch="handleStSwitch"
        @date-change="handleDateChange"
        @filter-change="handleFilterChange"
    />

    <!-- 雁阵图 -->
    <YanZhenChart
        :stock-list="stockList"
        :filter-params="filterParams"
        :stock-reason="filterParams.stockReason"
    />

    <!-- 股票详情侧边栏 -->
    <el-drawer
        v-model="detailVisible"
        title="股票详情"
        direction="rtl"
        size="30%"
    >
      <StockDetail v-if="selectedStock" :stock="selectedStock" />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElTabs, ElTabPane, ElDrawer } from 'element-plus'
import FilterPanel from './FilterPanel.vue'
import YanZhenChart from './YanZhenChart.vue'
import StockDetail from './StockDetail.vue' // 注意：这里应该是 StockDetail 而不是 StockPool
import { getLbjjData, getStockPoolData } from '@/api/stockPool'
import { formatDate } from '@/utils/date'
import { extractAllReasons } from '@/utils/filter'

// 响应式数据
const activeTab = ref('lbjj')
const tradeDate = ref(formatDate(new Date()))
const notShowSt = ref(0)
const stockList = ref([])
const allReasons = ref([])
const filterParams = ref({
  reasonList: [],
  stockName: '',
  stockReason: ''
})
const detailVisible = ref(false)
const selectedStock = ref(null)

// 初始化
onMounted(() => {
  fetchStockData()
})

// 监听标签切换
watch(() => activeTab.value, () => {
  fetchStockData()
})

// 监听日期和ST开关变化
watch([() => tradeDate.value, () => notShowSt.value], () => {
  fetchStockData()
})

// 获取股票数据
const fetchStockData = async () => {
  try {
    let res
    const params = {
      tradeDate: tradeDate.value,
      notShowSt: notShowSt.value
    }

    if (activeTab.value === 'lbjj') {
      res = await getLbjjData(params)
    } else {
      res = await getStockPoolData({
        ...params,
        poolType: activeTab.value
      })
    }

    stockList.value = res.data
    // 提取所有涨停原因
    allReasons.value = extractAllReasons(res.data)
  } catch (error) {
    console.error('获取股票数据失败：', error)
  }
}

// 事件处理
const handleTabChange = () => {
  fetchStockData()
}

const handleStSwitch = (val) => {
  notShowSt.value = val ? 1 : 0
}

const handleDateChange = (val) => {
  tradeDate.value = val
}

const handleFilterChange = (params) => {
  filterParams.value = params
}

// 选择股票（用于详情展示，需在雁阵图的stock-item上绑定点击事件传递）
const selectStock = (stock) => {
  selectedStock.value = stock
  detailVisible.value = true
}

// 暴露方法给子组件（需在YanZhenChart中调用）
defineExpose({
  selectStock
})
</script>

<style scoped>
.stock-pool {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
</style>