# 基于 Release 产物的部署指南

本文说明如何用 GitHub/Gitee Release 下载的产物部署知企建站系统，**无需源码构建**。
日常源码部署请走 `docker compose -f docker-compose.prod.yml up -d --build`（见 README）。

## 1. 下载产物

从 GitHub Releases（推荐）或 Gitee 发行版下载当前版本的附件：

| 附件 | 说明 |
| --- | --- |
| `zhi-admin.jar` | 后端可执行包（含 common/framework/system/quartz/generator 全部模块） |
| `frontend-dist.zip` | 前端构建产物（解压后 `index.html` 在根目录） |
| `deploy-assets.zip` | 部署配套：数据库脚本、nginx 配置、`.env` 示例、本指南 |

## 2. 整理目录

```bash
mkdir -p deploy/sql deploy/ssl uploadPath prometheus grafana
mv zhi-admin.jar deploy/
unzip frontend-dist.zip -d deploy/frontend-dist
unzip deploy-assets.zip -d deploy/tmp
mv deploy/tmp/00_init_database.sql deploy/sql/
mv deploy/tmp/nginx.conf deploy/nginx.conf
mv deploy/tmp/nginx-https.conf deploy/nginx-https.conf
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
    └── 00_init_database.sql
prometheus/
└── prometheus.yml
grafana/
└── provisioning/           # 已自动配置 Prometheus 数据源
uploadPath/                 # 图片上传目录（可预先创建，须可写）
docker-compose.release.yml
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

- 站点前台：`http://服务器IP/blog`
- 管理后台：`http://服务器IP/admin`
- 后端健康检查：`http://服务器IP/manage/actuator/health`
- 本机调试可直接访问 `http://127.0.0.1:8080`

## HTTPS（可选）

1. 将证书 `fullchain.pem`、`privkey.pem` 放入 `deploy/ssl/`。
2. 把 `docker-compose.release.yml` 中 zhi-ui 的配置挂载从 `nginx.conf` 换成 `nginx-https.conf`。
3. 重新创建前端容器：`docker compose -f docker-compose.release.yml up -d --force-recreate zhi-ui`。

启用后 80 端口会自动 301 跳转到 HTTPS。**未放置证书时不要切换 HTTPS 配置**（nginx 会启动失败），保持 `nginx.conf` 即可。

## 监控（release 编排已包含）

- Prometheus：`http://127.0.0.1:9090`（抓取 `zhi-admin:8080/manage/actuator/prometheus`）
- Grafana：`http://127.0.0.1:3001`，账号密码来自 `.env` 的 `GF_SECURITY_ADMIN_USER` / `GF_SECURITY_ADMIN_PASSWORD`；已通过 provisioning 自动配置 Prometheus 数据源

## 6. 更新版本

1. 下载新版 release 附件，替换 `deploy/zhi-admin.jar`，解压新版 `frontend-dist.zip` 覆盖 `deploy/frontend-dist`。
2. 重新启动容器：

```bash
docker compose -f docker-compose.release.yml up -d --force-recreate zhi-admin zhi-ui
```

MySQL/Redis 数据卷和 `uploadPath/` 会保留。

## 注意

- MySQL 初始化脚本只在数据卷首次创建时执行，已有数据卷不会重复建库；如需重置请先备份并删除 `mysql_data` 卷。
- 默认编排开放 HTTP 80 与 HTTPS 443；HTTPS 需要先放置证书并切换配置（见上文）。
- Gitee 发行版同 tag 重跑会更新/追加附件但不会删除旧附件；变更附件清单后需在发行版编辑页手动清理，或删除该发行版（保留 tag）后重跑重建。
