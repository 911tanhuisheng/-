package com.spingbootinit.service.impl;

import com.spingbootinit.config.ModerationProperties;
import com.spingbootinit.service.ContentModerationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ContentModerationServiceImpl implements ContentModerationService {

    @Resource
    private ModerationProperties moderationProperties;

    private List<String> normalizedWords = List.of();

    @PostConstruct
    public void init() {
        reloadWords();
    }

    private void reloadWords() {
        List<String> raw = moderationProperties.getBadWords();
        if (raw == null || raw.isEmpty()) {
            normalizedWords = List.of();
            return;
        }
        List<String> list = new ArrayList<>();
        for (String w : raw) {
            if (!StringUtils.hasText(w)) {
                continue;
            }
            String n = normalize(w);
            if (!n.isEmpty() && !list.contains(n)) {
                list.add(n);
            }
        }
        normalizedWords = List.copyOf(list);
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    @Override
    public boolean containsBadWord(String text) {
        return findMatchedBadWord(text) != null;
    }

    @Override
    public String findMatchedBadWord(String text) {
        if (!StringUtils.hasText(text) || normalizedWords.isEmpty()) {
            return null;
        }
        String hay = normalize(text);
        if (hay.isEmpty()) {
            return null;
        }
        for (String word : normalizedWords) {
            if (hay.contains(word)) {
                return word;
            }
        }
        return null;
    }
}
