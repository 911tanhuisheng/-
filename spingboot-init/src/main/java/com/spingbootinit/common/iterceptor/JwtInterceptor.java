package com.spingbootinit.common.iterceptor;


import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.service.UserService;
import com.spingbootinit.utils.JwtUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private JwtUtils jwtUtils;
    @Resource
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 浏览器跨域会先发 OPTIONS 预检，不能要求带 Token，否则预检失败会表现为 CORS 错误
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        // 获取 token：优先 Authorization；SSE(EventSource) 可用 query access_token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            String queryToken = request.getParameter("access_token");
            if (queryToken != null && !queryToken.isBlank()) {
                authHeader = "Bearer " + queryToken.trim();
            }
        }
        if (authHeader == null || authHeader.isBlank()) {
            throw new BusinessException(401, "请先登录");
        }
        authHeader = authHeader.trim();
        final String bearerPrefix = "Bearer ";
        if (!authHeader.regionMatches(true, 0, bearerPrefix, 0, bearerPrefix.length())) {
            throw new BusinessException(401, "请先登录");
        }
        String token = authHeader.substring(bearerPrefix.length()).trim();
        if (token.isEmpty()) {
            throw new BusinessException(401, "请先登录");
        }

        // 仅接受 access token（refresh 不能访问业务接口）
        if (!jwtUtils.validateAccessToken(token)) {
            throw new BusinessException(401, "token无效或已过期");
        }
        Long userIdFromToken = jwtUtils.getUserIdFromToken(token);
        if (userIdFromToken == null) {
            throw new BusinessException(401, "token无效或已过期");
        }
        User byIdWithCache = userService.getByIdWithCache(userIdFromToken);
        if (byIdWithCache == null) {
            throw new BusinessException(401, "用户不存在或token无效");
        }
        User user = userService.refreshExpiredBanState(byIdWithCache);

        userService.assertTokenSessionActive(userIdFromToken, token);
        enforceAccountStatus(request, token, userIdFromToken, user);

        // 将用户信息保存到ThreadLocal中
        UserContext.setCurrenUserId(userIdFromToken);
        UserContext.setCurrentRole(user.getUserRole());
        log.info("用户ID: {}", UserContext.getCurrentUserId());
        log.info("用户角色: {}", UserContext.getCurrentRole());
        return true;
    }

    /**
     * 禁用账号策略：
     * <ul>
     *   <li>禁用前签发的 token：全部接口拒绝，强制重新登录（40304）</li>
     *   <li>禁用后重新登录的 token：仅允许资料/账户/会话状态等基础接口（40303 拦截业务功能）</li>
     * </ul>
     */
    private void enforceAccountStatus(HttpServletRequest request, String token, Long userId, User user) {
        if (userService.isProfanityCommentBanActive(user)) {
            String servletPath = request.getServletPath();
            if (HttpMethod.POST.matches(request.getMethod()) && "/blog/comment/add".equals(servletPath)) {
                throw new BusinessException(
                        ResultCode.COMMENT_PROFANITY_BANNED.getCode(),
                        userService.profanityCommentBanMessage(user));
            }
        }
        if (userService.isProfanityAiAssistBanActive(user)) {
            String servletPath = request.getServletPath();
            if (HttpMethod.POST.matches(request.getMethod()) && "/bailian_assist/chat".equals(servletPath)) {
                throw new BusinessException(
                        ResultCode.AI_ASSIST_PROFANITY_BANNED.getCode(),
                        userService.profanityAiAssistBanMessage(user));
            }
        }
        Integer status = user.getStatus();
        if (status == null || status != 0) {
            return;
        }
        Long disabledAt = userService.getDisabledAtMillis(userId);
        Long issuedAt = jwtUtils.getIssuedAtMillisFromToken(token);
        boolean sessionRevoked = disabledAt != null && issuedAt != null && issuedAt < disabledAt;
        if (sessionRevoked) {
            throw new BusinessException(
                    ResultCode.ACCOUNT_SESSION_REVOKED.getCode(),
                    ResultCode.ACCOUNT_SESSION_REVOKED.getMessage());
        }
        String servletPath = request.getServletPath();
        if (!userService.isDisabledUserAllowedPath(servletPath)) {
            throw new BusinessException(
                    ResultCode.ACCOUNT_DISABLED.getCode(),
                    ResultCode.ACCOUNT_DISABLED.getMessage());
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

}
