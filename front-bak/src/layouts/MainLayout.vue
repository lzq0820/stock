<template>
  <div class="main-layout">
    <div class="sidebar">
      <SidebarMenu :default-active="activeMenu" @menu-select="handleMenuSelect" />
    </div>
    <div class="main-content">
      <component :is="currentViewComponent" />
    </div>
  </div>
</template>

<script>
import SidebarMenu from '@/components/SidebarMenu.vue';
import StockGradientView from '@/views/StockGradientView.vue';
import MyStocksView from '@/views/MyStocksView.vue';
import { MENU_CONFIG } from '@/config/menuConfig.js';

export default {
  name: 'MainLayout',
  components: {
    SidebarMenu,
    StockGradientView,
    MyStocksView
  },
  data() {
    return {
      activeMenu: MENU_CONFIG.DEFAULT_ACTIVE,
      currentViewComponent: this.getComponentByMenu(MENU_CONFIG.DEFAULT_ACTIVE)
    }
  },
  methods: {
    getComponentByMenu(menuIndex) {
      const menuItem = MENU_CONFIG.MENU_ITEMS[menuIndex];
      return menuItem ? menuItem.component : 'MyStocksView';
    },
    handleMenuSelect(index) {
      this.activeMenu = index;
      const menuItem = MENU_CONFIG.MENU_ITEMS[index];
      this.currentViewComponent = menuItem ? menuItem.component : 'MyStocksView';
    }
  }
}
</script>

<style scoped>
.main-layout {
  display: flex;
  min-height: 100vh;
  margin: 0;
  padding: 0;
  align-items: flex-start;
}

.sidebar {
  flex-shrink: 0;
  width: 260px;
  box-sizing: border-box;
  background-color: #f5f5f5;
  border-right: 1px solid #dcdfe6;
  height: auto;
  padding: 10px 0 0 0;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  box-sizing: border-box;
  min-height: 100vh;
}
</style>
