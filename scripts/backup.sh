#!/usr/bin/env bash
# 知企建站系统备份：MySQL 全库 + uploadPath 上传目录。
#
# 用法：
#   scripts/backup.sh
#   BACKUP_DIR=/data/backups scripts/backup.sh
#   DB_NAME=zhienterprise scripts/backup.sh
#
# 可配置环境变量：
#   APP_DIR           部署根目录（默认脚本上一级）
#   COMPOSE_FILE      指定 compose 文件（默认自动找 release → prod → dev）
#   ENV_FILE          .env 路径（默认 $APP_DIR/.env）
#   BACKUP_DIR        备份输出目录（默认 /var/backups/zhienterprise）
#   BACKUP_KEEP_DAYS  保留天数（默认 14，0 表示不清理）
#   UPLOAD_DIR        上传目录（默认 $APP_DIR/uploadPath）
#
# 产物：$BACKUP_DIR/backup-YYYYmmdd-HHMMSS/{db.sql.gz,uploadPath.tar.gz,manifest.txt,SHA256SUMS}
# 注意：数据库用 mysqldump --single-transaction 做一致性快照，运行中备份无需停站；
#       备份默认落在本机，必须自行同步到异地/对象存储，否则服务器损毁时备份会一起丢失。

set -euo pipefail

source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/common.sh"

BACKUP_DIR="${BACKUP_DIR:-/var/backups/zhienterprise}"
BACKUP_KEEP_DAYS="${BACKUP_KEEP_DAYS:-14}"
UPLOAD_DIR="${UPLOAD_DIR:-$APP_DIR/uploadPath}"

case "$BACKUP_KEEP_DAYS" in
  ''|*[!0-9]*) die "BACKUP_KEEP_DAYS 必须是非负整数：$BACKUP_KEEP_DAYS" ;;
esac

load_db_settings
resolve_compose
require_mysql_running

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR" 2>/dev/null || true

LOCK_DIR="$BACKUP_DIR/.backup.lock"
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  die "已有备份任务在运行（$LOCK_DIR）。若确认没有，请手动删除该目录后重试"
fi

STAMP="$(date +%Y%m%d-%H%M%S)"
TMP_DIR="$BACKUP_DIR/.tmp-$STAMP"
FINAL_DIR="$BACKUP_DIR/backup-$STAMP"

cleanup() {
  rm -rf "${TMP_DIR:-}" "$LOCK_DIR"
}
trap cleanup EXIT INT TERM

mkdir -p "$TMP_DIR"
chmod 700 "$TMP_DIR"

log "开始备份：db=$DB_NAME compose=$(basename "$COMPOSE_FILE")"
log "备份数据库 $DB_NAME ..."
compose exec -T -e MYSQL_PWD="$DB_ROOT_PASSWORD" mysql \
  mysqldump -uroot \
    --single-transaction --quick --routines --triggers --events \
    --no-tablespaces --default-character-set=utf8mb4 \
    "$DB_NAME" | gzip -9 > "$TMP_DIR/db.sql.gz"
log "数据库备份完成：$(du -h "$TMP_DIR/db.sql.gz" | cut -f1)"

if [ -d "$UPLOAD_DIR" ]; then
  log "打包上传目录 $UPLOAD_DIR ..."
  tar -czf "$TMP_DIR/uploadPath.tar.gz" -C "$(dirname "$UPLOAD_DIR")" "$(basename "$UPLOAD_DIR")"
  log "上传目录备份完成：$(du -h "$TMP_DIR/uploadPath.tar.gz" | cut -f1)"
else
  warn "上传目录不存在，跳过：$UPLOAD_DIR"
fi

{
  printf 'backup_time=%s\n' "$STAMP"
  printf 'host=%s\n' "$(hostname)"
  printf 'app_dir=%s\n' "$APP_DIR"
  printf 'compose_file=%s\n' "$COMPOSE_FILE"
  printf 'db_name=%s\n' "$DB_NAME"
  printf 'upload_dir=%s\n' "$UPLOAD_DIR"
} > "$TMP_DIR/manifest.txt"

write_checksums "$TMP_DIR"
mv "$TMP_DIR" "$FINAL_DIR"

if [ "$BACKUP_KEEP_DAYS" -gt 0 ]; then
  # 只清理本脚本命名的备份目录，避免误删同目录下的其它文件
  find "$BACKUP_DIR" -mindepth 1 -maxdepth 1 -type d -name 'backup-*' \
    -mtime +"$BACKUP_KEEP_DAYS" -exec rm -rf {} +
fi

log "备份完成：$FINAL_DIR"
log "恢复：scripts/restore.sh $FINAL_DIR"
warn "备份仍在同一台服务器上 —— 请同步到异地/对象存储并加密（备份包含全部业务数据）"
