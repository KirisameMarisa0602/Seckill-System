package com.kirisamemarisa.seckillsystem.exception;

import com.kirisamemarisa.seckillsystem.vo.RespBean;
import com.kirisamemarisa.seckillsystem.vo.RespBeanEnum;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常转 JSON。等价于 {@code @ControllerAdvice + @ResponseBody}，拦截全部 Controller 未处理异常。
 *
 * <p>在秒杀链路中位于 HTTP 出口：无论登录校验失败还是秒杀参数非法，前端都收到统一 {@link RespBean}，
 * 而不是栈信息。无独立 API 路径。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 按异常类型映射为 {@link RespBean}。
     *
     * @param e 任意未捕获异常
     * @return 业务枚举错误、参数校验信息，或兜底系统错误码
     * @implNote 不写 Redis/MQ/DB；未知异常会打 error 日志后返回通用失败
     */
    @ExceptionHandler(Exception.class)
    public RespBean ExceptionHandler(Exception e) {
        if (e instanceof GlobalException) {
            GlobalException ex = (GlobalException) e;
            return RespBean.error(ex.getRespBeanEnum());
        }
        // @RequestBody + @Valid 校验失败
        else if (e instanceof MethodArgumentNotValidException ex) {
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：" + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
            return respBean;
        }
        // 表单/查询参数绑定失败（非 JSON Body）
        else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：" + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
            return respBean;
        }
        // 方法级约束（如 @RequestParam @Min）校验失败
        else if (e instanceof ConstraintViolationException ex) {
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：" + ex.getMessage());
            return respBean;
        }
        else if (e instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException ex = (MissingServletRequestParameterException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：缺少必需参数 '" + ex.getParameterName() + "'");
            return respBean;
        }
        log.error("【系统全局异常拦截】", e);
        return RespBean.error(RespBeanEnum.ERROR);
    }
}
