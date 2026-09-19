package com.spingbootinit.config;



import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.stereotype.Component;



import java.util.ArrayList;

import java.util.List;



/**

 * 评论审核与封禁时长配置（application.yml 中 moderation.*）。

 */

@Data

@Component

@ConfigurationProperties(prefix = "moderation")

public class ModerationProperties {



    /** 违禁评论自动限制时长（小时） */

    private int commentBanHours = 72;



    /** 管理员「限时禁用」允许的最大小时数 */

    private int adminBanMaxHours = 8760;



    /** 违禁词列表 */

    private List<String> badWords = new ArrayList<>(List.of(

            "傻逼", "傻B", "草泥马", "你妈", "去死", "废物", "人渣", "贱人",

            "滚蛋", "神经病", "白痴", "弱智", "操你", "fuck", "shit", "bitch"

    ));



    public int resolvedCommentBanHours() {

        return Math.max(1, commentBanHours);

    }



    public int resolvedAdminBanMaxHours() {

        return Math.max(1, adminBanMaxHours);

    }

}

