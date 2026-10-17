# 知企建站系统（ZhiEnterprise）

企业官网建站系统：Spring Boot 3.5 + Vue 3 + Element Plus + TypeScript。

## 亮点

- **行业模板**：5 套模板 + 区块化企业首页（Hero / 数据 / 核心业务 / 解决方案 / 成果 / 客户 / 新闻 / CTA）
- **品牌定制**：站点 Logo、首页 Banner 多图轮播（≤5 张，单图自动退化）、版权/备案、表驱动导航、自定义页面
- **内容体系**：文章（资讯/核心业务/客户案例/解决方案）、分类标签、伪静态 `slug`/`alias`、文章 TDK、定时发布、Markdown/富文本双编辑器
- **获客转化**：在线咨询表单、留言板、友链申请、文章底部咨询 CTA
- **SEO**：`/sitemap.xml`、`/robots.txt`、RSS、canonical、JSON-LD（Organization/WebSite/NewsArticle/BreadcrumbList）
- **安全与运维**：JWT + 图形验证码 + 邮箱码防爆破、IP 限流、XSS 消毒、防盗链；Docker Compose 部署，监控栈可选

> 企业官网定位下的取舍：评论、留言板默认关闭（可后台开启），咨询表单默认开启；点赞/分享/收藏/归档/作者主页等博客功能已下线（见 `docs/ENTERPRISE_ROADMAP.md`）。

## 访问方式

| 入口 | 地址 |
| --- | --- |
| 站点前台 | http://localhost:3000/ |
| 管理后台 | http://localhost:3000/admin |
| API 文档 | http://localhost:8080/swagger-ui.html |
| Prometheus / Grafana | http://localhost:9090 / http://localhost:3001（需 `--profile monitoring`） |

默认账号 `admin`，默认密码 `admin123` —— ⚠️ **公网部署前必须修改**（RuoYi 知名默认口令，哈希见 `sql/00_init_database.sql`）：

```sql
UPDATE sys_user SET password = '<你的 bcrypt 哈希>' WHERE user_name = 'admin';
```

## 快速开始

**环境要求**：Java 17+、Maven 3.6+、Node.js 20.19+、MySQL 8.4+、Redis 6.2+（推荐 Docker）。

```bash
# Docker（推荐）
cp .env.example .env
docker compose -f docker-compose.dev.yml up -d --build

# 手动启动
mysql -u root -p zhienterprise < sql/00_init_database.sql
mvn clean install -DskipTests && mvn -pl zhi-admin spring-boot:run
cd zhi-ui && npm install && npm run dev
```

> 项目没有默认 `docker-compose.yml`，必须指定 dev / prod / release（release 为免源码构建的发行版部署，见 [docs/DEPLOYMENT_RELEASE.md](docs/DEPLOYMENT_RELEASE.md)）；3306 端口被占用时可改映射并在 `.env` 设 `DB_PORT`。

## 升级已有数据库

所有迁移收敛在唯一幂等脚本 `sql/00_init_database.sql`，**已有库重跑即可**（数据卷已存在的库不会自动执行 initdb，必须手动跑）：

```bash
mysql -u root -p zhienterprise < sql/00_init_database.sql
redis-cli -a "$REDIS_PASSWORD" DEL zhi:settings:all   # 或后台保存一次设置以清缓存
```

- **v1.1.0 无表/列变更**，仅新增设置种子 `site_logo`、`site_banner`、`contact_form_enabled`（默认 `true`），并把 `comment_enabled` 新库默认值改为 `false`
- `INSERT IGNORE` **不会覆盖存量值**；存量库如需按企业官网默认关闭评论，手动执行：
  `UPDATE zhi_setting SET config_value='false' WHERE config_key='comment_enabled';`
- 已下线功能的旧设置行与 `zhi_bookmark` 表保留无害，无需清理；Logo/Banner 历史绝对地址无需迁移（前端自动收敛）
- 幂等靠 `INSERT IGNORE` 与「按键/标题判存在」，**既有数据行不会被修复**，修正数据请另写 `sql/99_fix_*.sql`

## 项目结构

```
zhi-admin/      后端启动入口 + 前台公开接口（站点/认证/SEO/上传）
zhi-system/     站点内容与系统业务（文章/分类/评论/留言/页面/媒体/统计）
zhi-framework/  Security / JWT / 过滤器
zhi-common/     通用工具（Markdown、图片压缩、XSS、开关）
zhi-quartz/     定时任务        zhi-generator/  代码生成
zhi-ui/         Vue3 + TS 前端（views/site 企业官网、views/admin 后台）
sql/            唯一数据库脚本   docs/  项目文档
```

## 技术栈

后端 Spring Boot 3.5（Java 17）、MyBatis、MySQL 8.4、Redis 6.2、Spring Security 6 + JWT、Quartz；
前端 Vue 3.5 / TypeScript 5.9 / Vite 7 / Element Plus 2.13 / Pinia / Quill + marked / ECharts。

## 配置要点

- 环境变量模板 `.env.example`：`DB_*`、`REDIS_*`、`R_TOKEN_SECRET`（≥64 字符，`openssl rand -base64 64`）
- 生产缺关键密钥会阻止启动；开发可用 `SECURITY_VALIDATION_ENABLED=false` 跳过
- 邮件以 `MAIL_*` 为初值，**后台「站点设置 → 邮件服务」填写并测试后以数据库为准**；开发可设 `EMAIL_DEV_PRINT_CODE=true`
- **用户注册默认关闭**（`sys_config.sys.account.registerUser`，只认字面 `true`），必须走后台开关，直接改库不生效
- **功能开关**（后台 → 功能设置）：评论（默认关）、留言板（默认关）、咨询表单（默认开）、搜索、浏览量、邮件通知、防盗链等；规则统一为 `false`/`0` 关闭、缺省开启
- 上传图片只存站内相对路径 `/profile/upload/...`，反代需放行 `/profile/`；监控入口地址按「站点访问地址」自动推导，也可在后台单独填写

## 常见问题

1. **升级后缺菜单/设置/字段**：重跑 `sql/00_init_database.sql` 并清 `zhi:settings:all` 缓存
2. **sitemap/robots 404**：生产反代把这两个根路径转发到后端（:8080）
3. **邮箱验证码收不到**：后台「邮件服务」测试连接；开发期看控制台 dev-print-code
4. **Logo/Banner 裂图**：确认反代放行 `/profile/`；到后台品牌图重新保存一次即可自愈旧值
5. **`/archive`、`/author/:id`、`/bookmarks` 返回 404**：博客功能已下线，属预期
6. **PV/UV 无数据**：等每小时定时汇总（当天约 1 小时延迟）

## 文档

`AGENTS.md`（开发约定） · `docs/ENTERPRISE_ROADMAP.md`（路线图） · `docs/VERSION_MANAGEMENT.md`（发版） ·
`docs/SECURITY_CONFIG.md` · `docs/SECURITY_HARDENING.md` （安全） · `docs/图片压缩功能使用指南.md`

## 许可证与信息

MIT License · 当前版本 **v1.1.0** · Maven GroupId `top.nevell` · 维护者 nevell · 最后更新 2026-10-17
