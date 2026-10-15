<template>
  <nav class="site-crumb" aria-label="面包屑">
    <!-- 桌面/平板：完整链路（调用方保证 ≤3 级） -->
    <ol class="crumb-list">
      <li v-for="(item, i) in items" :key="`${item.label}-${i}`">
        <router-link v-if="item.to && i < items.length - 1" :to="item.to">
          {{ item.label }}
        </router-link>
        <span v-else class="crumb-current" aria-current="page">{{ item.label }}</span>
        <i v-if="i < items.length - 1" aria-hidden="true">/</i>
      </li>
    </ol>
    <!-- 移动端（方案文档 §5.2）：仅显示当前页 + 返回 -->
    <router-link v-if="parent?.to" class="crumb-back" :to="parent.to">
      <span aria-hidden="true">←</span>
      返回{{ parent.label }}
    </router-link>
    <span class="crumb-current crumb-inline">{{ current?.label }}</span>
  </nav>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue'
import { removeJsonLd, setJsonLd } from '@/utils/jsonld'

/** 面包屑层级：最后一项为当前页（无 to），其余项应带 to（父级链接） */
const props = defineProps<{
  items: Array<{ label: string; to?: string }>
}>()

const parent = computed(() =>
  props.items.length > 1 ? props.items[props.items.length - 2] : undefined
)
const current = computed(() =>
  props.items.length ? props.items[props.items.length - 1] : undefined
)

// 面包屑同步输出 BreadcrumbList（方案文档 §6.2），随组件卸载移除
watch(
  () => props.items,
  items => {
    if (!items.length) {
      removeJsonLd('zh-jsonld-breadcrumb')
      return
    }
    const origin = window.location.origin
    setJsonLd('zh-jsonld-breadcrumb', {
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: items.map((item, i) => ({
        '@type': 'ListItem',
        position: i + 1,
        name: item.label,
        item: item.to ? `${origin}${item.to}` : window.location.href
      }))
    })
  },
  { immediate: true, deep: true }
)

onBeforeUnmount(() => {
  removeJsonLd('zh-jsonld-breadcrumb')
})
</script>

<style scoped>
.site-crumb {
  font-size: var(--mo-fs-sm);
  color: var(--mo-n500);
}

.crumb-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.crumb-list li {
  display: inline-flex;
  align-items: center;
  min-width: 0;
}

.crumb-list i {
  font-style: normal;
  margin: 0 var(--mo-sp-2);
  opacity: 0.55;
}

.crumb-list a {
  position: relative;
  color: var(--mo-n500);
  text-decoration: none;
  transition: color 0.2s;
}

/* 触控目标 ≥44px（方案文档 §5.3）：透明内边距扩展，视觉尺寸不变 */
.crumb-list a::after {
  content: '';
  position: absolute;
  inset: -12px -8px;
}

.crumb-list a:hover {
  color: var(--mo-p700);
}

html.dark .crumb-list a:hover {
  color: var(--mo-p300);
}

.crumb-current {
  color: var(--mo-n600);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 42vw;
}

.crumb-back,
.crumb-inline {
  display: none;
}

@media (max-width: 767px) {
  .crumb-list {
    display: none;
  }

  .site-crumb {
    display: flex;
    align-items: center;
    gap: var(--mo-sp-3);
    min-width: 0;
  }

  .crumb-back {
    display: inline-flex;
    align-items: center;
    min-height: 44px;
    flex: none;
    color: var(--mo-n500);
    text-decoration: none;
  }

  .crumb-inline {
    display: inline-block;
    flex: 1;
    min-width: 0;
    max-width: none;
  }
}
</style>
