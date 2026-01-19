<template>
  <div class="date-picker">
    <el-date-picker
        v-model="selectedDate"
        type="date"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        placeholder="选择日期"
        :clearable="false"
        :editable="false"
        :disabled-date="getDisabledDateHandler"
    :picker-options="pickerOptions"
    @change="onDateChange"
    @pick="onPick"
    />
  </div>
</template>

<script>
export default {
  name: 'DatePicker',
  props: {
    modelValue: {
      type: String,
      default: () => new Date().toISOString().split('T')[0]
    },
    holidays: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue'],
  computed: {
    today() {
      return new Date().toISOString().slice(0, 10);
    },
    selectedDate: {
      get() {
        return this.modelValue || this.today;
      },
      set(val) {
        this.$nextTick(() => {
          this.$emit('update:modelValue', val || this.today);
        });
      }
    },
    // 预处理holidayDates，确保格式纯净
    pureHolidayDates() {
      // 去空格+去重+标准化日期格式
      return this.holidays.map(h => {
        // 强制转字符串+去空格+标准化
        const dateStr = (h.holidayDate || '').trim().replace(/\s+/g, '');
        // 二次校验日期格式（避免非法字符）
        return /^\d{4}-\d{2}-\d{2}$/.test(dateStr) ? dateStr : '';
      }).filter(Boolean); // 过滤空值
    },
    pickerOptions() {
      const holidayDates = this.pureHolidayDates;
      return {
        disabledDate: (time) => {
          // 获取当前时间（不包含时间部分）
          const currentDate = new Date();
          currentDate.setHours(0, 0, 0, 0);

          // 禁用超过当前日期的日期
          return time.getTime() > currentDate.getTime();
        },
        cellClassName: ({ date, type }) => {
          if (type === 'day') {
            const dateStr = this.formatDate(date);
            const dayOfWeek = date.getDay();
            const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
            const holiday = this.holidays.find(h => this.formatDate(new Date(h.holidayDate)) === dateStr);

            if (isWeekend || (holiday && holiday.isHoliday === 1)) {
              return 'holiday-cell';
            }
          }
          return '';
        },
        shortcuts: []
      };
    }
  },
  methods: {
    // 统一日期格式化方法（避免格式差异）
    formatDate(date) {
      if (!(date instanceof Date)) date = new Date(date);
      // 手动格式化，避免toISOString的时区问题
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    // 改为方法，确保每次调用都获取最新的holidayDates
    getDisabledDateHandler(time) {
      const holidayDates = this.pureHolidayDates;
      const dateStr = this.formatDate(time);

      // 获取当前时间（不包含时间部分）
      const currentDate = new Date();
      currentDate.setHours(0, 0, 0, 0);

      // 检查日期是否超过当前日期
      const isFutureDate = time.getTime() > currentDate.getTime();

      // 检查是否是节假日
      const isHoliday = holidayDates.includes(dateStr);

      // 如果是未来日期，则禁用
      return isFutureDate || isHoliday;
    },
    onDateChange(value) {
      const finalValue = value || this.today;
      this.$emit('update:modelValue', finalValue);
    },
    onPick({ maxDate, minDate, date }) {
      const selectedDate = date ? this.formatDate(date) : this.today;
      this.$emit('update:modelValue', selectedDate);
    }
  }

}
</script>

<style scoped>
/* 样式保持不变 */
.date-picker {
  margin-bottom: 10px;
}

:deep(.holiday-cell) {
  background-color: #ffe6e6;
  color: #ff0000;
}

:deep(.el-date-picker__clear) {
  display: none !important;
}

:deep(.el-input__inner) {
  color: #333 !important;
  pointer-events: auto !important;
  readonly: true;
}

:deep(.el-input__placeholder) {
  display: none !important;
}

:deep(.el-input__inner):focus {
  cursor: default;
  user-select: none;
}
</style>