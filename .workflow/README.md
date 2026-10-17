# Gitee Go 流水线说明

本目录是知企建站系统的 Gitee Go（码云 CI/CD）流水线配置，包含四条流水线：

| 文件 | 触发时机 | 作用 |
| --- | --- | --- |
| `MasterPipeline.yml` | push 到 `main` | 后端 + 前端全量测试构建 |
| `BranchPipeline.yml` | push 到非 `main` 分支 | 后端 + 前端全量测试构建 |
| `PRPipeline.yml` | 向 `main` 发起 Pull Request | 后端 + 前端全量测试构建 |
| `ReleasePipeline.yml` | 打 `v*` 开头的 tag（如 `v1.1.0`） | 全量测试构建 + 上传制品 + 自动创建 Gitee 发行版（部署骨架） |

## 启用步骤

1. 在 Gitee 仓库页面进入「流水线」，开通 Gitee Go（需要账号已绑定手机号）。开通时会自动生成默认的 `.workflow` 模板并产生一次提交，直接用本目录的文件替换即可。
   - `MasterPipeline.yml` / `BranchPipeline.yml` / `PRPipeline.yml` 会被平台按文件名自动识别；`ReleasePipeline.yml` 属于自定义流水线，如推送后未出现在流水线列表，在控制台「新建流水线」中选择该文件即可。
2. 首次运行流水线前，确认平台构建环境支持：
   - `jdkVersion: 17`（Spring Boot 3.3 要求 JDK 17+；Gitee Go 官方支持列表含 17）。
   - `mavenVersion: 3.6.3`（Gitee Go 官方最高支持 3.6.3，恰好满足 Spring Boot 3.3 的 Maven 3.6.3+ 要求；3.9.x 不在官方支持列表）。
   - `nodeVersion: '22'`（前端 Vite 7 要求 Node >= 20.19；平台插件若未提供 npm，由下方前端命令自带的 Node 22.14.0 兜底）。
   - 如果平台报版本不支持，直接在流水线可视化编辑界面修改对应字段，无需改仓库代码。
   - 后端命令会先执行 `yum install -y freetype || true`：Gitee Go 镜像缺少 `libfreetype.so.6`，而水印测试依赖 AWT 字体渲染，缺库会导致 `UnsatisfiedLinkError`；该命令在 yum 不可用时会被忽略，测试侧的容错兜底可保证构建不因此失败。
   - 前端命令不依赖平台 Node 插件：先尝试从 `cdn.npmmirror.com` 下载 Node 22.14.0 并加入 `PATH`（失败时忽略，回落到平台自带 Node），再用阿里源 registry 执行 `npm ci` 和 lint/format/test/build。若想换版本，只需同步修改 4 个流水线里的下载 URL 与解压目录。
3. 触发一次构建，验证两条链路：
   - 后端：`mvn -B clean verify` 会执行全部单测、checkstyle 和 JaCoCo 覆盖率门槛（60% 行 / 60% 分支）。
   - 前端：`npm ci` 后依次执行 ESLint、Prettier、Vitest、生产构建。

## 发布部署（ReleasePipeline）

`ReleasePipeline.yml` 包含构建、发布、部署（骨架）三个阶段：

- 构建：后端 `mvn verify` + 前端 lint/format/test/build，与 CI 一致。
- 发布：`publish@general_artifacts` 把后端 jar 和前端 dist 上传到制品库；`release@gitee` 插件自动创建仓库「发行版」（复用 `tagName` 指定的 tag，附件为后端 jar + `frontend-dist.zip` + `deploy-assets.zip`（内含 sql / nginx HTTP+HTTPS 配置 / .env 示例 / 部署指南 / prometheus / grafana 配置），与 GitHub Release 保持一致）。
- 部署：注释掉的骨架，配置方式见下。

发行版 tag：`release@gitee` 的 `tagName` 当前为 `v1.1.0`（复用触发流水线的 tag）。**每次发新版本时，把 `tagName`、`releaseName`、`description` 以及 `triggers.push.tags.exclude` 里的版本号改成对应版本**。`allowUpdate: true` 允许同 tag 重复发布时覆盖。

注意：`allowUpdate: true` 会更新/追加附件，但**不会删除旧附件**；变更附件清单后，需要在发行版编辑页手动删除旧文件，或删除该发行版（保留 tag）后重跑流水线重建。

后端构建前会校验触发 tag 与根 `pom.xml` 版本一致（`git tag --points-at HEAD` 与 `<version>` 比对）；Gitee Go 构建容器检不出 tag 时（如手动运行）仅打印告警不阻断，GitHub 侧为严格校验。

基于 release 产物的部署方式见 [docs/DEPLOYMENT_RELEASE.md](../docs/DEPLOYMENT_RELEASE.md) 和根目录 `docker-compose.release.yml`。

部署阶段（骨架）配置方式：

1. 在 Gitee Go 控制台创建「主机组」并在目标服务器安装 Agent，拿到 `hostGroupID`。
2. 取消 `ReleasePipeline.yml` 中 deploy 阶段的注释，把 `hostGroupID` 换成真实值。
3. 在流水线参数/凭据里配置敏感信息（不要提交到仓库）：
   - 服务器 SSH 密钥 / Agent 凭据
   - `R_TOKEN_SECRET`（JWT 密钥，>= 64 字符）
   - `DB_PASSWORD`、`REDIS_PASSWORD`、`DRUID_PASSWORD`
4. 服务器端确认 `docker-compose.prod.yml` 可用（MySQL 8.4、Redis 6.2+、`./uploadPath/` 挂载卷存在）。

发布流程建议：本地/仓库打 tag（`git tag v1.1.0 && git push origin v1.1.0`）→ 流水线构建测试 → 上传制品 → 服务器拉取并 `docker compose up -d`。

## 注意

- 免费额度：单个仓库开通即得 200 分钟构建时长（永久），个人/组织每月另有 500 分钟。全量流水线单次约 5~10 分钟，请勿让每个 commit 都触发发布流水线。
- `.env` 不要提交；服务器环境变量通过容器编排或 Gitee Go 凭据注入。
- tag 匹配规则如与平台实际行为不一致（通配符写法差异），在可视化界面调整触发条件即可。
