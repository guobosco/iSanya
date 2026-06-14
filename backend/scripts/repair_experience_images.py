import json
import shutil
import sqlite3
from pathlib import Path
from typing import List, Optional
from urllib.parse import urlsplit


BASE_DIR = Path(__file__).resolve().parents[1]
DB_PATH = BASE_DIR / "isanya.db"
STATIC_DIR = BASE_DIR / "static"
EXPERIENCE_DIR = STATIC_DIR / "experiences"
SERVICE_DIR = STATIC_DIR / "services"
PLACEHOLDER_SHA256 = "e330cd023298a812503e10a067a3f88e1cbc094f37f6fd2a88fdb6799495b37e"


def sha256_of(path: Path) -> str:
    import hashlib

    digest = hashlib.sha256()
    with path.open("rb") as file:
        while True:
            chunk = file.read(1024 * 1024)
            if not chunk:
                break
            digest.update(chunk)
    return digest.hexdigest()


def parse_json_list(raw_value: str) -> List[str]:
    try:
        data = json.loads(raw_value or "[]")
    except json.JSONDecodeError:
        return []
    return [item for item in data if isinstance(item, str)]


def resolve_static_path(raw_url: str) -> Optional[Path]:
    path = urlsplit(raw_url).path.strip("/")
    if not path.startswith("static/"):
        return None
    return STATIC_DIR / path.removeprefix("static/")


def list_service_pool() -> List[Path]:
    return sorted(path for path in SERVICE_DIR.rglob("*.jpg") if path.is_file())


def needs_repair(path: Path) -> bool:
    if not path.exists():
        return True
    try:
        return sha256_of(path) == PLACEHOLDER_SHA256
    except OSError:
        return True


def main() -> None:
    if not DB_PATH.exists():
        raise RuntimeError(f"数据库不存在: {DB_PATH}")

    service_pool = list_service_pool()
    if not service_pool:
        raise RuntimeError("没有可用的 service 图片，无法修复 experience 图片")

    repaired = 0
    pool_index = 0

    with sqlite3.connect(DB_PATH) as conn:
        rows = conn.execute("select id, cover_image_url, image_urls from experiences order by id").fetchall()

    for _, cover_image_url, image_urls_raw in rows:
        urls = []
        if cover_image_url:
            urls.append(cover_image_url)
        urls.extend(parse_json_list(image_urls_raw or "[]"))

        for raw_url in urls:
            target = resolve_static_path(raw_url)
            if target is None:
                continue
            if not needs_repair(target):
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            source = service_pool[pool_index % len(service_pool)]
            shutil.copy2(source, target)
            repaired += 1
            pool_index += 1

    print(f"已修复或补齐 experience 图片文件: {repaired}")


if __name__ == "__main__":
    main()
