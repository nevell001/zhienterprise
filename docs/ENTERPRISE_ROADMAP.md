# 企业建站化改造路线图（ENTERPRISE_ROADMAP）

> 定位：记录 ZhiEnterprise 从博客系统转型为企业建站系统的**已定决策与分阶段规划**，
> 供后续迭代按阶段验收。改造现状一句话：**机制层已完成（首页接管、导航裁剪、品牌化），
> 内容层与视觉层待补齐（企业内容模型、区块化首页、页面搭建体验）**。

## 一、现状盘点（2026-10）

已完成（机制层）：

- 企业官网模式：`home_page_slug` 首页被自定义页面接管，文章列表迁至 `/api/articles`；
- 导航裁剪：`nav_category_enabled` / `nav_tag_enabled` / `nav_archive_enabled`；
- 自定义页面（`zhi_page`）：Markdown 内容 + 页面级 TDK + `show_in_nav` + sitemap 收录；
- 品牌：站名/登录页/README/compose 均已统一「知企建站系统（ZhiEnterprise）」；
- SEO 基础：站点级 TDK、sitemap.xml、robots.txt、RSS；
- 完善的评论/审核/通知体系与认真的移动端适配。

关键缺口（内容层与视觉层）：

- 前台无产品/服务/案例/联系页面的内容模型与 UI；
- 企业首页只能用 Markdown 自定义页撑（`CustomPagePanel.vue`），做不出企业版式；
- 后台自定义页面编辑仅一个纯文本 textarea，无可视化能力；
- 「关于」页仍是博主人设（`about.vue`）；留言板默认关闭、无新留言通知。

## 二、已定决策

| 决策 | 内容 |
|---|---|
| 询盘转化**后置** | 不做 `zhi_message` 字段扩展（公司/电话/需求类型）、不做新留言通知、不做询盘跟进管理。「联系我们」保留最小形态：静态联系信息页 + 现有留言板开关。 |
| 核心业务 = 产品**或**服务，统一建模 | **不建**独立 `zhi_product` 表；`zhi_article` 增加 `article_type`（`news` 新闻 / `business` 核心业务 / `case` 案例），业务条目复用分类做下级组织（产品线/服务类型）。 |
| 形态命名可配 | 新增设置键 `business_label`（默认「核心业务」，可改「产品中心」/「服务项目」/「解决方案」），统一驱动导航菜单名、首页区块标题、面包屑与 SEO 标题——同一套代码适配产品型/服务型/混合型企业。 |
| 业务详情页通用版式 | 封面/图集 + 富文本正文 + 可选属性区（产品放参数表、服务放流程/套餐），不强绑产品字段。 |

## 三、分阶段规划

### Phase 0 — 快赢与卫生（✅ 已完成，2026-10-12）

1. ✅ 品牌残留清理：SQL 种子 `RuoYi` 关键词（含已有库幂等 UPDATE）、后台 `ruoyi.zip`、
   job 帮助文案包名、prometheus `zhienterprise` 标签、`/opt/zhienterprise` 运维文档路径、
   `package.json`/`package-lock` 的 `zhienterprise`、sonar projectKey、`components/RuoYi` 死代码目录、
   `design-spec.html` 旧「博客系统」定位文案；
2. ✅ 死代码清理：首页永不显示的 contact 弹窗（模板/脚本/亮暗样式）、
   `mo-blog.scss` 中无任何组件引用的 hero/cta/article-row/popular-*/page-btn 等规则
   （棘轮测试 `mo-blog.test.ts` 同步指向存活选择器）；
3. ✅ 前台设置白名单统一：新增 `FrontConfigKeys`，`/api/setting` 与
   `/common/site/setting` 共用同一份白名单，消除键集合漂移；
4. ✅ 种子数据企业化：分类（公司动态/行业资讯/产品知识/技术支持 + 二级栏目）、
   18 个企业标签、6 篇企业风示例文章（发布/案例/趋势/SEO 指南/服务/招聘）、
   友链收敛为技术栈官方站点、站点设置（描述/版权/SEO/问候语）去博客味。

   > 兼容性说明：分类/标签/文章/友链种子仅对**全新数据库**生效（INSERT IGNORE 幂等）；
   > 已有库保留用户内容不动，仅 RuoYi 关键词通过值匹配的 UPDATE 迁移。

### Phase 1 — 核心批次（✅ 已完成，2026-10-12）

1. ✅ **核心业务内容类型**
   - `zhi_article.article_type`（`news`/`business`/`case`）：实体 + Mapper 过滤、
     幂等 ALTER 迁移已有库，前台 `/api/article/list` 绑定实体即获得类型筛选；
   - 管理端「站点管理 → 核心业务管理」菜单（menu 2010，复用文章管理组件，按路由
     `/admin/business` 固定 `articleType=business`，权限沿用 `zhi:article:*`）；
     文章管理页新增类型筛选/类型列/编辑项；
   - 前台 `/api/business` 列表（卡片网格 + 分页）与 `/api/business/:id` 详情页；
   - sitemap：business/case 文章写规范入口 `/api/business/{id}`，
     `home_layout=business` 时收录列表页；
   - 设置键 `business_label`、`contact_phone`、`contact_address`（进前台白名单与设置页）。
2. ✅ **企业首页区块化模板**
   - 新增 `home_layout=business` 首页模板：Hero → 核心业务卡片 → 数据条 → 客户案例
     （无内容自动隐藏）→ 新闻动态 → CTA 联系区，优先级低于 `home_page_slug` 自定义页面；
   - 模板模式下导航注入「核心业务」入口（名称取 `business_label`）；
   - 组件样式全部走 `--mo-*`/`--el-*` token，通过 design-contract 与 dark-mode 棘轮测试。

   剩余：~~独立企业向主题~~（✅ 已完成，见 Phase 2.5）。

