<template>
  <svg :class="svgClass" aria-hidden="true">
    <use :xlink:href="iconName" :fill="color" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  iconClass: string
  className?: string
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  className: '',
  color: ''
})

/**
 * 图标缺失时的兜底：菜单/路由里配了不存在的图标名时，`<use>` 会渲染成空白，
 * 侧栏会出现一块看不见的占位。这里检查 symbol 是否注册，缺失则回退到通用图标。
 * 真正的错误仍由 `assets/icons/menu-icons.test.ts` 在 CI 拦截。
 */
const iconName = computed(() => {
  if (!props.iconClass) return '#icon-component'
  const registered =
    typeof document !== 'undefined' && document.getElementById(`icon-${props.iconClass}`)
  return registered ? `#icon-${props.iconClass}` : '#icon-component'
})
const svgClass = computed(() => {
  if (props.className) {
    return `svg-icon ${props.className}`
  }
  return 'svg-icon'
})
</script>

<style scope lang="scss">
.sub-el-icon,
.nav-icon {
  display: inline-block;
  font-size: 15px;
  margin-right: 12px;
  position: relative;
}

.svg-icon {
  width: 1em;
  height: 1em;
  position: relative;
  fill: currentColor;
  vertical-align: -2px;
}
</style>
