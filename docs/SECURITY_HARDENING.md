# 服务器与仓库加固清单（Security Hardening）

> 适用范围：知企建站系统的两个代码托管（Gitee `nevell/zhienterprise`、GitHub `nevell001/zhienterprise`）、
> 一台跑 Docker Compose 的生产服务器、以及开发用的 macOS 机器。
>
> 写法说明：每条都给出**为什么**、**怎么做（可直接复制的命令）**和**怎么验证**。
> 标 `[P0]` 的请优先做，`[P1]` 次之，`[P2]` 可以排期。

---

## 0. 先明确威胁模型

| 角色 | 能碰到什么 | 最坏后果 |
|---|---|---|
| 开发机（含 AI 编码助手）| 工作区代码、`~/.ssh/*`、`~/.gitconfig`、`.env` | 能推代码、能改仓库、若密钥可登录则能操作服务器 |
| Git 托管平台账号 | 仓库内容、Issues/PR、Actions/流水线 | 删库、改代码、注入流水线脚本 |
| 生产服务器 | 容器、数据库、上传目录、`.env` | 数据泄露/被删、被当作跳板 |

结论：**不要把"能推代码"等同于"能上服务器"** —— 两者必须是不同的凭据、不同的权限、可分别吊销。

---

## 1. 仓库侧（托管平台）

### 1.1 `[P0]` 开启并收紧分支保护

Gitee：仓库 → 管理 → 分支保护；GitHub：Settings → Branches → Add branch protection rule。

对 `main` 至少设置：

- 禁止任何人直接 push（含管理员），只能通过 PR/MR 合并；
- 合并前必须通过 CI（本仓库有 `.github/workflows/ci.yml` 与 `.workflow/ReleasePipeline.yml`）；
- 禁止 force push、禁止删除分支；
- 至少 1 个 review（个人项目可用"必须自己二次确认"替代，但要留记录）。

验证：`git push origin main` 应当被拒绝，PR 才能合并。

### 1.2 `[P0]` 保护 tags（防止别人借发布注入）

- GitHub：Settings → Tags → 保护 `v*`（只有 maintainer 可创建/移动）；
- Gitee：仓库 → 管理 → 标签保护（若有）；至少确认只有你自己有仓库写权限。

> 发布流水线只在 tag 推送时触发（`.github/workflows/release.yml`、Gitee Go `ReleasePipeline`），
> 能推 tag 等于能在 CI 里执行代码。**移动已发布的 tag 要慎重**（会自动重建 Release 附件）。

### 1.3 `[P0]` 账号层：2FA + 最小权限令牌

- Gitee / GitHub 都开启 2FA（密码 + 验证器）；
- 如果其他地方（CI、脚本、其他机器）需要访问，**只发细粒度令牌**：
  - GitHub fine-grained PAT：仅选本仓库、仅 `Contents: Read and write`，有效期 ≤ 90 天；
  - Gitee 私人令牌：只勾 `projects`，不要在别处复用。
- 定期在平台的 "Authorized OAuth Apps / Sessions" 里清掉不认识的会话。

### 1.4 `[P1]` 把 HTTPS push 的代理重写去掉（本机就有一个真实风险）

本机 `~/.gitconfig` 里存在过这类全局重写：

```ini
[url "https://ghproxy.net/https://github.com/"]
    insteadOf = https://github.com/
```

它的含义是：**任何** https 形式的 GitHub 操作都会被转发到第三方代理，凭据可能随请求一起发给对方。
推送请统一走 SSH（本仓库 `github` remote 已配置为 `git@github.com:nevell001/zhienterprise.git`），
并删掉代理重写：

```bash
git config --global --unset-all url."https://ghproxy.net/https://github.com/".insteadOf
git config --global --unset-all url."https://ghfast.top/https://github.com/".insteadOf
git config --global --get-regexp 'url\.'   # 应当为空
```

验证：`git push github main` 走 SSH（输出 `github.com:nevell001/zhienterprise.git`）；备份/同步脚本
`sync-github.sh` 也会在检测到代理 URL 时给出告警。