### Phase 2.5 — 企业主题与伪静态（✅ 已完成，2026-10-14）

1. ✅ **独立企业主题（theme-enterprise）**：`themes/enterprise.scss` 只重映射
   `--mo-p*/--mo-a*` 色板（靛蓝+粉 → 稳重蓝+青蓝）与标题字体（衬线 → 无衬线），
   不复制组件皮肤，避免双份漂移；前台布局在 `home_layout=business` 时挂
   `html.theme-enterprise`（含 Mo-Blog 深色 Element 主色修正），离开公开站点自动摘除；
2. ✅ **slug 伪静态路由**：`zhi_article.slug`（幂等 ALTER + 唯一性校验，软删除不占用），
   前台详情接口按「数字 ID 或 slug」统一解析（`/api/article/{key}`、`/api/business/{key}`
   双入口兼容历史链接），sitemap 优先输出别名，管理端文章表单新增「URL 别名」，
   6 篇示例文章已带别名（如 `/api/article/zhi-v1-release`）。

   备注：~~分类仍按数字 ID 访问~~（✅ alias 已接入，见 Phase 2.6）。

### Phase 2.6 — 分类 alias 伪静态（✅ 已完成，2026-10-14）

1. ✅ **分类 alias 伪静态**：`/api/category/{key}`、`/api/article/category/{key}`
   按「数字 ID 或 alias」统一解析（alias 只命中启用且未删除的分类）；
   前台三处分类链接改为 alias 优先、回退数字 ID；分类页先解析分类再按数字 ID
   加载文章，alias 未命中时回退「所有分类」视图；
2. ✅ **sitemap 补全**：收录启用中的分类页（alias 优先）；企业模板模式下
   联系页 `/api/contact` 一并入图。

### Phase 2.7 — 伪静态链路安全与性能复查（✅ 已完成，2026-10-14）

复查范围：article_type/伪静态/主题/JSON-LD/联系页全部新增链路。已修复：

1. 🔒 **安全回归修复**：分类详情接口重构时丢失了旧代码的 `del_flag` 校验，
   数字 ID 路径会返回已删除分类——`resolveFrontCategory` 数字路径补回删除过滤
   （status 口径与历史行为保持一致）；alias 路径本就只命中启用分类。
2. ⚡ **索引补齐**：`idx_slug` 此前只写在 CREATE TABLE（仅全新库有），
   已用 `sp_create_index_if_not_exists` 补幂等迁移；顺带给 `zhi_category.alias`
   加 `idx_alias`（伪静态点查列）。
3. ⚡ **详情单查询**：slug 路径原本解析一次 + 按 ID 重取一次；
   `selectArticleBySlug` 列集与 ById 对齐后去掉二次查询，slug 流量减半。

复查确认无问题项：新查询全部参数化（无注入面）；sitemap `writeUrl` 已做 XML 转义
（覆盖 admin 可控的 slug/alias）；JSON-LD 走 `textContent + JSON.stringify`（无 XSS）；
前台 `v-html` 统一经 `sanitizeArticleContent`；slug 查询只命中已发布文章（草稿不可探测）；
详情/正则字符集约束齐备。

留作后续的性能可选项（当前数据量下无感，先记录）：

- 企业首页模板每次访问发 4 个请求、`/api/stats/overview` 每次跑 5 个聚合 COUNT，
  可考虑短 TTL 缓存（复用 Redis，参照站点设置 600s 缓存模式）；
- `/sitemap.xml` 每次全量生成，文章量大后可加缓存；
- 分类页「解析分类 → 加载文章」为串行两请求，可改走已支持 alias 的
  `/api/article/category/{key}` 并行化。

### Phase 2 — 完善批次（✅ 已完成，2026-10-14）

1. ✅ **文章级 TDK**：`zhi_article` 补 `seo_title/seo_keywords/seo_description`
   （幂等 ALTER 迁移），管理端文章表单新增 SEO 区块，前台文章/业务详情页 SEO
   优先用文章级字段、回退标题/摘要；
2. ✅ **JSON-LD 结构化数据**：常驻布局注入 Organization（含联系方式 ContactPoint），
   文章与业务详情注入 Article（卸载时移除防路由残留），`utils/jsonld.ts` 带单测；
3. ✅ **「关于我们」企业化重做**：hero 从博主人设（头像/站长头衔）改为企业定位
   （站点名 + 描述），联系卡片补电话/地址（企业联系方式一等公民化）；
4. ✅ **「联系我们」页** `/api/contact`：电话/邮箱/地址卡片 + 留言板入口
   （受 `guestbook_enabled` 控制），企业模板模式导航注入入口；
5. ✅ **页面编辑器升级**：预置区块模板（Hero 横幅/三列卡片/图文左右/数据条/CTA 条）
   一键插入光标处 + 前台同管线实时预览（renderMarkdown + 消毒）。

   备注：编辑器保留 Markdown textarea + 区块模板方案（未换 Quill 富文本），
   如后续需要所写即所得可再评估。

### Phase 3 — 竞争力（v2，按需排期）

- ~~slug 伪静态路由~~（✅ Phase 2.5/2.6 已完成）；
- 询盘转化（已定后置）：`zhi_message` 字段扩展、新留言通知（复用评论通知链路）、询盘跟进状态；
- 自定义页面可视化搭建（区块化结构化字段 + 前端组件渲染）；
- ~~模板市场 / 一键换主题~~（✅ 首批落地，见 Phase 3.1；全站内页模板化见 3.2）。

### Phase 3.1 — 5 套行业模板与模板切换（✅ 已完成，2026-10-14）

