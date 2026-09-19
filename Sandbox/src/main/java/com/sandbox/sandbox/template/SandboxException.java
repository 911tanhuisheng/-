package com.sandbox.sandbox.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 沙箱业务异常：用于把“可预期失败”（编译错/运行错/超时/安全拦截）统一收敛到响应里。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SandboxException extends RuntimeException {
    public  SandboxError error;
    public  String userMessage;
    public  String detail;


}

