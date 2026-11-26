#!/usr/bin/env python3
"""Validate player displacement and third-person camera follow from renderer telemetry.

SwiftShader/TCG can deliver far fewer frames than a physical phone. The 0.25 m
floor is based on the 0.36 m measured in the 0.23.0 CI run; camera tracking and
visible-humanoid checks remain strict and independent.
"""
import math
import re
import sys

MIN_MOVEMENT_METERS = 0.25
CAMERA_TOLERANCE_METERS = 0.15
PATTERN = re.compile(
    r"PLAYER_STATE POS=(-?[0-9.]+),(-?[0-9.]+) "
    r"CAM=(-?[0-9.]+),(-?[0-9.]+) PARTS=(\d+) "
    r"FALLBACK=(\d+) GL=0x([0-9a-fA-F]+)"
)


def parse_telemetry(value):
    match = PATTERN.search(value)
    if not match:
        raise ValueError("Malformed renderer telemetry: " + value)
    x, z, camera_x, camera_z, parts, fallback, gl_error = match.groups()
    parts, fallback, gl_error = int(parts), int(fallback), int(gl_error, 16)
    if parts not in (11, 60) or fallback not in (0, 11):
        raise ValueError("Unexpected player/silhouette draw counts: " + value)
    if parts == 11 and fallback != 11:
        raise ValueError("Compatibility renderer did not submit all humanoid parts: " + value)
    if gl_error != 0:
        raise ValueError("Player batch reported OpenGL error: " + value)
    return tuple(map(float, (x, z, camera_x, camera_z))) + (parts, fallback)


def verify_motion(initial, moving):
    before = parse_telemetry(initial)
    after = parse_telemetry(moving)
    dx, dz = after[0] - before[0], after[1] - before[1]
    camera_dx, camera_dz = after[2] - before[2], after[3] - before[3]
    distance = math.hypot(dx, dz)
    camera_error = math.hypot(camera_dx - dx, camera_dz - dz)
    if distance < MIN_MOVEMENT_METERS:
        raise ValueError(
            "Joystick moved the player only %.2f m (minimum %.2f m for CPU-only TCG)"
            % (distance, MIN_MOVEMENT_METERS)
        )
    if camera_error > CAMERA_TOLERANCE_METERS:
        raise ValueError(
            "Third-person camera follow error %.2f m exceeds %.2f m"
            % (camera_error, CAMERA_TOLERANCE_METERS)
        )
    return {
        "distance": distance,
        "camera_error": camera_error,
        "parts": after[4],
        "fallback": after[5],
    }


def main(argv):
    if len(argv) != 3:
        raise SystemExit("usage: verify-player-motion.py INITIAL_TELEMETRY MOVING_TELEMETRY")
    result = verify_motion(argv[1], argv[2])
    print(
        "Verified %d player parts, %d compatibility parts, GL_NO_ERROR, "
        "%.2f m movement and %.2f m camera-follow error."
        % (result["parts"], result["fallback"], result["distance"], result["camera_error"])
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main(sys.argv))
    except ValueError as error:
        print(str(error), file=sys.stderr)
        raise SystemExit(1)
