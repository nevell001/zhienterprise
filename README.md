# 知企建站系统（ZhiEnterprise）

企业官网建站系统：Spring Boot 3.5 + Vue 3 + Element Plus + TypeScript。
从博客形态演进为企业官网形态——行业模板、区块化首页、品牌定制、内容与获客一体化。

## 🚀 项目特色

- **企业官网模板**：5 套行业模板（`home_layout=business`），首页区块化（Hero / 数据指标 / 核心业务 / 解决方案 / 成果 / 客户 / 新闻 / CTA），后台一键切换行业模板包
- **品牌定制**：站点 Logo、首页 Banner（**最多 5 张多图轮播**，单图自动退化）、站点名称/描述/版权/备案、自定义页面与表驱动导航、联系电话/地址
- **内容体系**：文章（资讯 / 核心业务 / 客户案例 / 解决方案）、分类、标签、伪静态（`slug`/`alias`）、文章级 TDK、定时发布、Markdown 与富文本双编辑器
- **获客转化**：联系我们页**在线咨询表单**（复用留言存储与审核）、留言板、友链申请、文章底部咨询 CTA
- **SEO 友好**：`/sitemap.xml`、`/robots.txt`、RSS、动态 title/description/keywords/canonical、JSON-LD（Organization / WebSite / NewsArticle / BreadcrumbList）
- **统计完善**：按访客 24h 去重的浏览量、每日 PV/UV 聚合、访问明细（筛选/导出/清理）
- **安全**：JWT + 图形验证码 + 邮箱码防爆破、IP 限流、XSS 消毒、防盗链白名单后台化
- **权限与开关**：Spring Security 细粒度权限；站点功能开关前后端口径一致（关闭后前台隐藏入口且接口拒绝）
- **一键部署**：Docker Compose 管理前后端、MySQL、Redis；Prometheus/Grafana 监控栈可选（`--profile monitoring` 启用）

> 企业官网定位下的功能取舍：评论默认关闭（保留可逆开关）、留言板默认关闭、咨询表单默认开启；点赞/分享/侧栏/归档/作者主页/收藏等博客功能已下线（详见 `docs/ENTERPRISE_ROADMAP.md`）。

## 🌐 访问方式

| 入口 | 地址 |
| --- | --- |
| 站点前台 | http://localhost:3000/ |
| 管理后台 | http://localhost:3000/admin |
| API 文档 | http://localhost:8080/swagger-ui.html |
| Prometheus / Grafana | http://localhost:9090 / http://localhost:3001（需 `--profile monitoring` 启用） |

默认账号 `admin`，默认密码 **`admin123`**（这是若依框架广为人知的默认口令，⚠️ **部署到公网前必须立即修改**；
`sql/00_init_database.sql` 里的 bcrypt 哈希即对应此口令。系统已把 `sys.user.initPassword`
改为 `CHANGE_ME` 并在首次登录时提示改密，但 `admin` 自身的种子密码仍是 `admin123`）。

若需改用自定义初始口令，请在导入数据库后执行：

```sql
UPDATE sys_user SET password = '<你的 bcrypt 哈希>' WHERE user_name = 'admin';
```

## 🚀 快速开始

### 环境要求

- Java 17+、Maven 3.6+
- Node.js 20.19+（Vite 7 要求，22.12+ 亦可）
- MySQL 8.4+、Redis 6.2+
- Docker Engine / Desktop / Colima（推荐，用于完整本地环境）

### 1. Docker Compose（推荐）

```bash
git clone https://gitee.com/nevell/zhienterprise.git
cd zhienterprise
cp .env.example .env          # 按需修改关键配置，DB_NAME 默认 zhienterprise
docker compose -f docker-compose.dev.yml up -d --build

docker compose -f docker-compose.dev.yml ps            # 查看状态
docker compose -f docker-compose.dev.yml logs -f zhi-admin
```

