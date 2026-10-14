<template>
  <div v-if="total > pageSize" class="pagination-container">
    <el-pagination
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="pageNum"
      @current-change="page => emit('page-change', page)"
    />
  </div>
</template>

<script setup lang="ts">
defineProps<{ total: number; pageSize: number; pageNum: number }>()
const emit = defineEmits<{ 'page-change': [page: number] }>()
</script>

<style lang="scss" scoped>
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: var(--mo-sp-8);
}
</style>
