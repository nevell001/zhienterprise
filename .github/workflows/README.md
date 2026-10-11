# GitHub Actions 说明

仓库同时托管在 Gitee（`origin`）和 GitHub（`github` 远端，由 `sync-github.sh` 同步），两套 CI/CD 互不干扰：

| 平台 | 配置文件 | 说明 |
| --- | --- | --- |
| Gitee Go | `.workflow/*.yml` | Gitee 侧流水线，由 Gitee Go 执行 |
| GitHub Actions | `.github/workflows/ci.yml` | push / PR 触发，后端 `mvn verify` + 前端依赖审计 + lint/format/test/build |
| GitHub Actions | `.github/workflows/release.yml` | 打 `v*` tag 触发，测试构建 + 自动创建 GitHub Release（后端 jar + 前端 dist + 部署配套 zip）+ 部署骨架 |
| GitHub Dependabot | `.github/dependabot.yml` | 依赖漏洞自动提醒与升级 PR（maven / npm / github-actions），仅 GitHub 远端生效 |

## 与 Gitee Go 的差异

- 触发方式一样：push 到任意分支、向 `main` 发 PR 跑 CI；打 `v1.0.0` 这样的 tag 跑发布流水线。
- 后端命令相同：`mvn -B clean verify`（编译 + 单测 + checkstyle + JaCoCo 覆盖率门槛），无需 MySQL/Redis。
- 前端命令相同：`npm ci` + lint + format + test + build。
- **依赖审计只在 GitHub 侧**：`npm run audit`（= `npm audit --omit=dev --audit-level=high --registry=https://registry.npmjs.org`，
  命令定义在 `zhi-ui/package.json`）加在 `ci.yml` 与 `release.yml`，`npm ci` 之后立刻跑，只卡产线依赖。
  Gitee Go 侧不加：默认镜像 npmmirror **未实现** `/-/npm/v1/security/*`（实测 404），要么在 Gitee runner 上
  也显式指向 npmjs 并先手工验证一次，要么就别加。Dependabot 同样只覆盖 GitHub，`origin`（Gitee）
  那边的依赖更新仍靠人工或从 GitHub 的 PR 往回同步。
- GitHub Actions 免费额度：公开仓库不限时长，私有仓库每月 2000 分钟。

## 发布部署

打 `v*` tag 时，`release.yml` 除了构建测试，还会用 `gh release create` 自动创建 GitHub Release：**发行说明取 `docs/releases/<tag>.md`**（缺失时回落到自动生成的说明文字），附件包括 `zhi-admin.jar`、`frontend-dist.zip` 和 `deploy-assets.zip`（含 sql 初始化脚本、nginx.conf、.env.example、部署指南）。`publish` job 会先删除同 tag 的旧 Release 再创建，支持重复发布覆盖。使用内置 `GITHUB_TOKEN`（`contents: write`）。该自动发布只对后续新打的 tag 生效。

构建前会校验 tag 版本与根 `pom.xml` 版本一致（如 `v1.0.0` 必须对应 `<version>1.0.0</version>`），不一致直接失败。

基于 release 产物的部署方式见 [docs/DEPLOYMENT_RELEASE.md](../docs/DEPLOYMENT_RELEASE.md) 和 `docker-compose.release.yml`。

`release.yml` 里的 `deploy` job 是注释掉的骨架。启用步骤：

1. 在 GitHub 仓库 Settings → Secrets and variables → Actions 添加：
   - `SERVER_HOST`、`SERVER_USER`、`SSH_PRIVATE_KEY`
   - 如走 Docker 镜像方案，另加镜像仓库凭据（如 `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`）
2. 取消 `release.yml` 中 deploy job 的注释，按需修改部署脚本。

## 注意

- 两个平台都会在 push 时跑一遍测试，耗时翻倍属正常现象；如只想保留一份，可以停用 Gitee Go 的某条流水线或删掉对应 `.workflow` 文件。
- 两套配置是独立的：改 CI 行为时需要同步改 `.workflow/` 和 `.github/workflows/` 两边，或只改你想用的一侧。
