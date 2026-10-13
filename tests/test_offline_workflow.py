import json
import tempfile
import unittest
from datetime import date
from pathlib import Path
from unittest.mock import patch
from test_untis_client import client
from test_settings import storage

ROOT = Path(__file__).resolve().parents[1]


class OfflineWorkflowTests(unittest.TestCase):
    def test_synthetic_completion_reopen_and_notes_use_only_temporary_state(self):
        with tempfile.TemporaryDirectory() as directory, patch('socket.socket', side_effect=AssertionError('No networking')):
            payload = json.loads((ROOT / 'fixtures/homework.json').read_text())
            state_path = Path(directory) / 'completion.json'
            client.save_homework_status(state_path, 'child1', 1, 'completed')
            updated = client.apply_tracking_state(payload, client.load_tracking_state(state_path), today=date(2026, 10, 13))
            self.assertEqual(updated['children']['child1']['homeworks'][0]['status'], 'completed')
            self.assertEqual(updated['children']['child2']['homeworks'][0]['status'], 'open')
            client.save_homework_status(state_path, 'child1', 1, 'open')
            updated = client.apply_tracking_state(payload, client.load_tracking_state(state_path), today=date(2026, 10, 13))
            self.assertEqual(updated['children']['child1']['homeworks'][0]['status'], 'open')
            storage.configure_paths(Path(directory))
            storage.save_note('child1', {'title': 'Synthetic reminder', 'text': 'Pack a sketchbook'})
            self.assertEqual(storage.load_notes()['children']['child1'][0]['title'], 'Synthetic reminder')

    def test_native_and_browser_fixtures_match_the_canonical_synthetic_input(self):
        expected = json.loads((ROOT / 'fixtures/homework.json').read_text())
        native = json.loads((ROOT / 'app/mobile/android/app/src/main/assets/homework.json').read_text())
        browser = (ROOT / 'docs/demo-data.js').read_text().split('const DEMO_HOMEWORK = ', 1)[1].rstrip().removesuffix(';')
        self.assertEqual(native, expected)
        self.assertEqual(json.loads(browser), expected)
