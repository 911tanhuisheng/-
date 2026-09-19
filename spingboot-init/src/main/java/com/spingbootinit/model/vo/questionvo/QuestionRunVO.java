package com.spingbootinit.model.vo.questionvo;

import com.spingbootinit.model.dto.questionsubmit.JudgeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 运行代码返回（沙箱 + 样例比对摘要）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRunVO {

    /** 沙箱 HTTP/业务层 code，0 通常表示调用成功 */
    private Integer sandboxCode;

    private String sandboxMessage;

    private JudgeInfo judgeInfo;

    /** 编译错误 / TLE / RE 等：不再展示逐条用例比对 */
    private boolean terminalError;

    /** 最多 3 条公开样例 */
    private List<QuestionRunCaseVO> cases;

    /** 非 terminal 且全部样例规范化后一致 */
    private boolean allSamplePassed;

    /**
     * 非致命提示：例如沙箱返回的 outputList 条数与样例组数不一致（仍尽量展示已返回的行）
     */
    private String warning;

    /** 结构化提示（单样例、疑似常量输出、未读 stdin 等），便于前端逐条展示 */
    private List<String> hints;

    /**
     * 启发式：疑似仅用常量 print 通过样例（非沙箱错误，而是「运行」只做字符串比对）
     */
    private boolean suspectedShortcut;
}
