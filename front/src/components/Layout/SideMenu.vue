<template>
  <el-menu
      :default-active="$route.path"
      class="side-menu"
      :unique-opened="true"
      router
  >
    <el-sub-menu index="stock">
      <template #title>
        <span>盘面分析</span>
      </template>
      <el-menu-item index="/stockPool">股票池</el-menu-item>
      <el-menu-item index="/dragonTiger">龙虎榜</el-menu-item>
    </el-sub-menu>

    <el-menu-item index="/optional">
      <span>自选</span>
    </el-menu-item>
  </el-menu>
</template>

<script setup>
import { ElMenu, ElSubMenu, ElMenuItem } from 'element-plus'
</script>

<style scoped>
.side-menu {
  height: 100vh;
  width: 200px;
}
</style>
