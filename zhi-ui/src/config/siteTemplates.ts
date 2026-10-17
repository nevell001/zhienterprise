/**
 * 企业官网 · 5 套行业模板注册表
 *
 * 设计出处：docs/design/企业官网UI-UX架构方案.md（规范基线）+
 * docs/design/templates/{themes.css,base.css,index.html}（两层分离原型：
 * base 共用组件层 + themes 变量层，模板差异只有三处——主色 / 字体 / 母题）。
 *
 * 本文件是"变量层 + 文案配置层"的 TS 侧实现；变量层的 CSS 侧在
 * assets/styles/themes/site-templates.scss（.tpl-site 基类 + .tpl-xxx 覆盖）。
 * 切换模板 = 站点设置 site_template + 前台 <html> 挂 .tpl-xxx 类，五套共用
 * 同一套首页组件（views/site/EnterpriseHome.vue），不需要动组件一行。
 *
 * 安全说明：hero.titleHtml 为仓库内置静态配置（非用户输入），可安全 v-html。
 */

export type SiteTemplateId = 'consult' | 'tech' | 'mfg' | 'trade' | 'digital'

export interface SiteTemplateShowcase {
  title: string
  desc: string
}

export interface SiteTemplate {
  /** 模板 id（设置键 site_template 的合法值） */
  id: SiteTemplateId
  /** 挂在 <html> 上的类名 */
  className: string
  /** 行业模板名 */
  name: string
  /** 适用行业说明（管理端下拉展示用） */
  industry: string
  /** 主色（管理端色点展示用，与 themes.css 一致） */
  color: string
  hero: {
    eyebrow: string
    /** 两行主标题，允许 <br> 与 <em>（内置静态配置） */
    titleHtml: string
    subtitle: string
  }
  /** 解决方案区块标题（随行业改：按行业/场景/工艺/业态解题） */
  solutionsTitle: string
  /** 核心业务两卡的标题与描述 */
  biz: {
    one: SiteTemplateShowcase
    two: SiteTemplateShowcase
    /** 无业务数据时的列表项占位（设计稿附录 A） */
    fallbackItems: string[]
  }
  /** 方案卡插画（SVG 内容片段，随模板换） */
  art: string
  /** 母题主视觉（Hero 右侧 SVG，参数为主色） */
  motif: (color: string) => string
}

/** Hero 信任指标条（设计稿附录 A 占位内容，不随模板变化） */
export const SITE_METRICS = [
  { value: '15', label: '年行业深耕' },
  { value: '1200+', label: '服务客户' },
  { value: '30+', label: '覆盖省份' },
  { value: '86', label: '技术专利' }
] as const

/** 数据成果（深色区块三项，设计稿占位内容） */
export const SITE_OUTCOMES = [
  { mark: '↓38', suffix: '%', label: '平均运营成本下降幅度' },
  { mark: '↑2.4', suffix: '×', label: '方案交付效率提升倍数' },
  { mark: '99.9', suffix: '%', label: '平台服务可用性' }
] as const

/** 解决方案精选四卡（行业占位，方案卡插画随模板变化） */
export const SOLUTION_SHOWCASE: SiteTemplateShowcase[] = [
  { title: '制造业数字化', desc: '打通产线数据与管理系统，让产能、质量、成本可视可控。' },
  { title: '金融服务', desc: '满足合规与风控要求的数据治理与业务连续性方案。' },
  { title: '零售连锁', desc: '整合线上线下会员与订单数据，支撑精准运营决策。' },
  { title: '医疗健康', desc: '面向机构与患者两端的信息流转与数据安全体系。' }
]

/* ---------- 母题 SVG（移植自原型 index.html MOTIFS） ---------- */

const MOTIF_CONSULT = (c: string) =>
  `<svg viewBox="0 0 500 500" fill="none" role="img" aria-label="指针罗盘母题">` +
  `<circle cx="250" cy="250" r="170" stroke="#E5E7EB" stroke-width="1"/>` +
  `<circle cx="250" cy="250" r="120" stroke="#E5E7EB" stroke-width="1"/>` +
  `<circle cx="250" cy="250" r="70" stroke="#E5E7EB" stroke-width="1"/>` +
  `<g stroke="#E5E7EB" stroke-width="1"><path d="M250 30v440M30 250h440"/>` +
  `<path d="M95 95l310 310M405 95L95 405" stroke-dasharray="2 8"/></g>` +
  `<path d="M250 250L360 140" stroke="${c}" stroke-width="3" stroke-linecap="round"/>` +
  `<circle cx="250" cy="250" r="12" fill="${c}"/><circle cx="250" cy="250" r="5" fill="#fff"/>` +
  `<path d="M250 74v26M250 400v26M74 250h26M400 250h26" stroke="${c}" stroke-width="1.5" stroke-linecap="round"/>` +
  `<g stroke="${c}" stroke-width="1.5" fill="none" opacity=".5"><path d="M250 130a120 120 0 0 1 106 62"/></g>` +
  `<circle cx="356" cy="192" r="4" fill="${c}"/><circle cx="144" cy="308" r="4" fill="${c}"/></svg>`

