package com.spingbootinit.model.vo.codevalidate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 与 Monaco IMarkerData 对齐：行列均为 1-based */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSyntaxDiagnosticVO {
    private int startLineNumber;
    private int startColumnNumber;
    private int endLineNumber;
    private int endColumnNumber;
    private String message;
    /** ERROR 或 WARNING */
    private String severity;
}
