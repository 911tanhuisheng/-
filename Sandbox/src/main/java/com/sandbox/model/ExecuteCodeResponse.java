package com.sandbox.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeResponse {
    /**
     * 业务码：0 表示成功，其他表示具体错误。
     */
    private Integer code;

    /**
     * 请求唯一标识，方便前后端与日志排查。
     */
    private String requestId;

    /**
     * 机器可识别的结果类型，例如 SUCCESS / COMPILE_ERROR。
     */
    private String resultType;

    private List<String> outputList;

    /**
     * 用户可读信息。
     */
    private String message;

    /**
     * 兼容旧调用方的状态字段。
     */
    private Integer status;

    /**
     * 判题信息。
     */
    private JudgeInfo judgeInfo;
}
