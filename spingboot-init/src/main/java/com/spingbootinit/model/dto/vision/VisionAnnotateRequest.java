package com.spingbootinit.model.dto.vision;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VisionAnnotateRequest {
    @NotBlank(message = "图片 URL 不能为空")
    private String imageUrl;

    @DecimalMin(value = "0.05", message = "置信度不能低于 0.05")
    @DecimalMax(value = "0.95", message = "置信度不能高于 0.95")
    private Double confidence = 0.25;

    @DecimalMin(value = "0.1", message = "IoU 不能低于 0.1")
    @DecimalMax(value = "0.95", message = "IoU 不能高于 0.95")
    private Double iou = 0.7;

    private String taskType = "IMAGE_OBJECT_COUNT";

    private String modelKey = "YOLO_GENERAL";
}
