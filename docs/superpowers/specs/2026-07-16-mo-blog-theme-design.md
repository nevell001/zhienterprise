# 墨 Blog 可切换全站主题设计

## 背景

`docs/design/` 提供了一套名为“墨 Blog”的 UI 设计方案，包含设计系统、组件规范和 5 个页面原型：

- `docs/design/design-spec.html`
- `docs/design/prototypes/shared.css`
- `docs/design/prototypes/home.html`
- `docs/design/prototypes/article.html`
- `docs/design/prototypes/editor.html`
- `docs/design/prototypes/auth.html`
- `docs/design/prototypes/profile.html`

本次需求是把这套设计落成知企建站系统的一个新主题。用户已确认采用“可切换新主题”策略：保留现有默认视觉，新增 `mo-blog` 主题，不直接替换旧主题。

## 目标

新增一套可切换的全站主题 `mo-blog`，覆盖博客前台、后台管理、登录注册、文章编辑和个人中心。

成功标准：

- 用户可以在前端状态中切换到 `mo-blog`，刷新后仍保持选择。
- 默认主题仍可用，现有页面结构和功能不被删除。
- `mo-blog` 主题启用时，根节点挂载稳定 class，例如 `theme-mo-blog`。
- 主题样式集中管理，避免把大批颜色、字体、阴影散落到各页面。
- 设计 Token 与 `docs/design/prototypes/shared.css` 保持一致。
- 前台页面尽量贴近原型；后台页面保留 RuoYi 的信息密度和工作流，只换主题视觉。

## 非目标

- 不引入新的 UI 组件库。
- 不重写后端接口。
- 不在第一版重做后台菜单权限、路由结构或数据模型。
- 不强制加载 Google Fonts。字体栈使用设计中的字体优先级，但以系统字体回退为默认安全路径。
- 不一次性重构全部业务组件为独立设计系统包。

## 主题架构

### 主题标识

新增主题 key：

```ts
type AppTheme = 'default' | 'mo-blog'
```

启用 `mo-blog` 时，在根容器或 `document.documentElement` 上挂载：

```html
<html class="theme-mo-blog">
```

如当前系统仍使用 `html.dark` 控制暗色模式，则 `theme-mo-blog` 与 `dark` 可以并存：

```html
<html class="theme-mo-blog dark">
```

### 状态持久化

主题状态放入现有设置 store：

- `zhi-ui/src/stores/settings.ts`
- `zhi-ui/src/store/modules/settings.ts` 如仍被旧模块引用，保持兼容

本地持久化 key 建议：

```ts
localStorage.setItem('app-theme', 'mo-blog')
```

读取顺序：

1. `localStorage.app-theme`
2. 现有 settings store 默认值
3. fallback: `default`

### 样式入口

新增集中主题样式：

```text
zhi-ui/src/assets/styles/themes/mo-blog.scss
zhi-ui/src/assets/styles/themes/index.scss
```

在全局样式入口引入：

```scss
@use './themes/index.scss';
```

所有 `mo-blog` 规则必须限定在 `.theme-mo-blog` 下：

```scss
.theme-mo-blog {
  --mo-p600: #4f46e5;
}
```

避免无前缀覆盖污染默认主题。

## 设计 Token

Token 来源于 `docs/design/prototypes/shared.css`。

### 品牌色

```scss
--mo-p50: #eef2ff;
--mo-p100: #e0e7ff;
--mo-p200: #c7d2fe;
--mo-p300: #a5b4fc;
--mo-p400: #818cf8;
--mo-p500: #6366f1;
--mo-p600: #4f46e5;
--mo-p700: #4338ca;
--mo-p800: #3730a3;
--mo-p900: #312e81;
```

`--mo-p600` 是主操作色，用于主按钮、链接激活态、导航激活态、焦点边框。

### 中性色

```scss
--mo-n50: #fafaf9;
--mo-n100: #f5f5f4;
--mo-n200: #e7e5e4;
--mo-n300: #d6d3d1;
--mo-n400: #a8a29e;
--mo-n500: #78716c;
--mo-n600: #57534e;
--mo-n700: #44403c;
--mo-n800: #292524;
--mo-n900: #1c1917;
```

