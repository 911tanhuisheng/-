#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${SCRIPT_DIR}/sandbox.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  cp "${SCRIPT_DIR}/sandbox.env.example" "${ENV_FILE}"
  echo "已创建 ${ENV_FILE}，请修改密钥后重新运行。"
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

command -v docker >/dev/null || { echo "缺少 docker"; exit 1; }
command -v java >/dev/null || { echo "缺少 Java 17+"; exit 1; }
command -v mvn >/dev/null || { echo "缺少 Maven"; exit 1; }
docker info >/dev/null || { echo "Docker 未启动或当前用户没有权限"; exit 1; }

MODEL="${REPO_ROOT}/Sandbox/docker/python-vision/models/hard-hat-best.pt"
[[ -s "${MODEL}" ]] || { echo "缺少安全帽模型: ${MODEL}"; exit 1; }

cd "${REPO_ROOT}"
docker build -t "${SANDBOX_PYTHON_IMAGE}" -f Sandbox/docker/python-vision/Dockerfile .
mvn -q -f Sandbox/pom.xml -DskipTests package

mkdir -p "${SANDBOX_TMP_DIR}" "${REPO_ROOT}/.runtime-logs"
JAR="$(find "${REPO_ROOT}/Sandbox/target" -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)"
[[ -n "${JAR}" ]] || { echo "未找到 Sandbox jar"; exit 1; }

PID_FILE="${REPO_ROOT}/.runtime-logs/sandbox.pid"
if [[ -f "${PID_FILE}" ]] && kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
  echo "Sandbox 已运行，PID=$(cat "${PID_FILE}")"
else
  nohup java -jar "${JAR}" >"${REPO_ROOT}/.runtime-logs/sandbox.log" 2>&1 &
  echo $! >"${PID_FILE}"
fi

for _ in {1..30}; do
  if curl -fsS "http://127.0.0.1:${SERVER_PORT}/api/health" | grep -q '"status":"ok"'; then
    echo "Sandbox 与 Docker 已就绪: http://127.0.0.1:${SERVER_PORT}/api/health"
    exit 0
  fi
  sleep 1
done

echo "Sandbox 启动失败，请查看 ${REPO_ROOT}/.runtime-logs/sandbox.log"
exit 1