生产/接近生产改用 `docker-compose.prod.yml`。项目**没有**默认 `docker-compose.yml`，必须明确指定 dev 或 prod。

### 2. 手动开发启动（可选）

需自行准备 MySQL 与 Redis 并初始化数据库：

```bash
mysql -u root -p
CREATE DATABASE zhienterprise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
mysql -u root -p zhienterprise < sql/00_init_database.sql
```

后端：

```bash
# 1) 从根目录安装各模块（zhi-system 以 jar 形式被 zhi-admin 依赖，
#    改过 zhi-system 后必须重新 install，否则 zhi-admin 用的是 ~/.m2 里的旧包）
mvn clean install -DskipTests

# 2) 启动（.env 由 spring.config.import 自动加载，从仓库根或 zhi-admin/ 目录启动均可）
mvn -pl zhi-admin spring-boot:run
```

前端：

```bash
cd zhi-ui && npm install && npm run dev
```

> 常见坑：若本机 3306 端口被占用，可把 `docker-compose.dev.yml` 里 mysql 的端口映射改成
> `3307:3306`，并在 `.env` 中设置 `DB_PORT=3307`（JDBC URL 已支持 `DB_PORT`）。

### 3. 升级已有数据库（重要）

所有表/列/索引/菜单/权限/设置种子都收敛在唯一且**幂等**的 `sql/00_init_database.sql`，**已有库直接重跑**即可：

```bash
mysql -u root -p zhienterprise < sql/00_init_database.sql
```

> 数据卷已存在的 Docker 数据库**不会**再自动执行 `docker-entrypoint-initdb.d`，必须手动重跑一次。
> 幂等依靠 `INSERT IGNORE` 与「按标题/键判存在」实现：**已存在的数据行不会被覆盖或修复**；
> 需要修正既有数据时另写一次性数据补丁（`sql/99_fix_*.sql` 命名约定）按需执行。

**v1.1.0 的数据库变化（本版无表/列变更，仅 `zhi_setting` 种子）**：

| 变化 | 新库行为 | 存量库说明 |
| --- | --- | --- |
| 新增 `site_logo`（默认空） | 站点设置可上传自定义 Logo | 重跑脚本自动补行；不补也有后端默认值（空），不报错 |
| 新增 `site_banner`（默认空，多图逗号分隔） | 首页 Hero Banner，最多 5 张轮播 | 同上；历史单图值无需迁移，自动兼容 |
| 新增 `contact_form_enabled`（默认 `true`） | 联系我们页咨询表单，写入「留言管理」 | 缺行时按“开启”处理（与全站开关口径一致），重跑脚本后变显式行 |
| `comment_enabled` 种子默认值 `true` → `false` | 新库评论默认关闭（后台可开） | `INSERT IGNORE` **不会覆盖**存量值；如需跟随企业官网默认，见下方手写 SQL |
| 移除已下线键的种子（`like_enabled`/`share_enabled`/`sidebar_enabled`/`nav_archive_enabled`/`author_*`/`blog_author`/`blog_signature`/`zhihu_url`/`author_description`） | 新库不再有这些键 | 存量库旧行**保留无害**：前台白名单已剔除，读不到也不会展示；需要干净可手动删除 |

重跑脚本后请**清理站点设置缓存**（脚本直改数据库不会触发后端缓存失效）：

```bash
# 方式一：清 Redis
redis-cli -a "$REDIS_PASSWORD" DEL zhi:settings:all
# 方式二：后台「站点设置」随意保存一次设置（会触发缓存清理）
```

存量库可选的「对齐企业官网默认」脚本（按需执行，不会自动跑）：

