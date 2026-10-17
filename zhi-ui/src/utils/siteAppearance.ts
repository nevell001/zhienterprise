import { SITE_TEMPLATE_CLASS_NAMES, type SiteTemplateId } from '@/config/siteTemplates'

/**
 * 企业官网外观类的挂载/摘除（<html> 上的 theme-enterprise / tpl-site / tpl-xxx）。
 *
 * 关键约束：关闭企业模板（home_layout 非 business）时必须把基类 **tpl-site** 一起移除。
 * 曾经只移除 tpl-xxx，导致经典站继续命中 `html.tpl-site` 的模板主色/变量映射，
 * 「主题颜色」在经典前台完全不生效。
 */
export function applySiteAppearanceClasses(
  root: DOMTokenList,
  enabled: boolean,
  tplId: SiteTemplateId
): void {
  root.toggle('theme-enterprise', enabled)
  SITE_TEMPLATE_CLASS_NAMES.forEach(className => root.remove(className))
  root.remove('tpl-site')
  if (enabled) {
    root.add('tpl-site', `tpl-${tplId}`)
  }
}
