package com.spingbootinit.utils;

import cn.hutool.core.util.RandomUtil;
import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 根据标题生成 URL slug（ASCII 化 + 去重由调用方处理）
 */
public final class BlogSlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^a-z0-9\\-]");

    private BlogSlugUtils() {
    }

    public static String slugifyTitle(String title) {
        if (title == null) {
            return "";
        }
        String t = Normalizer.normalize(title.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        t = t.toLowerCase(Locale.ROOT);
        t = NON_LATIN.matcher(t).replaceAll("-");
        t = t.replaceAll("-{2,}", "-");
        t = StringUtils.strip(t, "-");
        if (t.length() > 80) {
            t = t.substring(0, 80).replaceAll("-+$", "");
        }
        if (t.isEmpty()) {
            t = "post-" + RandomUtil.randomString(6).toLowerCase(Locale.ROOT);
        }
        return t;
    }

    public static String withRandomSuffix(String base) {
        return base + "-" + RandomUtil.randomString(4).toLowerCase(Locale.ROOT);
    }
}
