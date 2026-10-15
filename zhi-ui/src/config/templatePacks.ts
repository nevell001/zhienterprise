import type { SiteNavItem } from '@/api/site/nav'
import type { SiteTemplateId } from '@/config/siteTemplates'
import { SITE_TEMPLATES } from '@/config/siteTemplates'

/**
 * 行业模板包（方案文档 §2.6）：5 套「预置数据包」。
 *
 * 语义（与旧版运行时皮肤包的本质区别）：
 * 应用模板包 = 把包内的首页模块内容、导航、栏目名**导入为普通数据**，
 * 导入后管理员可随意修改；再应用另一包 = 再导入一次。新增行业只需新增包定义。
 */

export interface TemplatePackSection {
  type: string
  visible: boolean
  config: Record<string, any>
}

export interface TemplatePack {
  id: SiteTemplateId
  name: string
  /** 视觉预设类（.tpl-xxx，仍由 site_template 设置驱动挂载） */
  tplClass: string
  /** 首页模块序列（覆盖式导入到 slug=home 的 builder 页） */
  homeSections: TemplatePackSection[]
  headerNav: SiteNavItem[]
  footerNav: SiteNavItem[]
  settings: Record<string, string>
}

function navItem(
  label: string,
  linkValue: string,
  linkType: SiteNavItem['linkType'] = 'path',
  children?: SiteNavItem[]
): SiteNavItem {
  return { label, linkType, linkValue, visible: 1, ...(children ? { children } : {}) }
}

function bizNav(label: string, linkValue = '/business'): SiteNavItem {
  return navItem(label, linkValue)
}

/** 由行业模板配置派生的包序列（8 模块，与 builder 首页种子同构） */
function homeSectionsOf(id: SiteTemplateId): TemplatePackSection[] {
  const tpl = SITE_TEMPLATES.find(t => t.id === id)
  if (!tpl) return []
  return [
    {
      type: 'hero',
      visible: true,
      config: {
        eyebrow: tpl.hero.eyebrow,
        titleHtml: tpl.hero.titleHtml,
        subtitle: tpl.hero.subtitle,
        ctaPrimary: { label: `了解${tpl.biz.one.title}`, link: '/business' },
        ctaSecondary: { label: '预约咨询', link: '/contact' }
      }
    },
    { type: 'metrics', visible: true, config: {} },
    { type: 'biz-cards', visible: true, config: {} },
    {
      type: 'solutions',
      visible: true,
      config: { title: tpl.solutionsTitle }
    },
    { type: 'outcomes', visible: true, config: {} },
    { type: 'logos', visible: true, config: { count: 8 } },
    { type: 'news', visible: true, config: { count: 3 } },
    {
      type: 'cta',
      visible: true,
      config: { title: '把下一步交给我们', subtitle: '一个小时的免费诊断，换一份可执行的问题清单。' }
    }
  ]
}

function headerNavOf(id: SiteTemplateId): SiteNavItem[] {
  const tpl = SITE_TEMPLATES.find(t => t.id === id)
  const bizLabel = tpl ? `${tpl.biz.one.title.replace(/中心|服务.*/, '')}` : '核心业务'
  return [
    navItem('首页', '/'),
    bizNav(bizLabel, '/business'),
    navItem('新闻动态', '/news'),
    navItem('关于我们', '/about', 'path', [navItem('企业简介', '/about'), navItem('荣誉资质', '/honors'), navItem('发展历程', '/history')]),
    navItem('联系我们', '/contact'),
    navItem('咨询合作', '/contact', 'cta')
  ]
}

function aboutChildren(): SiteNavItem[] {
  return [navItem('企业简介', '/about'), navItem('荣誉资质', '/honors'), navItem('发展历程', '/history')]
}

function pack(
  id: SiteTemplateId,
  name: string,
  bizNavLabel: string
): TemplatePack {
  const tpl = SITE_TEMPLATES.find(t => t.id === id)
  const header = headerNavOf(id)
  const about = header.find(i => i.linkValue === '/about')
  if (about) about.children = aboutChildren()
  return {
    id,
    name,
    tplClass: `tpl-${id}`,
    homeSections: homeSectionsOf(id),
    headerNav: header,
    footerNav: [
      navItem('首页', '/'),
      bizNav(bizNavLabel, '/business'),
      navItem('新闻动态', '/news'),
      navItem('关于我们', '/about')
    ],
    settings: { business_label: tpl?.biz.one.title === '咨询中心' ? '咨询业务' : bizNavLabel }
  }
}

export const TEMPLATE_PACKS: TemplatePack[] = [
  pack('consult', '管理咨询', '咨询业务'),
  pack('tech', '信息技术', '核心业务'),
  pack('mfg', '制造业', '制造业务'),
  pack('trade', '商贸', '供应链业务'),
  pack('digital', '数码产品', '产品中心')
]

export function getTemplatePack(id: unknown): TemplatePack {
  return TEMPLATE_PACKS.find(p => p.id === id) ?? TEMPLATE_PACKS[0]
}