严格按 `docs/design/` 体系落地（企业官网UI-UX架构方案.md 规范基线 +
templates/ 的两层分离架构）：

1. ✅ **两层架构**：`config/siteTemplates.ts`（注册表：5 套行业模板的文案配置
   CONF / 母题主视觉 MOTIFS / 方案卡插画 ART，全部自原型移植）+
   `themes/site-templates.scss`（变量层：`.tpl-site` 基类默认 + `.tpl-consult/
   tech/mfg/trade/digital` 只覆盖主色/圆角/阴影/深色底/标题字体，含
   制造业竖线标题、数码浅色 Hero 差异与 reduced-motion/contrast 降级）；
2. ✅ **首页模板化重做**：`EnterpriseHome.vue` 按原型首页 10 模块结构实现
   （Hero+信任指标 → 核心业务两卡 → 解决方案精选 → 数据成果深色区 →
   Logo 墙 → 新闻动态 → 转化区块），滚动入场动效按 §4.3/§4.7 落地
   （`useScrollReveal`：IO 0.15 触发即 unobserve，不支持/减弱动效直接终态）；
   动态数据接入：核心业务=business 文章、新闻=news 文章、Logo 墙=友链，
   空数据回退设计稿占位；
3. ✅ **模板切换**：设置键 `site_template`（白名单 + 种子 + 管理端「界面主题 →
   官网行业模板」下拉带主色点）→ 前台 `<html>` 挂 `.tpl-site .tpl-xxx`
   （仅企业官网模板模式，离开公开站点摘除），非法值回退默认管理咨询模板；
4. ✅ **契约**：组件样式全走 `--tpl-*` 变量（字号/间距尽量 `--mo-*` token，
   通过字号/间距/裸 hex 三道棘轮），新增 6 个模板单测。

### Phase 3.2 — 全站内页模板化 + 短路径导航（✅ 已完成，2026-10-14）

1. ✅ **导航与页脚**（§1.1/§1.2）：EnterpriseNav（首页透明浮于 Hero → 滚动
   80px 实底收拢、咨询 CTA 常驻、关于我们二级下拉、移动端抽屉）+
   EnterpriseFooter（四栏 + 返回顶部）；博客模式零改动；
2. ✅ **导航短路径**：`/` 直接渲染首页；`/business`、`/business/:id`、`/news`、
   `/contact`、`/honors`、`/history` 短别名（`/api/*` 保留兼容）；
   企业模板模式 sitemap 输出短路径规范地址；
3. ✅ **关于我们三页**（§1.3）：企业简介（slug=about 正文 + 站点统计概览）、
   荣誉资质 `/honors`（分类 Tab + 证书墙占位 + slug=honors 说明位）、
   发展历程 `/history`（四阶段时间轴、荣誉节点方形、rv 入场）；
4. ✅ **新闻列表/详情**（§3.4）：`/news` 头条大图 + 2 列次条 + 分页；
   详情模板模式加阅读进度条与 `--tpl-*` 皮肤（吸顶目录/上下篇/相关阅读为既有能力）；
5. ✅ **核心业务列表/详情**（§3.2）：分类 Tab 筛选 + 封面缩略图卡片 + 分页；
   详情面包屑 + 相关推荐 + 底部转化条；
6. ✅ **联系我们**（§3.6 展示增强版，遵守询盘后置）：基础联系卡 + 分渠道咨询
   占位 + 地图占位与「获取路线」外链 + FAQ 手风琴（FAQPage 结构化数据）+
   留言板/邮件 CTA。

   遗留说明：荣誉/历程/FAQ/渠道/指标/成果/方案内容已接线「官网内容」设置键
   （JSON 结构化维护）。

### Phase 3.3 — 模块化建站（✅ M1~M4 + M5 首批已完成，2026-10-15/16）

详见 `docs/模块化建站架构方案.md`：zhi_page builder 页（sections JSON）+
PageRenderer/registry + 14 类模块组件 + 菜单管理（site_nav_item）+
行业模板包（预置数据包一键导入，取代固定模板语义）+ 导航短路径。

**仍未实现清单**（对照设计方案逐项盘点，2026-10-16）：

| 类别 | 未实现项 | 设计出处 |
|---|---|---|
| 板块缺失 | 解决方案独立板块（总览/详情/三层筛选/实施路径步骤条） | §1.3/§3.3 |
| 内容模型 | 业务详情卖点宫格、规格参数表、图集、服务流程步骤条（需 zhi_article 结构化字段扩展） | §1.3/§3.2 |
| 板块缺失 | 联系表单（姓名/公司/电话/类型/隐私同意，询盘解冻后）+ 真实地图嵌入 + 多地址 Tab | §1.3/§3.6 |
| 新闻板块 | 归档侧栏（桌面年份+热门标签，移动下拉）与标签交叉筛选 | §3.4 |
| 页脚 | 订阅条（需邮箱订阅后端） | §1.2 |
| 交互细节 | 首屏标题分行错峰上浮、视差体系（§4.4 全部）、时间轴滚动填充/点击节点/Tooltip/展开收起、荣誉点击查看大图、业务列表能力多选筛选/视图切换/已选条件胶囊/URL 同步、移动端「加载更多」 | §3/§4.5 |
| 无障碍 | ~~页脚「关闭动画」手动开关、saveData/设备内存降级~~（✅ 2026-10-16，motionPreference + 页脚开关 + .rv-fade）；FPS<50 检测降级 | §4.7 |
| 性能 | fetchpriority、AVIF/WebP+srcset、字体子集化（Inter/Noto 未实际引入）、首屏 CSS/JS 预算测量 | §6.1 |
| SEO | WebSite potentialAction 搜索、业务详情 Product/Service schema、面包屑可见但无 BreadcrumbList JSON-LD | §6.2 |
| 基础设施 | 启动期 schema 校验/迁移工具（升级依赖手动重跑脚本，site_nav_item 缺表事件即例证） | — |