### 1.5 `[P1]` 一次性的偏门设置检查

- 关闭 "允许非管理员创建仓库/转移仓库"（个人账号可忽略）；
- Webhook / 集成只留自己在用的，逐个看 **secret 是否为空**（空 secret 的 webhook 任何人都能伪造触发）；
- Actions 里不要把 `GITHUB_TOKEN` 默认权限设为 write（见 §3.1）。

---

## 2. 服务器侧（Linux + Docker Compose）

### 2.1 `[P0]` 专用部署用户 + 最小 sudo

不要用 `root` 或自己的管理员账号跑部署：

```bash
sudo useradd -m -s /bin/bash deploy
sudo mkdir -p /home/deploy/.ssh && sudo chmod 700 /home/deploy/.ssh
# 只放部署用的公钥（与开发机个人密钥分开）
sudo tee /home/deploy/.ssh/authorized_keys >/dev/null <<'EOF'
ssh-ed25519 AAAA... zhiblog-deploy
EOF
sudo chown -R deploy:deploy /home/deploy/.ssh && sudo chmod 600 /home/deploy/.ssh/authorized_keys

# sudo 白名单：只允许固定的 docker compose 命令，且不要 NOPASSWD 到 shell
sudo tee /etc/sudoers.d/zhiblog-deploy >/dev/null <<'EOF'
deploy ALL=(root) NOPASSWD: /usr/bin/docker compose -f /opt/zhiblog/docker-compose.prod.yml *
deploy ALL=(root) NOPASSWD: /usr/bin/systemctl restart zhiblog.service
EOF
sudo chmod 440 /etc/sudoers.d/zhiblog-deploy
sudo visudo -c    # 语法校验
```

验证：`sudo -l -U deploy` 只列出上面两条；`sudo -u deploy bash` 需要密码或直接被拒。

### 2.2 `[P0]` SSH 收口

`/etc/ssh/sshd_config`（或 `sshd_config.d/*.conf`）：

```ini
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
PubkeyAuthentication yes
AllowUsers deploy
MaxAuthTries 3
ClientAliveInterval 300
ClientAliveCountMax 2
```

```bash
sudo sshd -t && sudo systemctl reload ssh
sudo apt install -y fail2ban && sudo systemctl enable --now fail2ban   # 爆破自动封禁
```

更进一步（推荐其一）：

- 只监听内网/办公网：`ListenAddress 10.0.0.5`，或用云安全组把 22 端口限制到你的出口 IP；
- 上 VPN（WireGuard/Tailscale）后关闭公网 22；
- 改非标端口只能挡扫描器，**不是**安全措施。

验证：`ssh -o PreferredAuthentications=password deploy@host` 应立即失败。

### 2.3 `[P0]` `authorized_keys` 加"锁"（即使密钥泄露也做不了别的）

给部署密钥加上来源与用途限制（`command=` 会强制走固定命令，适合只做部署的钥匙）：

```
from="203.0.113.10,198.51.100.0/24",no-agent-forwarding,no-port-forwarding,no-X11-forwarding,no-pty ssh-ed25519 AAAA... zhiblog-deploy
```

如果只是日常 SSH 维护，至少加 `from=` 与 `no-agent-forwarding`。**每把钥匙一个用途，丢了只吊销那一把。**

### 2.4 `[P0]` 文件与凭据

```bash
# .env 只给所有者可读
sudo chown deploy:deploy /opt/zhiblog/.env && sudo chmod 600 /opt/zhiblog/.env
# 上传目录与数据库卷同样收紧
sudo chmod 750 /opt/zhiblog/uploadPath
sudo ls -l /opt/zhiblog/.env    # 应为 -rw------- deploy deploy
```

- `.env` **绝不进仓库**（本仓库 `.env.example` 只有占位符；已确认 `.gitignore` 忽略 `.env`）；
- 生产用 `SECURITY_VALIDATION_ENABLED` 保持默认（让 `SecurityConfigValidator` 在缺密钥时**阻止启动**），
  只有本地调试才设 `false`；
