package com.spingbootinit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CodeSandboxProperties.class)
public class CodeSandboxConfiguration {
}
