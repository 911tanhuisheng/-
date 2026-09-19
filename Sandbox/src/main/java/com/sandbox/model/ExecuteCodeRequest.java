package com.sandbox.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteCodeRequest {

    private List<String> inputList;

    private String code; // 用户代码

    private String language;

    /** 图像题用：与 inputList 一一对应的 Base64 图片。 */
    private List<String> imageBase64List;

}
