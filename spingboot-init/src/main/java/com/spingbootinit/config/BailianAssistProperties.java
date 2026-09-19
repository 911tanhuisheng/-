package com.spingbootinit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 阿里云百炼（DashScope 兼容 OpenAI 协议）。密钥见控制台 API-KEY，勿提交仓库。
 *
 * @see <a href="https://bailian.console.aliyun.com/">百炼控制台</a>
 */
@Data
@ConfigurationProperties(prefix = "bailian")
public class BailianAssistProperties {

    private boolean enabled = false;

    /** 与控制台「API-KEY」一致；建议环境变量 DASHSCOPE_API_KEY */
    private String apiKey = "";

    /**
     * 兼容模式 Chat Completions 根路径（勿带末尾斜杠）。
     * 默认：{@code https://dashscope.aliyuncs.com/compatible-mode/v1}
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    /** 百炼中可选模型名，如 qwen-plus、qwen-turbo 等 */
    private String model = "qwen-plus";

    private double temperature = 0.35;

    private int maxTokens = 2500;
}
