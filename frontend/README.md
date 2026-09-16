# Seckill Frontend

Vue 3 + TypeScript + Vite 前端，接口统一使用 `/api`。
开发环境默认把 `/api` 代理到本机 Nginx `80`（由 Nginx 去掉前缀后负载到 8080/8081）。
若未启动 Nginx，把 `VITE_DEV_PROXY_TARGET` 设为 `http://127.0.0.1:8080`，Vite 会自动去掉 `/api` 前缀直连后端。

```powershell
npm install
npm run dev
```

启动顺序：

1. MySQL、Redis、RabbitMQ
2. VS Code `Backend Cluster (8080 + 8081)`
3. Nginx
4. 前端 `npm run dev`

浏览器访问 `http://localhost:3000`（与后端 `CORS_ALLOWED_ORIGINS` 保持一致；若用 `127.0.0.1` 需把它一并加入白名单）。

生产构建：

```powershell
npm run build
```

将 `dist/` 内容复制到 Nginx 的 `html/` 目录。
