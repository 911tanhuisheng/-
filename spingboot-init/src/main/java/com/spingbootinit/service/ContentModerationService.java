package com.spingbootinit.service;

/**
 * 评论等内容违禁词检测。
 */
public interface ContentModerationService {

    /** 是否包含配置的违禁词 */
    boolean containsBadWord(String text);

    /** 命中的违禁词（用于日志/封禁原因，未命中返回 null） */
    String findMatchedBadWord(String text);
}
