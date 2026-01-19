<template>
  <div class="sidebar-menu">
    <el-menu
        :default-active="defaultActive"
        class="el-menu-vertical"
        @select="handleMenuSelect"
        mode="vertical"
    >
      <!-- 一级菜单：盘面 -->
      <el-sub-menu index="panmian" class="level-1">
        <template #title>
          <el-icon><FolderOpened /></el-icon>
          <span>盘面</span>
        </template>

        <!-- 二级菜单：自选 -->
        <el-menu-item index="panmian-zixuan" class="level-2">
          <el-icon><Star /></el-icon>
          <span>自选</span>
        </el-menu-item>

        <!-- 二级菜单：股票池 -->
        <el-sub-menu index="panmian-gupiao" class="level-2">
          <template #title>
            <el-icon><Grid /></el-icon>
            <span>股票池</span>
          </template>
          <!-- 三级菜单：涨停雁阵图 -->
          <el-menu-item index="gupiao-yanzhen" class="level-3">
            <el-icon><Histogram /></el-icon>
            <span>涨停雁阵图</span>
          </el-menu-item>
        </el-sub-menu>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script>
import { FolderOpened, Star, Grid, Histogram } from '@element-plus/icons-vue';

export default {
  name: 'SidebarMenu',
  components: {
    FolderOpened,
    Star,
    Grid,
    Histogram
  },
  props: {
    defaultActive: {
      type: String,
      default: () => MENU_CONFIG.DEFAULT_ACTIVE
    }
  },
  emits: ['menu-select'],
  methods: {
    handleMenuSelect(index) {
      this.$emit('menu-select', index);
    }
  }
}
</script>

<style scoped>
.sidebar-menu {
  /* 核心修改1：移除100vh，改为auto自适应菜单高度 */
  height: auto;
  width: 260px;
  background-color: #f5f5f5;
  border-right: 1px solid #dcdfe6;
  box-sizing: border-box;
  overflow-x: hidden;
  margin: 0;
  padding: 10px 0;
  /* 可选：添加最小高度，避免菜单为空时过窄 */
  min-height: 100px;
}

/* 整体菜单基础样式 */
.el-menu-vertical {
  border-right: none !important;
  /* 核心修改2：菜单高度自适应 */
  height: auto;
  background-color: transparent !important;
  --el-menu-bg-color: transparent !important;
  --el-menu-hover-bg-color: transparent !important;
  --el-menu-active-color: #409EFF !important;
}

/* 核心：统一菜单项容器样式（保留缩进，背景透明） */
.el-menu :deep(.el-menu-item),
.el-menu :deep(.el-sub-menu__title) {
  height: 40px;
  line-height: 40px;
  font-size: 14px;
  margin: 4px 8px !important;
  border-radius: 4px;
  transition: all 0.3s ease;
  box-sizing: border-box;
  background-color: transparent !important;
  padding: 0 !important;
  position: relative;
}

/* 一级菜单：盘面 - 最靠左 */
.el-menu :deep(.level-1 > .el-sub-menu__title) {
  padding-left: 15px !important;
  color: #333 !important;
}

/* 二级菜单：自选、股票池 - 比一级多缩进30px */
.el-menu :deep(.level-2) {
  &.el-menu-item,
  &.el-sub-menu > .el-sub-menu__title {
    padding-left: 45px !important;
    color: #555 !important;
  }
}

/* 三级菜单：涨停雁阵图 - 比二级多缩进30px */
.el-menu :deep(.level-3.el-menu-item) {
  padding-left: 75px !important;
  color: #666 !important;
}

/* 核心修改：菜单项内容容器（仅包裹文字，控制背景宽度） */
.el-menu :deep(.el-menu-item),
.el-menu :deep(.el-sub-menu__title) {
  & > span {
    display: inline-block;
    padding: 0 10px;
    border-radius: 4px;
    height: 40px;
    line-height: 40px;
    transition: all 0.3s ease;
  }
  & > .el-icon {
    margin-right: 8px;
    vertical-align: middle;
  }
}

/* 选中项样式（背景仅覆盖文字+少量边距） */
.el-menu :deep(.el-menu-item.is-active) > span {
  background-color: #e6f7ff !important;
  color: #409EFF !important;
  font-weight: 500;
}

/* 悬停效果（和选中样式一致，仅覆盖文字区域） */
.el-menu :deep(.el-menu-item:hover > span),
.el-menu :deep(.el-sub-menu__title:hover > span) {
  background-color: #e6f7ff !important;
  color: #409EFF !important;
}

/* 子菜单箭头位置（固定在右侧，不干扰缩进视觉） */
.el-menu :deep(.el-sub-menu__icon-arrow) {
  right: 15px !important;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  position: absolute;
  z-index: 1;
}
</style>