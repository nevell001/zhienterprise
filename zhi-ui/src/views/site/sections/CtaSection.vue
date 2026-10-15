<template>
  <section class="cta pad">
    <svg class="cta-bg" viewBox="0 0 1440 400" preserveAspectRatio="none" aria-hidden="true">
      <g stroke="currentColor" stroke-width="1" fill="none">
        <path
          d="M0 80h1440M0 200h1440M0 320h1440M120 0v400M400 0v400M680 0v400M960 0v400M1240 0v400"
        />
      </g>
    </svg>
    <div class="eh-wrap eh-cta-inner">
      <h2 class="rv">{{ cfg.title || '把下一步交给我们' }}</h2>
      <p class="rv d1">{{ cfg.subtitle || '一个小时的免费诊断，换一份可执行的问题清单。' }}</p>
      <div class="rv d2 eh-cta-btns">
        <router-link class="btn-p" :to="primary.link">
          {{ primary.label }}
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
            <path
              d="M3 8h10M9 4l4 4-4 4"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </router-link>
        <router-link class="btn-d" :to="secondary.link">{{ secondary.label }}</router-link>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ config?: Record<string, any> }>()

const cfg = computed(() => props.config ?? {})
const primary = computed(() => ({
  label: cfg.value.primary?.label || '预约咨询',
  link: cfg.value.primary?.link || '/contact'
}))
const secondary = computed(() => ({
  label: cfg.value.secondary?.label || '了解我们',
  link: cfg.value.secondary?.link || '/about'
}))
</script>
