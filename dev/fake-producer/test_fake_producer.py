import json
import random
import unittest
from unittest.mock import MagicMock, patch
from uuid import UUID

from fake_producer import create_fake_events, post_event


class FakeProducerTests(unittest.TestCase):
    def test_creates_valid_envelopes_for_every_event_type(self) -> None:
        events = create_fake_events("cage-1", random.Random(3))

        self.assertEqual([event["type"] for event in events], ["BEHAVIOR", "AUDIO", "WEIGHT"])
        self.assertEqual(len({event["eventId"] for event in events}), 3)
        for event in events:
            UUID(event["eventId"])
            self.assertEqual(event["cageId"], "cage-1")
            self.assertTrue(event["timestamp"].endswith("Z"))
            self.assertEqual(event["schemaVersion"], 1)

        behavior = events[0]["payload"]
        self.assertLessEqual(0, behavior["stillSeconds"])
        self.assertLessEqual(behavior["stillSeconds"], behavior["windowSeconds"])
        self.assertIn(
            behavior["color"],
            {"RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE", "BLACK", "WHITE"},
        )
        self.assertTrue(0 <= behavior["avgGroupDistance"] <= 1)
        self.assertTrue(0 <= behavior["probAnomaly"] <= 1)
        self.assertTrue(0 <= behavior["detectionConfidence"] <= 1)

        audio = events[1]["payload"]
        self.assertIn(audio["label"], {"DISTRESS", "NORMAL"})
        self.assertTrue(0 <= audio["probability"] <= 1)
        self.assertGreater(audio["durationMs"], 0)

        weight = events[2]["payload"]
        self.assertGreaterEqual(weight["grams"], 0)
        self.assertIs(weight["stable"], True)

    def test_posts_to_the_versioned_ingestion_endpoint_with_api_key(self) -> None:
        response = MagicMock()
        response.__enter__.return_value.status = 202
        event = create_fake_events("cage-1", random.Random(1))[0]

        with patch("fake_producer.urlopen", return_value=response) as open_url:
            post_event("http://localhost:8080/", "local-key", event)

        request = open_url.call_args.args[0]
        self.assertEqual(request.full_url, "http://localhost:8080/api/v1/ingestion/events")
        self.assertEqual(request.get_method(), "POST")
        self.assertEqual(request.get_header("X-api-key"), "local-key")
        self.assertEqual(json.loads(request.data), event)

    def test_reports_non_accepted_success_status(self) -> None:
        response = MagicMock()
        response.__enter__.return_value.status = 200
        event = create_fake_events("cage-1", random.Random(2))[0]

        with patch("fake_producer.urlopen", return_value=response):
            with self.assertRaisesRegex(RuntimeError, "unexpected status 200"):
                post_event("http://localhost:8080", "local-key", event)


if __name__ == "__main__":
    unittest.main()
