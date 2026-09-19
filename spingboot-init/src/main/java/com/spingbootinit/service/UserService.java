package com.spingbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.spingbootinit.model.dto.user.*;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.model.vo.uservo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;


/**
 * 用户服务接口。
 * <p>
 * 可以把它理解为“用户模块的业务能力清单”：
 * Controller 只负责接请求，而具体能做哪些事、每件事的输入输出是什么，
 * 在这里统一定义，真正实现则放在 {@code UserServiceImpl} 中。
 * </p>
 *
 * @author yaoyaoyan
 * @description 针对表【user(用户表)】的数据库操作Service
 * @createDate 2026-03-25 23:32:31
 */

public interface UserService extends IService<User> {
    /**
     * 根据 id 查询用户，并使用缓存。
     *
     * @param userId 用户 id
     * @return 用户信息
     */
     User getByIdWithCache(Long userId);

    /**
     * 注册新用户。
     *
     * @param registerRequest 前端提交的注册表单，通常包含用户名、邮箱、密码、确认密码等
     */
    void register(@Valid RegisterRequest registerRequest);

    /**
     * 用户登录。
     *
     * @param loginRequest 登录请求，包含用户名、密码、验证码等
     * @param request HTTP 请求对象，用来读取客户端 IP 等信息
     * @return 登录成功后返回 token、用户名、角色、头像等前端初始化所需信息
     */
    LoginVO login(@Valid LoginRequest loginRequest, HttpServletRequest request);

    /**
     * 使用 refreshToken 换取新的 accessToken（并轮换 refreshToken）。
     */
    TokenRefreshVO refreshTokens(@Valid RefreshTokenRequest request);

    /**
     * 更新当前登录用户资料。
     *
     * @param userId 当前登录用户 id，不由前端传入，而是由登录态解析得到
     * @param request 本次想要更新的资料字段；哪个字段非空，就更新哪个字段
     */
    void updateMyProfile(Long userId, @Valid UserUpdateMyRequest request);

    /**
     * 修改当前登录用户密码。
     *
     * @param userId 当前登录用户 id
     * @param request 包含旧密码、新密码等信息
     */
    void updatePassword(Long userId, @Valid UpdatePasswordRequest request);

    /**
     * 管理员分页查询全部用户。
     *
     * @param current 当前页码，从 1 开始
     * @param size 每页条数
     * @param keyword 可选，按用户名模糊搜索
     * @return 管理端用户分页结果，包含总数、分页参数、当前页记录
     */
    UserAdminPageVO pageUsersForAdmin(long current, long size, String keyword);

    /**
     * 管理员设置用户启用/禁用状态。
     *
     * @param operatorUserId 当前执行操作的管理员 id
     * @param request 包含目标用户 id 和目标状态
     */
    void setUserStatusByAdmin(Long operatorUserId, @Valid UserAdminSetStatusRequest request);

    /**
     * 管理员禁用用户时：记录禁用时间并作废其全部 refresh token。
     */
    void revokeSessionsOnDisable(Long userId);

    /**
     * 管理员解禁用户时：清除禁用时间标记。
     */
    void clearDisableMarkerOnEnable(Long userId);

    /**
     * 读取用户被禁用的时间戳（毫秒），无则 null。
     */
    Long getDisabledAtMillis(Long userId);

    /**
     * 校验 access/refresh 所属会话是否为当前唯一登录（否则其他设备已登录）。
     */
    void assertTokenSessionActive(Long userId, String token);

    /**
     * 禁用状态下仍允许访问的路径（资料、会话状态等）。
     */
    boolean isDisabledUserAllowedPath(String servletPath);

    /**
     * 当前登录用户的账号状态（供前端轮询）。
     */
    UserSessionStatusVO getSessionStatus(Long userId);

    /**
     * 评论含违禁词：限制发表评论指定天数（账号 status 仍为 1，不影响做题等）。
     */
    void banForProfanityComment(Long userId, String matchedWord);

    /**
     * 是否处于「违禁评论」限制期（仅限制发表评论，不影响 status）。
     */
    boolean isProfanityCommentBanActive(User user);

    /**
     * 违禁评论限制提示文案。
     */
    String profanityCommentBanMessage(User user);

    /**
     * 发表博客评论前校验（含违禁评论限制）。
     */
    void assertCanPostComment(Long userId);


    /**
     * 管理员提前解除「违禁评论」限制。
     */
    void clearProfanityCommentBanByAdmin(Long operatorUserId, Long userId);

    /**
     * 定时任务：解禁已到期的违禁评论限制。
     */
    void releaseExpiredProfanityBans();

    /**
     * AI 助手消息含违禁词：限制使用学习助手（账号 status 仍为 1）。
     */
    void banForProfanityAiAssist(Long userId, String matchedWord);

    boolean isProfanityAiAssistBanActive(User user);

    String profanityAiAssistBanMessage(User user);

    void assertCanUseAiAssist(Long userId);

    void clearProfanityAiAssistBanByAdmin(Long operatorUserId, Long userId);

    /**
     * 定时任务：解禁已到期的 AI 助手违禁限制。
     */
    void releaseExpiredProfanityAiBans();

    /**
     * 定时任务：解禁已到期的管理员限时禁用。
     */
    void releaseExpiredAdminBans();

    /**
     * 若封禁已到期则自动解禁并返回最新用户（供鉴权、会话状态等使用）。
     */
    User refreshExpiredBanState(User user);

    /**
     * 当前用户自助注销账号。
     *
     * @param userId 当前登录用户 id
     * @param request 注销确认参数，目前主要是登录密码
     */
    void deleteMyAccount(Long userId, @Valid DeleteMyAccountRequest request);

    /**
     * 查询当前用户的签到展示数据（读库，不写库）。
     *
     * @param userId 当前登录用户主键，从 JWT 解析后传入
     * @return 今日是否已签、累计次数、积分；用于前端禁用「今日签到」按钮等
     */
    UserCheckInStatusVO getCheckInStatus(Long userId);

    /**
     * 执行「今日首次签到」：积分 +100，累计次数 +1，并写入最后签到日为今天。
     * <p>
     * 若 {@link com.spingbootinit.model.entity.User#getLastCheckInDate()} 已是今天，则抛出业务异常「今日已签到」，
     * 避免重复加分（需在 Service 层保证，不要只靠前端按钮禁用）。
     * </p>
     *
     * @param userId 当前登录用户主键
     * @return 签到成功后的最新状态（与查询接口字段一致）
     */
    UserCheckInStatusVO checkInToday(Long userId);

    /**
     * 学习打卡日历：指定自然年内已签到日期列表。
     */
    UserCheckInCalendarVO getCheckInCalendar(Long userId, Integer year);

    /**
     * 全站用户榜分页。
     *
     * @param dimension 排序维度：{@code composite}（默认，综合）、{@code points}（积分优先）、{@code submissions}（总提交次数优先）
     */
    Page<UserLeaderboardRowVO> pageUserLeaderboard(long current, long pageSize, String dimension);

    /**
     * 将「已读公告游标」更新为不小于 {@code announcementId}（用于公告已读）
     */
    void updateLastReadAnnouncementUpTo(long userId, long announcementId);
}
