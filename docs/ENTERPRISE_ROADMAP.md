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

   剩余：~~独立企业向主题~~（✅ 已完成，见 Phase 2.5）。

### Phase 2.5 — 企业主题与伪静态（✅ 已完成，2026-10-14）

1. ✅ **独立企业主题（theme-enterprise）**：`themes/enterprise.scss` 只重映射
   `--mo-p*/--mo-a*` 色板（靛蓝+粉 → 稳重蓝+青蓝）与标题字体（衬线 → 无衬线），
   不复制组件皮肤，避免双份漂移；前台布局在 `home_layout=business` 时挂
   `html.theme-enterprise`（含 Mo-Blog 深色 Element 主色修正），离开公开站点自动摘除；
2. ✅ **slug 伪静态路由**：`blog_article.slug`（幂等 ALTER + 唯一性校验，软删除不占用），
   前台详情接口按「数字 ID 或 slug」统一解析（`/blog/article/{key}`、`/blog/business/{key}`
   双入口兼容历史链接），sitemap 优先输出别名，管理端文章表单新增「URL 别名」，
   6 篇示例文章已带别名（如 `/blog/article/zhi-v1-release`）。

   备注：~~分类仍按数字 ID 访问~~（✅ alias 已接入，见 Phase 2.6）。

### Phase 2.6 — 分类 alias 伪静态（✅ 已完成，2026-10-14）

1. ✅ **分类 alias 伪静态**：`/blog/category/{key}`、`/blog/article/category/{key}`
   按「数字 ID 或 alias」统一解析（alias 只命中启用且未删除的分类）；
   前台三处分类链接改为 alias 优先、回退数字 ID；分类页先解析分类再按数字 ID
   加载文章，alias 未命中时回退「所有分类」视图；
2. ✅ **sitemap 补全**：收录启用中的分类页（alias 优先）；企业模板模式下
   联系页 `/blog/contact` 一并入图。

### Phase 2 — 完善批次（✅ 已完成，2026-10-14）

1. ✅ **文章级 TDK**：`blog_article` 补 `seo_title/seo_keywords/seo_description`
   （幂等 ALTER 迁移），管理端文章表单新增 SEO 区块，前台文章/业务详情页 SEO
   优先用文章级字段、回退标题/摘要；
2. ✅ **JSON-LD 结构化数据**：常驻布局注入 Organization（含联系方式 ContactPoint），
   文章与业务详情注入 Article（卸载时移除防路由残留），`utils/jsonld.ts` 带单测；
3. ✅ **「关于我们」企业化重做**：hero 从博主人设（头像/站长头衔）改为企业定位
   （站点名 + 描述），联系卡片补电话/地址（企业联系方式一等公民化）；
4. ✅ **「联系我们」页** `/blog/contact`：电话/邮箱/地址卡片 + 留言板入口
   （受 `guestbook_enabled` 控制），企业模板模式导航注入入口；
5. ✅ **页面编辑器升级**：预置区块模板（Hero 横幅/三列卡片/图文左右/数据条/CTA 条）
   一键插入光标处 + 前台同管线实时预览（renderMarkdown + 消毒）。

   备注：编辑器保留 Markdown textarea + 区块模板方案（未换 Quill 富文本），
   如后续需要所写即所得可再评估。

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