## 四、验收口径
- Phase 1 完成后：一个服务型企业可以在**不写代码**的情况下，用后台完成
  「改导航文案 → 建核心业务条目 → 首页出现业务区块 → 发布一条公司动态」的完整闭环；
- 每阶段保持：`mvn clean install -DskipTests` 通过、`npm run test` 通过、
  `sql/00_init_database.sql` 在全新库与已有库重跑均幂等。

## 五、博客功能下线（2026-10-17）

按「全面转向企业官网」的口径，移除一批博客专属配置（均从设置页、store、`FrontConfigKeys`
前台白名单、`FrontSettingController` 默认值、SQL 种子五处同步删除，避免键集合漂移）：

| 项目 | 处理 | 说明 |
| --- | --- | --- |
| 评论 | **默认关闭**（`comment_enabled=false`） | 保留开关与后台「评论管理」：企业站极少需要评论，但保留可逆性；前台评论区与 `POST /api/comment` 同时失效 |
| 点赞 / 分享 | 移除 | `like_enabled` / `share_enabled` + 文章详情页点赞、分享按钮（后端点赞接口保留，评论点赞仍被评论区使用） |
| 右侧栏 | 移除 | `sidebar_enabled` + 文章列表/分类/标签页的三处侧栏渲染与样式 |
| 归档 | 移除 | `/archive` 页面与路由、`nav_archive_enabled`、导航/页脚「归档」入口 |
| 站长/社交死键 | 移除 | `blog_author`、`blog_signature`、`author_description`、`zhihu_url` 等无消费方键（页脚版权署名改为只取站点名称） |

**保留项**：分类与标签（核心业务列表用分类做 Tab 筛选、文章用标签）；搜索、留言板、
友链、浏览量统计（企业站同样需要）。**已有库注意**：种子只做 `INSERT IGNORE`，不会清理
存量键行 —— 功能层面无害（白名单已不含这些键），如需彻底清理按 `sql/README.md` 的
`99_fix_*.sql` 约定处理。

## 六、自定义 Logo / Banner（管理后台可配置，2026-10-17）
新增两个站点设置键，均可在「站点设置 → 站点信息 → 品牌图」上传（单图，存 `/profile` 下）：

| 键 | 用途 | 未配置时 |
| --- | --- | --- |
| `site_logo` | 前台导航（经典/企业）、页脚品牌、登录/注册/找回密码品牌区、JSON-LD `logo`、后台侧边栏 Logo | 回退 `blog_avatar` 站点头像 → 站名首字 |
| `site_banner` | 企业官网首页 Hero 右侧视觉区（16:9，替换模板母题插画） | 回退当前行业模板的母题插画 |

**上传地址口径**：后端 `/common/upload` 的 `url` 是带请求 Host 的绝对地址（Docker/代理下会变成 `http://zhi-admin:8080/...`），直接存会导致图片 404。前端统一只存相对路径（`fileName`，如 `/profile/upload/...`），并用 `utils/uploadUrl.ts` 的 `normalizeUploadUrl()` 兼容历史绝对地址；设置页保存时会把旧值写回为相对路径（自愈）。

实现要点：`FrontConfigKeys` 前台白名单 + SQL 种子同步新增（**不改白名单后台能存前台读不到**，前端 `FrontConfigKeysTest` 契约守卫）；消费端在 `EnterpriseNav` / `SiteLayout`（导航+页脚+JSON-LD）/ `sections/HeroSection.vue`（builder 首页真正在用的 Hero）/ `EnterpriseHome.vue`（老库回退首页）/ `utils/siteSettings.ts`（后台侧边栏）。

## 七、认证页（登录/注册/找回密码）统一企业风格（2026-10-17）


三页统一包进 `SiteLayout`、共用 `assets/styles/auth-enterprise.scss`（`ent-*` 类 + `--tpl-*` 变量层）与 `composables/useAuthBrand.ts`（品牌取值），与整站导航/字体/配色/圆角/阴影一致；深色模式走 `.tpl-site` 变量重映射。类名刻意避开 `.auth-card` 等旧类，以免被 `theme-dark.css` / `mo-blog.scss` 的 `!important` 棕色规则覆盖。

## 八、文章详情页去博客化（2026-10-17）

详情页从「博客阅读页」改为「企业官网新闻详情」：

**移除的博客元素**：阅读进度条、文章目录（TOC）、作者卡（头像/昵称/简介/「+ 关注」/获赞/评论数统计）、收藏按钮、阅读时长（⏱ 约 N 分钟）、标签云侧栏、侧栏迷你相关推荐、meta 里的作者署名。

**保留 / 新增**：面包屑、分类徽标、发布日期与更新日期、阅读量（`view_count_enabled` 开关控制）、摘要导语、封面图、正文、关键词胶囊（SEO 用，保留 `/tag/:id` 跳转）、**相关阅读卡片**（3 列，替代侧栏迷你列表）、**咨询 CTA**（引导到 `/contact`）、上/下一篇。评论区保留但受 `comment_enabled` 开关控制（默认关闭）。

**配套修复**：`ArticleMapper.xml` 的 `SimpleArticleResult` 与 `getPrevArticle` / `getNextArticle` 两条 SQL 漏取 `status`，而 `FrontController#getArticleDetail` 要求 `status != null && equals(1L)` 才输出导航项 —— 结果「上一篇/下一篇」永远不返回（静默失效）。已补 `status` 列与映射，并加 `ArticleNavigationQueryTest` 契约守卫。

## 九、企业官网全面巡检（2026-10-17）

