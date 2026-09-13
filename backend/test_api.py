import sys
import os
import unittest
from fastapi.testclient import TestClient

# Add current dir to sys.path
sys.path.insert(0, os.path.dirname(__file__))

from app.main import app

class TestSmtuApi(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_root(self):
        resp = self.client.get("/")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["status"], "online")
        print(" Root endpoint OK")

    def test_faculties(self):
        print("Fetching faculties...")
        resp = self.client.get("/api/faculties")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertIsInstance(data, list)
        self.assertGreater(len(data), 0)
        print(f" Faculties endpoint OK: received {len(data)} faculties")

    def test_groups(self):
        print("Fetching groups with filter...")
        resp = self.client.get("/api/groups?search=10274")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertIsInstance(data, list)
        self.assertTrue(any(g["name"] == "10274" for g in data))
        print(f" Groups endpoint OK: found group 10274 with ID {data[0]['id']}")

    def test_group_schedule(self):
        print("Fetching schedule for group 7738 (10274)...")
        resp = self.client.get("/api/schedule/group/7738")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertEqual(data["id"], "7738")
        self.assertFalse(data["is_teacher"])
        self.assertGreater(len(data["days"]), 0)
        first_day = data["days"][0]
        print(f" Group schedule OK: {data['title']}, {len(data['days'])} days, Day 1 ({first_day['day_name']}): {len(first_day['lessons'])} lessons")

    def test_teacher_search(self):
        print("Searching teacher 'Альбаев'...")
        resp = self.client.get("/api/teachers/search?q=Альбаев")
        self.assertEqual(resp.status_code, 200)
        data = resp.json()
        self.assertIsInstance(data, list)
        self.assertGreater(len(data), 0)
        teacher = data[0]
        print(f" Teacher search OK: found {teacher['name']} (ID: {teacher['id']})")
        
        # Test teacher schedule
        print(f"Fetching teacher schedule for ID {teacher['id']}...")
        resp_t = self.client.get(f"/api/schedule/teacher/{teacher['id']}")
        self.assertEqual(resp_t.status_code, 200)
        t_data = resp_t.json()
        self.assertEqual(t_data["id"], teacher["id"])
        self.assertTrue(t_data["is_teacher"])
        self.assertGreater(len(t_data["days"]), 0)
        print(f" Teacher schedule OK: {t_data['title']}, {len(t_data['days'])} days")

if __name__ == "__main__":
    unittest.main()
