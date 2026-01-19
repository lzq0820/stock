<template>
  <div class="side-menu" :style="{ width: isCollapse ? '60px' : '15%' }">
    <div class="collapse-btn" @click="toggleCollapse">
      <el-icon :icon="isCollapse ? Expand : Fold" />
    </div>
    <el-menu
        default-active="1"
        class="side-menu__menu"
        :collapse="isCollapse"
        router
    >
      <el-menu-item index="/optional">
        <el-icon><Menu /></el-icon>
        <template #title>自选</template>
      </el-menu-item>
      <el-sub-menu index="2">
        <template #title>
          <el-icon><Grid /></el-icon>
          <span>盘面</span>
        </template>
        <el-menu-item index="/stockPool">股票池</el-menu-item>
        <el-menu-item index="/dragonTiger">龙虎榜</el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMenu, ElMenuItem, ElSubMenu, ElIcon } from 'element-plus'
import { Menu, Grid, Expand, Fold } from '@element-plus/icons-vue'

const isCollapse = ref(false)
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}
</script>

<style scoped>
.side-menu {
  height: 100vh;
  border-right: 1px solid #e6e6e6;
  transition: width 0.3s ease;
  background: #fff;
}

.collapse-btn {
  text-align: center;
  padding: 10px 0;
  cursor: pointer;
  border-bottom: 1px solid #e6e6e6;
}

.side-menu__menu {
  height: calc(100% - 40px);
  border-right: none;
}
</style>