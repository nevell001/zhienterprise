#!/usr/bin/env bash
# 知企建站系统恢复：从 backup.sh 的产物恢复 MySQL 与 uploadPath。
#
# 用法：
#   scripts/restore.sh                      # 恢复最新的备份
#   scripts/restore.sh latest
#   scripts/restore.sh /var/backups/zhienterprise/backup-20261018-033000
#   scripts/restore.sh 20261018-033000      # 也可只给时间戳
#   scripts/restore.sh latest --db-only
#   scripts/restore.sh latest --uploads-only
#   scripts/restore.sh latest --yes         # 跳过交互确认（演练/自动化用）
#
# 行为：
#   1. 校验备份的 SHA256SUMS；
#   2. 默认先做一次“恢复前安全备份”（SKIP_SAFETY_BACKUP=1 可跳过）；
#   3. 停止 zhi-admin / zhi-ui，避免恢复过程中继续写入；
#   4. 导入数据库、解包 uploadPath；
#   5. 重新启动并等待 Actuator 健康检查。
#
# 注意：恢复会覆盖现有数据库表与同名上传文件，务必确认备份版本后再执行。

set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

BACKUP_DIR="${BACKUP_DIR:-/var/backups/zhienterprise}"
TARGET=""
MODE="all"
ASSUME_YES=0
APP_STOPPED=0
RESTORE_OK=0

usage() {
  sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

while [ $# -gt 0 ]; do
  case "$1" in
    --db-only) MODE="db" ;;
    --uploads-only) MODE="uploads" ;;
    --yes|-y) ASSUME_YES=1 ;;
    -h|--help) usage; exit 0 ;;
    -*) die "未知参数：$1（--help 查看用法）" ;;
    *)
      [ -z "$TARGET" ] || die "只能指定一个备份目录/时间戳"
      TARGET="$1"
      ;;
  esac
  shift
done

cleanup() {
  if [ "$APP_STOPPED" -eq 1 ] && [ "$RESTORE_OK" -ne 1 ]; then
    warn "恢复未完成，应用保持停止状态。排除故障后可重新执行本脚本"
  fi
}
trap cleanup EXIT

load_db_settings
resolve_compose
require_mysql_running

if [ -z "$TARGET" ] || [ "$TARGET" = "latest" ]; then
  TARGET="$(find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name 'backup-*' 2>/dev/null | sort | tail -n 1)"
  [ -n "$TARGET" ] || die "在 $BACKUP_DIR 下找不到备份目录"
elif [ ! -d "$TARGET" ]; then
  TARGET="$BACKUP_DIR/backup-$TARGET"
fi
[ -d "$TARGET" ] || die "备份目录不存在：$TARGET"

log "校验备份完整性：$TARGET"
verify_checksums "$TARGET"

if [ "$MODE" != "uploads" ]; then
  [ -f "$TARGET/db.sql.gz" ] || die "备份缺少 db.sql.gz：$TARGET"
fi
if [ "$MODE" != "db" ]; then
  [ -f "$TARGET/uploadPath.tar.gz" ] || die "备份缺少 uploadPath.tar.gz：$TARGET"
fi

if [ "$ASSUME_YES" -ne 1 ]; then
  echo "即将恢复到数据库 [$DB_NAME]（模式：$MODE），现有数据会被覆盖。"
  read -r -p "输入数据库名 $DB_NAME 确认，其它任意输入取消：" answer
  [ "$answer" = "$DB_NAME" ] || die "已取消恢复"
fi

if [ "${SKIP_SAFETY_BACKUP:-0}" != "1" ]; then
  log "恢复前安全备份 ..."
  "$SCRIPT_DIR/backup.sh"
fi

log "停止应用容器（zhi-admin / zhi-ui）..."
compose stop zhi-admin zhi-ui
APP_STOPPED=1

if [ "$MODE" != "uploads" ]; then
  log "恢复数据库 $DB_NAME ..."
  gunzip -c "$TARGET/db.sql.gz" | compose exec -T -e MYSQL_PWD="$DB_ROOT_PASSWORD" mysql \
    mysql -uroot --default-character-set=utf8mb4 "$DB_NAME"
  log "数据库恢复完成"
fi

if [ "$MODE" != "db" ]; then
  log "恢复上传目录到 $APP_DIR ..."
  tar -xzf "$TARGET/uploadPath.tar.gz" -C "$APP_DIR"
  log "上传目录恢复完成"
fi

log "启动应用容器 ..."
compose up -d zhi-admin zhi-ui
wait_for_health || die "恢复命令已执行，但后端健康检查未通过，请查看容器日志后再确认数据"

RESTORE_OK=1
log "恢复完成：$TARGET"
log "请打开站点首页与 /admin 抽查内容、图片与登录；如权限/设置未刷新，可重新登录或等待 Redis 缓存过期（设置缓存 10 分钟）"
