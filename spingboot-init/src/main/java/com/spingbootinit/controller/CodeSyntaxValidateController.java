package com.spingbootinit.controller;

import com.spingbootinit.common.result.Result;
import com.spingbootinit.model.dto.codevalidate.CodeSyntaxValidateRequest;
import com.spingbootinit.model.vo.codevalidate.CodeSyntaxValidateResponseVO;
import com.spingbootinit.service.impl.CodeSyntaxValidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 编辑器语法检查（g++/javac/python），供 Monaco 标红。
 */
@RestController
@RequestMapping("/code_validate")
@Tag(name = "CodeValidate", description = "代码语法检查")
public class CodeSyntaxValidateController {

    @Resource
    private CodeSyntaxValidateService codeSyntaxValidateService;

    @PostMapping("/syntax")
    @Operation(summary = "按语言做语法级检查，返回行列与错误信息")
    public Result<CodeSyntaxValidateResponseVO> syntax(@Valid @RequestBody CodeSyntaxValidateRequest request) {
        CodeSyntaxValidateResponseVO vo = codeSyntaxValidateService.validate(request.getLanguage(), request.getCode());
        return Result.success(vo);
    }
}
