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
PageRenderer/registry + 14 类模块组件 + 导航管理（site_nav_item）+
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