- `docker-compose.prod.yml` 已把 admin/前端端口绑到 `127.0.0.1`，对外只经 nginx —— 保持这样，
  不要让 8080/3000/9090/3001 暴露到公网。

### 2.5 `[P1]` Docker 权限就是 root 权限

能执行 `docker` 等价于 root（可挂载宿主 `/`）。因此：

- 不要把普通用户加进 `docker` 组（用 §2.1 的 sudo 白名单跑 compose）；
- 若必须加入，等同于给该用户 root，需按 root 的态度管理；
- 镜像来源固定：生产只用自己构建的镜像与官方基础镜像，CI 里不要 `docker run` 未固定 digest 的第三方镜像。

### 2.6 `[P1]` 备份与恢复演练

```bash
# 每天 03:30 备份数据库 + 上传目录，保留 14 天
sudo tee /etc/cron.d/zhiblog-backup >/dev/null <<'EOF'
30 3 * * * deploy /opt/zhiblog/scripts/backup.sh >> /var/log/zhiblog-backup.log 2>&1
EOF
```

`backup.sh` 要点：

```bash
set -euo pipefail
STAMP=$(date +%F)
OUT=/var/backups/zhiblog
mkdir -p "$OUT"
docker exec zhiblog-mysql mysqldump --single-transaction -uroot -p"$DB_ROOT_PASSWORD" zhienterprise | gzip > "$OUT/db-$STAMP.sql.gz"
tar czf "$OUT/upload-$STAMP.tar.gz" -C /opt/zhiblog uploadPath
find "$OUT" -type f -mtime +14 -delete
```

- 备份**必须异地**（对象存储/另一台机器），否则服务器被删就一起没了；
- 上传前加密（`age`/`gpg`）或选服务端加密的存储桶；
- **每季度真做一次恢复演练**：在测试环境 `gunzip -c db-*.sql.gz | mysql zhienterprise` 并打开页面确认。

### 2.7 `[P1]` 运行时可观测与告警

- 保留 `Actuator/Prometheus` 仅供内网（本仓库监控页已走 `127.0.0.1` 绑定 + nginx 代理）；
- 对登录失败率、5xx 比例、磁盘/内存设置阈值告警；
- 日志至少保留 30 天，并确认日志里不会打印验证码明文（只在 `EMAIL_DEV_PRINT_CODE=true` 的 dev 才打印，
  生产 `application-prod.yml` 已显式关闭）。

---

## 3. 发布与 CI

### 3.1 `[P1]` 收紧工作流权限

GitHub Actions 默认令牌权限可以全局设为只读，再按 job 提权：

```yaml
# .github/workflows/*.yml 顶部
permissions:
  contents: read
```

只有创建 Release 的 job 需要 `contents: write`，在**该 job** 内单独声明。
同时检查仓库 Settings → Actions → "Workflow permissions" 选择 **Read repository contents**。

### 3.2 `[P1]` Secrets 最小化与轮换

- 只把真正需要的密钥放进 CI Secrets（本仓库发布流程只需 `GITHUB_TOKEN`，SSH 相关不上 CI）；
- 定期轮换：`R_TOKEN_SECRET`（JWT）、`DB_PASSWORD`、`REDIS_PASSWORD`、`DRUID_PASSWORD`、`MAIL_PASSWORD`；
- 轮换 JWT 密钥会让所有已登录用户下线 —— 请在低峰期做，并先确认 `SecurityConfigValidator` 的 64 位长度要求。

### 3.3 `[P2]` 产物可追溯

- Release 附件（`zhi-admin.jar`、`frontend-dist.zip`、`deploy-assets.zip`）由 tag 触发构建；
  发布后记录 tag 与 HEAD 的对应关系，避免"线上跑的不是这个 tag"；
- 移动已发布 tag 会**重建**同名 Release 附件，操作前在 release notes 里注明原因。

### 3.4 `[P0]` 依赖漏洞扫描（前端已闭环，后端靠 Dependabot）

两道闸门，都不需要额外密钥：

