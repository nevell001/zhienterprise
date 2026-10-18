# 备份、恢复、升级与回滚

本文面向已部署的知企建站系统（源码部署或 Release 产物部署），覆盖三件事：

1. 定期备份数据库与上传文件，并真正做过恢复演练；
2. 版本升级时按正确顺序执行 `00_init_database.sql` 与 `99_*.sql` 补丁，不漏补丁；
3. 升级失败时如何把应用与数据回滚到升级前。

脚本位于仓库/发布包的 `scripts/` 目录：

| 脚本 | 作用 |
| --- | --- |
| `scripts/backup.sh` | 备份 MySQL 全库 + `uploadPath/`，生成校验和与清单 |
| `scripts/restore.sh` | 从备份恢复数据库/上传目录，恢复前自动做安全备份 |
| `scripts/upgrade.sh` | 按固定顺序执行 `00` + `99_*.sql`，重启后端并等待健康检查 |
| `scripts/common.sh` | 公共函数（被上面三个脚本 source，不单独执行） |

> 约定：脚本默认部署根目录是脚本的上一级。Release 布局应把 `scripts/` 放在
> `docker-compose.release.yml` 同级（如 `/opt/zhienterprise/scripts/`）；源码部署也可直接使用仓库根目录的 `scripts/`。

---

## 1. 备份

### 1.1 备份什么

| 内容 | 位置 | 备份方式 |
| --- | --- | --- |
| 数据库（文章、页面、设置、导航、留言、用户等） | MySQL 数据卷 | `backup.sh` 使用 `mysqldump --single-transaction` |
| 上传文件（图片、附件、头像） | `./uploadPath/` | `backup.sh` 打包为 `uploadPath.tar.gz` |
| 配置与密钥 | `.env` | **不随脚本备份**，按密钥管理流程单独保存/托管 |
| 编排与 nginx 配置 | compose / nginx 文件 | 随版本仓库与 Release 附件管理 |

备份产物是一个目录：

```text
/var/backups/zhienterprise/backup-20261018-033000/
├── db.sql.gz              # MySQL 全库（含触发器/存储过程/事件）
├── uploadPath.tar.gz      # 上传目录
├── manifest.txt           # 时间、主机、库名、compose 文件等
└── SHA256SUMS             # 校验和（restore.sh 会先校验）
```

### 1.2 执行

```bash
cd /opt/zhienterprise
./scripts/backup.sh
```

常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `BACKUP_DIR` | `/var/backups/zhienterprise` | 备份输出目录（建议 700 权限） |
| `BACKUP_KEEP_DAYS` | `14` | 保留天数，`0` 表示不自动清理 |
| `UPLOAD_DIR` | `$APP_DIR/uploadPath` | 上传目录 |
| `COMPOSE_FILE` | 自动检测 release → prod → dev | 指定编排文件 |
| `DB_NAME` / `DB_ROOT_PASSWORD` | 读取 `.env` | 也可由环境变量覆盖 |

脚本使用目录锁避免并发执行；MySQL 容器未运行时直接报错，不会产出空备份。

### 1.3 定时任务

```bash
# 备份目录与日志先交给部署用户（/var/backups 默认只有 root 可写）
sudo install -d -o deploy -g deploy -m 700 /var/backups/zhienterprise
sudo install -o deploy -g deploy -m 640 /dev/null /var/log/zhienterprise-backup.log

sudo tee /etc/cron.d/zhienterprise-backup >/dev/null <<'EOF'
30 3 * * * deploy cd /opt/zhienterprise && ./scripts/backup.sh >> /var/log/zhienterprise-backup.log 2>&1
EOF
```

### 1.4 必须异地保存

本机备份只能应对误删/误操作，**不能应对服务器或磁盘损毁**。至少做到：

- 每天把 `BACKUP_DIR` 同步到另一台机器或对象存储，例如
  `rclone sync /var/backups/zhienterprise remote:zhienterprise-backup`；
- 备份里包含全部业务数据与用户信息，上传前加密（如 `age` / `gpg`）或使用服务端加密的存储桶；
- 定期检查备份任务日志与磁盘空间，避免“任务在跑但一直失败”。