正文默认使用 `--mo-n700` 或 `--mo-n800`；边框使用 `--mo-n200`；页面背景使用 `--mo-n50`。

### 强调色和语义色

```scss
--mo-a50: #fdf2f8;
--mo-a100: #fce7f3;
--mo-a200: #fbcfe8;
--mo-a400: #f472b6;
--mo-a500: #ec4899;
--mo-a600: #db2777;
--mo-success: #10b981;
--mo-warning: #f59e0b;
--mo-danger: #ef4444;
--mo-info: #3b82f6;
```

### 字体

```scss
--mo-font-sans: Inter, 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
--mo-font-serif: 'Noto Serif SC', Georgia, 'Songti SC', serif;
--mo-font-mono: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
```

文章正文使用 `--mo-font-serif`，后台管理和表单使用 `--mo-font-sans`。

### 圆角和阴影

```scss
--mo-r-sm: 6px;
--mo-r-md: 8px;
--mo-r-lg: 12px;
--mo-r-xl: 16px;
--mo-r-full: 9999px;
--mo-shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
--mo-shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);
--mo-shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.05);
```

## Element Plus 映射

`mo-blog` 不替换 Element Plus，只覆盖主题变量和常见组件视觉。

需要覆盖的 Element Plus 组件：

- `ElButton`: 主按钮靛蓝，hover 使用 `--mo-p700`，圆角 8px。
- `ElInput`, `ElSelect`, `ElDatePicker`: focus 态蓝色光晕，边框 `--mo-n300`。
- `ElTag`: 支持蓝、绿、粉、琥珀、灰五类标签。
- `ElCard`: 白底、`--mo-n200` 边框、12px 圆角、轻阴影 hover。
- `ElTable`: 表头暖灰背景，hover 行使用 `--mo-n50`。
- `ElTabs`: 下划线激活态，主色 `--mo-p600`。
- `ElDialog`, `ElMessageBox`: 12px 圆角，遮罩保持系统默认，不加重视觉。
- `ElPagination`: 当前页靛蓝背景，其余页白底边框。
- `ElMenu`: 后台侧栏使用深色 stone 背景，激活态靛蓝。
- `ElSkeleton`: 颜色与暖灰背景协调。

## 页面映射

### 全局布局

设计来源：

- `shared.css` 的 `.app-nav`
- `BlogLayout.vue` 的现有博客导航和页脚
- 后台 `layout/index.vue`、`Sidebar`、`Navbar`

落地方式：

- 前台 `BlogLayout.vue` 在 `theme-mo-blog` 下采用设计稿的 60px sticky 顶部导航。
- 后台保留现有左侧菜单布局，但应用 stone 深色侧栏、靛蓝激活态、暖灰内容背景。
- 页脚在 `mo-blog` 下减少装饰波浪，转为简洁品牌信息和链接区。

### 首页

设计来源：`home.html`

映射到：

- `zhi-ui/src/views/blog/index.vue`
- 可复用现有文章列表、热门文章、搜索逻辑

关键视觉：

- 靛蓝渐变 Hero。
- 双栏布局：文章列表 + 侧边栏。
- 文章卡片白底、边框、轻阴影 hover。
- 标签云、分类导航、热门文章侧栏。

实现约束：

- 不改变文章列表 API。
- 保留现有搜索、分页、热门文章逻辑。
- 移动端变单栏，侧栏移动到文章列表之后。

### 文章详情

设计来源：`article.html`

映射到：

- `zhi-ui/src/views/blog/article/detail.vue`
- `zhi-ui/src/components/ArticleTOC.vue`

关键视觉：

- 文章头部居中，分类 badge、标题、作者和阅读元信息。
- 主体桌面三栏：目录、正文、作者卡或相关推荐。
- 正文使用衬线字体、17px、1.9 行高、最大宽度 680px。
- 代码块深色背景，引用块靛蓝左边框。
- 评论区采用头像 + 内容 + 操作的紧凑结构。

实现约束：

