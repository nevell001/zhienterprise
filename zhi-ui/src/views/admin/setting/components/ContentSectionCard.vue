<template>
  <section class="content-section">
    <header class="content-section__head">
      <span class="content-section__icon">
        <el-icon><component :is="icon" /></el-icon>
      </span>
      <div class="content-section__heading">
        <h4 class="content-section__title">{{ title }}</h4>
        <p v-if="desc" class="content-section__desc">{{ desc }}</p>
      </div>
      <div class="content-section__meta">
        <span class="content-section__count">{{ count }} 项</span>
        <slot name="actions"></slot>
        <el-button
          v-if="collapsible !== false"
          link
          class="content-section__toggle"
          :title="collapsed ? '展开' : '收起'"
          :aria-expanded="!collapsed"
          @click="collapsed = !collapsed"
        >
          <el-icon>
            <ArrowDown v-if="collapsed" />
            <ArrowUp v-else />
          </el-icon>
        </el-button>
      </div>
    </header>
    <div v-show="!collapsed" class="content-section__body">
      <slot></slot>
    </div>
  </section>
</template>

<script setup lang="ts" name="ContentSectionCard">
import { ref, type Component } from 'vue'
import { ArrowDown, ArrowUp } from '@element-plus/icons-vue'

defineProps<{
  title: string
  desc?: string
  icon: Component
  count: number
  collapsible?: boolean
}>()

const collapsed = ref(false)
</script>

<style scoped>
.content-section {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-bg-color);
  overflow: hidden;
}

.content-section + .content-section {
  margin-top: 14px;
}

.content-section__head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.content-section__icon {
  flex: none;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 12%, transparent);
  font-size: 18px;
}

.content-section__heading {
  flex: 1;
  min-width: 0;
}

.content-section__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.content-section__desc {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}

.content-section__meta {
  flex: none;
  display: flex;
  align-items: center;
  gap: 6px;
}

.content-section__count {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color);
}

.content-section__toggle {
  color: var(--el-text-color-secondary);
}

.content-section__body {
  padding: 14px 16px 16px;
}
</style>
