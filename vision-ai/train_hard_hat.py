from pathlib import Path

from ultralytics import YOLO


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DATASET_YAML = PROJECT_ROOT.parent / "datasets" / "hard-hat-workers-ready" / "data.yaml"
RUNS_DIR = PROJECT_ROOT / "vision-ai" / "runs"


def main() -> None:
    if not DATASET_YAML.exists():
        raise FileNotFoundError(f"数据集配置不存在: {DATASET_YAML}")

    model = YOLO("yolov8n.pt")
    model.train(
        data=str(DATASET_YAML),
        epochs=100,
        imgsz=640,
        batch=4,
        device=0,
        workers=0,
        patience=20,
        optimizer="SGD",
        lr0=0.01,
        momentum=0.937,
        project=str(RUNS_DIR),
        name="hard-hat-yolov8n",
        pretrained=True,
        cache=False,
        plots=True,
    )
    model.val(
        data=str(DATASET_YAML),
        split="test",
        imgsz=640,
        batch=8,
        device=0,
        project=str(RUNS_DIR),
        name="hard-hat-yolov8n-test",
        plots=True,
    )


if __name__ == "__main__":
    main()
