package com.spingbootinit.controller;


import com.google.code.kaptcha.Producer;
import com.spingbootinit.common.constant.RedisKeyConstants;
import com.spingbootinit.utils.ClientIpUtils;
import com.spingbootinit.utils.RedisUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 验证码图形验证码
 */
@Tag(name = "验证码", description = "验证码接口")
@RestController
public class CaptchaController {

    // 限制时间：比如 2 秒内只能点一次
    private static final int LIMIT_SECONDS = 2;
    // 验证码有效期（分钟）
    private static final int CAPTCHA_EXPIRE_MINUTES = 5;
    @Resource
    private RedisUtil redisUtil;
    @Resource
    private Producer captchaProducer;

    @GetMapping("/captcha")
    @Operation(summary = "获取验证码", description = "获取验证码")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 获取用户唯一标识 (简单起见用 IP，生产环境建议结合 User-Agent 或 SessionId)
        String clientIp = ClientIpUtils.resolve(request);
        String limitKey = RedisKeyConstants.captchaLimitKey(clientIp);

        // 2. 【核心限流逻辑】
        // 尝试在 Redis 中设置一个标记，如果设置成功（返回 true），说明之前没有标记，可以生成验证码
        // 如果返回 false，说明 2 秒内已经点过了，直接拒绝
        Boolean isFirstRequest = redisUtil.setIfAbsent(limitKey, "1", LIMIT_SECONDS, TimeUnit.SECONDS);

        if (Boolean.FALSE.equals(isFirstRequest)) {
            // 限流触发
            response.setContentType("application/json;charset=UTF-8");
            response.sendError(429, "请求太频繁，请稍后再试"); // 429 是标准的 "Too Many Requests" 状态码
            return;
        }

        // 3. 通过限流检查，开始生成验证码
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setDateHeader("Expires", 0);

        // 创建验证码
        String capText = captchaProducer.createText();
        // 生成图片验证码
        BufferedImage image = captchaProducer.createImage(capText);
        // 存入 Session
        //  request.getSession().setAttribute("captcha", capText);
        // 将文本验证码存入 Redis
        // 这里的 Key 最好也带上 IP 或者生成一个随机 UUID，避免被覆盖
        String codeKey = RedisKeyConstants.captchaCodeKey(clientIp);
        redisUtil.set(codeKey, capText, CAPTCHA_EXPIRE_MINUTES, TimeUnit.MINUTES);

        // 输出图片
        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        out.flush();
        out.close();
    }
}