<template>
  <div class="my-stocks-view">
    <h2>自选股</h2>
    <p>这里是自选股功能页面</p>
    <!-- 自选功能的具体实现 -->
  </div>
</template>

<script>export default {
  name: 'MyStocksView'
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
}
</style>
