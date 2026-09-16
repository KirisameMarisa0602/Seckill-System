/**
 * REST 控制器。路径与前端 {@code frontend/src/api/index.ts} 一一对应，开发环境经 Vite/Nginx 去掉 {@code /api} 前缀后打到这里。
 *
 * <p>鉴权方式：用户请求头 {@code token}，管理员请求头 {@code Admin-Token}。
 * 未登录接口用 {@code @AccessLimit(needLogin = false)}；秒杀写接口必须登录。
 */
package com.kirisamemarisa.seckillsystem.controller;
