<template>
  <div class="pool-switcher">
    <button
      v-for="pool in pools"
      :key="pool.key"
      :class="{ active: activePool === pool.key }"
      @click="switchPool(pool.key)"
    >
      {{ pool.label }}
    </button>
  </div>
</template>

<script>
export default {
  name: 'PoolSwitcher',
  props: {
    activePool: {
      type: String,
      default: 'zt'
    }
  },
  emits: ['switch-pool'],
  data() {
    return {
      pools: [
        { key: 'zt', label: '涨停连板池' },
        { key: 'dt', label: '跌停池' },
        { key: 'yesterday_zt', label: '昨日涨停池' },
        { key: 'broken_zt', label: '炸板池' },
        { key: 'super_stock', label: '强势股池' }
      ]
    };
  },
  methods: {
    switchPool(poolKey) {
      this.$emit('switch-pool', poolKey);
    }
  }
}
</script>

<style scoped>
.pool-switcher {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
button {
  padding: 5px 10px;
  cursor: pointer;
}
.active {
  background-color: #007bff;
  color: white;
}
</style>