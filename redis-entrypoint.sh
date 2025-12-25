#!/bin/sh
set -e

if [ "$SPRING_PROFILES_ACTIVE" = "prod" ] && [ -z "$SPRING_DATA_REDIS_PASSWORD" ]; then
  echo "错误：生产环境必须设置 Redis 密码！"
  exit 1
fi

if [ -n "$SPRING_DATA_REDIS_PASSWORD" ]; then
  echo "启动 Redis（已设置密码）"
  exec redis-server --appendonly yes --requirepass "$SPRING_DATA_REDIS_PASSWORD"
else
  echo "启动 Redis（未设置密码）"
  exec redis-server --appendonly yes
fi
