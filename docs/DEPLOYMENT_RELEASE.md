# 基于 Release 产物的部署指南

本文说明如何用 GitHub/Gitee Release 下载的产物部署知企建站系统，**无需源码构建**。
日常源码部署请走 `docker compose -f docker-compose.prod.yml up -d --build`（见 README）。

## 1. 下载产物

从 GitHub Releases（推荐）或 Gitee 发行版下载当前版本的附件：

| 附件 | 说明 |
| --- | --- |
| `zhi-admin.jar` | 后端可执行包（含 common/framework/system/quartz/generator 全部模块） |
| `frontend-dist.zip` | 前端构建产物（解压后 `index.html` 在根目录） |
| `deploy-assets.zip` | 部署配套：`sql/`（`00_init_database.sql` + 全部 `99_*.sql` 存量补丁 + README）、`scripts/`（备份/恢复/升级）、nginx 配置、compose 编排（`docker-compose.release.yml` + `docker-compose.base.yml`）、`.env` 示例、本指南与 [BACKUP_RESTORE.md](BACKUP_RESTORE.md) |

## 2. 整理目录

```bash
mkdir -p deploy/sql deploy/ssl uploadPath prometheus grafana scripts docs
mv zhi-admin.jar deploy/
unzip frontend-dist.zip -d deploy/frontend-dist
unzip deploy-assets.zip -d deploy/tmp
cp deploy/tmp/sql/* deploy/sql/
cp deploy/tmp/scripts/* scripts/
chmod +x scripts/*.sh
cp deploy/tmp/DEPLOYMENT_RELEASE.md deploy/tmp/BACKUP_RESTORE.md docs/
mv deploy/tmp/nginx.conf deploy/nginx.conf
mv deploy/tmp/nginx-https.conf deploy/nginx-https.conf
mv deploy/tmp/docker-compose.release.yml ./
mv deploy/tmp/docker-compose.base.yml ./
mv deploy/tmp/prometheus/* prometheus/
mv deploy/tmp/grafana/* grafana/
cp deploy/tmp/.env.example .env
rm -rf deploy/tmp
```

最终结构：

```text
deploy/
├── zhi-admin.jar
├── frontend-dist/          # index.html 在根目录
├── nginx.conf              # HTTP 默认配置
├── nginx-https.conf        # HTTPS 配置（可选，见下文）
├── ssl/                    # HTTPS 证书目录（fullchain.pem / privkey.pem）
└── sql/
    ├── 00_init_database.sql
    ├── 99_*.sql            # 存量升级补丁（幂等，升级时由 scripts/upgrade.sh 执行）
    └── README.md
scripts/
├── backup.sh               # 备份数据库 + uploadPath
├── restore.sh              # 恢复（含恢复前安全备份）
├── upgrade.sh              # 执行 00 + 99 补丁并重启后端
└── common.sh
docs/
├── DEPLOYMENT_RELEASE.md
└── BACKUP_RESTORE.md       # 备份/恢复/升级/回滚手册
prometheus/
└── prometheus.yml
grafana/
└── provisioning/           # 已自动配置 Prometheus 数据源
uploadPath/                 # 图片上传目录（可预先创建，须可写）
docker-compose.release.yml
docker-compose.base.yml     # 公共基础设施片段（mysql/redis/监控），必须与上面文件同目录
.env
```

## 3. 配置环境变量

编辑 `.env`（从 `.env.example` 复制），至少设置：

- `DB_PASSWORD` / `DB_ROOT_PASSWORD` / `DRUID_PASSWORD` / `REDIS_PASSWORD`（强密码）
- `R_TOKEN_SECRET`（>= 64 字符随机串，生产模式缺失会阻止启动）

`DB_HOST`、`REDIS_HOST` 保持 `mysql` / `redis`（容器网络内服务名）。

## 4. 启动

```bash
docker compose -f docker-compose.release.yml up -d
```

首次启动时 MySQL 会自动执行 `deploy/sql/00_init_database.sql` 完成建库初始化。

## 5. 验证

- 站点前台：`http://服务器IP/`
- 管理后台：`http://服务器IP/admin`
- 后端健康检查：`http://服务器IP/manage/actuator/health`
- 本机调试可直接访问 `http://127.0.0.1:8080`

## HTTPS（可选）

