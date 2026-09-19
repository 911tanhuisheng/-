package com.spingbootinit.service.impl;

import com.spingbootinit.common.constant.RedisKeyConstants;
import com.spingbootinit.service.UserStatusNotifyService;
import com.spingbootinit.utils.RedisUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class UserStatusNotifyServiceImpl implements UserStatusNotifyService {

    private static final long SSE_TIMEOUT_MS = 30L * 60 * 1000;

    @Resource
    private RedisUtil redisUtil;

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    @Override
    public long bumpStatusRevision(Long userId) {
        if (userId == null) {
            return 0L;
        }
        long rev = System.currentTimeMillis();
        redisUtil.set(RedisKeyConstants.statusRevisionKey(userId), String.valueOf(rev));
        pushToEmitters(userId, rev);
        return rev;
    }

    @Override
    public Long getStatusRevision(Long userId) {
        if (userId == null) {
            return 0L;
        }
        Object raw = redisUtil.get(RedisKeyConstants.statusRevisionKey(userId));
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> removeEmitter(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());
        try {
            long rev = getStatusRevision(userId);
            emitter.send(SseEmitter.event().name("connected").data(String.valueOf(rev)));
        } catch (IOException e) {
            cleanup.run();
        }
        return emitter;
    }

    private void pushToEmitters(Long userId, long rev) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) {
            return;
        }
        String data = String.valueOf(rev);
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("status").data(data));
            } catch (Exception e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(userId, list);
        }
    }
}
