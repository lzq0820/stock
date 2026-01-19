<template>
  <div id="app">
    <DatePicker v-model="selectedDate" :holidays="holidays" />
    <PoolSwitcher :active-pool="activePool" @switch-pool="switchPool" />
    <SearchBox stockBoxWidth="200px" @search="onSearch" @filter-change="onFilterChange"/>

    <div class="echelon-chart-container">
      <EchelonChart
          :stocks="stocks"
          :pool-type="activePool"
          :search-query="searchQuery"
          :clicked-reasons="clickedReasons"
          :selected-date="selectedDate"
          @reason-click="onReasonClick"
      />
    </div>
  </div>
</template>

<script>
import { ref, onMounted, watch } from 'vue';
import DatePicker from '@/components/DatePicker.vue';
import PoolSwitcher from '@/components/PoolSwitcher.vue';
import SearchBox from '@/components/SearchBox.vue';
import EchelonChart from '@/components/EchelonChart.vue';
import { fetchStockPool, fetchHolidays } from '@/utils/api.js';

export default {
  name: 'StockGradientView',
  components: {
    DatePicker,
    PoolSwitcher,
    SearchBox,
    EchelonChart
  },
  setup() {
    const selectedDate = ref(new Date().toISOString().split('T')[0]);
    const activePool = ref('zt');
    const searchQuery = ref('');
    const clickedReasons = ref([]);
    const notShowSt = ref(0); // 默认显示ST票
    const stocks = ref([]);
    const holidays = ref([]);

    const loadStocks = async () => {
      try {
        // 传递notShowSt参数
        stocks.value = await fetchStockPool(selectedDate.value, activePool.value, notShowSt.value);
      } catch (error) {
        console.error('Failed to load stocks:', error);
      }
    };

    const loadHolidays = async () => {
      try {
        const date = new Date(selectedDate.value);
        const year = date.getFullYear();
        holidays.value = await fetchHolidays(year);
      } catch (error) {
        console.error('Failed to load holidays:', error);
      }
    };

    const switchPool = (pool) => {
      activePool.value = pool;
    };

    const onSearch = (query) => {
      searchQuery.value = query;
      clickedReasons.value = [];
    };

    // 处理过滤条件变化
    const onFilterChange = (params) => {
      searchQuery.value = params.query;
      notShowSt.value = params.notShowSt;
      clickedReasons.value = [];
      loadStocks(); // 重新加载数据
    };

    const onReasonClick = (reasons) => {
      clickedReasons.value = reasons;
    };

    onMounted(() => {
      loadStocks();
      loadHolidays();
    });

    watch(selectedDate, () => {
      loadStocks();
      loadHolidays();
    });

    watch(activePool, () => {
      loadStocks();
    });

    return {
      selectedDate,
      activePool,
      searchQuery,
      clickedReasons,
      notShowSt,
      stocks,
      holidays,
      switchPool,
      onSearch,
      onFilterChange,
      onReasonClick
    };
  }
}
</script>

<style>
/* 样式保持不变 */
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  margin: 20px;
}

.echelon-chart-container {
  height: calc(100vh - 150px);
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: thin;
  scrollbar-color: #888 #f1f1f1;
  position: relative;
  margin-top: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}

.echelon-chart-container::-webkit-scrollbar {
  width: 6px;
}
.echelon-chart-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}
.echelon-chart-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}
.echelon-chart-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}

@media (prefers-color-scheme: dark) {
  #app {
    color: #e0e0e0;
    background-color: #ffffff;
  }
  .echelon-chart-container {
    scrollbar-color: #838383 #fffdfd;
  }
  .echelon-chart-container::-webkit-scrollbar-track {
    background: #222;
  }
  .echelon-chart-container::-webkit-scrollbar-thumb {
    background: #666;
  }
}
</style>