1. 将证书 `fullchain.pem`、`privkey.pem` 放入 `deploy/ssl/`。
2. 把 `docker-compose.release.yml` 中 zhi-ui 的配置挂载从 `nginx.conf` 换成 `nginx-https.conf`。
3. 重新创建前端容器：`docker compose -f docker-compose.release.yml up -d --force-recreate zhi-ui`。

启用后 80 端口会自动 301 跳转到 HTTPS。**未放置证书时不要切换 HTTPS 配置**（nginx 会启动失败），保持 `nginx.conf` 即可。

## 监控（可选，默认不启动）

release 编排里的 Prometheus / Grafana 声明了 `profiles: ["monitoring"]`，`up -d` **不会**启动它们（企业站点交付零负担）。需要时显式启用：

```bash
docker compose -f docker-compose.release.yml --profile monitoring up -d
# 只停用监控、不动站点（不要用 down：那会把 mysql/后端/前端一起停掉）：
docker compose -f docker-compose.release.yml --profile monitoring rm -sf prometheus grafana
```

启用后：

- Prometheus：`http://127.0.0.1:9090`（抓取 `zhi-admin:8080/manage/actuator/prometheus`）
- Grafana：`http://127.0.0.1:3001`，账号密码来自 `.env` 的 `GF_SECURITY_ADMIN_USER` / `GF_SECURITY_ADMIN_PASSWORD`；已通过 provisioning 自动配置 Prometheus 数据源

## 6. 更新版本

1. **先备份**：`./scripts/backup.sh`（`upgrade.sh` 也会自动做一次安全备份）。
2. 下载新版 release 附件，替换 `deploy/zhi-admin.jar`，解压新版 `frontend-dist.zip` 覆盖 `deploy/frontend-dist`。
3. 用新版 `deploy-assets.zip` 里的 `sql/` 完整覆盖 `deploy/sql/`（**必须包含全部 `99_*.sql`**）。
4. 执行数据库升级（先跑 `99_rename_blog_to_zhi.sql`（如存在），再跑 `00`，最后跑其余 `99_*.sql`，全部幂等）：

```bash
./scripts/upgrade.sh
```

5. 确认健康检查通过后，前台/后台各抽查一遍。

MySQL/Redis 数据卷和 `uploadPath/` 会保留。升级失败、需要回滚时，按
[BACKUP_RESTORE.md](BACKUP_RESTORE.md) 用升级前备份恢复，并换回上一版 jar/前端产物。

> 只补数据库结构、不换版本时，也只需要 `./scripts/upgrade.sh --no-backup`（已手工备份）或
> `docker compose -f docker-compose.release.yml exec -T mysql … < deploy/sql/00_init_database.sql`。
> 完整策略（定时备份、异地保存、每季度恢复演练）见 [BACKUP_RESTORE.md](BACKUP_RESTORE.md)。

## 注意

- MySQL 初始化脚本只在数据卷首次创建时执行，已有数据卷不会重复建库；如需重置请先备份并删除 `mysql_data` 卷。
- **`deploy/zhi-admin.jar` 必须是文件**：文件缺失时 Docker 会按 bind mount 自动创建同名**目录**，容器里 `java -jar` 直接失败（表现为 `zhi-admin` 一直 unhealthy、`zhi-ui` 不启动）。同理 `deploy/frontend-dist` 若缺失会被建成空目录，nginx 返回 403/404。建议 `up -d` 前先确认这两个路径是文件/目录而非误建的空目录。
- 需要 Docker Compose v2（编排用 `extends` 继承公共片段）；本指南统一用 `docker compose`（v2 插件），不要用旧的 `docker-compose` v1。
- 三套编排（`docker-compose.dev.yml` / `docker-compose.prod.yml` / `docker-compose.release.yml`）解析出的 compose project 都是目录名 `zhienterprise`，容器名与数据卷同名——**不能同时运行**；从 dev/prod 切到 release 会复用同一份 MySQL 数据卷（root 密码只在首次初始化生效）。
- 默认编排开放 HTTP 80 与 HTTPS 443；HTTPS 需要先放置证书并切换配置（见上文）。
- Gitee 发行版同 tag 重跑会更新/追加附件但不会删除旧附件；变更附件清单后需在发行版编辑页手动清理，或删除该发行版（保留 tag）后重跑重建。
