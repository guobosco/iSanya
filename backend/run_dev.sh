#!/usr/bin/env bash
set -euo pipefail

if [[ -x ".venv/bin/python" ]]; then
  PYTHON=".venv/bin/python"
else
  PYTHON="${PYTHON:-python3}"
fi

if ! "$PYTHON" -c "import uvicorn" >/dev/null 2>&1; then
  echo "uvicorn 未安装在当前 Python 环境：$PYTHON" >&2
  echo "请先执行：" >&2
  echo "  python3 -m venv .venv" >&2
  echo "  source .venv/bin/activate" >&2
  echo "  pip install -r requirements.txt" >&2
  exit 1
fi

HOST="${LULU_HOST:-0.0.0.0}"
PORT="${1:-${LULU_PORT:-8000}}"
"$PYTHON" -m uvicorn main:app --host "$HOST" --port "$PORT" --reload