### 巡检方式
真实运行环境 headless Chrome：26 个前台路由 + 10 个后台页逐页渲染，采集 HTTP 状态、控制台错误、失败请求、断图（`naturalWidth=0`）、横向溢出、导航/页脚存在性；桌面(1440)/移动(390)/深色三态；站内链接全量落地校验；亮/暗模式自动对比度扫描（逐文本节点计算前景/背景对比度）；SEO 端点与 JSON-LD 结构校验；首页 builder 模块完整性校验。

### 结果
- 26 个前台页全部 200、导航/页脚齐全、无断图、无横向溢出；59 个站内链接全部可达；移动端 16 页无溢出；深色 22 页对比度扫描全绿（修复后）。
- SEO：`/sitemap.xml`、`/robots.txt`、`/api/rss` 均 200；首页 JSON-LD=Organization+WebSite，文章页=NewsArticle+BreadcrumbList；sitemap 与数据库内容一致（9 篇文章 → 9 条，分类按 alias）。
- 功能：搜索（`?q=`）、留言板开关、评论开关、友链申请开关、浏览量、相关阅读、上/下一篇、导航表驱动均按配置生效；首页 8 个 builder 模块全部渲染。

### 本次修复
| 问题 | 修复 |
| --- | --- |
| sitemap 收录已下线的 `/archive`、关闭的 `/guestbook`、与首页重复的 `/page/home` | `SeoController` 静态清单与条件收录 + 3 个守卫测试 |
| 未知/错误 URL 把游客重定向到登录页 | `permission.ts` 未匹配路由 → `/404`，`/404` 加入白名单 + 2 个测试 |
| `/business` 分类筛选芯片深色下 2:1（落到旧博客 `.tag` 规则） | 页面内用 `--tpl-*` 补齐芯片样式 |
| 「置顶/推荐」角标亮色下 2.0~2.6:1 | 亮色改用 `--el-color-{danger,warning}-dark-4` |
| 富文本编辑器的图片上传从未初始化（quill 未就绪） | 改由 `<quill-editor @ready>` 挂载处理器（实测点击图片按钮→触发 file input click） |
| 404 页深色下文字不可见（写死 `#222`） | 配色改为 `--el-*` / `--mo-on-primary` 令牌 |

### 未处理（待决策）
- `/author/:id`（博客作者主页）与 `/bookmarks`（我的收藏）仍存在但已无入口/无收藏来源，属博客遗留。
- 联系我们页按设计稿应有「咨询表单」，当前只有联系方式卡片/分渠道/地图/FAQ/CTA。
- sitemap 未收录 `/agreement`、`/privacy`（法务页，是否收录取决于 SEO 策略）。

## 十、需要决策项的落地结果（2026-10-17）

| 决策项 | 结论 | 实施内容 |
| --- | --- | --- |
| 博客遗留页面 `/author/:id`、`/bookmarks` | **一并下线** | 删除两个路由/页面（`views/site/author`、`views/site/bookmark`）、SiteLayout「我的收藏」入口、`permission/request/SpaRouterController/WebMvcConfig` 白名单，以及后端 `BookmarkController`+Service+Mapper+Domain+XML 与测试（`zhi_bookmark` 表保留历史数据） |
| 联系我们页咨询表单 | **复用留言板接口** | 联系页新增「在线咨询」表单（姓名/公司/邮箱/电话/咨询类型/需求描述/图形验证码/隐私同意），提交走 `POST /api/message` 写入 `zhi_message`，后台「留言管理」统一审核回复；新增独立开关 `contact_form_enabled`（默认开启，站点设置「功能设置 → 咨询表单」）；留言板与咨询表单「任一开启」即可提交，读接口仍只受留言板开关控制 |
| sitemap 法务页 | **两个都收录** | `SeoController.STATIC_PATHS` 增加 `/agreement`、`/privacy`（sitemap 由 31 条增至 33 条） |
| 版本与推送 | **升 1.1.0 并推送** | 按 `docs/VERSION_MANAGEMENT.md` 更新 13 处版本号（根/子 POM、RuoYiConfig 兜底、package.json/lock、发布流水线 tag、文档），提交推送 Gitee |

## 十一、首页 Banner 多图轮播（2026-10-17）

原「首页 Banner」为单图（`:limit="1"`，`site_banner` 存单个路径）。按需求升级为多图轮播：

- **存储**：沿用 `site_banner` 单键，多图以逗号分隔（不新增表/键，历史单图值无需迁移）；
- **后台**：单图上限 5 张、16:9 裁剪、可拖动排序、按上传顺序轮播；达到上限后隐藏上传入口；
- **前台**：新增 `components/BannerCarousel.vue`——单图直接展示（无轮播控件），多图 5s 自动轮播、悬停/聚焦暂停、左右箭头 + 圆点指示 + 触摸滑动；遵循「减弱动效」（页脚开关 / `prefers-reduced-motion`）时不自动播放，手动切换仍可用；深色模式沿用 `--tpl-white` 令牌；
- **解析**：`utils/uploadUrl.ts#parseBannerUrls()` 统一做「逗号拆分 / 去重 / 上限 5 / 兼容容器名绝对地址 / `data:` 内联值不误拆」；
- **测试**：`BannerCarousel.test.ts` 7 例（单图无控件、多图自动轮播、悬停暂停、手动切换循环、减弱动效、触摸滑动）+ `parseBannerUrls` 8 例 + Hero 接入 2 例；
- **消费端**：builder 首页 `sections/HeroSection.vue` 与旧库回退首页 `EnterpriseHome.vue` 两处均已接入。

## 十二、后台菜单管理（原名导航管理）与链接目标（2026-10-17）

