"""Offline normalization and completion tracking. No account/network client."""
from __future__ import annotations

import json
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any


class UntisError(RuntimeError):
    pass


ELEMENT_TYPE_CLASS = 1
ELEMENT_TYPE_TEACHER = 2
ELEMENT_TYPE_SUBJECT = 3
ELEMENT_TYPE_ROOM = 4
ELEMENT_TYPE_STUDENT = 5


def normalize_office_hours(settings: dict[str, Any], hours: list[dict[str, Any]]) -> dict[str, Any]:
    rows = [
        {
            "teacher": item.get("teacher") or "",
            "teacherFullName": item.get("teacherFullName") or "",
            "teacherId": item.get("teacherId"),
            "date": iso_or_empty(parse_untis_date(item.get("date"))),
            "day": item.get("day") or "",
            "startTime": item.get("startTime") or "",
            "endTime": item.get("endTime") or "",
            "room": item.get("rooms") or "",
            "email": item.get("email") if settings.get("showEmail") else None,
            "phone": item.get("phone") if settings.get("showPhone") else None,
            "available": bool(item.get("available")),
            "registered": bool(item.get("registered")),
            "textByNoAppointment": item.get("textByNoAppointment") or settings.get("noAppointmentMsg") or "",
        }
        for item in hours
    ]
    rows.sort(key=lambda row: (row["teacherFullName"], row["date"], row["startTime"]))
    return {
        "ok": True,
        "schema_version": "untis_office_hours_v1",
        "fetchedAt": datetime.now().isoformat(timespec="seconds"),
        "settings": settings,
        "hours": rows,
    }


def normalize_cancelled_period(period: dict[str, Any], elements: dict[tuple[Any, Any], dict[str, Any]]) -> dict[str, Any]:
    def names(element_type: int) -> list[str]:
        result = []
        for element in period.get("elements", []):
            if element.get("type") != element_type:
                continue
            meta = elements.get((element_type, element.get("id")), {})
            result.append(meta.get("displayname") or meta.get("name") or str(element.get("id")))
        return result

    return {
        "id": period.get("id"),
        "date": iso_or_empty(parse_untis_date(period.get("date"))),
        "startTime": format_untis_time(period.get("startTime")),
        "endTime": format_untis_time(period.get("endTime")),
        "subject": ", ".join(names(ELEMENT_TYPE_SUBJECT)),
        "teacher": ", ".join(names(ELEMENT_TYPE_TEACHER)),
        "room": ", ".join(names(ELEMENT_TYPE_ROOM)),
        "text": period.get("substText") or period.get("periodText") or period.get("lessonText") or "",
        "state": period.get("cellState") or "",
    }


def normalize_homeworks(
    data: dict[str, Any],
    *,
    start: date,
    end: date,
    children: dict[str, dict[str, Any]],
    subjects: dict[str, dict[str, Any]] | None = None,
    today: date | None = None,
) -> dict[str, Any]:
    today = today or date.today()
    homeworks = {item["id"]: item for item in data.get("homeworks", [])}
    teachers = {item["id"]: item for item in data.get("teachers", [])}
    lessons = {item["id"]: item for item in data.get("lessons", [])}

    result_children: dict[str, Any] = {}
    for child_key, child in children.items():
        rows = []
        person_id = child["person_id"]
        for record in data.get("records", []):
            if person_id not in record.get("elementIds", []):
                continue
            homework = homeworks.get(record.get("homeworkId"))
            if not homework:
                continue
            due_date = parse_untis_date(homework.get("dueDate"))
            posted_date = parse_untis_date(homework.get("date"))
            lesson = lessons.get(homework.get("lessonId"), {})
            teacher = teachers.get(record.get("teacherId"), {})
            subject_name = lesson.get("subject") or ""
            subject_meta = (subjects or {}).get(str(subject_name).upper(), {})
            rows.append(
                {
                    "id": record.get("homeworkId"),
                    "subject": subject_name,
                    "subjectLongName": subject_meta.get("longName") or "",
                    "teacher": teacher.get("name") or "",
                    "postedDate": iso_or_empty(posted_date),
                    "dueDate": iso_or_empty(due_date),
                    "dueDateLabel": display_due_date(due_date),
                    "text": homework.get("text") or "",
                    "completed": False,
                    "isPastDue": bool(due_date and due_date < today),
                    "attachments": [attachment.get("name", "") for attachment in homework.get("attachments", [])],
                }
            )
        rows.sort(key=lambda row: (row["dueDate"] or "9999-12-31", row["subject"], row["teacher"], row["id"] or 0))
        result_children[child_key] = {
            "displayName": child["display_name"],
            "personId": person_id,
            "homeworks": rows,
        }

    return {
        "ok": True,
        "schema_version": "untis_homeworks_v1",
        "fetchedAt": datetime.now().isoformat(timespec="seconds"),
        "window": {"start": start.isoformat(), "end": end.isoformat()},
        "children": result_children,
    }


