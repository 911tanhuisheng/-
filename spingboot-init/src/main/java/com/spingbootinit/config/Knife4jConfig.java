package com.spingbootinit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("通用后端模版 API 文档")
                        .version("1.0")
                        .description("登录、注册、用户管理等接口")

                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com")));
    }

    // 移除分组配置，让Knife4j自动扫描所有接口
}