- **更名**：后台「站点管理 → 导航管理」改为「菜单管理」（`sys_menu` id=2011）；SQL 种子改名并附
  存量库幂等 `UPDATE ... WHERE menu_name='导航管理'`，重跑一次脚本即完成迁移。
- **问题**：原弹窗只有「链接页面」下拉（仅自定义页面），文章分类/标签只能手输 `/category/{id}`，
  管理员需自己知道分类 ID/别名，易写错且无法回显；这与「二级菜单指向某个文章分类」的常见用法不符。
- **改造**：链接目标改为分组选择器——常用页面（`/`、`/news`、`/business`、`/solutions`、
  `/about`、`/honors`、`/history`、`/contact`）、自定义页面、文章分类（`alias` 优先）、文章标签；
  选择后自动填标签（标签非空时不覆盖）与 `linkValue`，编辑时按已有链接值回显；
  表格增加「分类 / 标签」标记；`linkValue` 增加按类型校验（站内路径须以 `/` 开头、外链须 `http(s)`）。
- **不变式**：`site_nav_item.link_type` 仍只有 `path/url/cta`，分类/标签不新增类型、不改表结构。
- **测试**：`views/admin/nav/index.test.ts` 覆盖页面/分类/标签自动填充、编辑回显、提交载荷与栏目名称托管。

## 十三、站点设置「栏目名称」可靠性审计（2026-10-17）

- **链路确认**：`business_label` 为单一数据源，消费点包括头部/页脚导航（`/business` 项标签替换）、
  经典导航 `siteNav`、首页 `BizCardsSection` 区块眉题、`/business` 列表/详情页眉题与标题、
  页面文案 business 分组空值兜底、后台菜单管理的「栏目名称控制」标注。
- **实测通过**：改值后 `/common/site/setting` **立即**返回新值（`@SiteCacheEvict` 生效）；
  头部导航、页脚快速导航、首页区块、业务页眉题/标题、设置页输入框、菜单管理受控标注六处一致；
  清空值与纯空格均回退「核心业务」，且 **未被 `sys_config` 旧镜像复活**。
- **修复的可靠性缺口**：
  1. 后端原先无长度校验：直接调接口可写入 100 字符，撑坏导航/标题 → 现在 **trim + ≤20 字符**，
     超长返回「栏目名称长度不能超过20个字符」，并有单测锁定；
  2. 前台 `/business` 判定与后台不一致（前台精确匹配、后台 trim 匹配）→ 统一 trim 后精确匹配，
     避免脏值导致「后台说受控、前台不替换」；
  3. 老库缺行时公开接口返回空串 → `FrontSettingController` 补默认值「核心业务」，与前端 fallback 对齐。
- **剩余说明（非缺陷）**：设置页保存成功后通过 Pinia store 同步（同 SPA 内的导航/区块立即更新）；
  其它标签页/独立浏览器需刷新；`siteSettingsUpdated` 自定义事件目前无监听方（store 已足够），保留未删。

## 十四、行业模板「全页面 UI 适配」审计与修复（2026-10-17）

- **审计方式**：真实切换 5 套模板 × 21 个前台页面，读取 html 模板类、`--tpl-p`、关键元素计算样式签名，并做深色模式与截图对比。
- **发现的系统性问题（已修）**：模板变量原先声明在 `.tpl-site` 上，而各内页根节点也带 `.tpl-site` 类，页面根节点重新声明基类值 → `<html>` 上的模板值被覆盖，表现为**只有首页换肤、内页仍是默认蓝/基础圆角**。修复：基类与模板覆盖改挂 `html.tpl-site` / `html.tpl-xxx`（`site-templates-scope.test.ts` 守卫）。
- **连带修复**：
  1. 前台 Element Plus 主色映射到 `var(--tpl-p)`（`!important` 覆盖内联主题色），站点内表单/分页/按钮随模板；后台/弹层仍用「主题颜色」；
  2. 404 页（不在 SiteLayout 内）自行拉取公开设置并应用模板主色；
  3. 设置页文案写明「模板 vs 主题颜色」的职责边界。
- **修复后结果**：首页、新闻列表/详情、核心业务列表/详情、解决方案、关于、荣誉、历程、联系我们、登录/注册/找回密码均 5/5 模板可区分（浅色与深色）；404 主色跟随模板。
- **仍待迁移**（本轮未改，保持现状）：搜索页、分类/标签页、自定义页面正文、协议/隐私页仍使用 `--mo-*` 令牌，只随全局主题/深色变化，不随行业模板主色——需要逐页把颜色令牌换成 `--tpl-*`（或做局部颜色重映射）。
- **说明**：`site_template` 只切视觉（主色/圆角/字体/母题）；首页文案与母题内容由 builder sections 决定，`应用模板包` 才覆盖首页内容、头部/页脚导航与栏目名称。

## 十五、「主题颜色」覆盖度审计与修复（2026-10-17）

- **审计结论**：不是全覆盖，发现 2 个真实缺陷 + 若干覆盖缺口。
- **已修复**：
  1. **初始化读不到主题色**：`GET /system/config/configKey/theme_color` 返回 `{code,msg:"#409eff"}`（值在 msg），`initSiteSettings` 却读 `configValue` → 主题色/Logo/侧边栏/自定义 CSS 在启动时静默失效；新增 `readConfigValue()` 归一化（data/configValue/msg），并补真实形态测试。
  2. **登录后不刷新不生效**：`UnifiedLogin` 登录成功后补调 `initSiteSettings()`。
  3. **主色控件钉死在 Mo-Blog 靛蓝**：实心/plain 主按钮、Tabs、分页、侧栏菜单激活、TagsView、输入聚焦环、品牌 Logo 渐变改为 `var(--el-color-primary)` 派生，并用 `--mo-on-primary` 配前景色（实测亮主色 #409eff 下按钮文字自动变深墨 #06272e，对比度达标）。
