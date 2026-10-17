<template>
  <div class="content-editor">
    <div v-for="(item, index) in items" :key="index" class="content-editor__row">
      <div class="content-editor__fields">
        <label
          v-for="field in fields"
          :key="field.prop"
          class="content-field"
          :style="{ gridColumn: `span ${field.span ?? 12}` }"
        >
          <span class="content-field__label">{{ field.label }}</span>
          <el-input
            v-if="field.type === 'textarea'"
            v-model="item[field.prop]"
            type="textarea"
            :rows="field.rows ?? 2"
            :placeholder="field.placeholder"
          />
          <el-input v-else v-model="item[field.prop]" :placeholder="field.placeholder" />
        </label>
      </div>
      <div class="content-editor__ops">
        <el-button
          link
          :disabled="index === 0"
          title="上移"
          aria-label="上移"
          @click="emit('move', index, -1)"
        >
          <el-icon><Top /></el-icon>
        </el-button>
        <el-button
          link
          :disabled="index === items.length - 1"
          title="下移"
          aria-label="下移"
          @click="emit('move', index, 1)"
        >
          <el-icon><Bottom /></el-icon>
        </el-button>
        <el-button link type="danger" title="删除" aria-label="删除" @click="emit('remove', index)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>

    <p v-if="!items.length" class="content-editor__empty">
      暂无内容，前台会显示内置示例；可先添加一条。
    </p>

    <el-button class="content-editor__add" plain :icon="Plus" @click="emit('add')">
      {{ addLabel }}
    </el-button>
  </div>
</template>

<script setup lang="ts" name="ContentFieldsEditor">
import { Bottom, Delete, Plus, Top } from '@element-plus/icons-vue'
import type { ContentFieldSchema } from '../contentFields'

withDefaults(
  defineProps<{
    items?: Record<string, any>[]
    fields: ContentFieldSchema[]
    addLabel: string
  }>(),
  { items: () => [] }
)

const emit = defineEmits<{
  add: []
  remove: [index: number]
  move: [index: number, delta: number]
}>()
</script>

<style scoped>
.content-editor__row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
}

.content-editor__row + .content-editor__row {
  margin-top: 8px;
}

.content-editor__fields {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 8px 12px;
}

.content-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.content-field__label {
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.content-editor__ops {
  flex: none;
  display: flex;
  align-items: center;
  gap: 2px;
  padding-bottom: 1px;
}

.content-editor__empty {
  margin: 4px 0 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.content-editor__add {
  margin-top: 10px;
}

@media (max-width: 900px) {
  .content-editor__fields {
    grid-template-columns: 1fr;
  }

  .content-field {
    grid-column: 1 / -1 !important;
  }
}
</style>