```sql
-- 1) 评论默认关闭（企业官网口径；需要保留评论的站不要执行）
UPDATE zhi_setting SET config_value = 'false' WHERE config_key = 'comment_enabled';

-- 2) 可选：清理已下线功能的存量键（前台已不读取，删不删均无功能影响）
DELETE FROM zhi_setting WHERE config_key IN (
  'like_enabled','share_enabled','sidebar_enabled','nav_archive_enabled',
  'author_description','author_title','author_bio','author_location',
  'personal_website','github_url','weibo_url','zhihu_url','blog_author','blog_signature'
);

-- 3) 可选：历史 Logo/Banner 若存过「容器名绝对地址」（http://zhi-admin:8080/...），
--    前端读取时已自动收敛为相对路径，无需迁移；如需在库里直接修正可用：
-- UPDATE zhi_setting SET config_value = SUBSTRING(config_value, LOCATE('/profile/', config_value))
-- WHERE config_key IN ('site_logo','site_banner') AND config_value LIKE 'http%';
```

其它说明：

- 已下线功能的**数据表保留**（如 `zhi_bookmark` 收藏表）：接口与页面已删除，表不删以免丢失历史数据；确需清理可自行 `DROP TABLE`。
- 部署提示：`/sitemap.xml`、`/robots.txt` 在后端根路径，Nginx 同源反代时请转发到后端；PV/UV 曲线依赖每小时定时汇总（当天数据约 1 小时延迟）。
- 数据库名与应用账号为 `zhienterprise` / `zhienterprise_app`；脚本创建的账号使用占位密码，需同步为 `.env` 的 `DB_PASSWORD`
  （`ALTER USER 'zhienterprise_app'@'%' IDENTIFIED BY '...'`），否则应用连库会报认证失败。

## 📁 项目结构

```
zhienterprise/
├── zhi-admin/          # 后端启动入口 + 前台公开接口（站点/认证/SEO/上传）
├── zhi-system/         # 站点内容与系统业务（文章/分类/标签/评论/留言/页面/媒体/统计等）
├── zhi-framework/      # 框架核心（Security/JWT/过滤器）
├── zhi-common/         # 通用工具（Markdown 渲染、图片压缩、XSS、开关工具）
├── zhi-quartz/         # Quartz 定时任务
├── zhi-generator/      # 代码生成
├── zhi-ui/             # Vue3 + TS 前端（views/site 企业官网、views/admin 后台）
├── sql/                # 数据库脚本（唯一初始化/迁移入口）
├── docs/               # 项目文档（路线图、版本管理、安全加固等）
├── prometheus/         # Prometheus 配置
├── docker-compose.dev.yml
└── docker-compose.prod.yml
```

## 🎯 核心功能

### 前台展示

- **企业官网首页**：区块化落地页（Hero / 指标 / 核心业务 / 解决方案 / 成果 / 客户 / 新闻 / CTA），5 套行业模板，支持自定义页面接管首页
- **核心业务 / 解决方案**：行业与场景筛选、卡片列表、详情页；面向“客户挑战 / 方案架构 / 实施路径 / 成果数据”的叙事结构
- **新闻动态**：头条大图 + 次条网格；新闻详情采用企业官网结构（面包屑 → 分类/日期 → 关键词 → 相关阅读 → 咨询 CTA → 上/下一篇）
- **关于我们 / 荣誉资质 / 发展历程 / 联系我们**：联系方式分渠道卡片、到访指引、FAQ、**在线咨询表单**
- **分类 / 标签 / 搜索 / 留言板（可开关）/ 自定义页面 / 用户协议 / 隐私政策 / RSS**
- **品牌定制**：自定义 Logo、首页 Banner 多图轮播（最多 5 张，单图自动退化）
- **认证页**：登录 / 注册 / 找回密码，统一企业官网风格（邮箱验证码 + 图形验证码）
- **主题**：浅色 / 深色 / 跟随系统，深浅色文字按 WCAG AA 校验；支持「减弱动效」

### 后台管理