def apply_tracking_state(payload: dict[str, Any], state: dict[str, Any], today: date | None = None) -> dict[str, Any]:
    today = today or date.today()
    completed = set(state.get("completed", []))
    completed_at = state.get("completedAt", {}) if isinstance(state.get("completedAt", {}), dict) else {}
    for child_key, child in payload.get("children", {}).items():
        for homework in child.get("homeworks", []):
            state_key = homework_state_key(child_key, homework.get("id"))
            due_date = date.fromisoformat(homework["dueDate"]) if homework.get("dueDate") else None
            is_completed = state_key in completed
            is_past = bool(due_date and due_date < today)
            homework["completed"] = is_completed
            homework["completedAt"] = str(completed_at.get(state_key, "")) if is_completed else ""
            homework["isPastDue"] = is_past
            homework["status"] = "completed" if is_completed else "not_done" if is_past else "open"
            homework["isArchived"] = homework["status"] in {"completed", "not_done"}
    return payload


def homework_state_key(child_key: str, homework_id: Any) -> str:
    return f"{child_key}:{homework_id}"


def load_tracking_state(path: Path | None) -> dict[str, Any]:
    if not path or not path.exists():
        return {"schema_version": "untis_homework_state_v1", "completed": []}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return {"schema_version": "untis_homework_state_v1", "completed": []}
    if not isinstance(data, dict):
        return {"schema_version": "untis_homework_state_v1", "completed": []}
    completed = data.get("completed", [])
    completed_keys = {str(item) for item in completed}
    completed_at = data.get("completedAt", {})
    if not isinstance(completed_at, dict):
        completed_at = {}
    return {
        "schema_version": "untis_homework_state_v1",
        "completed": sorted(completed_keys),
        "completedAt": {
            str(key): str(value)
            for key, value in completed_at.items()
            if isinstance(completed_at, dict) and str(key) in completed_keys
        },
    }


def save_homework_status(path: Path, child_key: str, homework_id: Any, status: str) -> dict[str, Any]:
    path.parent.mkdir(parents=True, exist_ok=True)
    state = load_tracking_state(path)
    completed = set(state.get("completed", []))
    completed_at = state.get("completedAt", {}) if isinstance(state.get("completedAt", {}), dict) else {}
    key = homework_state_key(child_key, homework_id)
    if status == "completed":
        completed.add(key)
        completed_at.setdefault(key, datetime.now().date().isoformat())
    elif status == "open":
        completed.discard(key)
        completed_at.pop(key, None)
    else:
        raise UntisError("unsupported_homework_status")
    state["completed"] = sorted(completed)
    state["completedAt"] = {item: str(completed_at[item]) for item in state["completed"] if item in completed_at}
    tmp_path = path.with_suffix(path.suffix + ".tmp")
    tmp_path.write_text(json.dumps(state, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    tmp_path.replace(path)
    path.chmod(0o600)
    return state


def parse_untis_date(value: Any) -> date | None:
    if value in {None, "", 0}:
        return None
    text = str(value)
    return date(int(text[0:4]), int(text[4:6]), int(text[6:8]))


def format_untis_time(value: Any) -> str:
    if value in {None, ""}:
        return ""
    text = str(value).zfill(4)
    return f"{text[:-2]}:{text[-2:]}"


def yyyymmdd(value: date) -> int:
    return int(value.strftime("%Y%m%d"))


def iso_or_empty(value: date | None) -> str:
    return value.isoformat() if value else ""


def display_date(value: date | None) -> str:
    return value.strftime("%d.%m.%Y") if value else ""


def display_due_date(value: date | None) -> str:
    if not value:
        return ""
    weekdays = ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"]
    return f"{weekdays[value.weekday()]}, {value.strftime('%d.%m.')}"
