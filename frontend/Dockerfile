# ================================
# 构建阶段
# ================================
FROM node:20-alpine AS builder

WORKDIR /app

# 先复制package.json安装依赖（利用Docker缓存层）
COPY package*.json .
RUN npm ci --registry=https://registry.npmmirror.com

# 再复制源码构建
COPY . .
RUN npm run build

# ================================
# 运行阶段
# ================================
FROM nginx:alpine

# 复制前端构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80