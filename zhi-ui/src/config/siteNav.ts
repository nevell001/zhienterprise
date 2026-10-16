import type { SiteNavItem } from '@/api/site/nav'

/**
 * 企业官网默认导航（设计稿 §1.1）。
 *
 * 仅当后台「导航管理」无数据（未跑迁移脚本的老库）或接口异常时作为回退；
 * 正常情况下导航完全由 site_nav_item 表驱动。
 */

type NavLinkType = 'path' | 'url' | 'cta'

function item(
  label: string,
  linkValue: string,
  linkType: NavLinkType = 'path',
  children?: SiteNavItem[]
): SiteNavItem {
  return {
    label,
    linkType,
    linkValue,
    visible: 1,
    ...(children ? { children } : {})
  }
}

export const DEFAULT_HEADER_NAV: SiteNavItem[] = [
  item('首页', '/'),
  item('核心业务', '/business'),
  item('解决方案', '/solutions'),
  item('新闻动态', '/news'),
  item('关于我们', '/about', 'path', [
    item('企业简介', '/about'),
    item('荣誉资质', '/honors'),
    item('发展历程', '/history')
  ]),
  item('联系我们', '/contact'),
  item('咨询合作', '/contact', 'cta')
]

export const DEFAULT_FOOTER_NAV: SiteNavItem[] = [
  item('首页', '/'),
  item('核心业务', '/business'),
  item('解决方案', '/solutions'),
  item('新闻动态', '/news'),
  item('关于我们', '/about')
]
