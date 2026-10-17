<template>
  <div
    v-if="images.length > 0"
    class="banner-carousel"
    :class="{ 'is-single': images.length === 1 }"
    role="region"
    aria-roledescription="carousel"
    :aria-label="label"
    @mouseenter="pause"
    @mouseleave="resume"
    @focusin="pause"
    @focusout="resume"
  >
    <div
      class="bc-track"
      @touchstart.passive="onTouchStart"
      @touchend.passive="onTouchEnd"
      @keydown.left.prevent="prev"
      @keydown.right.prevent="next"
    >
      <img
        v-for="(src, index) in images"
        :key="`${index}-${src}`"
        :src="src"
        :alt="`${label} ${index + 1}`"
        class="bc-slide"
        :class="{ 'is-active': index === activeIndex }"
        :loading="index === 0 ? 'eager' : 'lazy'"
        decoding="async"
        :style="{ objectFit: fits[index] || 'cover' }"
        :aria-hidden="index !== activeIndex"
        @load="onImageLoad($event, index)"
      />
    </div>

    <template v-if="images.length > 1">
      <button type="button" class="bc-nav bc-prev" aria-label="上一张" @click.stop="prev">
        <span aria-hidden="true">‹</span>
      </button>
      <button type="button" class="bc-nav bc-next" aria-label="下一张" @click.stop="next">
        <span aria-hidden="true">›</span>
      </button>
      <div class="bc-dots" role="tablist" :aria-label="`${label}切换`">
        <button
          v-for="(src, index) in images"
          :key="`dot-${index}`"
          type="button"
          class="bc-dot"
          :class="{ 'is-active': index === activeIndex }"
          role="tab"
          :aria-selected="index === activeIndex"
          :aria-label="`第 ${index + 1} 张`"
          @click.stop="go(index)"
        ></button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { shouldSkipMotion } from '@/utils/motionPreference'
import { pickBannerFit } from '@/utils/bannerFit'

/**
 * 首页 Banner 轮播。
 *
 * - 1 张：直接展示，不出现任何轮播控件；
 * - 多张：自动轮播（默认 5s）、悬停/聚焦暂停、圆点 + 左右箭头、触摸滑动；
 * - 尊重「减弱动效」（页脚手动开关 / 系统 prefers-reduced-motion）：不自动播放，
 *   仅保留手动切换（方案文档 §4.7 动效降级约定）。
 */
const props = withDefaults(
  defineProps<{
    /** 已解析为站内相对路径的图片列表（见 utils/uploadUrl#parseBannerUrls） */
    images: string[]
    /** 无障碍标签前缀，同时用于图片 alt */
    label?: string
    /** 自动轮播间隔（毫秒） */
    interval?: number
  }>(),
  { label: '首页 Banner', interval: 5000 }
)

const activeIndex = ref(0)
const paused = ref(false)
/** 每张图的 object-fit：宽幅/竖幅差异过大时用 contain 避免裁掉画面文字 */
const fits = ref<Array<'cover' | 'contain'>>([])

function onImageLoad(event: Event, index: number) {
  const img = event.target as HTMLImageElement | null
  const track = (img?.parentElement as HTMLElement | null) ?? null
  const boxRatio = track && track.clientHeight > 0 ? track.clientWidth / track.clientHeight : 16 / 9
  const imageRatio =
    img && img.naturalWidth > 0 ? img.naturalWidth / Math.max(img.naturalHeight, 1) : 0
  fits.value[index] = pickBannerFit(imageRatio, boxRatio)
}
let timer: ReturnType<typeof setInterval> | undefined
let touchStartX: number | null = null

const clearTimer = () => {
  if (timer) {
    clearInterval(timer)
    timer = undefined
  }
}

const startTimer = () => {
  clearTimer()
  if (props.images.length <= 1) return
  timer = setInterval(
    () => {
      // 每轮都重新判断：用户在页脚切换「减弱动效」后立即停止自动播放
      if (paused.value || shouldSkipMotion()) return
      activeIndex.value = (activeIndex.value + 1) % props.images.length
    },
    Math.max(1500, props.interval)
  )
}

const go = (index: number) => {
  const total = props.images.length
  if (total === 0) return
  activeIndex.value = ((index % total) + total) % total
}

const next = () => go(activeIndex.value + 1)
const prev = () => go(activeIndex.value - 1)

const pause = () => {
  paused.value = true
}
const resume = () => {
  paused.value = false
}

const onTouchStart = (event: TouchEvent) => {
  touchStartX = event.touches[0]?.clientX ?? null
}

const onTouchEnd = (event: TouchEvent) => {
  if (touchStartX === null) return
  const endX = event.changedTouches[0]?.clientX ?? touchStartX
  const delta = endX - touchStartX
  touchStartX = null
  if (Math.abs(delta) < 40) return
  if (delta > 0) prev()
  else next()
}

watch(
  () => props.images.length,
  length => {
    if (activeIndex.value >= length) activeIndex.value = 0
    startTimer()
  }
)

onMounted(startTimer)
onBeforeUnmount(clearTimer)
</script>

<style scoped>
.banner-carousel {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: var(--tpl-surface, #f7f8fa);
}

.bc-track {
  position: relative;
  width: 100%;
  height: 100%;
}

.bc-slide {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}

.bc-slide.is-active {
  opacity: 1;
}

/* 单图不参与淡入切换（避免首屏闪一下） */
.banner-carousel.is-single .bc-slide {
  transition: none;
}

/* 左右箭头：半透明深底 + 白箭头，在任意图片上都有足够对比度 */
.bc-nav {
  position: absolute;
  top: 50%;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  color: var(--tpl-white, white);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  background: rgba(11, 15, 25, 0.42);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 50%;
  transform: translateY(-50%);
  transition:
    background 0.2s,
    opacity 0.2s;
}

.bc-nav:hover,
.bc-nav:focus-visible {
  background: rgba(11, 15, 25, 0.68);
}

.bc-prev {
  left: 12px;
}

.bc-next {
  right: 12px;
}

/* 圆点指示器 */
.bc-dots {
  position: absolute;
  bottom: 12px;
  left: 50%;
  z-index: 2;
  display: flex;
  gap: 8px;
  transform: translateX(-50%);
}

.bc-dot {
  width: 8px;
  height: 8px;
  padding: 0;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(11, 15, 25, 0.25);
  border-radius: 50%;
  transition:
    background 0.2s,
    width 0.2s;
}

.bc-dot.is-active {
  width: 20px;
  background: var(--tpl-white, white);
  border-radius: 999px;
}

.bc-dot:focus-visible,
.bc-nav:focus-visible {
  outline: 2px solid var(--tpl-white, white);
  outline-offset: 2px;
}

/* 减弱动效：取消淡入与过渡，只做瞬时切换 */
@media (prefers-reduced-motion: reduce) {
  .bc-slide {
    transition: none;
  }
}

:global(html.motion-off) .bc-slide {
  transition: none;
}

@media (max-width: 640px) {
  .bc-nav {
    width: 30px;
    height: 30px;
    font-size: 18px;
  }

  .bc-prev {
    left: 8px;
  }

  .bc-next {
    right: 8px;
  }

  .bc-dots {
    bottom: 8px;
  }
}
</style>