---

## 2. 恢复

### 2.1 使用脚本

```bash
cd /opt/zhienterprise

# 恢复最新备份（会要求输入数据库名确认）
./scripts/restore.sh

# 指定备份
./scripts/restore.sh /var/backups/zhienterprise/backup-20261018-033000

# 只恢复数据库 / 只恢复上传目录
./scripts/restore.sh latest --db-only
./scripts/restore.sh latest --uploads-only

# 演练或自动化：跳过交互确认
./scripts/restore.sh latest --yes
```

脚本行为：

1. 校验 `SHA256SUMS`，备份损坏会直接中止；
2. 默认先执行一次**恢复前安全备份**（`SKIP_SAFETY_BACKUP=1` 可跳过）；
3. 停止 `zhi-admin` / `zhi-ui`，避免恢复过程中继续写入；
4. 导入数据库、解包 `uploadPath.tar.gz`；
5. 重新启动并等待 `/manage/actuator/health` 通过。

恢复完成后请抽查：首页、新闻/详情页、后台登录、图片是否正常；如果权限或站点设置看起来是旧值，
重新登录或等待 Redis 设置缓存过期（10 分钟）。

### 2.2 手工等价命令

脚本不可用时可按下面步骤手工恢复（以 Release 布局为例）：

```bash
# 1. 停应用
docker compose -f docker-compose.release.yml stop zhi-admin zhi-ui

# 2. 恢复数据库（须先校验 SHA256SUMS）
gunzip -c backup-20261018-033000/db.sql.gz \
  | docker compose -f docker-compose.release.yml exec -T mysql \
      sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 zhienterprise'

# 3. 恢复上传目录（覆盖同名文件）
tar -xzf backup-20261018-033000/uploadPath.tar.gz -C /opt/zhienterprise

# 4. 启动
docker compose -f docker-compose.release.yml up -d zhi-admin zhi-ui
```

说明：数据库导入采用“按表 DROP + 重建”，**不会删除备份中不存在的额外表**。如果要求恢复到与备份
完全一致的结构，请先在测试环境验证，或手工清理多余对象后再导入。

### 2.3 季度恢复演练（必做）

备份没有验证过就等于没有备份。至少每季度在**测试环境**完整走一遍：

1. 取一份生产备份（可先脱敏），在测试机执行 `./scripts/restore.sh <备份目录> --yes`；
2. 确认后端健康检查通过、后台可登录；
3. 对比关键数据量（示例）：

   ```sql
   SELECT COUNT(*) FROM zhi_article;
   SELECT COUNT(*) FROM zhi_page;
   SELECT COUNT(*) FROM zhi_message;
   SELECT COUNT(*) FROM zhi_setting;
   ```

4. 打开首页、新闻详情、联系我们、留言板，确认图片来自 `uploadPath`、表单能提交；
5. 记录演练时间、备份时间戳、恢复耗时与发现的问题。

只有数据库、没有测试环境时，可退而求其次把备份导入同机 scratch 库验证：

```bash
docker compose -f docker-compose.release.yml exec -T mysql \
  sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS zhienterprise_drill CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"'

gunzip -c backup-20261018-033000/db.sql.gz \
  | docker compose -f docker-compose.release.yml exec -T mysql \
      sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 zhienterprise_drill'

# 验证后删除演练库（确认库名无误再执行）
docker compose -f docker-compose.release.yml exec -T mysql \
  sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE zhienterprise_drill"'
```

> 注意：`sql/00_init_database.sql` 不含 `CREATE DATABASE` / `USE`，演练时库名必须由命令行显式给出；
> 这也是防止“在副本库演练却写进生产库”的硬约定（见 `sql/README.md`）。

---

## 3. 升级

### 3.1 升级包含什么

Release 的 `deploy-assets.zip` 现在包含：

- `sql/00_init_database.sql` —— 唯一幂等初始化/补结构入口；
- `sql/99_*.sql` —— 存量数据补丁（表改名、导航去重等），**全部幂等**；
- `scripts/` —— 备份、恢复、升级脚本；
- `docs/DEPLOYMENT_RELEASE.md` 与本文档。

