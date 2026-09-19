package com.spingbootinit.model.vo.vision;

import lombok.Data;

@Data
public class VisionDetectionBoxVO {
    private String label;
    private Double confidence;
    private Double x1;
    private Double y1;
    private Double x2;
    private Double y2;
}
