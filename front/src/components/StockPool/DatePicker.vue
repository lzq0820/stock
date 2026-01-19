<template>
  <div class="date-picker-container">
    <!-- 可基于 Element Plus/UI 的 DatePicker 封装，示例用 Element Plus -->
    <el-date-picker
        v-model="selectedDate"
        type="date"
        placeholder="选择日期"
        @change="handleDateChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElDatePicker } from 'element-plus';

// 定义日期绑定值
const selectedDate = ref<string | null>(null);
// 日期变更事件
const handleDateChange = (date: string | null) => {
  console.log('选中的日期：', date);
  // 可通过 emit 向外传递日期
  // emit('date-change', date);
};

// 若需要对外暴露事件/属性，可补充 defineEmits/defineProps
// const emit = defineEmits<{
//   'date-change': [date: string | null]
// }>();
</script>

<style scoped>
.date-picker-container {
  /* 自定义样式 */
}
</style>