- 保留现有点赞、收藏、分享、评论功能开关。
- `ArticleTOC` 只改主题样式，不改变目录生成逻辑。

### 登录注册

设计来源：`auth.html`

映射到：

- `zhi-ui/src/views/blog/auth/Login.vue`
- `zhi-ui/src/views/blog/auth/Register.vue`
- `zhi-ui/src/views/blog/auth/ForgotPassword.vue`
- `zhi-ui/src/views/login.vue` 或 `UnifiedLogin.vue`

关键视觉：

- 桌面左右分屏：左侧品牌靛蓝渐变说明，右侧表单。
- 移动端隐藏品牌图，只保留表单。
- 表单 focus 态和错误态按设计 Token 映射。

实现约束：

- 保留现有验证码、邮箱验证码、登录跳转、鉴权逻辑。
- 第三方登录图标如当前无后端能力，不新增真实入口，只保留现有功能。

### 文章编辑

设计来源：`editor.html`

映射到：

- 后台文章管理页 `zhi-ui/src/views/admin/blog/article/index.vue`
- 若项目已有独立编辑组件，则优先主题化现有编辑器和弹窗

关键视觉：

- 顶部编辑操作区：标题、保存状态、预览、发布。
- 编辑器工具栏紧凑，按钮使用图标或短文字。
- 表单区域使用三列元信息：分类、标签、封面、摘要、可见性、评论权限。

实现约束：

- 第一版不重写编辑器内核。
- 优先调整现有文章新增/编辑弹窗、富文本/Markdown 区域、发布确认视觉。
- 自动保存如当前没有完整 API 支撑，不在第一版新增真实自动保存，只设计保存状态占位或沿用现有保存动作。

### 个人中心

设计来源：`profile.html`

映射到：

- `zhi-ui/src/views/admin/system/user/user/profile/index.vue`
- 如存在前台用户中心，优先覆盖前台用户中心；否则后台个人资料页按主题化处理

关键视觉：

- 顶部渐变 cover + 头像 + 用户元信息。
- tabs 管理文章、通知、消息、账号设置。
- 表格在桌面端保持信息密度；移动端隐藏次要列。

实现约束：

- 不新增粉丝/关注等后端模型。
- 若现有数据不足，统计项只映射已有字段。

### 后台管理

设计来源：设计系统和 `profile.html` 的表格/筛选/卡片规范。

映射到：

- `zhi-ui/src/layout/index.vue`
- `zhi-ui/src/layout/components/Sidebar/*`
- `zhi-ui/src/layout/components/Navbar.vue`
- `zhi-ui/src/assets/styles/sidebar.scss`
- `zhi-ui/src/assets/styles/element-ui.scss`

关键视觉：

- 左侧菜单使用 `--mo-n900` 深色背景。
- 激活菜单使用 `--mo-p600`。
- 页面背景 `--mo-n50`，列表和表单区域白底细边框。
- 表格、表单、按钮、分页由 Element Plus 覆盖统一处理。

实现约束：

- 不重排后台 CRUD 页面结构。
- 只做主题样式覆盖和少量 class 接入。

## 组件映射

从设计到现有代码的映射如下：

| 设计组件 | 原型 class | 现有目标 |
| --- | --- | --- |
| 顶部导航 | `.app-nav` | `BlogLayout.vue`, `Navbar.vue` |
| 品牌标识 | `.brand`, `.brand-logo` | `BlogLayout.vue` brand 区域 |
| 文章卡片 | `.article-card` | `views/blog/index.vue` 的文章列表项 |
| 标签 | `.tag`, `.tc` | `ElTag`, 页面内 tag class |
| 侧栏卡片 | `.sidebar-widget` | 首页分类/标签/热门文章区域 |
| 目录 | `.toc` | `ArticleTOC.vue` |
| 文章正文 | `.article-content` | `detail.vue .content-body` |
| 作者卡 | `.author-card` | 文章详情侧栏或现有作者信息块 |
| 评论项 | `.comment-item` | 文章详情评论区 |
| 编辑工具栏 | `.editor-toolbar` | 文章编辑弹窗/编辑器区域 |
| 发布弹窗 | `.modal` | `ElDialog`, `ElMessageBox` |
| 登录容器 | `.auth-wrapper` | 登录/注册页面根容器 |
| 个人封面 | `.profile-cover` | 用户资料页头部 |
| 管理表格 | `.article-table` | `ElTable` 主题覆盖 |
| 通知项 | `.notif-item` | 如现有通知列表存在则映射；否则暂不新增 |

