package com.kirisamemarisa.seckillsystem.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryAddressRequest {
    @NotBlank(message = "收货人不能为空")
    @Size(max = 64, message = "收货人不能超过64个字符")
    private String receiverName;

    @NotBlank(message = "收货手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "收货手机号格式不正确")
    private String receiverPhone;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址不能超过255个字符")
    private String detail;
}
