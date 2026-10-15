import type { Component } from 'vue'
import HeroSection from './HeroSection.vue'
import MetricsSection from './MetricsSection.vue'
import BizCardsSection from './BizCardsSection.vue'
import SolutionsSection from './SolutionsSection.vue'
import OutcomesSection from './OutcomesSection.vue'
import LogosWallSection from './LogosWallSection.vue'
import NewsSection from './NewsSection.vue'
import CtaSection from './CtaSection.vue'

/**
 * 模块组件注册表：section.type → 组件。
 *
 * 前向兼容：builder 页数据里出现未注册的 type 时渲染层跳过该模块，
 * 旧数据不受新模块上线影响；新模块上线只需在此注册。
 */
export const SECTION_REGISTRY: Record<string, Component> = {
  hero: HeroSection,
  metrics: MetricsSection,
  'biz-cards': BizCardsSection,
  solutions: SolutionsSection,
  outcomes: OutcomesSection,
  logos: LogosWallSection,
  news: NewsSection,
  cta: CtaSection
}

export interface SiteSection {
  type: string
  config?: Record<string, any>
  visible?: boolean
}

/** 解析 zhi_page.sections（JSON 字符串），过滤不可见/未注册的模块 */
export function parseSections(raw: unknown): (SiteSection & { component: Component })[] {
  let list: unknown = raw
  if (typeof raw === 'string') {
    try {
      list = JSON.parse(raw)
    } catch {
      list = []
    }
  }
  if (!Array.isArray(list)) return []
  return (list as SiteSection[])
    .filter(s => s && typeof s.type === 'string' && s.visible !== false && SECTION_REGISTRY[s.type])
    .map(s => ({ ...s, component: SECTION_REGISTRY[s.type] }))
}
