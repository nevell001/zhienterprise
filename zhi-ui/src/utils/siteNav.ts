/**
 * 站点前台顶部导航的组装规则。
 *
 * 导航由「功能开关 + 首页模式 + 自定义页面」共同决定：home_page_slug 非空时
 * 首页被自定义页面接管，文章列表迁到 /news，导航补一个「文章」入口；
 * nav_category_enabled / nav_tag_enabled / nav_archive_enabled 控制分类/标签/归档三个入口
 * （只作用于本函数组装的经典导航；企业官网模式的导航来自「导航管理」表驱动，这三个开关不参与）。
 * 抽成纯函数以便单测直接断言结果。
 */

export interface NavMenuItem {
  name: string
  path: string
}

export interface NavPageItem {
  title?: string
  slug?: string
  showInNav?: string
}

export interface BuildNavMenusOptions {
  /** 首页是否已被自定义页面接管（home_page_slug 非空） */
  customHomePage: boolean
  guestbookEnabled: boolean
  categoryEnabled: boolean
  tagEnabled: boolean
  archiveEnabled: boolean
  /** 企业官网模板模式（home_layout=business），导航补「核心业务」入口 */
  businessEnabled?: boolean
  /** 核心业务栏目名称（business_label，缺省「核心业务」） */
  businessLabel?: string
  /** 企业官网模板模式下补「联系我们」入口 */
  contactEnabled?: boolean
  pages?: NavPageItem[]
}

/** 自定义页面的前台访问路径（与 router/site.ts 的 /page/:slug 一致） */
export function pagePath(slug: string): string {
  return `/page/${slug}`
}

/** 从站内路径反解自定义页面 slug；非 /page/ 路径返回空串 */
export function pageSlugFromPath(link: unknown): string {
  if (typeof link !== 'string') return ''
  const match = /^\/page\/([A-Za-z0-9_-]+)\/?$/.exec(link.trim())
  return match ? match[1] : ''
}

/** 企业官网模式判定：home_page_slug 非空即首页被自定义页面接管 */
export function hasCustomHomePage(slug: unknown): boolean {
  return typeof slug === 'string' && slug.trim() !== ''
}

/** 导航树节点（管理侧与联动操作共用的最小形状） */
export interface NavTreeNode {
  id?: number
  parentId?: number
  label?: string
  linkType?: string
  linkValue?: string
  sort?: number
  children?: NavTreeNode[]
}

/** 平铺导航树（含二级项），联动查找用 */
export function flattenNavTree(items: NavTreeNode[]): NavTreeNode[] {
  const out: NavTreeNode[] = []
  const walk = (list: NavTreeNode[]) => {
    list.forEach(item => {
      out.push(item)
      if (item.children?.length) walk(item.children)
    })
  }
  walk(items)
  return out
}

/** 导航树中指向该页面的全部项（含二级项与重复项） */
export function navItemsOfPage(items: NavTreeNode[], slug: string): NavTreeNode[] {
  if (!slug) return []
  return flattenNavTree(items).filter(item => pageSlugFromPath(item.linkValue) === slug)
}

/** 顶级项下一个可用排序号（追加到末尾） */
export function nextTopSort(items: NavTreeNode[]): number {
  const tops = items.filter(item => !item.parentId)
  return Math.max(0, ...tops.map(item => item.sort ?? 0)) + 1
}

export function buildNavMenus(options: BuildNavMenusOptions): NavMenuItem[] {
  const items: NavMenuItem[] = [{ name: '首页', path: '/' }]

  if (options.customHomePage) {
    items.push({ name: '文章', path: '/news' })
  }
  if (options.businessEnabled) {
    items.push({ name: options.businessLabel || '核心业务', path: '/business' })
  }
  if (options.contactEnabled) {
    items.push({ name: '联系我们', path: '/contact' })
  }
  if (options.categoryEnabled) {
    items.push({ name: '分类', path: '/category' })
  }
  if (options.tagEnabled) {
    items.push({ name: '标签', path: '/tag' })
  }
  if (options.archiveEnabled) {
    items.push({ name: '归档', path: '/archive' })
  }
  if (options.guestbookEnabled) {
    items.push({ name: '留言板', path: '/guestbook' })
  }
  ;(options.pages || []).forEach(page => {
    if (page?.showInNav === '1' && page.slug) {
      items.push({ name: page.title || page.slug, path: pagePath(page.slug) })
    }
  })

  return items
}
