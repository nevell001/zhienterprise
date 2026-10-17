<template>
  <header class="eh-hero">
    <div class="eh-wrap eh-hero-grid">
      <div class="rv">
        <span class="eyebrow">{{ hero.eyebrow }}</span>
        <!-- 分行错峰上浮（方案文档 §4.3 首屏标题分行）：titleHtml 按 <br> 拆行，
             每行为受控来源（内置模板配置或后台保存内容） -->
        <!-- eslint-disable-next-line vue/no-v-html -->
        <h1 class="eh-title">
          <span v-for="(line, li) in titleLines" :key="li" class="line" v-html="line"></span>
        </h1>
        <p class="eh-sub">{{ hero.subtitle }}</p>
        <div class="eh-cta">
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
          <router-link class="btn-s" :to="secondary.link">{{ secondary.label }}</router-link>
        </div>
      </div>
      <!-- 自定义 Banner 优先（站点设置 → 站点信息 → 首页 Banner），未配置回退模板母题插画 -->
      <div v-if="bannerUrls.length" class="hero-viz hero-banner rv d1">
        <BannerCarousel :images="bannerUrls" :label="`${settings.blog_name || '首页'} Banner`" />
      </div>
      <!-- eslint-disable-next-line vue/no-v-html -->
      <div v-else class="hero-viz rv d1" v-html="motifSvg"></div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteTemplate } from '@/config/siteTemplates'
import { useScrollReveal } from '@/composables/useScrollReveal'
import BannerCarousel from '@/components/BannerCarousel.vue'
import { parseBannerUrls } from '@/utils/uploadUrl'

/** config（模块配置）缺省字段回退当前行业模板的品牌文案包 */
const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const tpl = computed(() => getSiteTemplate(settings.value?.site_template))

const cfg = computed(() => props.config ?? {})
const hero = computed(() => ({
  eyebrow: str(cfg.value.eyebrow) || tpl.value.hero.eyebrow,
  titleHtml: str(cfg.value.titleHtml) || tpl.value.hero.titleHtml,
  subtitle: str(cfg.value.subtitle) || tpl.value.hero.subtitle
}))

/** 标题按 <br> 拆行（每行独立上浮，行内保留 <em> 强调） */
const titleLines = computed(() => hero.value.titleHtml.split(/<br\s*\/?>/i))
const primary = computed(() => ({
  label: str(cfg.value.ctaPrimary?.label) || '了解我们的业务',
  link: str(cfg.value.ctaPrimary?.link) || '/business'
}))
const secondary = computed(() => ({
  label: str(cfg.value.ctaSecondary?.label) || '预约咨询',
  link: str(cfg.value.ctaSecondary?.link) || '/contact'
}))

/** 首页 Banner 大图（站点设置上传）；为空时回退模板母题插画 */
// 首页 Banner 支持多图（逗号分隔）：单图直接展示，多图自动轮播
const bannerUrls = computed(() =>
  parseBannerUrls((settings.value as Record<string, unknown>)?.site_banner)
)

const motifSvg = computed(() => tpl.value.motif(tpl.value.color))

const str = (v: unknown) => (typeof v === 'string' ? v.trim() : '')
</script>
