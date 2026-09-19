package com.spingbootinit.model.dto.bailian;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BailianAssistTurnDTO {

    @NotBlank
    @Pattern(regexp = "(?i)(user|assistant)", message = "role 须为 user 或 assistant")
    private String role;

    @NotBlank
    @Size(max = 12000)
    private String content;
}
