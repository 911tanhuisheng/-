# Ubuntu 判题节点部署与验收

## 首次部署

```bash
cd <项目根目录>
cp deploy/ubuntu/sandbox.env.example deploy/ubuntu/sandbox.env
nano deploy/ubuntu/sandbox.env
chmod +x deploy/ubuntu/start-sandbox.sh
./deploy/ubuntu/start-sandbox.sh
```

脚本会检查 Docker、Java、Maven和安全帽权重，构建 `mayue-oj-python-vision:1.0`，打包并后台启动 Sandbox，最后确认 Docker 健康状态。

主服务配置：

```text
SANDBOX_URL=http://<Ubuntu-IP>:8080/api/sandbox
SANDBOX_AUTH_KEY=<与 sandbox.env 完全一致>
```

不要把 8080 端口直接暴露到公网，只允许主 API 服务器访问。

## 验收

```bash
curl http://127.0.0.1:8080/api/health
docker image inspect mayue-oj-python-vision:1.0
docker run --rm --network none mayue-oj-python-vision:1.0 \
  python3 -c "from ultralytics import YOLO; print(YOLO('/models/hard-hat-best.pt').names)"
```

健康接口必须返回 `status=ok`，模型类别必须包含 `head`、`helmet`、`person`。

如启动失败：

```bash
tail -n 200 .runtime-logs/sandbox.log
docker info
```