- **验证**：管理员后台页面重载后 `--el-color-primary` 内联 = 主题色，主按钮/分页/侧栏激活/标签页随之变化；企业前台仍按行业模板主色（设计边界）。
- **仍不覆盖（已记录）**：匿名访客与无 `system:config:query` 的用户拿不到 `theme_color`；企业官网前台以模板主色为准；统计图表（#409EFF/#E6A23C）与文章编辑器（#4f46e5）仍有硬编码；`.tpl-site` 之外的 teleport 弹层不跟随模板/主题。

## 十六、主题色彻底化：`--mo-p*` 色阶由 `--theme-primary` 派生（2026-10-17）

- **目标**：让「界面主题 → 主题颜色」（以及企业官网的行业模板主色）真正驱动全站视觉，而不是只改变量层、组件仍停在旧靛蓝。
- **实现**：
  1. `themes/mo-blog.scss` 的 `:root` 不再写死 `--mo-p50~p900`，改为 `color-mix` 从 `--theme-primary` 派生（50–500 向白、700–900 向黑，比例对齐原靛蓝色板）；`handleThemeStyle` 写内联 `--theme-primary`，改主题色即整站换色。
  2. 深色模式（`theme-dark.css` 与各页深色块）的 `--mo-p50/p300/p500` 改为「主题色 × 深底 / 白色」混合，保持深色可读性；`p600–p900` 维持原约定不参与深色重映射。
  3. `themes/enterprise.scss` 移除写死的蓝色板，改为 `--theme-primary: var(--tpl-p) !important`：企业官网整站（含搜索/分类/标签/自定义页/协议页等原 `--mo-*` 页面）统一跟随**行业模板主色**；后台继续跟随「主题颜色」。
- **验证**：模板 consult/digital 下搜索、分类、标签、自定义页、协议页的 `--mo-p600` 分别等于 `#185fa5` / `#c2410c`；主题色 `#409eff`/`#0f6e56` 下后台 `--mo-p600` 即时跟随；浅色/深色对比度扫描（前台 8 页 + 后台 6 页，阈值 <2.5:1）无新增低对比，后台浅色仅剩既有的禁用按钮/警告标签噪声。
- **遗留**：匿名访客仍拿不到 `theme_color`（需公开白名单，另行决策）；统计图表硬编码色未接线。

## 十七、主题色覆盖收尾（2026-10-17）

- **匿名与普通用户**：`theme_color` 加入前台公开白名单（`FrontConfigKeys`），`SiteLayout` 在拿��公开设置后应用主题色；匿名访客、无 `system:config:query` 的注册用户不再看到默认靛蓝。404 页优先主题色。
- **经典模式修复**：`SiteLayout` 关闭企业模板时只移除 `tpl-xxx`、漏移除 `tpl-site`，经典站仍被 `html.tpl-site` 的模板变量接管（主题色失效）。抽成 `applySiteAppearanceClasses()` 并单测（`siteAppearance.test.ts`）后修复。
- **图表接主题色**：`getChartThemeColors()` 增加 `primary/primaryLight/success/warning/danger`，统计概览/文章/用户页的写死色（`#409EFF`/`#67C23A`/`#E6A23C`）改为读取主题色。
- **组件级硬编码清理**：消息框确认按钮、标签/分类选择器、头像上传、富文本编辑器、文章卡片、401/404 错误页、后台文章编辑器与页面示例区块、默认头像占位色，全部改为令牌派生。
- **防回归守卫**：新增 `src/theme-color-contract.test.ts` 扫描源码，禁止在声明与 ECharts option 中写死品牌色（`var()` fallback 与预设选择器白名单除外）。
- **验证**：匿名经典模式实测 `--el-color-primary` = 主题色 `#334155`、`--mo-p600` 同步、主按钮 `rgb(51,65,85)`；企业模式仍为模板色 `#185fa5`；统计页 `getChartThemeColors().primary = #334155`；404 容器主色 = 主题色。全量门禁（前端测试/构建 + 后端 `mvn clean verify`）通过。

## 十八、深浅色配色审计与修复（2026-10-17）

- **触发**：菜单管理页「链接类型/编辑」等颜色在深/浅色下观感异常。
- **根因 1（暗色变量被覆盖）**：`variables.module.scss` 同时被 JS（`import variables from '...module.scss'`）与 `layout/index.vue` 的 scoped 样式引入，Vite 生成 **哈希类名副本**（`html:not(._dark_xxx)` / `html[data-v-xxx]:not(.dark)`）；暗色模式下这些「非 dark」规则命中 `<html>`，把浅色变量覆盖到暗色值上（实测 `--el-text-color-secondary` 在 dark 下仍为 `#78716c`）。
  - 修复：拆分为 `_vars.scss`（SCSS 变量）+ `variables.scss`（全局 CSS 变量，仅 index.scss 引入）；删除 `variables.module.scss`；侧栏 JS 值改 `sidebarTheme.ts`，宽度用 `--base-sidebar-width`。
- **根因 2（一刀切暗色规则）**：暗色块里 `.el-tag`、`.el-button--default` 全部灰化 → 标签语义色丢失、文字按钮（编辑/删除）颜色不分；实心主按钮前景色泄漏到 `is-link`。已按 `:not()` 精确作用域修复。
- **根因 3（未提亮深色）**：深色主题色（`#334155`）直接压在深底上不可读。暗色下 Element 主色改为向白混合派生，并同步 light/dark 变体与配对前景色。
- **配色校准**：浅色 `success/warning/danger/info/placeholder` 压到 ≥4.5:1；暗色 `effect="dark"` 语义标签改深底+提亮文字；面包屑、侧栏 hover/激活、默认文字按钮、info 标签、导航 CTA 前景全部令牌化。
- **验证**：后台 6 页 + 前台 8 页（含搜索/分类/文章/关于/联系/登录）在浅色与深色下按 **4.5:1**（排除禁用态）扫描均为 0 处；菜单管理页两种模式截图核对。
- **说明**：语义色（成功/警告/危险）为全局校准，会影响所有使用这些 Element 语义色的按钮/标签（更深的绿/橙/红），属预期。

