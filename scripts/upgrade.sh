#!/usr/bin/env bash
# 知企建站系统数据库升级：执行 00 初始化脚本与 99_*.sql 存量补丁。
#
# 用法：
#   scripts/upgrade.sh                 # 交互确认 + 自动安全备份
#   scripts/upgrade.sh --yes           # 跳过交互确认（演练/自动化用）
#   scripts/upgrade.sh --no-backup     # 跳过自动备份（仅在测试库/已手工备份时使用）
#
# SQL 目录：默认 $APP_DIR/deploy/sql（release 布局），否则 $APP_DIR/sql（源码布局）；
#           也可用 SQL_DIR=/path/to/sql 指定。
#
# 执行顺序（顺序是硬约定）：
#   1. 00_init_database.sql —— 唯一幂等入口：补齐表/列/索引/种子；
#   2. 其余 99_*.sql（按文件名排序）—— 一次性数据补丁，全部幂等。
#
# 全部脚本必须幂等（重复执行 0 行变化）。升级前会自动备份，升级后重启 zhi-admin 并等待健康检查。

set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

ASSUME_YES=0
SKIP_BACKUP=0
APP_STOPPED=0
UPGRADE_OK=0

usage() {
  sed -n '2,18p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

while [ $# -gt 0 ]; do
  case "$1" in
    --yes|-y) ASSUME_YES=1 ;;
    --no-backup) SKIP_BACKUP=1 ;;
    -h|--help) usage; exit 0 ;;
    *) die "未知参数：$1（--help 查看用法）" ;;
  esac
  shift
done

cleanup() {
  if [ "$APP_STOPPED" -eq 1 ] && [ "$UPGRADE_OK" -ne 1 ]; then
    warn "升级未完成，zhi-admin 保持停止状态。可先用备份回滚（见 docs/BACKUP_RESTORE.md），或修复后重跑本脚本"
  fi
}
trap cleanup EXIT

load_db_settings
resolve_compose
resolve_sql_dir
require_mysql_running

[ -f "$SQL_DIR/00_init_database.sql" ] || die "SQL 目录缺少 00_init_database.sql：$SQL_DIR"

log "SQL 目录：$SQL_DIR"
log "将执行：00_init_database.sql → 其余 99_*.sql"

if [ "$ASSUME_YES" -ne 1 ]; then
  echo "即将升级数据库 [$DB_NAME]，执行前会自动备份。"
  read -r -p "输入数据库名 $DB_NAME 确认，其它任意输入取消：" answer
  [ "$answer" = "$DB_NAME" ] || die "已取消升级"
fi

if [ "$SKIP_BACKUP" -eq 0 ]; then
  log "升级前安全备份 ..."
  "$SCRIPT_DIR/backup.sh"
fi

log "停止 zhi-admin（升级期间不接受写入）..."
compose stop zhi-admin
APP_STOPPED=1

run_sql() {
  local file="$1"
  [ -f "$file" ] || return 0
  log "执行 $(basename "$file") ..."
  compose exec -T -e MYSQL_PWD="$DB_ROOT_PASSWORD" mysql \
    mysql -uroot --default-character-set=utf8mb4 "$DB_NAME" < "$file"
}

run_sql "$SQL_DIR/00_init_database.sql"

while IFS= read -r patch; do
  run_sql "$patch"
done < <(find "$SQL_DIR" -maxdepth 1 -type f -name '99_*.sql' | sort)

log "重启 zhi-admin ..."
compose up -d zhi-admin
wait_for_health || die "升级后健康检查未通过，请查看 zhi-admin 日志；必要时按 docs/BACKUP_RESTORE.md 回滚"

UPGRADE_OK=1
log "升级完成。若后台权限未立即生效，请重新登录（权限缓存在 Redis，登录时重建）"
