# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ZhiEnterprise (知企建站系统) is a full-stack enterprise website building system built on Spring Boot 3.5 + Vue 3 + Element Plus + TypeScript. The backend is a multi-module Maven project based on RuoYi-Vue 3.9.1, upgraded to Jakarta EE and Java 17.

**Current Version**: v1.1.0 (defined in `pom.xml`)

**Brand Identity**:
- Brand name: "知企建站系统（ZhiEnterprise）"
- Theme name: "Mo-Blog" (mo-blog) - refers to the visual theme style only
- Internal package structure uses `com.zhi.*` (framework naming convention)
- Never display "若依" or legacy "ZhiBlog/知博" branding in user-facing code

## Common Commands

### Backend (Java/Maven)

```bash
# From project root
mvn clean install -DskipTests           # Build all modules
cd zhi-admin && mvn spring-boot:run   # Run backend server (port 8080)

# Testing
mvn test -Dtest=TestClassName            # Run single test
mvn test -pl zhi-system               # Test specific module
mvn test jacoco:report                   # Coverage report
mvn checkstyle:check                    # Code style check

# Check parent version consistency across modules
for m in zhi-common zhi-system zhi-framework zhi-quartz zhi-generator zhi-admin; do
  grep -A 5 "<parent>" $m/pom.xml | grep -E "(groupId|artifactId|version)"
done
```

### Frontend (Vue 3/Vite + TypeScript)

```bash
cd zhi-ui
npm run dev              # Dev server (port 3000)
npm run build:prod      # Production build
npm run test             # Run Vitest tests
npm run test:coverage    # Coverage report
npm run lint             # Fix ESLint issues
npm run format           # Format with Prettier
```

**Important**: If HMR becomes stale, restart with `npm run dev -- --force`.

### Docker Deployment

```bash
docker compose -f docker-compose.dev.yml up -d --build    # Development
docker compose -f docker-compose.prod.yml up -d --build   # Production
```

No default `docker-compose.yml` exists - must specify dev or prod.

**Monitoring stack (optional)**: Prometheus/Grafana in prod compose are behind `profiles: ["monitoring"]` — enterprise deliveries get `up -d` without them; operators enable via `docker compose -f docker-compose.prod.yml --profile monitoring up -d`.

### Database

```bash
mysql -u root -p
CREATE DATABASE zhienterprise CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
mysql -u root -p zhienterprise < sql/00_init_database.sql
```

**SQL Migration**: 仓库仅保留唯一幂等脚本 `sql/00_init_database.sql`（历史迁移脚本已并入）。
已有数据库升级时直接重跑该脚本即可（补齐表/列/索引/菜单/权限并重算评论数）。
幂等性已在 MySQL 8.4 实测（全新建库 + 已有库重跑，数据无重复）。注意：种子数据必须显式指定
主键 `id`（或用带 `COUNT(*)` 判断的存储过程，如 `sp_insert_sample_articles`），否则
`INSERT IGNORE` 不会忽略任何行、每次重跑都会追加重复数据——`zhi_category` 与
`zhi_friend_link` 曾因此重复，脚本内已补显式 id 与历史重复行清理语句。

## Architecture

### Multi-Module Backend Structure

- **zhi-admin**: Main entry point, contains controllers for web/admin endpoints
- **zhi-framework**: Core framework (Security config, JWT filter, MyBatis, Redis config, AOP aspects)
- **zhi-system**: Business logic layer - includes both system admin and 站点内容 features
- **zhi-common**: Shared utilities (file upload, image compression, XSS filter, cache, exceptions)
- **zhi-quartz**: Scheduled task management
- **zhi-generator**: Code generation for CRUD modules

**Note**: Modules were renamed from `ruoyi-*` to `zhi-*`, but the main class is still `com.zhi.RuoYiApplication` (legacy name kept for compatibility). The package structure is `com.zhi.*`.

### Frontend Structure

- `views/site/`: Public-facing site pages
- `views/admin/`: Admin management (dynamically loaded by permissions)
- `api/site/` & `api/system/`: Typed API calls
- `stores/`: Pinia state management (TypeScript)
- 100% TypeScript migration completed (230+ files)

### Key Architectural Patterns

**Backend Request Flow**: Controller → Service → Mapper (MyBatis)

**Content Controllers** (in zhi-system):
- `ArticleController` - Article CRUD, search, status
- `CategoryController`, `TagController`, `CommentController`
- `FriendLinkController`, `SettingController`
- `UnifiedAuthController` - Unified login for admin + registered users (JWT)

**Site Feature Toggles** (stored in `zhi_setting` table):
- `comment_enabled`（企业官网默认关闭）/ `comment_review`, `view_count_enabled`
- `search_enabled`, `footer_enabled`, `copyright_enabled`, `guestbook_enabled`, `contact_form_enabled`（联系我们页咨询表单，复用留言存储）
- 品牌图: `site_logo`（优先于 `blog_avatar`）/ `site_banner`（企业首页 Hero）
- 企业官网模式: `home_page_slug` (首页接管自定义页面), `nav_category_enabled`, `nav_tag_enabled`
- **已下线**（不要再加回）：`like_enabled` / `share_enabled` / `sidebar_enabled` / `nav_archive_enabled`、`author_*` 系列、`blog_author`；`/author/:id` 与 `/bookmarks` 页面及收藏后端接口也已删除
- 企业官网模板: `home_layout` (`business`=区块化企业首页, 优先级低于 `home_page_slug`), `business_label` (核心业务栏目名，导航/首页区块共用), `contact_phone`/`contact_address`；模板模式会向前台 `<html>` 挂 `theme-enterprise` 类（`themes/enterprise.scss` 重映射 `--mo-*` 色板为稳重蓝系）
- 文章伪静态: `zhi_article.slug`（可空、未删除范围内唯一），前台详情按「数字 ID 或 slug」统一解析，sitemap 优先输出别名
- 分类伪静态: `zhi_category.alias`，`/api/category/{key}` 与 `/api/article/category/{key}` 按「数字 ID 或 alias」统一解析（alias 仅命中启用且未删除的分类），前台链接 alias 优先
- 文章内容类型: `zhi_article.article_type` (`news` 资讯 / `business` 核心业务 / `case` 客户案例)；后两类的规范入口是 `/api/business`，管理端「核心业务管理」菜单复用文章管理组件并按路由固定类型
- 文章级 TDK: `zhi_article.seo_title/seo_keywords/seo_description`（详情页 SEO 与 JSON-LD 优先使用，回退标题/摘要）；JSON-LD 注入走 `utils/jsonld.ts`（Organization 在 SiteLayout、Article 在详情页，卸载时移除）

