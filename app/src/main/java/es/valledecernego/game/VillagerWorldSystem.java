package es.valledecernego.game;

/**
 * World-space movement for the three named residents.
 * Workers walk between their home positions and the farm, pasture or forest
 * work areas. Positions are independent from job progression and are saved.
 */
public final class VillagerWorldSystem {
    public static final float WALK_SPEED = 1.35f;
    private static final float ARRIVAL_DISTANCE = 0.04f;
    private static final float TWO_PI = (float) (Math.PI * 2.0d);
    private static final float MAX_SAVE_COORDINATE = 100.0f;

    private static final float[] HOME_X = {-0.9f, 0.0f, 0.9f};
    private static final float HOME_Z = -1.8f;

    private final State[] states = {
        new State("Lúa", HOME_X[0], HOME_Z),
        new State("Brais", HOME_X[1], HOME_Z),
        new State("Xela", HOME_X[2], HOME_Z)
    };

    /** Advances walking and turning using game time, never wall-clock time. */
    public synchronized void update(long deltaMs, VillagerSystem.Worker[] workers) {
        if (deltaMs <= 0L || workers == null) return;
        int count = Math.min(states.length, workers.length);
        double maxStep = WALK_SPEED * (deltaMs / 1000.0d);

        for (int i = 0; i < count; i++) {
            State state = states[i];
            VillagerSystem.Worker worker = workers[i];
            state.job = worker.job();

            float targetX = targetX(state.job, i);
            float targetZ = targetZ(state.job);
            float dx = targetX - state.x;
            float dz = targetZ - state.z;
            float distance = (float) Math.hypot(dx, dz);

            if (distance <= ARRIVAL_DISTANCE) {
                state.x = targetX;
                state.z = targetZ;
                state.walking = false;
            } else {
                state.facing = (float) Math.atan2(dx, dz);
                if (maxStep >= distance) {
                    state.x = targetX;
                    state.z = targetZ;
                    state.walking = false;
                } else {
                    float fraction = (float) (maxStep / distance);
                    state.x += dx * fraction;
                    state.z += dz * fraction;
                    state.walking = true;
                }
            }

            // A slower phase keeps idle residents subtly animated at their posts.
            double phaseRate = state.walking ? 0.008d : 0.00018d;
            double phaseDelta = Math.min(deltaMs, 100_000L) * phaseRate;
            state.walkPhase = (float) ((state.walkPhase + phaseDelta) % TWO_PI);
        }
    }

    static float targetX(int job, int workerIndex) {
        switch (job) {
            case VillagerSystem.FARMER: return -1.6f;
            case VillagerSystem.RANCHER: return 5.1f;
            case VillagerSystem.WOODCUTTER: return -8.2f;
            default: return HOME_X[workerIndex];
        }
    }

    static float targetZ(int job) {
        switch (job) {
            case VillagerSystem.FARMER: return 2.8f;
            case VillagerSystem.RANCHER: return -3.2f;
            case VillagerSystem.WOODCUTTER: return -3.4f;
            default: return HOME_Z;
        }
    }

    public synchronized Position[] snapshot() {
        Position[] copy = new Position[states.length];
        for (int i = 0; i < states.length; i++) {
            State state = states[i];
            copy[i] = new Position(state.name, state.job, state.x, state.z,
                state.facing, state.walking, state.walkPhase);
        }
        return copy;
    }

    public synchronized String saveState() {
        StringBuilder out = new StringBuilder("1|");
        for (int i = 0; i < states.length; i++) {
            if (i > 0) out.append(';');
            out.append(Float.toString(states[i].x)).append(',')
                .append(Float.toString(states[i].z));
        }
        return out.toString();
    }

    /** Loads all positions atomically; malformed or out-of-world data is ignored. */
    public synchronized void loadState(String saved) {
        if (saved == null || saved.trim().isEmpty()) return;
        String[] parts = saved.split("\\|", -1);
        if (parts.length != 2 || !"1".equals(parts[0])) return;
        String[] savedPositions = parts[1].split(";", -1);
        if (savedPositions.length != states.length) return;

        float[] xs = new float[states.length];
        float[] zs = new float[states.length];
        try {
            for (int i = 0; i < states.length; i++) {
                String[] coordinates = savedPositions[i].split(",", -1);
                if (coordinates.length != 2) return;
                xs[i] = Float.parseFloat(coordinates[0]);
                zs[i] = Float.parseFloat(coordinates[1]);
                if (!validCoordinate(xs[i]) || !validCoordinate(zs[i])) return;
            }
        } catch (NumberFormatException ignored) {
            return;
        }

        for (int i = 0; i < states.length; i++) {
            states[i].x = xs[i];
            states[i].z = zs[i];
            states[i].walking = false;
            states[i].walkPhase = 0.0f;
        }
    }

    private static boolean validCoordinate(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value)
            && Math.abs(value) <= MAX_SAVE_COORDINATE;
    }

    private static final class State {
        final String name;
        int job = VillagerSystem.IDLE;
        float x;
        float z;
        float facing;
        boolean walking;
        float walkPhase;

        State(String name, float x, float z) {
            this.name = name;
            this.x = x;
            this.z = z;
        }
    }

    public static final class Position {
        private final String name;
        private final int job;
        private final float x;
        private final float z;
        private final float facing;
        private final boolean walking;
        private final float walkPhase;

        private Position(String name, int job, float x, float z, float facing,
                         boolean walking, float walkPhase) {
            this.name = name;
            this.job = job;
            this.x = x;
            this.z = z;
            this.facing = facing;
            this.walking = walking;
            this.walkPhase = walkPhase;
        }

        public String name() { return name; }
        public int job() { return job; }
        public float x() { return x; }
        public float z() { return z; }
        public float facing() { return facing; }
        public boolean walking() { return walking; }
        public float walkPhase() { return walkPhase; }
    }
}
