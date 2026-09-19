FROM python:3.11-slim

ENV PIP_NO_CACHE_DIR=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    YOLO_CONFIG_DIR=/tmp/Ultralytics

RUN apt-get update \
    && apt-get install -y --no-install-recommends libglib2.0-0 libgl1 libgomp1 \
    && rm -rf /var/lib/apt/lists/* \
    && pip install pillow opencv-python-headless ultralytics easyocr \
    && mkdir -p /models \
    && python -c "from ultralytics import YOLO; m=YOLO('yolov8n.pt'); import shutil; shutil.copyfile(m.ckpt_path, '/models/yolov8n.pt')" \
    && python -c "import easyocr; easyocr.Reader(['ch_sim','en'], gpu=False)"

COPY Sandbox/docker/python-vision/models/hard-hat-best.pt /models/hard-hat-best.pt

WORKDIR /app
