package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class VillagerAnimationSystemTest {
    @Test public void legsSwingInOppositeDirections() {
        float left = VillagerAnimationSystem.legAngle((float) (Math.PI / 2.0), true, true);
        float right = VillagerAnimationSystem.legAngle((float) (Math.PI / 2.0), true, false);
        assertEquals(0.48f, left, 0.0001f);
        assertEquals(-left, right, 0.0001f);
    }

    @Test public void armsCounterSwingAgainstLegs() {
        float leg = VillagerAnimationSystem.legAngle(1.0f, true, true);
        float arm = VillagerAnimationSystem.armAngle(1.0f, true, true);
        assertEquals(-leg * 0.72f, arm, 0.0001f);
    }

    @Test public void idleMotionIsSubtleAndBodyBobRemainsSmall() {
        float leg = VillagerAnimationSystem.legAngle(1.3f, false, true);
        float bob = VillagerAnimationSystem.bodyBob(1.3f, false);
        assertTrue(Math.abs(leg) <= 0.0251f);
        assertTrue(Math.abs(bob) <= 0.0061f);
    }

    @Test public void limbCentreFollowsAnArcOfConstantLength() {
        float halfLength = 0.21f;
        for (int i = 0; i < 12; i++) {
            float angle = i * 0.17f - 0.8f;
            float y = VillagerAnimationSystem.limbCenterYOffset(halfLength, angle);
            float z = VillagerAnimationSystem.limbCenterZOffset(halfLength, angle);
            assertEquals(halfLength, (float) Math.hypot(y, z), 0.0001f);
        }
    }
}
