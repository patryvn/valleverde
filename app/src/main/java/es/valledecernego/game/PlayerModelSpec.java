package es.valledecernego.game;

/**
 * Declarative low-poly player model. Dimensions are in world units.
 * The renderer consumes this same list, so the tests validate real render data.
 */
public final class PlayerModelSpec {
    public enum Shape { BOX, SPHERE, LIMB }
    public enum Motion { NONE, LEFT_LEG, RIGHT_LEG, LEFT_ARM, RIGHT_ARM }

    public static final class Part {
        public final String id;
        public final Shape shape;
        public final Motion motion;
        public final float x, y, z, sx, sy, sz;
        public final float r, g, b, alpha;
        public final boolean followsBody;

        private Part(String id, Shape shape, Motion motion,
                     float x, float y, float z, float sx, float sy, float sz,
                     float r, float g, float b, float alpha, boolean followsBody) {
            this.id=id; this.shape=shape; this.motion=motion;
            this.x=x; this.y=y; this.z=z;
            this.sx=sx; this.sy=sy; this.sz=sz;
            this.r=r; this.g=g; this.b=b; this.alpha=alpha;
            this.followsBody=followsBody;
        }
    }

    private static Part part(String id, Shape shape, Motion motion,
                             float x, float y, float z, float sx, float sy, float sz,
                             float r, float g, float b, boolean followsBody) {
        return new Part(id,shape,motion,x,y,z,sx,sy,sz,r,g,b,1f,followsBody);
    }

    private static final Part[] PARTS = {
        part("left-boot",Shape.BOX,Motion.NONE,-.13f,.14f,.09f,.23f,.16f,.35f,.12f,.085f,.055f,false),
        part("right-boot",Shape.BOX,Motion.NONE,.13f,.14f,.09f,.23f,.16f,.35f,.12f,.085f,.055f,false),
        part("left-leg",Shape.LIMB,Motion.LEFT_LEG,-.13f,.61f,0f,.17f,.50f,.18f,.14f,.17f,.15f,false),
        part("right-leg",Shape.LIMB,Motion.RIGHT_LEG,.13f,.61f,0f,.17f,.50f,.18f,.14f,.17f,.15f,false),
        part("torso",Shape.BOX,Motion.NONE,0f,.99f,0f,.52f,.64f,.32f,.15f,.36f,.23f,true),
        part("jacket-hem",Shape.BOX,Motion.NONE,0f,.72f,.012f,.53f,.12f,.33f,.11f,.27f,.17f,true),
        part("collar",Shape.BOX,Motion.NONE,0f,1.31f,.015f,.19f,.14f,.20f,.72f,.69f,.55f,true),
        part("zipper",Shape.BOX,Motion.NONE,0f,1.00f,.166f,.025f,.45f,.014f,.76f,.70f,.52f,true),
        part("left-pocket",Shape.BOX,Motion.NONE,-.15f,.98f,.17f,.12f,.13f,.025f,.20f,.42f,.27f,true),
        part("right-pocket",Shape.BOX,Motion.NONE,.15f,.98f,.17f,.12f,.13f,.025f,.20f,.42f,.27f,true),
        part("left-shoulder",Shape.SPHERE,Motion.NONE,-.29f,1.20f,0f,.16f,.16f,.16f,.15f,.36f,.23f,true),
        part("right-shoulder",Shape.SPHERE,Motion.NONE,.29f,1.20f,0f,.16f,.16f,.16f,.15f,.36f,.23f,true),
        part("left-arm",Shape.LIMB,Motion.LEFT_ARM,-.31f,1.20f,0f,.15f,.44f,.17f,.17f,.39f,.25f,true),
        part("right-arm",Shape.LIMB,Motion.RIGHT_ARM,.31f,1.20f,0f,.15f,.44f,.17f,.17f,.39f,.25f,true),
        part("left-hand",Shape.SPHERE,Motion.NONE,-.32f,.82f,.025f,.09f,.11f,.09f,.75f,.54f,.38f,true),
        part("right-hand",Shape.SPHERE,Motion.NONE,.32f,.82f,.025f,.09f,.11f,.09f,.75f,.54f,.38f,true),
        part("neck",Shape.BOX,Motion.NONE,0f,1.35f,.015f,.17f,.18f,.17f,.72f,.51f,.36f,true),
        part("head",Shape.SPHERE,Motion.NONE,0f,1.57f,.005f,.30f,.35f,.28f,.79f,.59f,.43f,true),
        part("left-ear",Shape.SPHERE,Motion.NONE,-.30f,1.57f,.005f,.065f,.095f,.075f,.72f,.51f,.36f,true),
        part("right-ear",Shape.SPHERE,Motion.NONE,.30f,1.57f,.005f,.065f,.095f,.075f,.72f,.51f,.36f,true),
        part("nose",Shape.SPHERE,Motion.NONE,0f,1.53f,.245f,.045f,.055f,.045f,.68f,.46f,.32f,true),
        part("left-eye",Shape.SPHERE,Motion.NONE,-.095f,1.60f,.238f,.035f,.027f,.018f,.07f,.055f,.04f,true),
        part("right-eye",Shape.SPHERE,Motion.NONE,.095f,1.60f,.238f,.035f,.027f,.018f,.07f,.055f,.04f,true),
        part("hair",Shape.SPHERE,Motion.NONE,0f,1.72f,-.045f,.31f,.17f,.29f,.12f,.075f,.045f,true),
        part("cap",Shape.SPHERE,Motion.NONE,0f,1.82f,.005f,.34f,.085f,.31f,.22f,.17f,.09f,true),
        part("backpack",Shape.BOX,Motion.NONE,0f,1.02f,-.205f,.34f,.42f,.15f,.30f,.19f,.11f,true),
        part("left-backpack-strap",Shape.BOX,Motion.NONE,-.19f,1.16f,-.115f,.055f,.38f,.055f,.11f,.23f,.15f,true),
        part("right-backpack-strap",Shape.BOX,Motion.NONE,.19f,1.16f,-.115f,.055f,.38f,.055f,.11f,.23f,.15f,true)
    };

    private PlayerModelSpec() { }

    public static int size() { return PARTS.length; }
    public static Part partAt(int index) { return PARTS[index]; }
    public static Part[] parts() { return PARTS.clone(); }
    public static float nominalHeight() { return 1.92f; }

    public static Part find(String id) {
        for (Part part : PARTS) if (part.id.equals(id)) return part;
        return null;
    }
}
