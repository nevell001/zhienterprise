import type { SiteTemplateShowcase } from '@/config/siteTemplates'
import { SITE_METRICS, SITE_OUTCOMES, SOLUTION_SHOWCASE } from '@/config/siteTemplates'

/**
 * 官网内容设置（JSON 设置键）的安全解析层。
 *
 * 7 个内容键由管理后台「官网内容」页签结构化维护（zhi_setting JSON 数组）：
 * site_metrics / site_outcomes / home_solutions / site_channels /
 * site_faq / site_honors / site_history。
 *
 * 解析原则：值缺失、非法 JSON、元素缺关键字段时一律回退内置占位
 * （设计稿附录 A），绝不让脏数据导致页面空白或报错。
 */

export interface SiteMetric {
  value: string
  label: string
}

export interface SiteOutcome {
  mark: string
  suffix: string
  label: string
}

export interface SiteChannel {
  name: string
  desc: string
}

export interface SiteFaq {
  q: string
  a: string
}

export interface SiteHonor {
  name: string
  issuer: string
  year: string
  cat: string
}

export interface SiteHistoryEvent {
  year: string
  title: string
  desc: string
  honor: boolean
}

export interface SiteHistoryStage {
  range: string
  name: string
  theme: string
  events: SiteHistoryEvent[]
}

/* ---------- 内置占位（设计稿附录 A；JSON 键缺失/损坏时的回退） ---------- */

export const DEFAULT_CHANNELS: SiteChannel[] = [
  { name: '商务合作', desc: '产品采购、方案报价与商务洽谈，请备注「商务合作」。' },
  { name: '技术支持', desc: '已交付项目的部署、运维与故障响应，请备注「技术支持」。' },
  { name: '渠道加盟', desc: '代理商与生态伙伴计划，请备注「渠道加盟」。' },
  { name: '媒体采访', desc: '品牌活动、采访与内容合作，请备注「媒体采访」。' }
]

export const DEFAULT_FAQS: SiteFaq[] = [
  {
    q: '咨询后多久能得到回复？',
    a: '工作时间内我们将在 1 个工作日内与您联系；紧急事项建议直接电话联系。'
  },
  {
    q: '是否支持上门调研与定制方案？',
    a: '支持。我们会先进行线上初诊，再根据情况安排上门调研并输出可执行的方案与报价。'
  },
  {
    q: '合作流程是怎样的？',
    a: '需求沟通 → 方案与报价 → 合同签订 → 实施交付 → 验收与长期陪跑，全程有专属对接人。'
  },
  {
    q: '如何验证贵司的资质与案例？',
    a: '可在「关于我们 → 荣誉资质」查看证书与奖项，「核心业务」中查看典型客户案例。'
  }
]

export const DEFAULT_HONORS: SiteHonor[] = [
  { name: '高新技术企业证书', issuer: '省科学技术厅', year: '2023', cat: '资质证书' },
  { name: '软件企业认定证书', issuer: '省工业和信息化厅', year: '2022', cat: '资质证书' },
  { name: '行业年度解决方案奖', issuer: '行业峰会组委会', year: '2024', cat: '行业奖项' },
  { name: '客户满意度金奖', issuer: '行业协会', year: '2023', cat: '行业奖项' },
  { name: 'ISO 9001 质量管理体系', issuer: '认证机构', year: '2021', cat: '体系认证' },
  { name: 'ISO 27001 信息安全管理体系', issuer: '认证机构', year: '2024', cat: '体系认证' }
]