## 响应式策略

断点沿用设计文档：

- `lg`: `>= 1024px`
- `md`: `768px - 1023px`
- `sm`: `< 768px`

页面策略：

- 首页：桌面双栏，移动单栏。
- 文章详情：桌面目录 + 正文 + 侧栏；平板隐藏目录；移动仅正文和评论。
- 后台：不改布局机制，移动端沿用现有侧栏/抽屉策略。
- 登录：桌面左右分屏，移动只显示表单。
- 个人中心：移动隐藏表格次要列，必要时改卡片列表作为后续增强。

## 测试和验收

### 自动化验证

每个实现批次至少运行：

```bash
cd zhi-ui
npx vue-tsc --noEmit --pretty false
npx eslint src --quiet
npm run format:check
npm run test -- --run
npm run build:prod
```

如改动后端或 Maven 配置，再运行：

```bash
mvn -q verify
```

### 视觉验收

至少人工或浏览器截图检查：

- `/blog`
- `/blog/article/:id`
- `/blog/category`
- `/blog/tag`
- `/blog/archive`
- `/blog/about`
- `/blog/auth/login`
- `/blog/auth/register`
- `/login` 或统一登录页
- `/admin/blog/article`
- `/admin/system/user/profile` 或现有个人资料入口

检查点：

- 根节点 class 切换后默认主题和 `mo-blog` 都能显示。
- 移动宽度下导航、卡片、表格不重叠。
- 文章正文宽度、行高、代码块和引用块符合设计。
- 后台表格和表单仍保持可扫描的信息密度。
- 暗色模式与 `theme-mo-blog` 不互相破坏；如第一版未深度适配暗色，必须保持可读。

## 分阶段落地

### 阶段 1：主题基础设施

- 增加 `app-theme` 状态和 localStorage 持久化。
- 根节点挂载 `theme-mo-blog`。
- 增加 `themes/mo-blog.scss` 和 token。
- 覆盖 Element Plus 常用组件。
- 在设置面板或已有主题入口增加可切换项。

### 阶段 2：博客前台

- 主题化 `BlogLayout.vue`。
- 主题化首页、分类、标签、归档、关于。
- 主题化文章详情和 `ArticleTOC`。
- 主题化评论区、标签、分页、侧栏组件。

### 阶段 3：登录注册

- 主题化博客登录、注册、找回密码。
- 主题化统一登录页或管理登录页。
- 保留验证码和现有鉴权流程。

### 阶段 4：后台管理和编辑器

- 主题化后台 layout、Sidebar、Navbar、TagsView。
- 主题化后台表格、筛选表单、按钮、弹窗、分页。
- 主题化文章编辑/发布流程。
- 主题化用户资料页。

### 阶段 5：验证和收敛

- 全量前端质量门禁。
- 生产构建体积对比。
- 桌面/移动关键页面截图检查。
- 修复主题切换、暗色模式和响应式边角问题。

## 风险

- 现有页面有大量 scoped 样式，主题覆盖可能需要在页面根 class 下补专门规则。
- 后台页面很多，第一版不应追求每个业务页面完全像原型，只保证主题一致和可用性。
- 编辑器原型包含自动保存等交互，当前系统未必有对应后端能力；第一版不新增真实自动保存。
- `docs/design/` 中的文案和路由是概念原型，不能直接替换现有路由和接口。
- 全站主题切换与 `html.dark` 暗色模式共存时，需要避免颜色变量冲突。

## 决策

- 采用“主题 Token + 页面映射层”方案。
- 新主题 key 为 `mo-blog`。
- 默认主题保留。
- 第一版不新增外部依赖。
- 主题样式必须集中并限定在 `.theme-mo-blog` 下。
