import os

import uvicorn


if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=os.getenv("VISION_HOST", "127.0.0.1"),
        port=int(os.getenv("VISION_PORT", "8090")),
        reload=False,
    )
