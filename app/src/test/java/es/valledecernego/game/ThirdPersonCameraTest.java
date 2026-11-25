package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThirdPersonCameraTest {
    @Test public void cameraStaysBehindThePlayerAtZeroYaw() {
        ThirdPersonCamera.Pose pose = ThirdPersonCamera.follow(0f, 5f, 0f, 0f, 0f);
        assertEquals(0f, pose.x(), 0.0001f);
        assertTrue("camera must remain behind a player facing +Z", pose.z() < 5f);
    }

    @Test public void cameraOrbitsToTheCorrectSideWhenYawChanges() {
        ThirdPersonCamera.Pose pose = ThirdPersonCamera.follow(2f, 3f, 0f,
            (float) (Math.PI / 2.0), 0f);
        assertTrue(pose.x() > 2f);
        assertEquals(3f, pose.z(), 0.0001f);
    }

    @Test public void cameraKeepsAStableCloserThirdPersonDistance() {
        float[] yaws = {0f, .8f, 1.5707963f, -2.2f};
        for (float yaw : yaws) {
            ThirdPersonCamera.Pose pose = ThirdPersonCamera.follow(3f, -4f, .7f, yaw, .12f);
            double dx = pose.x() - 3f;
            double dy = pose.y() - pose.targetY();
            double dz = pose.z() + 4f;
            assertEquals(4.2, Math.sqrt(dx*dx + dy*dy + dz*dz), .001);
        }
    }

    @Test public void cameraFollowsPlayerTranslationAndJumpHeight() {
        ThirdPersonCamera.Pose first = ThirdPersonCamera.follow(1f, 2f, 0f, .3f, .1f);
        ThirdPersonCamera.Pose moved = ThirdPersonCamera.follow(4f, -3f, 1.2f, .3f, .1f);
        assertEquals(3f, moved.x() - first.x(), 0.0001f);
        assertEquals(-5f, moved.z() - first.z(), 0.0001f);
        assertEquals(1.2f, moved.y() - first.y(), 0.0001f);
        assertEquals(1.2f, moved.targetY() - first.targetY(), 0.0001f);
    }
}
