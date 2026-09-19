# 毕业设计项目交付清单

## 建议提交内容

- `前端/myoj`：Vue 3 前端源码
- `spingboot-init`：Spring Boot 主服务源码与数据库脚本
- `Sandbox`：Docker 判题沙箱及图像判题镜像
- `vision-ai`：YOLOv8、EasyOCR 自动标注服务
- `docs`：系统架构、图像判题设计与毕业论文实验指南
- `deploy`：Ubuntu 部署脚本
- 两份 `hard-hat-best.pt`：本地标注服务和 Sandbox 分别使用
- `DELIVERY_MANIFEST.json`：文件大小与 SHA-256 完整性清单

## 不放入提交包的内容

- `.venv`、`node_modules`：可根据依赖清单重新安装
- `target`、`dist`、Jar、class：可重新构建
- 重复训练权重 `best.pt`、`last.pt`：正式模型 `hard-hat-best.pt` 已保留
- `.git`、IDE 配置、日志、缓存和临时代码
- `.env`、`ap.env`、`application-local.yml`：可能包含密码或密钥

## 收到项目后的启动顺序

1. 安装并启动 MySQL、Redis、MinIO、Docker。
2. 导入 `spingboot-init/src/main/resources/db/schema.sql`。
3. 复制 `.env.example` 为 `.env`，填写本机配置。
4. 在 Ubuntu 按 `deploy/ubuntu/README.md` 启动 Sandbox。
5. 在 `vision-ai` 创建虚拟环境并安装 `requirements.txt`，运行 `python run.py`。
6. 在 `spingboot-init` 执行 `mvn spring-boot:run`。
7. 在 `前端/myoj` 执行 `npm install` 和 `npm run dev`。

## 提交前验收

- `mvn -f spingboot-init/pom.xml test`
- `mvn -f Sandbox/pom.xml test`
- `npm --prefix 前端/myoj install && npm --prefix 前端/myoj run build`
- `curl http://127.0.0.1:8080/api/health`
- 新增一道人脸/车辆或安全帽图像题，包含公开样例和隐藏用例，提交代码得到 Accepted。

生成精简交付包：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\create-delivery-package.ps1
```