1. **CI 阻断**：`ci.yml`（push/PR）与 `release.yml`（打 tag）在 `npm ci` 之后立刻跑 `npm run audit`
   （即 `npm audit --omit=dev --audit-level=high --registry=https://registry.npmjs.org`）。只查**产线依赖**，
   因为 dev 工具链（vitest/eslint 一类的 DoS 类 CVE）不会进 `dist/`，把它们算进闸门只会让红线永远挂着、
   最后被人加 `|| true` 关掉。`--registry` 必须钉在 npmjs：本机 `~/.npmrc` 的默认镜像是 npmmirror，
   它**没有实现** `/-/npm/v1/security/*`（实测 `404 [NOT_IMPLEMENTED]`），不钉住的话审计直接报错而不是给出结论。
2. **自动提 PR**：`.github/dependabot.yml` 对 `maven`（指向根 POM，会解析整个反应堆）、
   `npm`（`/zhi-ui`）与 `github-actions` 每周（周一 02:00 Asia/Shanghai）比对
   GitHub Advisory Database，patch/minor 合并成单个 PR、major 另开。安全类 PR 不受
   `open-pull-requests-limit` 限制，所以它不会被排队排掉。

要点与已知边界：

- **Dependabot 只在 GitHub 远端生效**，`origin`（Gitee）没有等价能力；Gitee 侧的依赖更新仍需人工
  `npm outdated` / `mvn versions:display-dependency-updates`，或者以 GitHub 的 PR 为准再同步过去。
  同理，`npm audit` 那道闸门也没有加进 `.workflow/*.yml`：Gitee Go 的 runner 用的是未实现审计端点的镜像，
  要加就得在 Gitee 侧也显式指到 npmjs，先手工验证一次再放开。
- 首次推送后要确认仓库 **Settings → Code security and analysis** 里 **Dependabot alerts** 与
  **Dependabot security updates** 都已勾选。**这一步不做，上面那行"比对 Advisory Database"是不成立的**：
  只注册 `dependabot.yml` 只会得到"有新版本"的升级 PR，不会有 CVE 告警，也不会有安全类 PR。
  实测本仓库推送后（2026-10-02）`GET /repos/nevell001/zhienterprise/dependabot/alerts` 返回
  `403 Dependabot alerts are disabled for this repository`，`security_and_analysis.dependabot_security_updates`
  仍是 `disabled`。命令行开：`gh api -X PUT repos/nevell001/zhienterprise/vulnerability-alerts`
  （令牌需要 `admin:repo_hook` scope，没有就走 UI）。
- 开了 alerts 之后才有意义的那句"安全类 PR 不受 `open-pull-requests-limit` 限制"才有出处可查：
  `gh pr list --author app/dependabot`。
- 根 POM 的 `<repositories>` 只有阿里云 public 镜像、没有需要认证的仓库，所以 Dependabot 能直接解析；
  **将来若加入私有源**，必须同时在本文件加 `registries:` 配置，否则 Maven 生态会静默不报警。
- 后端**没有**在 CI 里跑 OWASP dependency-check：它需要 NVD API Key，且无 Key 时首次同步要 30~60 分钟，
  会让每次 push 都变慢。当前由 Dependabot 覆盖 Maven CVE。真需要 SARIF 进 Code Scanning 时，
  再单独加一个 `schedule:` 触发的 workflow，不要挂进 `ci.yml`。
- 本地随时可查（GitHub 之外的第一道自查）：

  ```bash
  # 产线依赖（CI 闸门口径，含 npmjs 审计端点）
  cd zhi-ui && npm run audit
  # 含 dev，用于排查工具链
  cd zhi-ui && npm audit --audit-level=high --registry=https://registry.npmjs.org
  # 后端看已知 CVE 需要工具，先用版本时效自查
  mvn -q versions:display-dependency-updates
  ```

---

## 4. 开发机（macOS）

- **密钥一用途一把**：个人 SSH、部署 SSH、平台 API 分开；本仓库约定 GitHub 用 `~/.ssh/github`，
  服务器用另一把（放在 `~/.ssh/config` 的 `Host` 块里，配 `IdentitiesOnly yes`）；
