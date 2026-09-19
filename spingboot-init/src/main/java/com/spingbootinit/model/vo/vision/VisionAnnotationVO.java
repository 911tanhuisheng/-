package com.spingbootinit.model.vo.vision;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VisionAnnotationVO {
    private String model;
    private Integer width;
    private Integer height;
    private Map<String, Integer> counts;
    private List<VisionDetectionBoxVO> boxes;
    @JsonProperty("inference_ms")
    private Double inferenceMs;
    @JsonProperty("ocr_text")
    private String ocrText;
    @JsonProperty("image_analysis")
    private Map<String, Object> imageAnalysis;
}