## 十九、官网内容配置体验改版（2026-10-17）

- **问题**：原「官网内容」页签是长列表 + 纯 placeholder 输入：无字段标签、无排序、无分区操作，页面又长又难定位。
- **改版**：
  - 8 个内容分区卡片：标题 / 说明 / 条数 / 「恢复默认」/ 折叠；
  - 扁平列表用 schema 驱动的 `ContentFieldsEditor`（显式字段标签、12 栅格、行内上移/下移/删除、空态提示、添加按钮）；
  - 发展历程改为「阶段卡片 + 阶段内事件」两级编辑；
  - 页面文案按 5 个模板页逐页编辑，字段带默认值提示；
  - 深/浅色与窄屏（≤900px 自动单列）适配。
- **不变**：数据键与 JSON 结构、保存接口、前台回退逻辑完全不变；改版只改编辑态 UI。
- **测试**：`index.test.ts` 新增分区渲染、行排序、分区恢复默认、计数同步、页面文案恢复默认 5 条行为用例。

## 二十、侧栏菜单图标修复（2026-10-17）

- **现象**：后台侧栏「解决方案管理」没有图标（一块空白），其余菜单正常。
- **根因**：`sys_menu` 种子给 2012 配的 `lightning` 不在前端 `src/assets/icons/svg/` 图标集中；`SvgIcon` 直接输出 `<use href="#icon-lightning">`，找不到 symbol 即为空，不报错。
- **修复**：种子与存量库先后改为 `skill`、`guide`、`star`：`skill` 与「媒体管理」的 `component` 太接近，`guide` 又与「菜单管理」重复，最终用 `star`（星标/精选方案）；SQL 保留幂等迁移 `skill←lightning`、`guide←skill`、`star←guide`，并用 `menu-icons.test.ts` 断言解决方案图标不得与其他站点菜单重复。
- **防回归**：新增 `src/assets/icons/menu-icons.test.ts`（解析 SQL 种子的 sys_menu 图标 ↔ svg 目录比对）；`SvgIcon` 增加缺失兜底（回退 `component` 图标），避免配置错误时再出现空白。

## 二十一、默认隐藏「系统工具」菜单（2026-10-17）

- 代码生成 / 系统接口属研发工具，客户侧不需要出现在默认后台导航。
- 处理：`sys_menu` 中 3（系统工具）的 `visible` 改为 `'1'`（隐藏），SQL 种子与存量库幂等迁移同步；菜单与 `tool:*` 权限保留，子路由仍可 URL 直达，可在「菜单管理」中打开「可见」恢复。
- 验证：`/getRouters` 中 `Admin/tool` 为 `hidden=true`；侧栏实测不再出现「系统工具 / 代码生成 / 系统接口」，其余菜单正常。

## 二十二、核心业务 / 产品或服务发布链路审计（2026-10-17）

- **快捷发布污染内容格式**：后台列表「发布 / 转草稿」原先调用 `updateArticle` 提交整包字段（且列表查询不含 content/format），后端 `parseArticleFromParams` 会把缺省的 `format` 默认成 html → Markdown 文章被静默转成 HTML，mapper 的无条件 `publish_time = #{publishTime}` 还会清掉定时时间。
  - 修复：前端改走 `PUT /system/article/status`（只提交 ids + status）；后端 `format` 仅在显式提交时覆盖；插入时由服务层补默认 `html`。
- **状态接口 ids 强转隐患**：Jackson 把 JSON 数字反序列化为 `Integer`，控制器 `(List<Long>)` 强转后 service 迭代会 `ClassCastException`；改为 `parseTagIds` 统一转 `List<Long>`，并校验 status/isTop/isRecommend 非空。
- **固定栏目类型漂移**：核心业务/解决方案页允许把文章改成其他类型，发布后从当前列表消失；固定栏目改为只显示当前类型（不可切换）。
- **定时发布不清缓存**：`publishScheduledArticles` 补 `@SiteCacheEvict(keyPattern = "zhi:*")`，并加入缓存失效契约测试的写方法清单。
- **草稿污染 PV/UV**：`getArticleDetail` 原先先计数再校验发布状态；调整为「发布/删除校验 → 计数」，草稿/已删除 ID 直接访问不再增加浏览量或写访问明细。
- **验证**：后端全量 `mvn clean verify` 89 测试通过（ArticleController 31 / ArticleServiceImpl 72 / 缓存契约 3 / FrontController 20）；前端 216 文件 / 1896 测试、lint/format/typecheck/build 全绿。

## 二十三、官网内容按标签页分组（2026-10-17）

- 官网内容从「8 张卡片纵向铺开」改为 **4 个标签页**：首页内容（指标/成果/方案）、荣誉与历程（荣誉墙/时间轴）、联系与问答（渠道/FAQ）、页面文案（5 个模板页）。
- 标签页内仍复用 `ContentSectionCard` + `ContentFieldsEditor`，字段标签、排序、恢复默认、折叠能力不变；数据模型与保存链路完全不变。
- 验证：`el-tabs` 实测标签为「首页内容 / 荣誉与历程 / 联系与问答 / 页面文案」，切换后对应分区显示；`index.test.ts` 新增标签分组断言。
