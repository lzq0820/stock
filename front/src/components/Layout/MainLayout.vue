<template>
  <div class="main-layout">
    <SideMenu />
    <div class="main-layout__content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import SideMenu from './SideMenu.vue'
</script>

<style scoped>
.main-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
}

.main-layout__content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}
</style>