const MOTIF_TECH = (c: string) =>
  `<svg viewBox="0 0 500 500" fill="none" role="img" aria-label="节点连接母题">` +
  `<g stroke="#E5E7EB" stroke-width="1"><path d="M40 90h420M40 180h420M40 270h420M40 360h420M40 450h420"/>` +
  `<path d="M100 40v420M190 40v420M280 40v420M370 40v420M460 40v420"/></g>` +
  `<path d="M150 130h80v60h-80z" stroke="${c}" stroke-width="2"/>` +
  `<path d="M270 130h80v60h-80z" stroke="${c}" stroke-width="2"/>` +
  `<path d="M210 340h100v70H210z" stroke="${c}" stroke-width="2"/>` +
  `<path d="M190 190h40v20h40v130M310 190h-40" stroke="${c}" stroke-width="1.5" stroke-dasharray="4 5"/>` +
  `<circle cx="150" cy="130" r="5" fill="${c}"/><circle cx="350" cy="130" r="5" fill="${c}"/>` +
  `<circle cx="260" cy="410" r="5" fill="${c}"/><circle cx="410" cy="375" r="9" fill="#E9A23B"/>` +
  `<rect x="205" y="235" width="110" height="56" fill="${c}" opacity=".08"/>` +
  `<text x="260" y="268" text-anchor="middle" font-family="Inter,sans-serif" font-size="15" font-weight="600" fill="${c}">数据中枢</text>` +
  `</svg>`

const MOTIF_MFG = (c: string) =>
  `<svg viewBox="0 0 500 500" fill="none" role="img" aria-label="模块榫接母题">` +
  `<g stroke="#E5E7EB" stroke-width="1"><path d="M40 130h420M40 220h420M40 310h420M40 400h420"/></g>` +
  `<rect x="60" y="130" width="120" height="90" stroke="${c}" stroke-width="2"/>` +
  `<rect x="190" y="130" width="120" height="90" stroke="${c}" stroke-width="2" fill="${c}" opacity=".07"/>` +
  `<rect x="320" y="130" width="120" height="90" stroke="${c}" stroke-width="2"/>` +
  `<rect x="125" y="230" width="250" height="80" stroke="${c}" stroke-width="2"/>` +
  `<rect x="125" y="320" width="120" height="70" stroke="${c}" stroke-width="2" fill="${c}" opacity=".07"/>` +
  `<rect x="255" y="320" width="120" height="70" stroke="${c}" stroke-width="2"/>` +
  `<g fill="${c}" opacity=".55"><path d="M180 172h10v10h-10zM200 182h10v10h-10z"/><path d="M310 262h10v10h-10zM330 272h10v10h-10z"/></g>` +
  `<path d="M40 430h420" stroke="${c}" stroke-width="2" stroke-dasharray="6 6"/>` +
  `<circle cx="60" cy="430" r="5" fill="${c}"/><circle cx="440" cy="430" r="5" fill="${c}"/>` +
  `<text x="250" y="472" text-anchor="middle" font-family="Inter,sans-serif" font-size="15" font-weight="600" fill="${c}" letter-spacing="3">精度与咬合</text>` +
  `</svg>`

const MOTIF_TRADE = (c: string) =>
  `<svg viewBox="0 0 500 500" fill="none" role="img" aria-label="流通曲线母题">` +
  `<g stroke="#E5E7EB" stroke-width="1"><path d="M40 130h420M40 250h420M40 370h420"/></g>` +
  `<path d="M30 330c70-120 150-40 220-110s150-20 220-90" stroke="${c}" stroke-width="3" stroke-linecap="round"/>` +
  `<path d="M30 380c70-120 150-40 220-110s150-20 220-90" stroke="${c}" stroke-width="2" stroke-linecap="round" opacity=".4"/>` +
  `<path d="M30 280c70-120 150-40 220-110s150-20 220-90" stroke="${c}" stroke-width="2" stroke-linecap="round" opacity=".4"/>` +
  `<circle cx="250" cy="200" r="9" fill="${c}"/>` +
  `<circle cx="120" cy="238" r="6" fill="${c}" opacity=".6"/><circle cx="390" cy="160" r="6" fill="${c}" opacity=".6"/>` +
  `<g stroke="${c}" stroke-width="1.5" fill="none" opacity=".55"><rect x="196" y="146" width="108" height="44" rx="10"/><path d="M250 246v34M250 280h-60M250 280h60"/></g>` +
  `<text x="250" y="420" text-anchor="middle" font-family="Inter,sans-serif" font-size="15" font-weight="600" fill="${c}" letter-spacing="3">连接与流通</text>` +
  `</svg>`

