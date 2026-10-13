from __future__ import annotations

import unittest
import importlib.util
import json
import os
import sys
from datetime import date
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch


REPO_ROOT = Path(__file__).resolve().parents[1]
_client_path = REPO_ROOT / "developer/server-interface" / "api" / "untis_client.py"
_client_spec = importlib.util.spec_from_file_location("untis_test_client", _client_path)
if not _client_spec or not _client_spec.loader:
    raise ImportError("could not load Untis client module")
client = importlib.util.module_from_spec(_client_spec)
sys.modules[_client_spec.name] = client
_client_spec.loader.exec_module(client)


SYNTHETIC_CHILDREN = {"child1": {"display_name": "Sample Child 1", "person_id": 1000001}, "child2": {"display_name": "Sample Child 2", "person_id": 1000002}}


class UntisClientTests(unittest.TestCase):


    def test_homeworks_are_split_by_child_and_sorted_by_due_date(self) -> None:
        payload = client.normalize_homeworks(
            {
            "records": [
                {"homeworkId": 20, "teacherId": 2, "elementIds": [1000002]},
                {"homeworkId": 10, "teacherId": 1, "elementIds": [1000001]},
                {"homeworkId": 11, "teacherId": 1, "elementIds": [1000001]},
                {"homeworkId": 999, "teacherId": 1, "elementIds": [1000001]},
            ],
                "homeworks": [
                    {"id": 10, "lessonId": 101, "date": 20260510, "dueDate": 20260520, "text": "later", "completed": False, "attachments": []},
                    {"id": 11, "lessonId": 102, "date": 20260509, "dueDate": 20260518, "text": "earlier", "completed": False, "attachments": []},
                    {"id": 20, "lessonId": 201, "date": 20260509, "dueDate": 20260517, "text": "child2", "completed": False, "attachments": []},
                ],
                "teachers": [{"id": 1, "name": "TEA"}, {"id": 2, "name": "SAM"}],
                "lessons": [{"id": 101, "subject": "M"}, {"id": 102, "subject": "D"}, {"id": 201, "subject": "E"}],
            },
            start=date(2026, 5, 1),
            end=date(2026, 5, 31),
            children=SYNTHETIC_CHILDREN,
            subjects={"M": {"longName": "Mathematics"}},
            today=date(2026, 5, 16),
        )

        child1 = payload["children"]["child1"]["homeworks"]
        child2 = payload["children"]["child2"]["homeworks"]

        self.assertEqual([row["id"] for row in child1], [11, 10])
        self.assertEqual([row["id"] for row in child2], [20])
        self.assertEqual(child1[0]["dueDateLabel"], "Mo, 18.05.")
        self.assertEqual(child1[1]["subjectLongName"], "Mathematics")

    def test_past_due_is_marked_only_when_due_date_has_passed(self) -> None:
        payload = client.normalize_homeworks(
            {
                "records": [
                    {"homeworkId": 1, "teacherId": 1, "elementIds": [1000001]},
                    {"homeworkId": 2, "teacherId": 1, "elementIds": [1000001]},
                ],
                "homeworks": [
                    {"id": 1, "lessonId": 1, "date": 20260501, "dueDate": 20260515, "text": "past", "completed": False, "attachments": []},
                    {"id": 2, "lessonId": 1, "date": 20260501, "dueDate": 20260516, "text": "today", "completed": False, "attachments": []},
                ],
                "teachers": [{"id": 1, "name": "TEA"}],
                "lessons": [{"id": 1, "subject": "M"}],
            },
            start=date(2026, 5, 1),
            end=date(2026, 5, 31),
            children=SYNTHETIC_CHILDREN,
            today=date(2026, 5, 16),
        )

        rows = payload["children"]["child1"]["homeworks"]

        self.assertTrue(rows[0]["isPastDue"])
        self.assertFalse(rows[1]["isPastDue"])

    def test_tracking_state_marks_completed_and_archived(self) -> None:
        payload = client.normalize_homeworks(
            {
                "records": [{"homeworkId": 1, "teacherId": 1, "elementIds": [1000001]}],
                "homeworks": [{"id": 1, "lessonId": 1, "date": 20260501, "dueDate": 20260520, "text": "future", "completed": False, "attachments": []}],
                "teachers": [{"id": 1, "name": "TEA"}],
                "lessons": [{"id": 1, "subject": "M"}],
            },
            start=date(2026, 5, 1),
            end=date(2026, 5, 31),
            children=SYNTHETIC_CHILDREN,
            today=date(2026, 5, 16),
        )

        tracked = client.apply_tracking_state(
            payload,
            {"completed": ["child1:1"], "completedAt": {"child1:1": "2026-05-16"}},
            today=date(2026, 5, 16),
        )
        row = tracked["children"]["child1"]["homeworks"][0]

        self.assertEqual(row["status"], "completed")
        self.assertTrue(row["completed"])
        self.assertEqual(row["completedAt"], "2026-05-16")
        self.assertTrue(row["isArchived"])

    def test_save_homework_status_records_and_removes_completion_date(self) -> None:
        with TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / "homework_state.json"
            completed = client.save_homework_status(path, "child1", 1, "completed")
            self.assertIn("child1:1", completed["completed"])
            self.assertRegex(completed["completedAt"]["child1:1"], r"^\d{4}-\d{2}-\d{2}$")
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)

            reopened = client.save_homework_status(path, "child1", 1, "open")
            self.assertNotIn("child1:1", reopened["completed"])
            self.assertNotIn("child1:1", reopened["completedAt"])

            with self.assertRaises(client.UntisError):
                client.save_homework_status(path, "child1", 1, "bad")

    def test_tracking_state_loader_tolerates_missing_corrupt_and_legacy_shapes(self) -> None:
        self.assertEqual(client.load_tracking_state(None)["completed"], [])
        with TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / "homework_state.json"
            path.write_text("{bad", encoding="utf-8")
            self.assertEqual(client.load_tracking_state(path)["completed"], [])

            path.write_text("[]", encoding="utf-8")
            self.assertEqual(client.load_tracking_state(path)["completed"], [])

            path.write_text(json.dumps({"completed": [1, "child1:2"], "completedAt": "bad"}), encoding="utf-8")
            self.assertEqual(client.load_tracking_state(path), {"schema_version": "untis_homework_state_v1", "completed": ["1", "child1:2"], "completedAt": {}})

    def test_low_level_formatters_handle_empty_values(self) -> None:
        self.assertIsNone(client.parse_untis_date(None))
        self.assertEqual(client.format_untis_time(None), "")
        self.assertEqual(client.format_untis_time(855), "08:55")
        self.assertEqual(client.yyyymmdd(date(2026, 5, 18)), 20260518)
        self.assertEqual(client.iso_or_empty(None), "")
        self.assertEqual(client.display_date(None), "")
        self.assertEqual(client.display_date(date(2026, 5, 18)), "18.05.2026")
        self.assertEqual(client.display_due_date(None), "")

    def test_past_due_uncompleted_homework_is_not_done_archive(self) -> None:
        payload = client.normalize_homeworks(
            {
                "records": [{"homeworkId": 1, "teacherId": 1, "elementIds": [1000001]}],
                "homeworks": [{"id": 1, "lessonId": 1, "date": 20260501, "dueDate": 20260515, "text": "past", "completed": False, "attachments": []}],
                "teachers": [{"id": 1, "name": "TEA"}],
                "lessons": [{"id": 1, "subject": "M"}],
            },
            start=date(2026, 5, 1),
            end=date(2026, 5, 31),
            children=SYNTHETIC_CHILDREN,
            today=date(2026, 5, 16),
        )

        tracked = client.apply_tracking_state(payload, {"completed": []}, today=date(2026, 5, 16))
        row = tracked["children"]["child1"]["homeworks"][0]

        self.assertEqual(row["status"], "not_done")
        self.assertFalse(row["completed"])
        self.assertTrue(row["isArchived"])

    def test_office_hours_are_normalized_and_hide_private_fields_when_configured(self) -> None:
        payload = client.normalize_office_hours(
            {"showEmail": False, "showPhone": True, "noAppointmentMsg": "By appointment"},
            [
                {
                    "teacher": "ABC",
                    "teacherFullName": "Teacher Name",
                    "teacherId": 12,
                    "date": 20260520,
                    "day": "Mittwoch",
                    "startTime": "10:00",
                    "endTime": "10:15",
                    "email": "review6@example.invalid",
                    "phone": "123",
                    "available": True,
                }
            ],
        )

        row = payload["hours"][0]
        self.assertIsNone(row["email"])
        self.assertEqual(row["phone"], "123")
        self.assertEqual(row["date"], "2026-05-20")

    def test_cancelled_period_uses_element_names(self) -> None:
        row = client.normalize_cancelled_period(
            {
                "id": 1,
                "date": 20260520,
                "startTime": 855,
                "endTime": 945,
                "cellState": "CANCEL",
                "substText": "ENTFAELLT",
                "elements": [
                    {"type": client.ELEMENT_TYPE_SUBJECT, "id": 4},
                    {"type": client.ELEMENT_TYPE_TEACHER, "id": 5},
                    {"type": client.ELEMENT_TYPE_ROOM, "id": 6},
                ],
            },
            {
                (client.ELEMENT_TYPE_SUBJECT, 4): {"name": "M"},
                (client.ELEMENT_TYPE_TEACHER, 5): {"name": "TEA"},
                (client.ELEMENT_TYPE_ROOM, 6): {"displayname": "101"},
            },
        )

        self.assertEqual(row["date"], "2026-05-20")
        self.assertEqual(row["startTime"], "08:55")
        self.assertEqual(row["subject"], "M")
        self.assertEqual(row["text"], "ENTFAELLT")


if __name__ == "__main__":
    unittest.main()
