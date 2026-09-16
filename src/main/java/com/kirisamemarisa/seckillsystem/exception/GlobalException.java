package com.kirisamemarisa.seckillsystem.exception;

import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 业务异常载体：把 {@link RespBeanEnum} 沿调用栈抛到 {@link GlobalExceptionHandler}，再转成统一 JSON。
 *
 * <p>在秒杀链路中用于控制器/服务主动中断（如验证码接口参数非法），避免每个接口自己拼 {@code RespBean.error}。
 * 无对应前端路径，前端只看到处理后的 {@code code/message}。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GlobalException extends RuntimeException {
    private RespBeanEnum respBeanEnum;
}
