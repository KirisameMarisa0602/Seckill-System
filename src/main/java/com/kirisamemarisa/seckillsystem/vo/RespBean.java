package com.kirisamemarisa.seckillsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 HTTP JSON 外壳：{@code code}/{@code message}/{@code obj}。
 * 成功用 {@link #success()}；失败把 {@link RespBeanEnum} 的码和文案填进去。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespBean {
    /** 业务码：200 成功，其它见 {@link RespBeanEnum}。 */
    private long code;
    /** 给前端展示的文案。 */
    private String message;
    /** 成功时的业务数据；失败一般为 {@code null}。 */
    private Object obj;

    /** 无数据的成功响应。 */
    public static RespBean success() {
        return new RespBean(RespBeanEnum.SUCCESS.getCode(), RespBeanEnum.SUCCESS.getMessage(), null);
    }

    /** 带业务数据的成功响应，{@code obj} 即前端拿到的解包结果。 */
    public static RespBean success(Object obj) {
        return new RespBean(RespBeanEnum.SUCCESS.getCode(), RespBeanEnum.SUCCESS.getMessage(), obj);
    }

    /** 按枚举填 code/message，{@code obj} 为空。 */
    public static RespBean error(RespBeanEnum respBeanEnum) {
        return new RespBean(respBeanEnum.getCode(), respBeanEnum.getMessage(), null);
    }
}
