package com.spingbootinit.judo.strategy;

import com.spingbootinit.model.dto.question.JudgeCase;
import com.spingbootinit.model.dto.question.JudgeConfig;
import com.spingbootinit.model.dto.questionsubmit.JudgeInfo;
import com.spingbootinit.model.entity.Question;
import com.spingbootinit.model.enums.JudgeInfoMessageEnum;

import java.util.List;
import java.util.Objects;

/**
 * Java 程序的判题策略
 */
public class JavaLanguageJudgeStrategy implements JudgeStrategy {

    /**
     * 执行判题
     *
     * @param judgeContext
     * @return
     */

    /**
     * 与 JudgeConfig、沙箱返回的 time 单位保持一致：这里按「毫秒」示例
     */
    private static final long JAVA_EXTRA_TIME_MS = 1000L;

    @Override
    public JudgeInfo doJudge(JudgeContext judgeContext) {
        JudgeInfo judgeInfo = judgeContext.getJudgeInfo();
        Long memory = judgeInfo.getMemory();
        Long time = judgeInfo.getTime();

        List<String> inputList = judgeContext.getInputList();
        List<String> outputList = judgeContext.getOutputList();
        Question question = judgeContext.getQuestion();
        List<JudgeCase> judgeCaseList = judgeContext.getJudgeCaseList();

        JudgeInfoMessageEnum judgeInfoMessageEnum = JudgeInfoMessageEnum.ACCEPTED;
        JudgeInfo judgeInfoResponse = new JudgeInfo();
        judgeInfoResponse.setMemory(memory);
        judgeInfoResponse.setTime(time);

        // 1. 用例数量一致性
        if (inputList == null || outputList == null || judgeCaseList == null) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
            return judgeInfoResponse;
        }
        if (outputList.size() != judgeCaseList.size() || outputList.size() != inputList.size()) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
            return judgeInfoResponse;
        }

        // 2.逐一比对
        for (int i = 0; i < judgeCaseList.size(); i++) {
            String output = normalize(judgeCaseList.get(i).getOutput());
            String actual = normalize(outputList.get(i));
            if (!Objects.equals(output, actual)) {
                judgeInfoResponse.setMessage(JudgeInfoMessageEnum.WRONG_ANSWER.getValue());
                return judgeInfoResponse;
            }
        }

        // 3.资源限制
        JudgeConfig cfg = question.getJudgeConfig();
        if (cfg == null) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.ACCEPTED.getValue());
            return judgeInfoResponse;
        }
        Long needMemory = cfg.getMemoryLimit();
        Long needTime = cfg.getTimeLimit();

        if (needTime != null && memory != null && memory > needMemory) {
            judgeInfoResponse.setMessage(JudgeInfoMessageEnum.MEMORY_LIMIT_EXCEEDED.getValue());
            return judgeInfoResponse;
        }
        // Java：从总耗时里扣除 JVM/启动等额外时间后再和题目时限比（按你产品定义调整）
        if (needTime != null && time != null) {
            long effective = time - JAVA_EXTRA_TIME_MS;
            if (effective > needTime) {
                judgeInfoResponse.setMessage(JudgeInfoMessageEnum.TIME_LIMIT_EXCEEDED.getValue());
                return judgeInfoResponse;
            }
        }
        judgeInfoResponse.setMessage(judgeInfoMessageEnum.getValue());
        return judgeInfoResponse;
    }
    /**
     * 统一换行与空白：每行 trim 后去掉行内所有空白（解决 {@code [0,1]} 与 {@code [0, 1]} 等格式差异）。
     * 换行仍保留为行分隔符，避免多行输出被压成一行后误判为相同。
     */
    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String normalized = s.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines[i].replaceAll("\\s+", ""));
        }
        return sb.toString().trim();
    }

}
