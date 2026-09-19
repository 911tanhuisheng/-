from __future__ import annotations

import asyncio
import io
import os
from collections import Counter
from functools import lru_cache
from ipaddress import ip_address
from urllib.parse import urlparse

import httpx
from fastapi import FastAPI, HTTPException
from PIL import Image, UnidentifiedImageError
from pydantic import BaseModel, ConfigDict, Field, HttpUrl
from ultralytics import YOLO
import numpy as np


APP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_REGISTRY = {
    "YOLO_GENERAL": os.getenv("VISION_MODEL_GENERAL", "yolov8n.pt"),
    "YOLO_HELMET": os.getenv(
        "VISION_MODEL_HELMET", os.path.join(APP_DIR, "models", "hard-hat-best.pt")
    ),
}
MAX_IMAGE_BYTES = int(os.getenv("VISION_MAX_IMAGE_BYTES", str(10 * 1024 * 1024)))
REQUEST_TIMEOUT = float(os.getenv("VISION_REQUEST_TIMEOUT_SECONDS", "15"))

app = FastAPI(title="MaYue OJ Vision AI", version="1.0.0")
model_lock = asyncio.Lock()


class DetectionRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    image_url: HttpUrl = Field(alias="imageUrl")
    confidence: float = Field(default=0.25, ge=0.05, le=0.95)
    iou: float = Field(default=0.7, ge=0.1, le=0.95)
    task_type: str = Field(default="IMAGE_OBJECT_COUNT", alias="taskType")
    model_key: str = Field(default="YOLO_GENERAL", alias="modelKey")


class DetectionBox(BaseModel):
    label: str
    confidence: float
    x1: float
    y1: float
    x2: float
    y2: float


class DetectionResponse(BaseModel):
    model: str
    width: int
    height: int
    counts: dict[str, int]
    boxes: list[DetectionBox]
    inference_ms: float
    ocr_text: str | None = None
    image_analysis: dict[str, int | float | str] | None = None


@lru_cache(maxsize=4)
def get_model(model_key: str) -> YOLO:
    path = MODEL_REGISTRY.get(model_key)
    if path is None:
        raise ValueError(f"unsupported model key: {model_key}")
    if model_key == "YOLO_HELMET" and not os.path.isfile(path):
        raise FileNotFoundError(f"custom model does not exist: {path}")
    return YOLO(path)


@lru_cache(maxsize=1)
def get_ocr_reader():
    import easyocr
    return easyocr.Reader(["ch_sim", "en"], gpu=False)


def reject_unsafe_remote_url(raw_url: str) -> None:
    parsed = urlparse(raw_url)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname:
        raise HTTPException(status_code=400, detail="image_url must be an HTTP(S) URL")
    # Local MinIO is intentionally allowed for this deployment. Link-local and multicast
    # addresses remain blocked to avoid common SSRF targets.
    try:
        addr = ip_address(parsed.hostname)
        if addr.is_link_local or addr.is_multicast or addr.is_unspecified:
            raise HTTPException(status_code=400, detail="unsafe image host")
    except ValueError:
        pass


async def download_image(url: str) -> Image.Image:
    reject_unsafe_remote_url(url)
    try:
        async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT, follow_redirects=True) as client:
            async with client.stream("GET", url) as response:
                response.raise_for_status()
                content_type = response.headers.get("content-type", "").lower()
                if content_type and not content_type.startswith("image/"):
                    raise HTTPException(status_code=400, detail="URL does not point to an image")
                body = bytearray()
                async for chunk in response.aiter_bytes():
                    body.extend(chunk)
                    if len(body) > MAX_IMAGE_BYTES:
                        raise HTTPException(status_code=413, detail="image is larger than configured limit")
    except HTTPException:
        raise
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=400, detail=f"failed to download image: {exc}") from exc

    try:
        image = Image.open(io.BytesIO(body))
        image.load()
        return image.convert("RGB")
    except (UnidentifiedImageError, OSError) as exc:
        raise HTTPException(status_code=400, detail="invalid image data") from exc


def infer(image: Image.Image, confidence: float, iou: float, task_type: str, model_key: str) -> DetectionResponse:
    width, height = image.size
    detections: list[DetectionBox] = []
    counts: Counter[str] = Counter()
    result = None
    normalized_task = task_type.upper()
    if normalized_task not in {"IMAGE_OCR", "IMAGE_ANALYSIS"}:
        model = get_model(model_key)
        result = model.predict(source=image, conf=confidence, iou=iou, verbose=False)[0]
    if result is not None and result.boxes is not None:
        xyxy = result.boxes.xyxy.cpu().tolist()
        classes = result.boxes.cls.cpu().tolist()
        confidences = result.boxes.conf.cpu().tolist()
        for coords, class_id, score in zip(xyxy, classes, confidences):
            label = str(result.names[int(class_id)]).lower()
            counts[label] += 1
            detections.append(
                DetectionBox(
                    label=label,
                    confidence=round(float(score), 4),
                    x1=round(float(coords[0]) / width, 6),
                    y1=round(float(coords[1]) / height, 6),
                    x2=round(float(coords[2]) / width, 6),
                    y2=round(float(coords[3]) / height, 6),
                )
            )
    ocr_text = None
    if normalized_task == "IMAGE_OCR":
        lines = get_ocr_reader().readtext(np.asarray(image), detail=0, paragraph=True)
        # OJ 每个用例使用单条 stdout，合并为一行避免沙箱日志清洗时丢失内容。
        ocr_text = " ".join(str(line).strip() for line in lines if str(line).strip())
    gray = np.asarray(image.convert("L"), dtype=np.float32)
    gx = np.abs(np.diff(gray, axis=1)).mean() if width > 1 else 0.0
    gy = np.abs(np.diff(gray, axis=0)).mean() if height > 1 else 0.0
    analysis = {
        "width": width,
        "height": height,
        "mode": "RGB",
        "mean_gray": round(float(gray.mean()), 2),
        "edge_strength": round(float(gx + gy), 2),
    }
    return DetectionResponse(
        model=("easyocr-zh-en" if normalized_task == "IMAGE_OCR" else
               "opencv-pillow" if normalized_task == "IMAGE_ANALYSIS" else model_key),
        width=width,
        height=height,
        counts=dict(sorted(counts.items())),
        boxes=detections,
        inference_ms=round(float(sum(result.speed.values())), 2) if result is not None else 0.0,
        ocr_text=ocr_text,
        image_analysis=analysis,
    )


@app.get("/health")
def health() -> dict[str, object]:
    return {"status": "ok", "models": MODEL_REGISTRY}


@app.post("/detect", response_model=DetectionResponse)
async def detect(request: DetectionRequest) -> DetectionResponse:
    image = await download_image(str(request.image_url))
    async with model_lock:
        try:
            return await asyncio.to_thread(
                infer, image, request.confidence, request.iou, request.task_type, request.model_key
            )
        except HTTPException:
            raise
        except Exception as exc:
            raise HTTPException(status_code=500, detail=f"YOLO inference failed: {exc}") from exc
