/**
 * 工具类。当前仅保留 {@link com.kirisamemarisa.seckillsystem.utils.MD5Util}，
 * 用于旧账号从 MD5 密码平滑升级到 BCrypt；新注册一律走 {@code PasswordEncoder}。
 */
package com.kirisamemarisa.seckillsystem.utils;
