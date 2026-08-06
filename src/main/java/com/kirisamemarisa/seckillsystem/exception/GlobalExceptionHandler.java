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

@Slf4j
//把所有的controller报错拦截，处理后通过jackson传给前端，相当于 @ControllerAdvice + @ResponseBody
@RestControllerAdvice
public class GlobalExceptionHandler {
    //所有异常都接受
    @ExceptionHandler(Exception.class)
    public RespBean ExceptionHandler(Exception e) {
        //如果是我们自己规定的GlobalException
        if (e instanceof GlobalException) {
            GlobalException ex = (GlobalException) e;
            return RespBean.error(ex.getRespBeanEnum());
        }
        //处理@RequestBody 类型的参数校验异常
        else if (e instanceof MethodArgumentNotValidException ex) {
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：" + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
            return respBean;
        }
        //Spring框架自带的表单类型的参数校验异常
        else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：" + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
            return respBean;
        }
        //处理单一参数校验异常
        else if (e instanceof ConstraintViolationException ex) {
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：" + ex.getMessage());
            return respBean;
        }
        //处理漏传或没传必定需要的 @RequestParam 参数这一类异常
        else if (e instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException ex = (MissingServletRequestParameterException) e;
            RespBean respBean = RespBean.error(RespBeanEnum.BIND_ERROR);
            respBean.setMessage("参数校验异常：缺少必需参数 '" + ex.getParameterName() + "'");
            return respBean;
        }
        //加上未知错误的日志打印，决不能在生产环境吞噬核心异常！
        log.error("【系统全局异常拦截】", e);
        return RespBean.error(RespBeanEnum.ERROR);
    }
}