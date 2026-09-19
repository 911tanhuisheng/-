package com.spingbootinit.judo.mq;

import com.spingbootinit.config.AsyncConfig;
import com.spingbootinit.judo.JudgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * async 模式专用：在独立线程池里执行 {@link JudgeService#doJudge(long)}，HTTP 提交接口可立即返回。
 */
@Slf4j
@Component
public class JudgeAsyncExecutor {

    @Resource
    @Lazy
    private JudgeService judgeService;

    @Async(AsyncConfig.JUDGE_TASK_EXECUTOR)
    public void submitJudgeAsync(Long submitId) {
        if (submitId == null) {
            return;
        }
        try {
            judgeService.doJudge(submitId);
        } catch (Exception e) {
            log.error("[异步判题] 异常 submitId={}", submitId, e);
        }
    }
}