### 3.2 标准步骤

```bash
cd /opt/zhienterprise

# 0. 先备份（upgrade.sh 也会自动做一次）
./scripts/backup.sh

# 1. 替换应用产物
#    - deploy/zhi-admin.jar
#    - deploy/frontend-dist/（解压覆盖）
#    - deploy/nginx.conf（如新版有变更）

# 2. 用新版的 sql 目录覆盖 deploy/sql
rsync -a --delete <解压后的 deploy-assets>/sql/ deploy/sql/

# 3. 执行数据库升级
./scripts/upgrade.sh

# 4. 验证
curl -fsS http://127.0.0.1:8080/manage/actuator/health
```

`upgrade.sh` 的执行顺序是硬约定：

1. `99_rename_blog_to_zhi.sql`（如存在）—— 旧库 `blog_*` 表改名必须**先于** `00`；
2. `00_init_database.sql` —— 补齐表、列、索引、种子与数据库例程；
3. 其余 `99_*.sql`（按文件名排序）—— 一次性数据补丁。

脚本会先停 `zhi-admin`、执行 SQL、再启动并等待健康检查；失败时后端保持停止状态并给出回滚提示。

- 所有脚本都应幂等：重复执行第二次为 0 行变化；
- 已登录用户的权限缓存在 Redis，表/权限升级后如权限未生效，请重新登录；
- 数据库升级通常只需秒级，但仍建议在低峰维护窗口执行。

### 3.3 只改库、不换版本

补结构/补种子（例如新增列）可以只重跑 `00`：

```bash
./scripts/upgrade.sh --no-backup    # 已手工备份时
```

不需要重建镜像或重新发布前端。

---

## 4. 回滚

数据库升级是**向前兼容、幂等**的，但 `99_*.sql` 可能修改既有数据，因此没有自动“逆向 SQL”。
回滚以备份恢复为准。

### 4.1 应用回滚（未改数据或数据可兼容）

保留上一版的 `zhi-admin.jar`、`frontend-dist/`、`nginx.conf`，替换回去后重建容器：

```bash
docker compose -f docker-compose.release.yml up -d --force-recreate zhi-admin zhi-ui
```

### 4.2 数据回滚

```bash
# 恢复到升级前的安全备份（upgrade.sh 输出的 backup-* 目录）
./scripts/restore.sh /var/backups/zhienterprise/backup-<升级前时间戳> --db-only --yes

# 如升级也动过上传目录（常规升级不会），再恢复上传：
./scripts/restore.sh /var/backups/zhienterprise/backup-<升级前时间戳> --uploads-only --yes
```

完整回滚顺序：**停应用 → 恢复数据库 → 换回旧版产物 → 启动 → 验证**。
`restore.sh` 已经包含停应用、恢复前安全备份和重启健康检查。

### 4.3 注意事项

- 恢复后 Redis 中可能残留旧会话/设置缓存：重新登录即可；必要时在维护窗口清理 Redis 缓存；
- 如果升级前没备份，请**不要**尝试手工删列/删表“逆向”升级 —— 先停站，从最近的异地备份恢复；
- `.env` 不在备份内：回滚时若改过密钥，需要把 `.env` 一并恢复到旧值，否则旧会话/JWT 会全部失效（通常可接受）。

---

## 5. 检查清单

**日常**

- [ ] 每日备份任务成功（看日志与备份目录时间戳）
- [ ] 备份已同步到异地/对象存储
- [ ] 磁盘剩余空间足够（数据库 + 上传目录 + 保留期）

**升级前**

- [ ] 已手工/自动备份，且知道备份路径
- [ ] 新 Release 的 `sql/` 已完整覆盖到 `deploy/sql/`（含 `99_*.sql`）
- [ ] 留有上一版 jar / 前端产物

**升级后**

- [ ] `upgrade.sh` 健康检查通过
- [ ] 首页、新闻详情、后台登录、图片正常
- [ ] 抽查后台内容与权限

**每季度**

- [ ] 在测试环境完成一次 `restore.sh` 恢复演练并记录结果