export const DEFAULT_STAGES: SiteHistoryStage[] = [
  {
    range: '2011 - 2014',
    name: '初创期',
    theme: '找准方向，活下来',
    events: [
      {
        year: '2011',
        title: '公司成立',
        desc: '核心团队组建，确立深耕行业信息化的发展方向。',
        honor: false
      },
      {
        year: '2012',
        title: '首个标杆客户',
        desc: '签约第一家行业头部客户，产品完成从 0 到 1 的验证。',
        honor: false
      },
      {
        year: '2014',
        title: '获得高新技术企业认定',
        desc: '研发投入与自主知识产权获得官方认可。',
        honor: true
      }
    ]
  },
  {
    range: '2015 - 2019',
    name: '成长期',
    theme: '打磨产品，规模复制',
    events: [
      {
        year: '2015',
        title: '产品线成型',
        desc: '三大产品线发布，完成从项目交付到标准产品的转型。',
        honor: false
      },
      {
        year: '2017',
        title: '服务客户突破 500 家',
        desc: '建立全国服务网络，交付与售后体系全面标准化。',
        honor: false
      },
      {
        year: '2019',
        title: '通过 ISO 9001 认证',
        desc: '质量管理体系与国际标准接轨。',
        honor: true
      }
    ]
  },
  {
    range: '2020 - 2023',
    name: '领先期',
    theme: '行业头部，方案引领',
    events: [
      {
        year: '2020',
        title: '行业解决方案体系发布',
        desc: '覆盖制造、金融、零售、医疗四大行业的方案矩阵成型。',
        honor: false
      },
      {
        year: '2022',
        title: '服务客户突破 1200 家',
        desc: '续约率与客户满意度连续三年提升。',
        honor: false
      },
      {
        year: '2023',
        title: '斩获行业年度解决方案奖',
        desc: '头部客户的联合实践获得行业认可。',
        honor: true
      }
    ]
  },
  {
    range: '2024 -',
    name: '生态期',
    theme: '开放共建，长期主义',
    events: [
      {
        year: '2024',
        title: '通过 ISO 27001 认证',
        desc: '信息安全管理体系升级，为数据类业务护航。',
        honor: true
      },
      {
        year: '2025',
        title: '合作伙伴计划启动',
        desc: '与上下游厂商共建交付与培训生态。',
        honor: false
      }
    ]
  }
]

/* ---------- 解析 ---------- */

function parseArray<T>(raw: unknown, fallback: T[], isValid: (item: T) => boolean): T[] {
  // 回退一律返回深拷贝：调用方修改解析结果不得污染内置占位
  if (typeof raw !== 'string' || !raw.trim()) return structuredClone(fallback)
  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return structuredClone(fallback)
    const items = parsed.filter(isValid)
    return items.length > 0 ? items : structuredClone(fallback)
  } catch {
    return structuredClone(fallback)
  }
}

const str = (v: unknown) => (typeof v === 'string' ? v.trim() : '')

export function getSiteMetrics(raw: unknown): SiteMetric[] {
  return parseArray<SiteMetric>(
    raw,
    [...SITE_METRICS],
    i => Boolean(str(i?.value)) && Boolean(str(i?.label))
  )
}

export function getSiteOutcomes(raw: unknown): SiteOutcome[] {
  return parseArray<SiteOutcome>(
    raw,
    [...SITE_OUTCOMES],
    i => Boolean(str(i?.mark)) && Boolean(str(i?.label))
  )
}

export function getHomeSolutions(raw: unknown): SiteTemplateShowcase[] {
  return parseArray<SiteTemplateShowcase>(
    raw,
    [...SOLUTION_SHOWCASE],
    i => Boolean(str(i?.title)) && Boolean(str(i?.desc))
  )
}

export function getSiteChannels(raw: unknown): SiteChannel[] {
  return parseArray<SiteChannel>(
    raw,
    DEFAULT_CHANNELS,
    i => Boolean(str(i?.name)) && Boolean(str(i?.desc))
  )
}

export function getSiteFaqs(raw: unknown): SiteFaq[] {
  return parseArray<SiteFaq>(raw, DEFAULT_FAQS, i => Boolean(str(i?.q)) && Boolean(str(i?.a)))
}

export function getSiteHonors(raw: unknown): SiteHonor[] {
  return parseArray<SiteHonor>(
    raw,
    DEFAULT_HONORS,
    i => Boolean(str(i?.name)) && Boolean(str(i?.cat))
  )
}

export function getSiteHistoryStages(raw: unknown): SiteHistoryStage[] {
  return parseArray<SiteHistoryStage>(
    raw,
    DEFAULT_STAGES,
    stage =>
      Boolean(str(stage?.name)) &&
      Array.isArray(stage?.events) &&
      stage.events.length > 0 &&
      stage.events.every(e => Boolean(str(e?.title)))
  )
}
