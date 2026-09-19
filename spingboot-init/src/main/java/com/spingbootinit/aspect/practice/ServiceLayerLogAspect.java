package com.spingbootinit.aspect.practice;

import com.spingbootinit.model.dto.user.LoginRequest;
import com.spingbootinit.model.dto.user.RegisterRequest;
import com.spingbootinit.utils.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 【AOP 入门练习】拦截 Service 层方法，统一打印「进入 → 耗时 → 结束」。
 * <p>
 * 学习要点：
 * <ul>
 *   <li>{@link Aspect}：声明这是一个切面（里面写横切逻辑）</li>
 *   <li>{@link Pointcut}：定义「切哪里」（哪些地方的方法要被织入）</li>
 *   <li>{@link Around}：环绕通知，包住原方法；必须调用 {@code joinPoint.proceed()} 才会执行原方法</li>
 *   <li>{@link Order}：多个切面时，数字越小越「靠外」（越先包住调用链）</li>
 * </ul>
 * 自己动手试：
 * <ol>
 *   <li>启动项目，调用一次 /user/login 或 /user/register，看控制台是否出现 [AOP-Service] 日志</li>
 *   <li>注释掉 {@code result = joinPoint.proceed();} 只留一行 throw，观察接口是否不再执行业务（理解 proceed 的含义）</li>
 *   <li>修改切入点：只切 {@code UserServiceImpl}（把 pointcut 换成更窄的 execution）</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@Order(10)
public class ServiceLayerLogAspect {

    /**
     * 切入点：com.spingbootinit.service 包及其子包（含 impl）里，任意类的任意 public 方法。
     * <p>
     * execution 格式：execution(返回值 全限定类名.方法名(参数))
     * <ul>
     *   <li>第一个 {@code *}：任意返回值</li>
     *   <li>{@code com.spingbootinit.service..*}：包 service 下任意层子包里的任意类（.. 表示子包）</li>
     *   <li>{@code .*}：任意方法名</li>
     *   <li>{@code (..)}：任意参数列表</li>
     * </ul>
     */
    @Around("execution(* com.spingbootinit.controller.UserController.login(..)) || " +
            "execution(* com.spingbootinit.controller.UserController.register(..))")
    public Object logAroundServiceCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();
        String username = extractUsername(args);
        HttpServletRequest request = resolveRequest(args);
        String ip = resolveIp(request);
        String uri = request != null ? request.getRequestURI() : "N/A";
        String httpMethod = request != null ? request.getMethod() : HttpMethod.POST.name();

        long start = System.currentTimeMillis();
        log.info("[AOP-Auth] >>> {} {} method={}.{} username={} ip={}",
                httpMethod, uri, className, methodName, username, ip);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[AOP-Auth] <<< FAIL {} {} method={}.{} username={} ip={} costMs={} exType={} message={}",
                    httpMethod, uri, className, methodName, username, ip, cost,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }

        long cost = System.currentTimeMillis() - start;
        log.info("[AOP-Auth] <<< SUCCESS {} {} method={}.{} username={} ip={} costMs={}",
                httpMethod, uri, className, methodName, username, ip, cost);
        return result;
    }

    private String extractUsername(Object[] args) {
        if (args == null) {
            return "N/A";
        }
        for (Object arg : args) {
            if (arg instanceof LoginRequest loginRequest) {
                return safe(loginRequest.getUsername());
            }
            if (arg instanceof RegisterRequest registerRequest) {
                return safe(registerRequest.getUsername());
            }
        }
        return "N/A";
    }

    private HttpServletRequest resolveRequest(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest request) {
                    return request;
                }
            }
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "N/A";
        }
        String ip = ClientIpUtils.resolve(request);
        return ip == null || ip.isBlank() ? "N/A" : ip;
    }

    private String safe(String val) {
        return (val == null || val.isBlank()) ? "N/A" : val;
    }
}
