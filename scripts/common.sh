#!/usr/bin/env bash
# 知企建站系统运维脚本公共函数：由 backup.sh / restore.sh / upgrade.sh source，不单独执行。
# 依赖：bash 4+、docker compose v2；不依赖源码目录，release 部署（scripts/ 与 compose 同级）可直接用。

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="${APP_DIR:-$(cd "$SCRIPT_DIR/.." && pwd)}"
ENV_FILE="${ENV_FILE:-$APP_DIR/.env}"

log() { printf '[%s] %s\n' "$(date '+%F %T')" "$*"; }
warn() { printf '[%s] WARN: %s\n' "$(date '+%F %T')" "$*" >&2; }
die() { printf '[%s] ERROR: %s\n' "$(date '+%F %T')" "$*" >&2; exit 1; }

# 读取 .env 中的单个键（不 source 整个文件，避免值里的空格/注释被当成 shell 语法执行）。
env_value() {
  local key="$1" value
  [ -f "$ENV_FILE" ] || return 0
  value="$(sed -n "s/^[[:space:]]*${key}[[:space:]]*=[[:space:]]*//p" "$ENV_FILE" | tail -n 1 | tr -d '\r')"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  value="${value%\"}"; value="${value#\"}"
  value="${value%\'}"; value="${value#\'}"
  printf '%s' "$value"
}

load_db_settings() {
  DB_NAME="${DB_NAME:-$(env_value DB_NAME)}"
  DB_NAME="${DB_NAME:-zhienterprise}"
  DB_ROOT_PASSWORD="${DB_ROOT_PASSWORD:-$(env_value DB_ROOT_PASSWORD)}"
  [ -n "$DB_ROOT_PASSWORD" ] || die "缺少 DB_ROOT_PASSWORD（请在 $ENV_FILE 或环境变量中设置）"
}

resolve_compose() {
  command -v docker >/dev/null 2>&1 || die "未安装 docker"
  docker compose version >/dev/null 2>&1 || die "需要 Docker Compose v2（docker compose）"

  if [ -z "${COMPOSE_FILE:-}" ]; then
    local candidate
    for candidate in docker-compose.release.yml docker-compose.prod.yml docker-compose.dev.yml; do
      if [ -f "$APP_DIR/$candidate" ]; then COMPOSE_FILE="$APP_DIR/$candidate"; break; fi
    done
  fi
  [ -n "${COMPOSE_FILE:-}" ] || die "找不到 compose 文件（可用 COMPOSE_FILE 指定）"
  case "$COMPOSE_FILE" in
    /*) ;;
    *) COMPOSE_FILE="$APP_DIR/$COMPOSE_FILE" ;;
  esac
  [ -f "$COMPOSE_FILE" ] || die "compose 文件不存在：$COMPOSE_FILE"
}

compose() { docker compose -f "$COMPOSE_FILE" "$@"; }

require_mysql_running() {
  local cid
  cid="$(compose ps -q mysql 2>/dev/null || true)"
  [ -n "$cid" ] || die "MySQL 容器未运行，请先启动：docker compose -f $(basename "$COMPOSE_FILE") up -d mysql"
}

resolve_sql_dir() {
  if [ -z "${SQL_DIR:-}" ]; then
    local candidate
    for candidate in "$APP_DIR/deploy/sql" "$APP_DIR/sql"; do
      if [ -d "$candidate" ]; then SQL_DIR="$candidate"; break; fi
    done
  fi
  [ -n "${SQL_DIR:-}" ] && [ -d "$SQL_DIR" ] || die "找不到 SQL 目录（可用 SQL_DIR 指定）"
}

write_checksums() {
  local dir="$1"
  ( cd "$dir" && if command -v sha256sum >/dev/null 2>&1; then
      sha256sum ./*.gz > SHA256SUMS
    else
      shasum -a 256 ./*.gz > SHA256SUMS
    fi )
}

verify_checksums() {
  local dir="$1"
  [ -f "$dir/SHA256SUMS" ] || die "备份缺少 SHA256SUMS：$dir"
  ( cd "$dir" && if command -v sha256sum >/dev/null 2>&1; then
      sha256sum -c SHA256SUMS
    else
      shasum -a 256 -c SHA256SUMS
    fi )
}

# 等待后端 Actuator 健康端点通过（最多 tries * 3 秒）
wait_for_health() {
  local tries="${1:-40}" i=1
  while [ "$i" -le "$tries" ]; do
    if compose exec -T zhi-admin curl -fsS http://localhost:8080/manage/actuator/health >/dev/null 2>&1; then
      log "后端健康检查通过"
      return 0
    fi
    sleep 3
    i=$((i + 1))
  done
  warn "后端健康检查未在 $((tries * 3)) 秒内通过，请查看：docker compose -f $(basename "$COMPOSE_FILE") logs --tail=100 zhi-admin"
  return 1
}
