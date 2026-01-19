<template>
  <div class="icon-switch-container">
    <!-- 基于 Element Plus 的 Switch 结合 Icon 封装 -->
    <el-switch
        v-model="switchValue"
        :active-icon="ActiveIcon"
        :inactive-icon="InactiveIcon"
        @change="handleSwitchChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElSwitch, ElIcon } from 'element-plus';
// 示例图标（需安装 @element-plus/icons-vue）
import { Check, Close } from '@element-plus/icons-vue';

// 开关绑定值
const switchValue = ref<boolean>(false);
// 激活/未激活图标
const ActiveIcon = Check;
const InactiveIcon = Close;

// 开关变更事件
const handleSwitchChange = (value: boolean) => {
  console.log('开关状态：', value);
  // emit('switch-change', value);
};

// 对外暴露事件
// const emit = defineEmits<{
//   'switch-change': [value: boolean]
// }>();
</script>

<style scoped>
.icon-switch-container {
  /* 自定义样式 */
}
</style>