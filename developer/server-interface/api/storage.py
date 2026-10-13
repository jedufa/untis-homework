from __future__ import annotations

import json
import os
import uuid
from datetime import datetime
from pathlib import Path

STATE_PATH: Path
DOMAIN_ICONS_PATH: Path
NOTES_PATH: Path
APPEARANCE_PATH: Path
DOMAIN_STYLES_PATH: Path


def configure_paths(data_dir: Path) -> None:
    global STATE_PATH, DOMAIN_ICONS_PATH, NOTES_PATH, APPEARANCE_PATH, DOMAIN_STYLES_PATH
    data_dir.mkdir(parents=True, exist_ok=True)
    STATE_PATH = data_dir / "homework_state.json"
    DOMAIN_ICONS_PATH = data_dir / "domain_icons.json"
    NOTES_PATH = data_dir / "personal_notes.json"
    APPEARANCE_PATH = data_dir / "appearance.json"
    DOMAIN_STYLES_PATH = data_dir / "domain_styles.json"


def load_domain_icons() -> dict[str, object]:
    if not DOMAIN_ICONS_PATH.exists():
        return {"icons": {}, "children": {}}
    try:
        data = json.loads(DOMAIN_ICONS_PATH.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return {"icons": {}, "children": {}}
    if not isinstance(data, dict):
        return {"icons": {}, "children": {}}
    icons = data.get("icons", {})
    children = data.get("children", {})
    normalized_children = {}
    if isinstance(children, dict):
        for child_key, child_icons in children.items():
            if isinstance(child_icons, dict):
                normalized_children[str(child_key)] = {
                    str(key): str(value)
                    for key, value in child_icons.items()
                    if str(value).startswith("data:image/")
                }
    return {
        "icons": {
            str(key): str(value)
            for key, value in icons.items()
            if str(value).startswith("data:image/")
        }
        if isinstance(icons, dict)
        else {},
        "children": normalized_children,
    }


def save_domain_icon(child_key: str, subject_key: str, image_data: str) -> dict[str, object]:
    if not child_key or not subject_key:
        raise ValueError("invalid_icon")
    if image_data and (not image_data.startswith("data:image/") or len(image_data) > 700_000):
        raise ValueError("invalid_icon")
    payload = load_domain_icons()
    children = payload.setdefault("children", {})
    if not isinstance(children, dict):
        children = {}
        payload["children"] = children
    child_icons = children.setdefault(child_key, {})
    if not isinstance(child_icons, dict):
        child_icons = {}
        children[child_key] = child_icons
    if image_data:
        child_icons[subject_key] = image_data
    else:
        child_icons.pop(subject_key, None)
    payload["schema_version"] = "untis_domain_icons_v2"
    write_private_json(DOMAIN_ICONS_PATH, payload)
    return payload


def load_appearance() -> dict[str, object]:
    return load_json_object(APPEARANCE_PATH, {"schema_version": "untis_appearance_v1", "children": {}})


def save_child_appearance(child_key: str, appearance: dict[str, object]) -> dict[str, object]:
    if not child_key:
        raise ValueError("invalid_appearance")
    payload = load_appearance()
    children = payload.setdefault("children", {})
    if not isinstance(children, dict):
        children = {}
        payload["children"] = children
    children[child_key] = normalize_appearance(appearance)
    payload["schema_version"] = "untis_appearance_v1"
    write_private_json(APPEARANCE_PATH, payload)
    return payload


def normalize_appearance(raw: dict[str, object]) -> dict[str, str]:
    theme = str(raw.get("theme") or "ocean").strip()[:40]
    font = str(raw.get("font") or "system").strip()[:40]
    bg_color = str(raw.get("bgColor") or "#edf2f5").strip()
    bg_image = str(raw.get("bgImage") or "")
    if len(bg_color) != 7 or not bg_color.startswith("#"):
        bg_color = "#edf2f5"
    if bg_image and (not bg_image.startswith("data:image/") or len(bg_image) > 1_500_000):
        bg_image = ""
    return {"theme": theme, "font": font, "bgColor": bg_color, "bgImage": bg_image}


def load_domain_styles() -> dict[str, object]:
    return load_json_object(DOMAIN_STYLES_PATH, {"schema_version": "untis_domain_styles_v1", "children": {}})


def save_domain_styles(children: dict[str, object]) -> dict[str, object]:
    payload = {"schema_version": "untis_domain_styles_v1", "children": normalize_domain_styles(children)}
    write_private_json(DOMAIN_STYLES_PATH, payload)
    return payload


def normalize_domain_styles(children: dict[str, object]) -> dict[str, dict[str, dict[str, str]]]:
    normalized: dict[str, dict[str, dict[str, str]]] = {}
    for child_key, styles in children.items():
        if not isinstance(styles, dict):
            continue
        child_styles: dict[str, dict[str, str]] = {}
        for subject_key, style in styles.items():
            if not isinstance(style, dict):
                continue
            color = str(style.get("color") or "").strip()
            if len(color) == 7 and color.startswith("#"):
                child_styles[str(subject_key)] = {"color": color}
        normalized[str(child_key)] = child_styles
    return normalized


def load_notes() -> dict[str, object]:
    if not NOTES_PATH.exists():
        return {"schema_version": "untis_personal_notes_v1", "children": {}}
    try:
        data = json.loads(NOTES_PATH.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return {"schema_version": "untis_personal_notes_v1", "children": {}}
    children = data.get("children", {}) if isinstance(data, dict) else {}
    normalized_children: dict[str, list[dict[str, str]]] = {}
    if isinstance(children, dict):
        for child_key, notes in children.items():
            if not isinstance(notes, list):
                continue
            normalized_children[str(child_key)] = [normalize_note(item) for item in notes if isinstance(item, dict)]
            normalized_children[str(child_key)].sort(key=lambda item: (item["date"], item["title"], item["id"]))
    return {"schema_version": "untis_personal_notes_v1", "children": normalized_children}


def normalize_note(raw: dict[str, object]) -> dict[str, str]:
    return {
        "id": str(raw.get("id") or uuid.uuid4().hex),
        "title": str(raw.get("title") or "").strip()[:80],
        "text": str(raw.get("text") or "").strip()[:2000],
        "category": str(raw.get("category") or "").strip()[:40],
        "date": normalized_note_date(raw.get("date")),
        "color": normalized_note_color(raw.get("color")),
        "icon": str(raw.get("icon") or "*").strip()[:4] or "*",
        "updatedAt": str(raw.get("updatedAt") or datetime.now().isoformat(timespec="seconds")),
    }


def normalized_note_date(value: object) -> str:
    text = str(value or "").strip()
    try:
        datetime.strptime(text, "%Y-%m-%d")
    except ValueError:
        return datetime.now().date().isoformat()
    return text


def normalized_note_color(value: object) -> str:
    text = str(value or "").strip()
    return text if len(text) == 7 and text.startswith("#") else "#fff7cc"


def save_note(child_key: str, note: dict[str, object]) -> dict[str, object]:
    if not child_key:
        raise ValueError("invalid_note")
    payload = load_notes()
    children = payload.setdefault("children", {})
    if not isinstance(children, dict):
        children = {}
        payload["children"] = children
    notes = children.setdefault(child_key, [])
    if not isinstance(notes, list):
        notes = []
        children[child_key] = notes
    normalized = normalize_note({**note, "updatedAt": datetime.now().isoformat(timespec="seconds")})
    if not normalized["title"] and not normalized["text"]:
        raise ValueError("invalid_note")
    for index, existing in enumerate(notes):
        if isinstance(existing, dict) and str(existing.get("id")) == normalized["id"]:
            notes[index] = normalized
            break
    else:
        notes.append(normalized)
    notes.sort(key=lambda item: (str(item.get("date", "")), str(item.get("title", "")), str(item.get("id", ""))))
    write_private_json(NOTES_PATH, payload)
    return payload


def delete_note(child_key: str, note_id: str) -> dict[str, object]:
    payload = load_notes()
    children = payload.setdefault("children", {})
    if isinstance(children, dict) and isinstance(children.get(child_key), list):
        children[child_key] = [item for item in children[child_key] if str(item.get("id")) != note_id]
    write_private_json(NOTES_PATH, payload)
    return payload


def write_private_json(path: Path, payload: dict[str, object]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_suffix(".json.tmp")
    tmp_path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    tmp_path.replace(path)
    os.chmod(path, 0o600)


def load_json_object(path: Path, fallback: dict[str, object]) -> dict[str, object]:
    if not path.exists():
        return fallback
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return fallback
    return data if isinstance(data, dict) else fallback