- 私钥建议加 passphrase，并用 `ssh-add --apple-use-keychain` 存进钥匙串（否则 agent 里会有明文可用的私钥）；
- `ssh-add -l` 随时可看 agent 里有什么；
- 不要把 `.env`、私钥、`~/code` 下的凭据放进任何可同步到云端的目录；
- 装上"屏幕锁定 + FileVault"，笔记本丢了才不至于直接可用。

---

## 5. 一页速查（复制粘贴用）

```bash
# —— 仓库 ——
# 1. 开启 main 分支保护与 tag 保护（平台 UI）
# 2. 开启 2FA，签发细粒度 PAT（≤90 天）
# 3. 去掉 https 代理重写
git config --global --unset-all url."https://ghproxy.net/https://github.com/".insteadOf
git config --global --unset-all url."https://ghfast.top/https://github.com/".insteadOf

# —— 依赖 ——
(cd zhi-ui && npm run audit)   # 产线依赖，与 CI 同口径（审计端点钉在 npmjs）

# —— 服务器 ——
sudo sshd -t && sudo systemctl reload ssh      # PermitRootLogin no / PasswordAuthentication no / AllowUsers deploy
sudo visudo -c                                  # sudo 白名单语法
sudo chmod 600 /opt/zhiblog/.env && sudo chown deploy:deploy /opt/zhiblog/.env
sudo -l -U deploy                               # 只应看到固定的 compose / 重启命令
docker ps --format '{{.Names}}\t{{.Ports}}'     # 确认 8080/3306/6379 只绑 127.0.0.1
crontab -l -u deploy                            # 备份任务存在
```

**季度动作**：轮换 PAT 与平台密钥 → 检查 `authorized_keys` 是否有废弃钥匙 → 跑一次备份恢复演练 →
复查告警是否还能收到 → 过一遍 Dependabot 开着的安全 PR（`gh pr list --author app/dependabot`）。

---

## 6. 本仓库已做的相关加固（对照用，勿回退）

- 生产 compose 的 admin/前端端口绑定 `127.0.0.1`，对外只经 nginx；
- `docker-compose.prod.yml` 的 `zhi-admin` 使用 `env_file: .env`，密钥不写进编排文件；
- 生产 `application-prod.yml` 显式 `email-code.dev-print-code: false`（不在日志里打印验证码）；
- `SecurityConfigValidator` 在生产缺 `R_TOKEN_SECRET`/`DRUID_PASSWORD`/`REDIS_PASSWORD`/`DB_PASSWORD` 时**阻止启动**；
- 上传白名单移除 `html`/`htm`（防止上传即执行），并对 `/profile/` 加 `X-Content-Type-Options: nosniff`；
- 客户端 IP 只信任可信代理链，nginx 覆写 `X-Forwarded-For`（限流/去重不被伪造头绕过）；
- 前台暴露接口收敛了 `email` 字段（友链/评论列表走专用投影）；
- 所有管理接口都有 `@PreAuthorize`，并由 `ControllerPermissionPolicyTest` 反射扫描兜底；
- 依赖已升到 Boot 3.3.13（Spring 6.1.21 / Security 6.3.10 / Tomcat 10.1.42），去掉了主动降级的 BOM 覆盖；
- 产线依赖审计 `npm run audit` 已进 `ci.yml` 与 `release.yml`（发 tag 也过这道闸；命令本体在
  `zhi-ui/package.json`，审计端点钉在 npmjs，因为 npmmirror 未实现 `/-/npm/v1/security/*`），
  `.github/dependabot.yml` 覆盖 maven / npm / github-actions 三个生态（见 3.4）；
- 前端依赖已修掉两个高危传递项：`axios` 1.18.1 → 1.20.0（原型污染 gadget / ReDoS / header 注入 / SSRF 一串）、
  `brace-expansion` → 1.1.21 / 2.1.7（CPU DoS），当前 `--omit=dev` 口径为 0 漏洞。

> 相关文档：`docs/SECURITY_CONFIG.md`（启动校验与必需密钥）、`AGENTS.md`（开发约定）、
> `SYNC_GITHUB.md`（双远端同步与 SSH 推送）。
