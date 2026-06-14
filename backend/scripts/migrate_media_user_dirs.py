import json
import shutil
import sqlite3
from pathlib import Path
from typing import List, Optional, Set, Tuple
from urllib.parse import urlsplit, urlunsplit


BASE_DIR = Path(__file__).resolve().parents[1]
DB_PATH = BASE_DIR / "isanya.db"
STATIC_DIR = BASE_DIR / "static"


def parse_json_list(raw_value: Optional[str]) -> List[str]:
    if not raw_value:
        return []
    try:
        data = json.loads(raw_value)
    except json.JSONDecodeError:
        return []
    return [item for item in data if isinstance(item, str)]


def build_new_url(raw_url: str, biz_plural: str, user_id: str) -> Tuple[str, Optional[str]]:
    parsed = urlsplit(raw_url)
    path = parsed.path
    prefix = f"static/{biz_plural}/"
    if not path.startswith(prefix):
        return raw_url, None

    relative_path = path[len(prefix):]
    parts = [part for part in relative_path.split("/") if part]
    filename = parts[-1] if parts else ""
    if not filename:
        return raw_url, None

    new_path = f"{prefix}{user_id}/{filename}"
    new_url = urlunsplit(("", "", new_path, parsed.query, ""))
    source_path = STATIC_DIR / biz_plural / filename if len(parts) == 1 else STATIC_DIR / biz_plural / Path(*parts)
    return new_url, str(source_path)


def resolve_source_path(source_path: Optional[str], biz_plural: str, target_filename: str) -> Optional[str]:
    candidates = []
    if source_path:
        candidates.append(Path(source_path))
    candidates.append(STATIC_DIR / biz_plural / target_filename)

    seen = set()
    for candidate in candidates:
        raw = str(candidate)
        if raw in seen:
            continue
        seen.add(raw)
        if candidate.exists() and candidate.is_file():
            return raw
    return source_path


def ensure_target_copy(source_path: Optional[str], biz_plural: str, user_id: str, target_filename: str) -> Optional[str]:
    if not source_path:
        return None
    source = Path(source_path)
    if not source.exists():
        return None

    target_dir = STATIC_DIR / biz_plural / user_id
    target_dir.mkdir(parents=True, exist_ok=True)
    target = target_dir / target_filename
    if source.resolve() != target.resolve():
        shutil.copy2(source, target)
    return str(target)


def migrate_table(
    conn: sqlite3.Connection,
    table: str,
    owner_field: str,
    biz_plural: str,
) -> Tuple[int, Set[str]]:
    rows = conn.execute(
        f"select id, {owner_field}, cover_image_url, image_urls from {table}"
    ).fetchall()
    updated = 0
    copied_flat_files: Set[str] = set()

    for row_id, owner_id, cover_image_url, image_urls_raw in rows:
        if not owner_id:
            continue

        changed = False
        new_cover = cover_image_url
        new_images = parse_json_list(image_urls_raw)

        migrated_cover, source_cover = build_new_url(cover_image_url or "", biz_plural, owner_id)
        cover_filename = Path(urlsplit(migrated_cover).path).name if migrated_cover else ""
        resolved_cover_source = resolve_source_path(source_cover, biz_plural, cover_filename) if cover_filename else source_cover
        if cover_filename:
            ensure_target_copy(resolved_cover_source, biz_plural, owner_id, cover_filename)
            if resolved_cover_source and Path(resolved_cover_source).parent == (STATIC_DIR / biz_plural):
                copied_flat_files.add(resolved_cover_source)
        if migrated_cover != (cover_image_url or ""):
            new_cover = migrated_cover
            changed = True

        rewritten_images: List[str] = []
        for old_url in new_images:
            migrated_url, source_path = build_new_url(old_url, biz_plural, owner_id)
            target_filename = Path(urlsplit(migrated_url).path).name if migrated_url else ""
            resolved_source_path = resolve_source_path(source_path, biz_plural, target_filename) if target_filename else source_path
            if target_filename:
                ensure_target_copy(resolved_source_path, biz_plural, owner_id, target_filename)
                if resolved_source_path and Path(resolved_source_path).parent == (STATIC_DIR / biz_plural):
                    copied_flat_files.add(resolved_source_path)
            if migrated_url != old_url:
                changed = True
            rewritten_images.append(migrated_url)

        if changed:
            conn.execute(
                f"update {table} set cover_image_url = ?, image_urls = ? where id = ?",
                (new_cover, json.dumps(rewritten_images, ensure_ascii=False), row_id),
            )
            updated += 1

    return updated, copied_flat_files


def cleanup_flat_files(file_paths: Set[str]) -> int:
    removed = 0
    for raw_path in sorted(file_paths):
        path = Path(raw_path)
        if path.exists() and path.is_file():
            path.unlink()
            removed += 1
    return removed


def main() -> None:
    if not DB_PATH.exists():
        raise RuntimeError(f"数据库不存在: {DB_PATH}")

    with sqlite3.connect(DB_PATH) as conn:
        service_updated, service_copies = migrate_table(
            conn=conn,
            table="services",
            owner_field="creator_id",
            biz_plural="services",
        )
        experience_updated, experience_copies = migrate_table(
            conn=conn,
            table="experiences",
            owner_field="host_id",
            biz_plural="experiences",
        )
        conn.commit()

    removed = cleanup_flat_files(service_copies | experience_copies)

    print(f"services 已迁移记录: {service_updated}")
    print(f"experiences 已迁移记录: {experience_updated}")
    print(f"已清理旧平铺文件: {removed}")


if __name__ == "__main__":
    main()
