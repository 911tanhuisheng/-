package com.spingbootinit.model.dto.codevalidate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CodeSyntaxValidateRequest {

    @NotBlank
    @Pattern(regexp = "cpp|java|python")
    private String language;

    @NotBlank
    @Size(max = 100_000)
    private String code;
}
