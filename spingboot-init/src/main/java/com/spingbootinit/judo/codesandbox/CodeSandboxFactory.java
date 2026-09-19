package com.spingbootinit.judo.codesandbox;


import com.spingbootinit.judo.codesandbox.impl.ExampleCodeSandbox;
import com.spingbootinit.judo.codesandbox.impl.RemoteCodeSandbox;
import com.spingbootinit.judo.codesandbox.impl.ThirdPartyCodeSandbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 代码沙箱工厂：必须交给 Spring 管理并从容器取 Bean，
 * 不能 {@code new ExampleCodeSandbox()}，否则 {@code @Resource RestTemplate} 等依赖为 null。
 */
@Component
@RequiredArgsConstructor
public class CodeSandboxFactory {

    private final ExampleCodeSandbox exampleCodeSandbox;
    private final RemoteCodeSandbox remoteCodeSandbox;
    private final ThirdPartyCodeSandbox thirdPartyCodeSandbox;

    /**
     * @param type 与配置 {@code codesandbox.type} 一致：example / remote / thirdParty
     */
    public CodeSandbox newInstance(String type) {
        if (type == null || type.isBlank()) {
            return exampleCodeSandbox;
        }
        return switch (type.trim()) {
            case "remote" -> remoteCodeSandbox;
            case "thirdParty" -> thirdPartyCodeSandbox;
            default -> exampleCodeSandbox;
        };
    }
}
