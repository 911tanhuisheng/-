package com.spingbootinit.model.vo.codevalidate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSyntaxValidateResponseVO {
    private List<CodeSyntaxDiagnosticVO> diagnostics;
    /** 如本机未安装编译器时的说明 */
    private String notice;
}