**Public vs Admin API Pattern**:
- `/api/**` endpoints: Public-facing, use `@Anonymous` annotation for unauthenticated access
- `/system/**` endpoints: Admin backend, require JWT + `@PreAuthorize("@ss.hasPermi(...)")`
- Some entities have both: e.g., friend links use `/system/friendLink/*` (admin) and `/system/friendLink/front/list` (public)

**Notification System** (`zhi_notification` table):
- Triggered by: comment submission, comment audit (approve/reject)
- Recipient logic in `CommentServiceImpl.sendCommentNotification()`:
  - Reply (`parentId > 0`): notify parent comment's author
  - Top-level: notify article's `authorId`
  - Self-notifications skipped (commenter == recipient)
- Audits use type=`audit` / `reject`, comments use type=`comment` / `reply`
- Frontend bell in both `SiteLayout.vue` (站点前台) and `layout/components/Navbar.vue` (admin)

**Article Search**:
- Primary: FULLTEXT search using `MATCH(title, content) AGAINST(...)` requiring `ft_article_title_content` index
- Fallback: `ArticleServiceImpl` catches `UncategorizedSQLException` and falls back to LIKE search, sets `fullTextDisabled` flag for the session
- This means search works even if migrations aren't applied (with degraded performance)

**Comment Status Values**:
- `0` = 待审核 (pending review)
- `1` = 已发布 (published, also used for approved)
- `2` = 已删除 (deleted, also used for rejected)

The `comment_review` 站点设置 controls whether new comments default to status `0` or `1`.

### Version Management

Version defined in `pom.xml` (`<version>` and `<app.version>`). When updating:
1. Update root `pom.xml` `<version>` and `<app.version>`
2. Update all 6 sub-module parent `<version>` references
3. Frontend version in `package.json` (and the two `"version"` fields in `package-lock.json`) is aligned with the backend (currently 1.1.0)

See `docs/VERSION_MANAGEMENT.md` for detailed instructions.

## Important Configuration Details

### Spring Boot 3.x Migration

- **Java 17 required** (was Java 8)
- **jakarta.* namespace** (not javax.*) - ensure dependencies support Jakarta EE
- **Spring Security 6.x APIs**: `antMatchers()` → `requestMatchers()`, `authorizeRequests()` → `authorizeHttpRequests()`
- No `WebSecurityConfigurerAdapter` - uses `SecurityFilterChain` bean

### Security Configuration

Located in `zhi-framework/config/SecurityConfig.java`:
- Public site frontend (`/api/**`, `/common/site/**`) allows anonymous access
- Admin backend requires JWT authentication
- Swagger, Druid, Actuator open in non-prod environments

### Security Validation

Automatic validation at startup via `SecurityConfigValidator`:

**Required Configs** (use `openssl rand -base64 64` for secrets):
- `R_TOKEN_SECRET`: Must be ≥64 characters
- `DRUID_PASSWORD`: Must be non-empty
- `REDIS_PASSWORD`: Must be non-empty
- `DB_PASSWORD`: Must be set and not a weak password

**Environment Behavior**:
- **Dev**: Shows warnings for missing configs, allows startup
- **Prod**: Blocks startup if any required security config is missing

Disable validation in dev only: `export SECURITY_VALIDATION_ENABLED=false`

### Environment Variables

Configure via `.env` file (copy from `.env.example`). Loaded by `spring.config.import`
in `application.yml` (`optional:file:./.env[.properties]` with `../.env` fallback), so it
resolves whether the backend starts from the repo root or from `zhi-admin/`. Note that
`cd zhi-admin && mvn spring-boot:run` forks the app with `zhi-admin/` as working directory —
historically spring-dotenv could not find the root `.env` from there and every `${DB_*:default}`
fell back (e.g. `root`), which is why bare local startup failed against a passworded MySQL.

```bash
# Database
DB_HOST=localhost
DB_USERNAME=zhienterprise_app
DB_PASSWORD={your_password}
DB_NAME=zhienterprise

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD={your_password}

# JWT
R_TOKEN_SECRET={your_secret_key_64_chars}

# Email (for verification codes)
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD

# Captcha
CAPTCHA_ENABLED
```

### Image Compression

Three upload endpoints with intelligent compression:
- `/common/upload/compressed` - General compression
- `/common/upload/avatar` - Avatar compression (200x200)
- `/common/upload/thumbnail` - Thumbnail compression (400x400)

Configurable in `application.yml` via `ImageCompressConfig`.

## Key Files to Reference

- `AGENTS.md` - Comprehensive development guide
- `docs/VERSION_MANAGEMENT.md` - Version update procedures
- `docs/SECURITY_CONFIG.md` - Security configuration details
