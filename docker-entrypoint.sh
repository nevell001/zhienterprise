#!/bin/sh
# 知企建站后端入口脚本（容器内以 root 启动，仅用于修正日志卷属主后降权运行）。
#
# 背景：/app/logs 由命名卷 admin_logs 挂载，而命名卷首次挂载到非空镜像目录时的属主
# 取决于创建它的容器 —— release（官方镜像、root 运行）会留下 root:root 的卷，
# 之后 prod/dev（appuser 运行）里的 logback 就会 `./logs/xxx.log (Permission denied)`
# 并直接启动失败。这里每次启动都修正属主，避免依赖卷的既有状态。
#
# 不使用 -R 之外的额外参数是为了保持幂等与低开销；/app/uploadPath 可能是宿主机绑定
# 挂载，改它的属主会影响宿主机文件，故意不处理。
set -e

mkdir -p /app/logs
chown -R appuser:appuser /app/logs

exec /usr/sbin/runuser -u appuser -- "$@"