- **站点设置**：站点信息（名称/描述/品牌图/版权/备案/联系信息/首页显示/监控入口）、SEO 优化、界面主题、官网内容（页面文案/渠道/FAQ/荣誉/历程/数据）、功能设置、协议页面、邮件服务
- **内容管理**：文章（草稿/发布/定时、Markdown 与富文本、置顶/推荐）、核心业务管理、解决方案管理、分类、标签、媒体库
- **运营管理**：评论（审核/导出）、留言管理（审核/回复/删除/导出，含咨询表单提交）、友链申请审核、导航管理、页面管理（区块化页面）
- **系统管理**：用户 / 角色权限 / 系统配置 / 操作日志 / 定时任务 / 代码生成 / 访问明细与统计

### 系统与统计

- 统一认证（JWT）、注册与邮箱找回密码（图形验证码 + 邮箱码防爆破）
- 图片压缩/缩放/水印上传并自动入库、防盗链（Referer 白名单后台可管理）、XSS 消毒
- 邮件通知（评论/回复/审核结果，可开关）+ 过期验证码定时清理
- 浏览量按访客 24h 去重；每日 PV/UV 聚合与曲线
- `/sitemap.xml` + `/robots.txt` + RSS + 前台动态 SEO meta + JSON-LD
- 监控（Actuator / Prometheus / Grafana）、全局错误保护

## 🏗️ 技术栈

- **后端**：Spring Boot 3.5（Java 17）、MyBatis + PageHelper、MySQL 8.4、Redis 6.2、Spring Security 6 + JWT、flexmark（Markdown 渲染）、Thumbnailator（图片压缩）、Quartz
- **前端**：Vue 3.5 / TypeScript 5.9 / Vite 7、Element Plus 2.13、Pinia、Vue Router、Quill + marked、DOMPurify、ECharts

## 🔧 配置说明

环境变量模板见 `.env.example`，核心项：

```bash
# 数据库
DB_HOST=localhost
DB_USERNAME=zhienterprise_app
DB_PASSWORD={your_app_password}
DB_ROOT_PASSWORD={your_root_password}
DB_NAME=zhienterprise

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD={your_password}

# JWT（≥64 字符）
R_TOKEN_SECRET={your_secret_key}
```

- `R_TOKEN_SECRET` 建议用 `openssl rand -base64 64` 生成
- 生产环境缺关键密钥时 `SecurityConfigValidator` 会**阻止启动**；开发环境可用 `SECURITY_VALIDATION_ENABLED=false` 跳过
- 邮件以 `MAIL_*` / yml 作为初值，**到后台 → 站点设置 → 邮件服务 填写并「测试连接」，保存后热更新生效、以数据库为准**；开发可设 `EMAIL_DEV_PRINT_CODE=true` 让验证码打印到控制台而不发信
- **用户注册默认关闭**：真实开关是 `sys_config` 的 `sys.account.registerUser`，且只认字面 `"true"`（`"1"`/`"on"` 都算关闭）。请从「站点设置 → 功能设置 → 用户注册」切换，**不要手改 SQL**——`sys_config` 有 Redis 缓存，直接改库不生效
- **站点功能开关**（站点设置 → 功能设置，规则统一：`false`/`0` 为关闭，缺省视为开启）：评论（默认关）、留言板（默认关）、咨询表单（默认开）、搜索、浏览量统计、页脚/版权/备案、邮件通知、防盗链等
- 防盗链（`referer_*`）与邮件通知（`email_notify_enabled`）默认取 .env/yml，**保存到后台设置后以数据库为准**
- 图片压缩见 `application.yml` 的 `image.compress`（默认开启，阈值 2MB、最大 2560×1440、头像 200、缩略图 400）
- **监控入口**（后台监控页里的 Prometheus / Grafana / Actuator 地址）默认按「站点访问地址」自动推导
  （域名 + 9090 / 域名 + 3001，Actuator 走同源 `/manage/actuator`），也可在
  后台 → 站点设置 → 站点信息 → **监控入口** 单独填写其它域名；换域名**无需重新构建前端**。
  生产 compose 出于安全只把 Prometheus / Grafana 绑定在 `127.0.0.1`，如需外网访问请自行反代或使用隧道
