from pathlib import Path

import torch
from ultralytics import YOLO


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DATASET_YAML = PROJECT_ROOT.parent / "datasets" / "hard-hat-workers-ready" / "data.yaml"
RUNS_DIR = PROJECT_ROOT / "vision-ai" / "runs"


def main() -> None:
    print(f"PyTorch: {torch.__version__}")
    print(f"CUDA available: {torch.cuda.is_available()}")
    if not torch.cuda.is_available():
        raise RuntimeError("CUDA 不可用，请重启电脑后再测试")
    print(f"GPU: {torch.cuda.get_device_name(0)}")
    if not DATASET_YAML.exists():
        raise FileNotFoundError(f"数据集配置不存在: {DATASET_YAML}")

    model = YOLO("yolov8n.pt")
    result = model.train(
        data=str(DATASET_YAML),
        epochs=1,
        fraction=0.02,
        imgsz=512,
        batch=2,
        workers=0,
        device=0,
        optimizer="SGD",
        amp=True,
        project=str(RUNS_DIR),
        name="hard-hat-smoke-test",
        exist_ok=True,
        plots=False,
    )
    best = Path(result.save_dir) / "weights" / "best.pt"
    if not best.exists():
        raise RuntimeError(f"测试结束但未生成权重: {best}")
    print("\nSMOKE_TEST_SUCCESS")
    print(f"Weight: {best}")


if __name__ == "__main__":
    main()