const MOTIF_DIGITAL = (c: string) =>
  `<svg viewBox="0 0 500 500" fill="none" role="img" aria-label="产品轮廓母题">` +
  `<rect x="110" y="110" width="280" height="280" rx="64" stroke="${c}" stroke-width="3"/>` +
  `<rect x="160" y="165" width="180" height="150" rx="20" stroke="${c}" stroke-width="2" fill="${c}" opacity=".06"/>` +
  `<circle cx="250" cy="360" r="11" stroke="${c}" stroke-width="2"/>` +
  `<path d="M250 66v22M250 412v22M66 250h22M412 250h22" stroke="${c}" stroke-width="2" stroke-linecap="round" opacity=".5"/>` +
  `<circle cx="400" cy="150" r="22" fill="#fff" stroke="${c}" stroke-width="2"/><circle cx="400" cy="150" r="8" fill="${c}"/>` +
  `<path d="M100 400c0-40 32-72 72-72" stroke="${c}" stroke-width="2" stroke-dasharray="4 5" opacity=".5"/>` +
  `<text x="250" y="452" text-anchor="middle" font-family="Inter,sans-serif" font-size="15" font-weight="600" fill="${c}" letter-spacing="3">实体与细节</text>` +
  `</svg>`

/* ---------- 方案卡插画（移植自原型 index.html ART） ---------- */

const ART_CONSULT =
  '<g stroke="currentColor" stroke-width="1.5" fill="none"><path d="M40 170l60-40 60 40M40 170v60M100 130v100M160 170v60"/></g><circle cx="100" cy="250" r="8" fill="currentColor"/>'
const ART_TECH =
  '<g stroke="currentColor" stroke-width="1.5" fill="none"><rect x="30" y="40" width="60" height="50" rx="6"/><rect x="110" y="40" width="60" height="50" rx="6"/><rect x="70" y="110" width="60" height="50" rx="6"/></g>'
const ART_MFG =
  '<g stroke="currentColor" stroke-width="1.5" fill="none"><rect x="30" y="40" width="70" height="60" rx="4"/><rect x="110" y="45" width="60" height="55" rx="4"/><rect x="55" y="115" width="90" height="50" rx="4"/></g>'
const ART_TRADE =
  '<path d="M20 150c40-60 80-20 120-50s80-10 120-40" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><circle cx="140" cy="100" r="7" fill="currentColor"/>'
const ART_DIGITAL =
  '<rect x="45" y="45" width="110" height="110" rx="26" stroke="currentColor" stroke-width="1.5"/><rect x="70" y="72" width="60" height="52" rx="10" stroke="currentColor" stroke-width="1.2"/>'

/* ---------- 5 套模板（差异表见 templates/README.md 第二节） ---------- */

