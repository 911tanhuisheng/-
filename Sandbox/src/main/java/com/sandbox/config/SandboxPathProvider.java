package com.sandbox.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class SandboxPathProvider {

    private final Path tmpCodePath;

    public SandboxPathProvider(SandboxProperties sandboxProperties) {
        String configuredPath = sandboxProperties.getTmpCodeDir();
        if (StrUtil.isBlank(configuredPath)) {
            configuredPath = "tmpCode";
        }
        Path path = Path.of(configuredPath);
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path);
        }
        this.tmpCodePath = path.normalize().toAbsolutePath();
    }

    public Path getTmpCodePath() {
        return tmpCodePath;
    }
}
