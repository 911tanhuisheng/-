# 码跃 OJ（MaYue OJ）
本项目是面向智能科学与技术专业毕业设计开发的在线编程判题系统。课题名称建议使用：

> **融合图像识别的智能化在线编程判题系统设计与实现**

传统 OJ 主要比较程序的文本输入与输出，难以直接支持目标检测、OCR 和图像属性分析等视觉编程任务。本系统在常规题库、代码编辑、竞赛和多语言判题基础上，引入 YOLOv8、EasyOCR、OpenCV、MinIO 与独立 Docker Sandbox，形成“题图获取—智能标注—题目生成—隔离运行—多策略判题—结果反馈”的完整闭环。

系统包含教师端和学生端：教师可以上传、拍摄或由本地生成式模型生成题图，系统自动识别、分类并填充题目；学生编写 Python、Java 或 C++ 程序，代码在受限容器内运行，判题中心根据题型选择文本匹配、数量容差、分类标签、检测框 IoU、OCR 编辑距离或数值容差策略完成评测。

后端 Spring Boot 3 · 前端 Vue 3 · MySQL · Redis · MinIO · YOLOv8 · EasyOCR · Docker Sandbox

## 研究目标与特色

- 在传统 OJ 中扩展图像目标计数、图像分类、目标检测框、OCR 和图像属性五类视觉编程题。
- 将模型能力用于教师出题和标准答案生成，同时让学生代码在隔离环境中完成实际推理。
- 使用公开样例和隐藏图像用例分离机制，避免学生直接硬编码样例答案。
- 根据任务语义选择不同判题策略，而不是只进行字符串完全相等比较。
- 支持 COCO 通用目标检测与本项目训练的校园安全帽模型切换。
- 题图统一存入 MinIO，出题、做题、判题和提交记录使用同一套数据链路。

## 系统角色

| 角色 | 主要功能 |
|------|----------|
| 管理员 / 教师 | AI 辅助出题、题图上传或生成、自动标注、隐藏用例配置、题目与比赛管理 |
| 学生 | 浏览题库、查看公开样例、在线编程、样例运行、提交评测、查看错误原因 |
| 判题节点 | 编译和运行用户代码、限制时间与内存、加载视觉模型、返回标准化执行结果 |

## 图像题闭环

```text
上传/拍照/AI 生成题图
          ↓
      MinIO 存储
          ↓
YOLOv8 / EasyOCR / OpenCV 自动标注
          ↓
AI 结构化题面、分类和标准答案
          ↓
公开样例 + 隐藏图像测试用例
          ↓
学生代码进入 Docker Sandbox 运行
          ↓
判题策略比较学生输出与标准结果
          ↓
AC / WA / CE / RE / TLE / MLE
```

其中，自动标注用于减轻教师录题工作量，发布前仍允许人工复核；正式提交使用隐藏图片重新运行学生程序，确保评测结果来自代码能力而不是样例记忆。

## 预览

将截图放到 [`docs/screenshots/`](./docs/screenshots/) 后，下方会自动显示（文件名见该目录说明）。

| 首页 | 做题页 |
| :---: | :---: |
| ![首页](./docs/screenshots/home.png) | ![做题页](./docs/screenshots/problem.png) |

| 提交结果 | 比赛榜单 |
| :---: | :---: |
| ![提交结果](./docs/screenshots/submission.png) | ![比赛榜单](./docs/screenshots/contest.png) |

---

## 功能

- 题库做题、样例运行、提交评测（Monaco 编辑器）
- 竞赛与排行榜
- 用户系统（JWT、单端登录、签到日历）
- 博客、评论、站内通知
- 管理端（题目 / 比赛 / 公告 / 用户）
- 做题页 AI 助手（可选，阿里云百炼 SSE）
- 图像编程题：上传/AI 生成题图，YOLOv8、校园安全模型、EasyOCR 自动标注
- 智能判题：文本精确匹配、目标数量容差、分类标签、检测框 IoU、OCR 编辑距离、图像属性容差
- 公开样例与隐藏图像用例隔离，防止学生直接硬编码标准答案

---

## 架构

主服务 `spingboot-init` 负责业务与落库；**Sandbox** 在 Docker 里跑用户代码，两者 HTTP 通信。

```
浏览器 → 主 API (:8888) → MySQL / Redis
              ↓
         Sandbox (:8080) → Docker
```

更细的部署图、时序图、表结构见 [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)。

图像智能判题闭环见 [docs/VISION_JUDGE_DESIGN.md](./docs/VISION_JUDGE_DESIGN.md)，毕业论文与实验准备见 [docs/GRADUATION_THESIS_GUIDE.md](./docs/GRADUATION_THESIS_GUIDE.md)，Ubuntu 一键部署 Sandbox 见 [deploy/ubuntu/README.md](./deploy/ubuntu/README.md)。

毕业设计提交时不要直接打包虚拟环境和依赖缓存。运行 `powershell -ExecutionPolicy Bypass -File .\tools\create-delivery-package.ps1` 可在项目同级目录生成不含密钥、缓存和编译产物的精简压缩包，清单见 [DELIVERY_CHECKLIST.md](./DELIVERY_CHECKLIST.md)。

