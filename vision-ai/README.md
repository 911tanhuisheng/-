# Vision AI service

YOLOv8 + EasyOCR automatic annotation service for OJ image questions.

Supported authoring modes:

- object counting
- dominant-object classification
- object detection boxes
- Chinese/English OCR
- basic image feature analysis

```powershell
cd vision-ai
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python run.py
```

Health check: `GET http://127.0.0.1:8090/health`.

The first general detection downloads `yolov8n.pt`; the first OCR request downloads
the EasyOCR Chinese/English models. Model routing keys are:

- `YOLO_GENERAL`: COCO general detection (`VISION_MODEL_GENERAL`)
- `YOLO_HELMET`: trained campus hard-hat detection (`VISION_MODEL_HELMET`)
- `EASYOCR_ZH_EN`: Chinese/English OCR
- `OPENCV_ANALYSIS`: image size, grayscale mean and edge strength

`POST /detect` accepts `modelKey`. The default custom model is
`models/hard-hat-best.pt`.
