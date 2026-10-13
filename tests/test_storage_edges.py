from __future__ import annotations

import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from test_settings import storage


class StorageEdgeTests(unittest.TestCase):

    def test_corrupt_storage_files_fall_back_to_empty_payloads(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            storage.configure_paths(Path(tmpdir))
            storage.DOMAIN_ICONS_PATH.write_text("[]", encoding="utf-8")
            storage.NOTES_PATH.write_text("{bad", encoding="utf-8")
            storage.APPEARANCE_PATH.write_text("[]", encoding="utf-8")

            self.assertEqual(storage.load_domain_icons(), {"icons": {}, "children": {}})
            self.assertEqual(storage.load_notes(), {"schema_version": "untis_personal_notes_v1", "children": {}})
            self.assertEqual(
                storage.load_appearance(),
                {"schema_version": "untis_appearance_v1", "children": {}},
            )
            self.assertEqual(
                storage.load_domain_styles(),
                {"schema_version": "untis_domain_styles_v1", "children": {}},
            )


    def test_domain_icon_validation_rejects_invalid_payloads_and_filters_legacy_data(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            storage.configure_paths(Path(tmpdir))
            storage.DOMAIN_ICONS_PATH.write_text(
                json.dumps(
                    {
                        "icons": {"M": "data:image/png;base64,abc", "D": "https://example.test/icon.png"},
                        "children": {"child1": {"E": "data:image/jpeg;base64,abc", "F": "bad"}},
                    }
                ),
                encoding="utf-8",
            )

            self.assertEqual(
                storage.load_domain_icons(),
                {
                    "icons": {"M": "data:image/png;base64,abc"},
                    "children": {"child1": {"E": "data:image/jpeg;base64,abc"}},
                },
            )
            with self.assertRaises(ValueError):
                storage.save_domain_icon("", "M", "data:image/png;base64,abc")
            with self.assertRaises(ValueError):
                storage.save_domain_icon("child1", "M", "not-an-image")

            storage.DOMAIN_ICONS_PATH.write_text(
                json.dumps({"children": {"child1": []}}),
                encoding="utf-8",
            )
            repaired = storage.save_domain_icon("child1", "M", "data:image/png;base64,abc")
            self.assertEqual(repaired["children"]["child1"]["M"], "data:image/png;base64,abc")

    def test_appearance_and_domain_styles_are_sanitized(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            storage.configure_paths(Path(tmpdir))
            storage.APPEARANCE_PATH.write_text(json.dumps({"children": []}), encoding="utf-8")
            appearance = storage.save_child_appearance(
                "child1",
                {
                    "theme": "forest",
                    "font": "rounded",
                    "bgColor": "not-a-color",
                    "bgImage": "https://example.test/image.png",
                },
            )
            styles = storage.save_domain_styles(
                {
                    "child1": {
                        "M": {"color": "#ffcc00"},
                        "D": {"color": "red"},
                        "E": "not-a-style",
                    },
                    "child2": "not-a-dict",
                }
            )

            self.assertEqual(appearance["children"]["child1"]["bgColor"], "#edf2f5")
            self.assertEqual(appearance["children"]["child1"]["bgImage"], "")
            self.assertEqual(styles["children"], {"child1": {"M": {"color": "#ffcc00"}}})

            with self.assertRaises(ValueError):
                storage.save_child_appearance("", {})

    def test_note_normalization_update_and_validation_edges(self) -> None:
        with tempfile.TemporaryDirectory() as tmpdir:
            storage.configure_paths(Path(tmpdir))
            saved = storage.save_note(
                "child1",
                {
                    "id": "note-1",
                    "title": "  Pack bag  ",
                    "text": "  bring books  ",
                    "category": "school",
                    "date": "bad-date",
                    "color": "blue",
                    "icon": "",
                },
            )
            note = saved["children"]["child1"][0]

            self.assertEqual(note["title"], "Pack bag")
            self.assertEqual(note["text"], "bring books")
            self.assertRegex(note["date"], r"^\d{4}-\d{2}-\d{2}$")
            self.assertEqual(note["color"], "#fff7cc")
            self.assertEqual(note["icon"], "*")

            updated = storage.save_note("child1", {"id": "note-1", "title": "Updated", "text": ""})
            self.assertEqual(len(updated["children"]["child1"]), 1)
            self.assertEqual(updated["children"]["child1"][0]["title"], "Updated")

            with self.assertRaises(ValueError):
                storage.save_note("child1", {"title": "", "text": ""})
            with self.assertRaises(ValueError):
                storage.save_note("", {"title": "No child"})

            storage.NOTES_PATH.write_text(json.dumps({"children": []}), encoding="utf-8")
            repaired_children = storage.save_note("child1", {"title": "Repaired children"})
            self.assertEqual(repaired_children["children"]["child1"][0]["title"], "Repaired children")

            storage.NOTES_PATH.write_text(json.dumps({"children": {"child1": "not-a-list"}}), encoding="utf-8")
            repaired_notes = storage.save_note("child1", {"title": "Repaired notes"})
            self.assertEqual(repaired_notes["children"]["child1"][0]["title"], "Repaired notes")


if __name__ == "__main__":
    unittest.main()