export const SITE_TEMPLATES: SiteTemplate[] = [
  {
    id: 'consult',
    className: 'tpl-consult',
    name: '管理咨询',
    industry: '咨询 / 专业服务 · 权威可信，衬线标题，圆角偏小',
    color: '#185FA5',
    hero: {
      eyebrow: '战略判断 · 长期陪跑',
      titleHtml: '把复杂的问题<br>变成<em>清晰的决策</em>',
      subtitle:
        '面向【行业】企业提供战略与运营咨询，从诊断到落地，全程陪跑，让每一次决策都有据可依。'
    },
    solutionsTitle: '按行业，看我们如何解题',
    biz: {
      one: { title: '咨询中心', desc: '战略诊断、组织优化、运营提升的结构化咨询服务。' },
      two: { title: '顾问中心', desc: '行业顾问全程陪跑，把方法论落成可执行的改进项。' },
      fallbackItems: ['战略诊断 · 占位', '组织优化 · 占位', '运营提升 · 占位']
    },
    art: ART_CONSULT,
    motif: MOTIF_CONSULT
  },
  {
    id: 'tech',
    className: 'tpl-tech',
    name: '信息技术',
    industry: '软件 / IT 服务 · 稳定可靠，墨绿低饱和',
    color: '#0F6E56',
    hero: {
      eyebrow: '稳定可靠 · 技术沉淀',
      titleHtml: '让复杂的系统<br>变得<em>简单可靠</em>',
      subtitle: '面向【行业】企业提供产品与服务一体化解决方案，从架构设计到长期运维，全程陪跑。'
    },
    solutionsTitle: '按场景，看我们如何解题',
    biz: {
      one: { title: '产品中心', desc: '面向不同规模与场景的标准化产品模块，可组合、可独立部署。' },
      two: { title: '服务中心', desc: '咨询诊断、方案设计、部署实施、运营陪跑的全周期服务。' },
      fallbackItems: ['平台产品 · 占位', '数据治理 · 占位', '安全合规 · 占位']
    },
    art: ART_TECH,
    motif: MOTIF_TECH
  },
  {
    id: 'mfg',
    className: 'tpl-mfg',
    name: '制造业',
    industry: '制造 / 工业 · 精准硬朗，小圆角，标题带竖线',
    color: '#B0442A',
    hero: {
      eyebrow: '精密制造 · 产线经验',
      titleHtml: '让每一道工序<br>都<em>精准可控</em>',
      subtitle: '面向制造企业提供产线数字化与质量管理方案，让产能、质量、成本可视可控。'
    },
    solutionsTitle: '按工艺，看我们如何解题',
    biz: {
      one: { title: '产品中心', desc: '产线系统与工业软件，模块化部署，与现有设备即插即用。' },
      two: { title: '服务工程', desc: '从工艺诊断到产线改造的工程化实施与质保服务。' },
      fallbackItems: ['产线系统 · 占位', '质量追溯 · 占位', '设备管理 · 占位']
    },
    art: ART_MFG,
    motif: MOTIF_MFG
  },
  {
    id: 'trade',
    className: 'tpl-trade',
    name: '商贸',
    industry: '商贸 / 零售 · 流通高效，紫调柔化，圆角偏大',
    color: '#7C4A9E',
    hero: {
      eyebrow: '流通效率 · 商贸经验',
      titleHtml: '让每一笔流转<br>都<em>高效顺畅</em>',
      subtitle: '面向商贸与零售企业提供供应链与会员数字化方案，打通线上线下全链路数据。'
    },
    solutionsTitle: '按业态，看我们如何解题',
    biz: {
      one: { title: '产品中心', desc: '供应链与门店数字化产品，打通商品、订单与会员数据。' },
      two: { title: '运营服务', desc: '全渠道运营陪跑，从选品到复购的持续优化服务。' },
      fallbackItems: ['供应链 · 占位', '会员运营 · 占位', '全渠道 · 占位']
    },
    art: ART_TRADE,
    motif: MOTIF_TRADE
  },
  {
    id: 'digital',
    className: 'tpl-digital',
    name: '数码产品',
    industry: '消费电子 / 数码 · 年轻亲和，大圆角，视觉最轻',
    color: '#C2410C',
    hero: {
      eyebrow: '持续创新 · 产品经验',
      titleHtml: '让每一次选择<br>都<em>简单安心</em>',
      subtitle: '面向【行业】企业提供产品与服务一体化解决方案，从选购到售后，全程陪伴。'
    },
    solutionsTitle: '按场景，看我们如何解题',
    biz: {
      one: { title: '产品中心', desc: '硬件产品与配套软件，一体式设计，开箱即用。' },
      two: { title: '服务中心', desc: '延保、以旧换新与上门服务的完整售后体系。' },
      fallbackItems: ['硬件产品 · 占位', '配套软件 · 占位', '延保服务 · 占位']
    },
    art: ART_DIGITAL,
    motif: MOTIF_DIGITAL
  }
]

export const DEFAULT_SITE_TEMPLATE_ID: SiteTemplateId = 'consult'

/** 设置键 site_template 的合法值集合（供挂载/清理类名使用） */
export const SITE_TEMPLATE_CLASS_NAMES = SITE_TEMPLATES.map(t => t.className)

/** 非法值一律回退默认模板（与 featureSwitch 的宽松口径一致：只认合法字面量） */
export function normalizeSiteTemplate(value: unknown): SiteTemplateId {
  return SITE_TEMPLATES.some(t => t.id === value)
    ? (value as SiteTemplateId)
    : DEFAULT_SITE_TEMPLATE_ID
}

export function getSiteTemplate(id: unknown): SiteTemplate {
  const normalized = normalizeSiteTemplate(id)
  return SITE_TEMPLATES.find(t => t.id === normalized) ?? SITE_TEMPLATES[0]
}
