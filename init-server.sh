#!/bin/bash
# save as: init-server.sh
# chmod +x init-server.sh && sudo ./init-server.sh

set -e

echo "=== 创建数据目录 ==="
mkdir -p /data/uploads /data/postgres

echo "=== 设置权限 ==="
# nginx/前端容器以 nginx 用户(uid=101)运行，但uploads由后端(uid=1000)写入
# 后端容器非 root 用户 uid=1000
chown -R 1000:1000 /data/uploads
# postgres 容器 uid=999
chown -R 999:999 /data/postgres

echo "=== 创建 .env 模板 ==="
cat > /data/.env.example << 'EOF'
# 复制为 .env 并填写实际值
# cp /data/.env.example /data/.env

# AI配置（必填，去 https://siliconflow.cn 注册获取）
SILICONFLOW_API_KEY=your_api_key_here

# 以下默认值可不修改，生产建议改强
POSTGRES_PASSWORD=competition123
REDIS_PASSWORD=redis123
RABBITMQ_PASSWORD=competition123
JWT_SECRET=campus-competition-jwt-secret-key-2026
JWT_EXPIRATION=7200
EOF

echo "=== 完成 ==="
echo "请执行：cp /data/.env.example /data/.env && vim /data/.env 填入 SILICONFLOW_API_KEY"
echo "然后在项目根目录运行：docker compose up -d --build"
