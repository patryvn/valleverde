import unittest

from verify_player_motion import verify_motion


def telemetry(x, camera_x=None, parts=60, fallback=11, gl="0000", z=5.0, camera_z=0.96):
    if camera_x is None:
        camera_x = x
    return (
        "PLAYER_STATE POS=%.2f,%.2f CAM=%.2f,%.2f PARTS=%d FALLBACK=%d GL=0x%s"
        % (x, z, camera_x, camera_z, parts, fallback, gl)
    )


class PlayerMotionVerificationTest(unittest.TestCase):
    def test_cpu_only_tcg_motion_and_camera_follow_are_accepted(self):
        result = verify_motion(telemetry(0.00), telemetry(0.36))
        self.assertAlmostEqual(0.36, result["distance"], places=2)
        self.assertAlmostEqual(0.00, result["camera_error"], places=2)
        self.assertEqual(60, result["parts"])
        self.assertEqual(11, result["fallback"])

    def test_stationary_player_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "only 0.00 m"):
            verify_motion(telemetry(0.00), telemetry(0.00))

    def test_camera_must_follow_the_same_translation(self):
        with self.assertRaisesRegex(ValueError, "camera follow error"):
            verify_motion(telemetry(0.00), telemetry(0.36, camera_x=0.10))

    def test_open_gl_error_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "OpenGL error"):
            verify_motion(telemetry(0.00), telemetry(0.36, gl="0502"))

    def test_incomplete_compatibility_silhouette_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "Unexpected player/silhouette"):
            verify_motion(telemetry(0.00), telemetry(0.36, parts=7, fallback=7))


if __name__ == "__main__":
    unittest.main()
