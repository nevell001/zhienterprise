/**
 * 博客前台顶部导航的组装规则。
 *
 * 导航由「功能开关 + 首页模式 + 自定义页面」共同决定：home_page_slug 非空时
 * 首页被自定义页面接管，文章列表迁到 /blog/articles，导航补一个「文章」入口；
 * nav_category_enabled / nav_tag_enabled / nav_archive_enabled 控制三个博客向入口
 * （企业官网场景在后台关掉即可裁剪导航）。抽成纯函数以便单测直接断言结果。
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
  pages?: NavPageItem[]
}

export function hasCustomHomePage(slug: unknown): boolean {
  return typeof slug === 'string' && slug.trim() !== ''
}

export function buildNavMenus(options: BuildNavMenusOptions): NavMenuItem[] {
  const items: NavMenuItem[] = [{ name: '首页', path: '/blog' }]

  if (options.customHomePage) {
    items.push({ name: '文章', path: '/blog/articles' })
  }
  if (options.categoryEnabled) {
    items.push({ name: '分类', path: '/blog/category' })
  }
  if (options.tagEnabled) {
    items.push({ name: '标签', path: '/blog/tag' })
  }
  if (options.archiveEnabled) {
    items.push({ name: '归档', path: '/blog/archive' })
  }
  if (options.guestbookEnabled) {
    items.push({ name: '留言板', path: '/blog/guestbook' })
  }
  ;(options.pages || []).forEach(page => {
    if (page?.showInNav === '1' && page.slug) {
      items.push({ name: page.title || page.slug, path: `/blog/page/${page.slug}` })
    }
  })

  return items
}
