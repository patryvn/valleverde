package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerModelTransformTest {
    @Test public void everyPartStaysFiniteAndNearThePlayerAtDifferentFacings() {
        float[] yaws = {0f, .7f, 1.5707963f, 3.1415926f, -1.2f};
        for (float yaw : yaws) {
            for (PlayerModelSpec.Part part : PlayerModelSpec.parts()) {
                PlayerModelTransform.Pose pose = PlayerModelTransform.partPose(
                    12f, .4f, -8f, yaw, .02f, part, .35f);
                assertTrue(Float.isFinite(pose.x));
                assertTrue(Float.isFinite(pose.y));
                assertTrue(Float.isFinite(pose.z));
                assertTrue(Float.isFinite(pose.pitch));
                assertTrue(Math.hypot(pose.x - 12f, pose.z + 8f) < 1.0);
                assertTrue(pose.y > .3f && pose.y < 2.6f);
            }
        }
    }

    @Test public void animatedLimbUpperPivotRemainsAnchored() {
        PlayerModelSpec.Part[] limbs = {
            PlayerModelSpec.find("left-arm"),
            PlayerModelSpec.find("right-arm"),
            PlayerModelSpec.find("left-leg"),
            PlayerModelSpec.find("right-leg")
        };
        float[] yaws = {0f, .8f, 1.5707963f, -2.2f};
        for (PlayerModelSpec.Part limb : limbs) {
            for (float yaw : yaws) {
                for (int step = 0; step < 24; step++) {
                    float angle = step * .11f - 1.1f;
                    PlayerModelTransform.Pose pose = PlayerModelTransform.partPose(
                        3f, .2f, 7f, yaw, .015f, limb, angle);
                    float dx = pose.x - 3f;
                    float dz = pose.z - 7f;
                    float cosine = (float) Math.cos(yaw);
                    float sine = (float) Math.sin(yaw);
                    float localX = dx * cosine - dz * sine;
                    float localZ = dx * sine + dz * cosine;
                    float half = limb.sy * .5f;

                    assertEquals(limb.x, localX, .0001f);
                    assertEquals(limb.y - half * (float) Math.cos(angle),
                        pose.y - .2f - (limb.followsBody ? .015f : 0f), .0001f);
                    assertEquals(limb.z - half * (float) Math.sin(angle), localZ, .0001f);
                    assertEquals(limb.y,
                        pose.y - .2f - (limb.followsBody ? .015f : 0f)
                            + half * (float) Math.cos(angle), .0001f);
                    assertEquals(limb.z,
                        localZ + half * (float) Math.sin(angle), .0001f);
                }
            }
        }
    }

    @Test public void movingThePlayerTranslatesEveryPartWithoutChangingItsScale() {
        PlayerModelSpec.Part part = PlayerModelSpec.find("head");
        PlayerModelTransform.Pose first = PlayerModelTransform.partPose(
            0f, 0f, 0f, .4f, 0f, part, 0f);
        PlayerModelTransform.Pose moved = PlayerModelTransform.partPose(
            5f, 1.25f, -3f, .4f, 0f, part, 0f);
        assertEquals(5f, moved.x - first.x, .0001f);
        assertEquals(1.25f, moved.y - first.y, .0001f);
        assertEquals(-3f, moved.z - first.z, .0001f);
        assertEquals(first.sx, moved.sx, .0001f);
        assertEquals(first.sy, moved.sy, .0001f);
        assertEquals(first.sz, moved.sz, .0001f);
    }
}
