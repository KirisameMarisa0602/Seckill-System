/**
 * 统一异常模型。业务通过抛 {@link com.kirisamemarisa.seckillsystem.exception.GlobalException}
 * 携带 {@link com.kirisamemarisa.seckillsystem.vo.RespBeanEnum}，由全局处理器转成 JSON，避免控制器到处 try-catch。
 */
package com.kirisamemarisa.seckillsystem.exception;