- 上传端点：`/common/upload[/compressed|avatar|thumbnail|article-cover|mobile|watermark]`、`/common/uploads`、用户头像 `/system/user/profile/avatar`、图片处理 `/system/image/*`
- **上传图片地址口径**：后台只保存站内相对路径（`/profile/upload/...`），前端统一经 `normalizeUploadUrl()` 收敛历史绝对地址；反代需放行 `/profile/`（仓库自带 `nginx.conf` 已配置）

## ❓ 常见问题

1. **升级后缺新菜单/新权限/新字段/新设置项，或评论数不一致**：重跑 `sql/00_init_database.sql`（幂等，会补齐并重算评论数），然后清 `zhi:settings:all` 缓存；注意**已存在的数据行不会被修复**，需修正既有数据时另写一次性补丁（`sql/99_fix_*.sql`）
2. **「每日阅读 PV/UV」没有数据**：等定时任务（每小时）首次汇总；当天数据约 1 小时延迟
3. **`/sitemap.xml`、`/robots.txt` 404**：生产反代把这两个根路径转发到后端（:8080）
4. **邮箱验证码/通知邮件收不到**：到「站点设置 → 邮件服务」填 SMTP 并点「测试连接」（失败原因会直接显示），确认「邮件服务」卡片上的 dev-print-code 状态；开发期可设 `EMAIL_DEV_PRINT_CODE=true` 在控制台查看验证码
5. **注册页提示「未开放注册」**：`sys.account.registerUser` 默认关闭，去「站点设置 → 功能设置 → 用户注册」打开（只认字面 `true`，且必须走后台，直接改库不会生效）
6. **上传的 Logo/Banner 显示裂图**：确认反代已放行 `/profile/`；历史版本可能存过 `http://zhi-admin:8080/...` 这类容器名地址，前台已自动收敛为相对路径，到后台「站点信息 → 品牌图」重新保存一次即可把库里的旧值写回相对路径
7. **访问 `/archive`、`/author/:id`、`/bookmarks` 显示 404**：这些博客功能已随企业官网定位下线，属预期行为
8. **咨询表单提交后在哪里看**：数据写入「留言管理」（与留言板同一张表、同一套审核），按 `comment_review` 决定是否需审核后公开
9. **媒体删除后文件仍在 / 角色 2 看不到媒体管理**：前者是“尽力删除”（占用时仅删记录）；后者确认已重跑 00 并在角色管理中补勾权限
10. **登录后闪退、改了前端代码页面没变化**：确认后端已重启；前端清浏览器缓存，容器内 dev server 已启用轮询监听
11. **监控页里的 Prometheus/Grafana 地址是 localhost 或打不开**：地址按「站点访问地址」推导，先确认后台该地址已填成真实域名；也可在「监控入口」里直接写完整地址。生产环境这两个服务默认只监听 `127.0.0.1`（安全考虑），外网访问需自行反代或走 SSH 隧道；且监控栈默认不随 `up -d` 启动，需 `docker compose -f docker-compose.prod.yml --profile monitoring up -d` 显式启用

## 📚 文档

- `AGENTS.md` — 开发约定与踩坑记录（改动前必读）
- `docs/ENTERPRISE_ROADMAP.md` — 企业官网改造路线图与各阶段取舍
- `docs/VERSION_MANAGEMENT.md` — 版本号唯一来源与发版流程
- `docs/SECURITY_CONFIG.md` / `docs/SECURITY_HARDENING.md` — 安全配置与服务器加固
- `docs/图片压缩功能使用指南.md` — 图片上传/压缩说明

## 📄 许可证

本项目基于 [MIT 许可证](LICENSE) 开源。

## 📊 项目信息

- **项目名称**：知企建站系统（ZhiEnterprise）　**当前版本**：v1.1.0（标签 `v1.1.0`）　**Maven GroupId**：top.nevell
- **项目地址**：https://gitee.com/nevell/zhienterprise
- **维护者**：nevell
- **最后更新**：2026-10-17
