<template>
  <div class="search-box" :style="{ width: stockBoxWidth }">
    <div class="search-and-filter">
      <input type="text" v-model="query" placeholder="搜索股票名或原因..." @input="handleInput" />
      <!-- 使用Element Plus的Switch组件 -->
      <div class="st-filter">
        <el-switch
            v-model="hideSt"
            size="small"
            inline-prompt
            :active-text="'隐藏ST票'"
            :inactive-text="'显示ST票'"
            :active-value="true"
            :inactive-value="false"
            @change="handleFilterChange"
        />
      </div>
      <!-- 新增清空按钮 -->
      <button v-if="query" class="clear-btn" @click="clearSearch" aria-label="清空搜索内容">
        ×
      </button>
    </div>
  </div>
</template>

<script>
import { ElSwitch } from 'element-plus';

export default {
  name: 'SearchBox',
  components: {
    ElSwitch
  },
  props: {
    stockBoxWidth: {
      type: String,
      default: '200px'
    }
  },
  emits: ['search', 'filter-change'],
  data() {
    return {
      query: '',
      hideSt: false, // 默认不隐藏ST票（即显示ST票）
      timer: null
    };
  },
  methods: {
    handleInput() {
      clearTimeout(this.timer);
      this.timer = setTimeout(() => {
        this.$emit('search', this.query);
      }, 500);
    },
    // 新增过滤条件变化处理
    handleFilterChange() {
      // 根据hideSt的值确定notShowSt的值：隐藏ST票(hideSt=true)时传1，否则传0
      const notShowSt = this.hideSt ? 1 : 0;
      this.$emit('filter-change', {
        query: this.query,
        notShowSt: notShowSt
      });
    },
    // 新增清空搜索的方法
    clearSearch() {
      // 清空输入框内容
      this.query = '';
      // 立即触发搜索（空内容）
      this.$emit('search', '');
      // 清理防抖定时器
      clearTimeout(this.timer);
    }
  },
  beforeUnmount() {
    clearTimeout(this.timer);
  }
}
</script>

<style scoped>
.search-box {
  margin-bottom: 10px;
  /* 改为相对定位，让清空按钮可以绝对定位 */
  position: relative;
}

.search-and-filter {
  display: flex;
  align-items: center;
  gap: 10px;
}

input[type="text"] {
  flex: 1;
  padding: 12px 15px;
  padding-right: 40px;
  /* 右侧预留空间给清空按钮 */
  box-sizing: border-box;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  min-height: 44px;
  font-size: 14px;
  line-height: 1.5;
}

input[type="text"]:focus {
  outline: none;
  border-color: #2196f3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.2);
}

input[type="text"]::placeholder {
  color: #999;
  opacity: 1;
}

/* ST过滤选项样式 */
.st-filter {
  display: flex;
  align-items: center;
  font-size: 14px;
  min-height: 44px;
  padding: 0 8px;
}

/* 清空按钮样式 */
.clear-btn {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: #999;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.clear-btn:hover {
  color: #333;
  background-color: #f5f5f5;
}

.clear-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.2);
}
</style>
