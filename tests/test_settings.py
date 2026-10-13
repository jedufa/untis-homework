from __future__ import annotations

import json
import os
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


REPO_ROOT = Path(__file__).resolve().parents[1]
_storage_path = REPO_ROOT / "developer/server-interface" / "api" / "storage.py"
_storage_spec = importlib.util.spec_from_file_location("untis_test_storage", _storage_path)
if not _storage_spec or not _storage_spec.loader:
    raise ImportError("could not load Untis storage module")
storage = importlib.util.module_from_spec(_storage_spec)
sys.modules[_storage_spec.name] = storage
_storage_spec.loader.exec_module(storage)


class SettingsTests(unittest.TestCase):


    def test_save_domain_icon_persists_image_data(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            storage.configure_paths(Path(tmpdir))
            icon_path = storage.DOMAIN_ICONS_PATH
            icons = storage.save_domain_icon("child1", "M", "data:image/png;base64,abc")

            self.assertEqual(icons["children"]["child1"]["M"], "data:image/png;base64,abc")
            self.assertEqual(icon_path.stat().st_mode & 0o777, 0o600)
            persisted = json.loads(icon_path.read_text(encoding="utf-8"))
            self.assertEqual(persisted["children"]["child1"]["M"], "data:image/png;base64,abc")

    def test_save_domain_icon_can_remove_custom_image(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            storage.configure_paths(Path(tmpdir))
            storage.save_domain_icon("child1", "M", "data:image/png;base64,abc")
            icons = storage.save_domain_icon("child1", "M", "")

            self.assertNotIn("M", icons["children"]["child1"])

    def test_notes_can_be_saved_and_deleted_per_child(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            storage.configure_paths(Path(tmpdir))
            notes_path = storage.NOTES_PATH
            saved = storage.save_note(
                "child1",
                {
                    "title": "Violin idea",
                    "text": "Practice before dinner",
                    "category": "remember",
                    "date": "2026-05-16",
                    "color": "#ffeeaa",
                    "icon": "♪",
                },
            )
            note = saved["children"]["child1"][0]
            self.assertEqual(note["title"], "Violin idea")
            self.assertEqual(note["icon"], "♪")
            self.assertEqual(notes_path.stat().st_mode & 0o777, 0o600)

            deleted = storage.delete_note("child1", note["id"])

            self.assertEqual(deleted["children"]["child1"], [])


if __name__ == "__main__":
    unittest.main()
