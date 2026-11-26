package es.valledecernego.game;

/** Pure joystick math shared by the HUD and the renderer. */
public final class JoystickControl {
    private static final float DEAD_ZONE = 0.08f;

    private JoystickControl() { }

    public static Vector fromTouch(float touchX,float touchY,
                                   float centerX,float centerY,float range) {
        if (!isFinite(range) || range <= 0f
                || !isFinite(touchX) || !isFinite(touchY)
                || !isFinite(centerX) || !isFinite(centerY)) {
            return new Vector(0f,0f);
        }
        return new Vector(clamp((touchX-centerX)/range),
                          clamp((touchY-centerY)/range));
    }

    public static boolean isMoving(float x,float y) {
        return isFinite(x) && isFinite(y) && Math.hypot(x,y) > DEAD_ZONE;
    }

    private static float clamp(float value) {
        return Math.max(-1f,Math.min(1f,value));
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    public static final class Vector {
        public final float x;
        public final float y;

        private Vector(float x,float y) {
            this.x=x;
            this.y=y;
        }
    }
}
