package com.spingbootinit.aspect.practice;

import cn.hutool.core.util.StrUtil;
import com.spingbootinit.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 【顺序演示】与 {@link ServiceLayerLogAspect} 一起看控制台输出顺序。
 * <p>
 * {@code @Order(1)} 比 {@code ServiceLayerLogAspect} 的 {@code @Order(10)} 数字更小，
 * 因此这个切面的 @Before 会在「另一切面的 @Around 最外层之内、靠近目标方法的一侧」参与拼装
 * （直观感受：多切面时谁在更外层，由 Order 控制）。
 * <p>
 * 练习：把两处的 Order 数字对调，重新调用接口，观察日志先后顺序变化。
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class AspectOrderDemoAspect {

//    @Pointcut("execution(* com.spingbootinit.service..*.*(..))")
//    public void serviceLayerMethods() {
//    }

    @Around("@annotation(Annon)")
    public Object beforeServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {

        // 通过aop来获取这个方法名和参数名
        String name = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String method = joinPoint.getSignature().getName();

        // 获取方法上的注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Annon annotation = methodSignature.getMethod().getAnnotation(Annon.class);
        if(annotation !=null){
            // 获取注解上的名字
            String simpleName = annotation.annotationType().getSimpleName();
            // 注解上的属性
            String value = annotation.value();
            log.info("注解的名字：{},注解上的属性：{}",simpleName,value);
        }

        if (StrUtil.isAllBlank(name, method)) {
            throw new BusinessException("获取的方法和类名不存在");
        }
        //todo
        long l = System.currentTimeMillis();
        log.info("进入到类名：{}，方法名：{}，开始的时间:{}", name, method, l);


        Object re;

        re = joinPoint.proceed();
        long l1 = System.currentTimeMillis();

        long useTime = l1 - l;
        log.warn("总共使用的时间是：{}", useTime);
        return re;
    }
}
