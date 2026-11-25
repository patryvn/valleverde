package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class VillagerWorldSystemTest {
    private float distance(VillagerWorldSystem.Position position, float x, float z) {
        return (float) Math.hypot(position.x() - x, position.z() - z);
    }

    @Test public void assignedWorkersWalkTowardTheirWorkStations() {
        VillagerSystem jobs = new VillagerSystem();
        VillagerWorldSystem world = new VillagerWorldSystem();
        assertTrue(jobs.assignJob(0, VillagerSystem.FARMER));

        VillagerWorldSystem.Position before = world.snapshot()[0];
        float initialDistance = distance(before,
            VillagerWorldSystem.targetX(VillagerSystem.FARMER, 0),
            VillagerWorldSystem.targetZ(VillagerSystem.FARMER));

        world.update(1_000L, jobs.snapshot());
        VillagerWorldSystem.Position after = world.snapshot()[0];

        assertTrue(after.walking());
        assertTrue(distance(after,
            VillagerWorldSystem.targetX(VillagerSystem.FARMER, 0),
            VillagerWorldSystem.targetZ(VillagerSystem.FARMER)) < initialDistance);
    }

    @Test public void changingJobRedirectsWorkerAndIdleReturnsThemHome() {
        VillagerSystem jobs = new VillagerSystem();
        VillagerWorldSystem world = new VillagerWorldSystem();
        jobs.assignJob(1, VillagerSystem.RANCHER);
        world.update(2_000L, jobs.snapshot());

        VillagerWorldSystem.Position beforeChange = world.snapshot()[1];
        float beforeDistance = distance(beforeChange,
            VillagerWorldSystem.targetX(VillagerSystem.WOODCUTTER, 1),
            VillagerWorldSystem.targetZ(VillagerSystem.WOODCUTTER));

        jobs.assignJob(1, VillagerSystem.WOODCUTTER);
        world.update(1_000L, jobs.snapshot());
        VillagerWorldSystem.Position afterChange = world.snapshot()[1];
        assertTrue(distance(afterChange,
            VillagerWorldSystem.targetX(VillagerSystem.WOODCUTTER, 1),
            VillagerWorldSystem.targetZ(VillagerSystem.WOODCUTTER)) < beforeDistance);

        jobs.assignJob(1, VillagerSystem.IDLE);
        world.update(20_000L, jobs.snapshot());
        VillagerWorldSystem.Position home = world.snapshot()[1];
        assertEquals(0.0f, home.x(), 0.001f);
        assertEquals(-1.8f, home.z(), 0.001f);
        assertFalse(home.walking());
    }

    @Test public void worldPositionsSurviveSaveAndRestore() {
        VillagerSystem jobs = new VillagerSystem();
        VillagerWorldSystem world = new VillagerWorldSystem();
        jobs.assignJob(2, VillagerSystem.WOODCUTTER);
        world.update(1_250L, jobs.snapshot());

        String saved = world.saveState();
        VillagerWorldSystem restored = new VillagerWorldSystem();
        restored.loadState(saved);

        VillagerWorldSystem.Position[] expected = world.snapshot();
        VillagerWorldSystem.Position[] actual = restored.snapshot();
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].x(), actual[i].x(), 0.0001f);
            assertEquals(expected[i].z(), actual[i].z(), 0.0001f);
        }

        String beforeInvalidLoad = restored.saveState();
        restored.loadState("1|999,0;0,0;0,0");
        assertEquals(beforeInvalidLoad, restored.saveState());
    }

    @Test public void idleResidentsKeepASeparateSlowAnimationPhase() {
        VillagerSystem jobs = new VillagerSystem();
        VillagerWorldSystem world = new VillagerWorldSystem();

        world.update(1_000L, jobs.snapshot());
        VillagerWorldSystem.Position idle = world.snapshot()[0];

        assertFalse(idle.walking());
        assertEquals(0.18f, idle.walkPhase(), 0.0001f);
    }

    @Test public void idleResidentsLeaveHomeToStrollAroundTheVillage() {
        VillagerSystem jobs = new VillagerSystem();
        VillagerWorldSystem world = new VillagerWorldSystem();
        world.update(31_000L, jobs.snapshot());
        VillagerWorldSystem.Position[] residents = world.snapshot();
        assertTrue(Math.abs(residents[0].x() - (-0.9f)) > 0.1f);
        assertTrue(Math.abs(residents[1].z() - (-1.8f)) > 0.1f);
    }

    @Test public void nonPositiveDeltaDoesNotMoveWorkers() {
        VillagerSystem jobs = new VillagerSystem();
        VillagerWorldSystem world = new VillagerWorldSystem();
        jobs.assignJob(0, VillagerSystem.FARMER);
        String before = world.saveState();
        world.update(0L, jobs.snapshot());
        world.update(-100L, jobs.snapshot());
        assertEquals(before, world.saveState());
    }
}
