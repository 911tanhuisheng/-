package com.spingbootinit.utils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 转义符号
 * @author: zhangtian
 * @date: 2023/03/07
 * @description:
 */
@Component
public class JsonUtils{

    public List<String> normalizeWebsiteList(List<String> websites) {
        List<String> out = new ArrayList<>();
        for (String item : websites) {
            if (item == null) continue;
            String s = item.trim();
            if (s.isEmpty()) continue;

            // 去掉首尾多余引号，例如 "\"fdf\""、"\"rf\""、'"abc"'
            while ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                if (s.length() <= 1) break;
                s = s.substring(1, s.length() - 1).trim();
            }
            // 处理被转义的引号
            s = s.replace("\\\"", "\"").trim();
            while ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                if (s.length() <= 1) break;
                s = s.substring(1, s.length() - 1).trim();
            }
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }
}
