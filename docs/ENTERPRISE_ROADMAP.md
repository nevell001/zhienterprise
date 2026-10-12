# 企业建站化改造路线图（ENTERPRISE_ROADMAP）

> 定位：记录 ZhiEnterprise 从博客系统转型为企业建站系统的**已定决策与分阶段规划**，
> 供后续迭代按阶段验收。改造现状一句话：**机制层已完成（首页接管、导航裁剪、品牌化），
> 内容层与视觉层待补齐（企业内容模型、区块化首页、页面搭建体验）**。

## 一、现状盘点（2026-10）

已完成（机制层）：

- 企业官网模式：`home_page_slug` 首页被自定义页面接管，文章列表迁至 `/blog/articles`；
- 导航裁剪：`nav_category_enabled` / `nav_tag_enabled` / `nav_archive_enabled`；
- 自定义页面（`blog_page`）：Markdown 内容 + 页面级 TDK + `show_in_nav` + sitemap 收录；
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
| 询盘转化**后置** | 不做 `blog_message` 字段扩展（公司/电话/需求类型）、不做新留言通知、不做询盘跟进管理。「联系我们」保留最小形态：静态联系信息页 + 现有留言板开关。 |
| 核心业务 = 产品**或**服务，统一建模 | **不建**独立 `blog_product` 表；`blog_article` 增加 `article_type`（`news` 新闻 / `business` 核心业务 / `case` 案例），业务条目复用分类做下级组织（产品线/服务类型）。 |
| 形态命名可配 | 新增设置键 `business_label`（默认「核心业务」，可改「产品中心」/「服务项目」/「解决方案」），统一驱动导航菜单名、首页区块标题、面包屑与 SEO 标题——同一套代码适配产品型/服务型/混合型企业。 |
| 业务详情页通用版式 | 封面/图集 + 富文本正文 + 可选属性区（产品放参数表、服务放流程/套餐），不强绑产品字段。 |

## 三、分阶段规划

### Phase 0 — 快赢与卫生（✅ 已完成，2026-10-12）

1. ✅ 品牌残留清理：SQL 种子 `RuoYi` 关键词（含已有库幂等 UPDATE）、后台 `ruoyi.zip`、
   job 帮助文案包名、prometheus `zhiblog` 标签、`/opt/zhiblog` 运维文档路径、
   `package.json`/`package-lock` 的 `zhiblog`、sonar projectKey、`components/RuoYi` 死代码目录、
   `design-spec.html` 旧「博客系统」定位文案；
2. ✅ 死代码清理：首页永不显示的 contact 弹窗（模板/脚本/亮暗样式）、
   `mo-blog.scss` 中无任何组件引用的 hero/cta/article-row/popular-*/page-btn 等规则
   （棘轮测试 `mo-blog.test.ts` 同步指向存活选择器）；
3. ✅ 前台设置白名单统一：新增 `BlogFrontConfigKeys`，`/blog/setting` 与
   `/common/blog/setting` 共用同一份白名单，消除键集合漂移；
4. ✅ 种子数据企业化：分类（公司动态/行业资讯/产品知识/技术支持 + 二级栏目）、
   18 个企业标签、6 篇企业风示例文章（发布/案例/趋势/SEO 指南/服务/招聘）、
   友链收敛为技术栈官方站点、站点设置（描述/版权/SEO/问候语）去博客味。

   > 兼容性说明：分类/标签/文章/友链种子仅对**全新数据库**生效（INSERT IGNORE 幂等）；
   > 已有库保留用户内容不动，仅 RuoYi 关键词通过值匹配的 UPDATE 迁移。

### Phase 1 — 核心批次（✅ 已完成，2026-10-12）

1. ✅ **核心业务内容类型**
   - `blog_article.article_type`（`news`/`business`/`case`）：实体 + Mapper 过滤、
     幂等 ALTER 迁移已有库，前台 `/blog/article/list` 绑定实体即获得类型筛选；
   - 管理端「站点管理 → 核心业务管理」菜单（menu 2010，复用文章管理组件，按路由
     `/admin/blog/business` 固定 `articleType=business`，权限沿用 `blog:article:*`）；
     文章管理页新增类型筛选/类型列/编辑项；
   - 前台 `/blog/business` 列表（卡片网格 + 分页）与 `/blog/business/:id` 详情页；
   - sitemap：business/case 文章写规范入口 `/blog/business/{id}`，
     `home_layout=business` 时收录列表页；
   - 设置键 `business_label`、`contact_phone`、`contact_address`（进前台白名单与设置页）。
2. ✅ **企业首页区块化模板**
   - 新增 `home_layout=business` 首页模板：Hero → 核心业务卡片 → 数据条 → 客户案例
     （无内容自动隐藏）→ 新闻动态 → CTA 联系区，优先级低于 `home_page_slug` 自定义页面；
   - 模板模式下导航注入「核心业务」入口（名称取 `business_label`）；
   - 组件样式全部走 `--mo-*`/`--el-*` token，通过 design-contract 与 dark-mode 棘轮测试。

   剩余：独立企业向主题（稳重蓝/青配色，去粉色强调）留待后续批次。

### Phase 2 — 完善批次（v1.2）

- 「关于我们」页重做（公司介绍/团队/发展历程/荣誉资质，替换 `about.vue` 博主人设）；
- 「联系我们」页（静态信息 + 地图 + 现有留言板开关，不做表单扩展）；
- `blog_page` 编辑器升级：复用文章编辑器的 Quill 富文本 + 预置区块模板
  （hero / 多列卡片 / 图文左右 / 数据条 / CTA 条）；
- 文章级 TDK 字段（`blog_article` 补 `seo_title/keywords/description`）；
- JSON-LD 结构化数据（Organization + Article）。

### Phase 3 — 竞争力（v2，按需排期）

- slug 伪静态路由（`/news/{slug}`、`/business/{slug}`，`blog_category.alias` 已有字段）；
- 询盘转化（已定后置）：`blog_message` 字段扩展、新留言通知（复用评论通知链路）、询盘跟进状态；
- 自定义页面可视化搭建（区块化结构化字段 + 前端组件渲染）；
- 模板市场 / 一键换主题。

## 四、验收口径

- Phase 1 完成后：一个服务型企业可以在**不写代码**的情况下，用后台完成
  「改导航文案 → 建核心业务条目 → 首页出现业务区块 → 发布一条公司动态」的完整闭环；
- 每阶段保持：`mvn clean install -DskipTests` 通过、`npm run test` 通过、
  `sql/00_init_database.sql` 在全新库与已有库重跑均幂等。