> **判题服务建议部署在 Linux + Docker。** 主站和前端可在 Windows 开发；生产环境不要把 Sandbox 端口直接暴露公网。说明见 [Sandbox/README.md](./Sandbox/README.md)。

---

## 目录

```text
前端/myoj/          # Vue 3 前端
spingboot-init/     # 主业务 API（目录名是历史拼写，未改包名）
Sandbox/            # 判题沙箱
docs/               # 架构文档与截图
```

---

## 本地运行

### 依赖

JDK 17+、Node 20.19+ / 22.12+、MySQL 8+、Redis、MinIO、Docker（跑 Sandbox 时需要）

### 步骤

1. 初始化库表：

```bash
mysql -u root -p < spingboot-init/src/main/resources/db/schema.sql
```

内置管理员 `admin` / `123456`（仅开发用，上线请改密）和 15 道入门题。需要完整的 40 道基础算法题库时，再导入 [`question_bank_expansion.sql`](./spingboot-init/src/main/resources/db/question_bank_expansion.sql)；该脚本新增 25 道原创 OJ 题，不会覆盖已有题目。

2. 配置密钥：复制 [.env.example](./.env.example) 为 `.env`，或按 `application-local.yml.example` 建本地覆盖文件（不要提交 Git）。

3. 构建图像判题 Python 镜像，再启动 Sandbox（需 Docker）：

```bash
docker build -t mayue-oj-python-vision:1.0 -f Sandbox/docker/python-vision/Dockerfile .
cd Sandbox && mvn spring-boot:run
```

4. 启动主服务（`codesandbox.url` 默认指向 `http://127.0.0.1:8080/api/sandbox`）：

```bash
cd spingboot-init && mvn spring-boot:run
```

5. 启动 YOLOv8 + EasyOCR 自动标注服务：

```powershell
cd vision-ai
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
python run.py
```

若需要“AI 生成题图 → 自动识别 → 自动出题”，Windows + NVIDIA 环境可直接在项目根目录运行：

```powershell
start-image-ai.bat
```

可直接双击 `start-image-ai.bat`。首次运行会自动安装 ComfyUI、NVIDIA PyTorch、YOLO/OCR 依赖并下载 Stable Diffusion 1.5 模型；后续会跳过已安装内容直接启动。新电脑需预先安装 Git、Python 3.12/3.13 和 NVIDIA 显卡驱动。脚本同时启动 `vision-ai:8090` 和 `ComfyUI:8188`，生成图片由后端转存到 MinIO，不依赖阿里云 OSS。

6. 前端：

```bash
cd 前端/myoj && npm install && npm run dev
```

管理员可以创建目标计数、图像分类、检测框、OCR、图像属性分析五类图像编程题。选择题型并上传题图后，点击「一键识别并生成题目」，系统会自动生成标题、题面、分类、参考答案、公开样例、标准输出与评测限制；发布前需人工复核 AI 标注。

目标检测类题目可选择 `YOLO_GENERAL`（COCO 通用场景）或 `YOLO_HELMET`（本项目训练的校园安全帽模型，类别为 head / helmet / person）；OCR 与图像属性题分别自动路由到 EasyOCR 和 OpenCV。模型选择会随题目保存，并同步到识别服务、学生参考代码和隔离判题镜像。

- 页面：http://localhost:5173  
- 接口文档：http://localhost:8888/api/doc.html  

生产可参考 `前端/myoj/deploy/nginx.example.conf`。

---

## 文档

| 文件 | 说明 |
|------|------|
| [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) | 架构与接口模块 |
| [docs/VISION_JUDGE_DESIGN.md](./docs/VISION_JUDGE_DESIGN.md) | 图像出题、模型路由与智能判题闭环 |
| [docs/GRADUATION_THESIS_GUIDE.md](./docs/GRADUATION_THESIS_GUIDE.md) | 毕业论文结构、实验指标与答辩演示建议 |
| [spingboot-init/src/main/resources/db/README.md](./spingboot-init/src/main/resources/db/README.md) | 数据库脚本 |
| [Sandbox/README.md](./Sandbox/README.md) | 判题机部署 |
| [deploy/ubuntu/README.md](./deploy/ubuntu/README.md) | Ubuntu 判题节点部署与验收 |
| [DELIVERY_CHECKLIST.md](./DELIVERY_CHECKLIST.md) | 源码提交、恢复和验收清单 |

---

## 安全与开源

**配置**

- 仓库内 `application.yml` 已脱敏，本地真实密码写在 `.env` 或 `application-local.yml`（已在 `.gitignore`）。
- 首次 clone 后请复制 [.env.example](./.env.example) 或 `application-local.yml.example` 再填自己的值。

**若曾提交过含密钥的旧配置**

- 在 MinIO、百炼、邮箱、JWT 等服务中**轮换密钥**后再公开仓库；仅改当前文件无法清除 Git 历史中的泄露。

**许可**

- [MIT](./LICENSE)
- 源码包不分发任何第三方音乐或歌词。音乐播放器组件仅作为前端功能实现保留；如需演示，应使用本人原创或具有明确再分发许可的音频，并另行标注作者与许可证。
