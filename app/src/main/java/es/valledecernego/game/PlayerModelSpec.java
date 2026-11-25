package es.valledecernego.game;

/**
 * Declarative low-poly player model. Dimensions are in world units.
 * The renderer consumes this same list, so the tests validate real render data.
 */
public final class PlayerModelSpec {
    public enum Shape { BOX, SPHERE, LIMB }
    public enum Motion { NONE, LEFT_LEG, RIGHT_LEG, LEFT_ARM, RIGHT_ARM }

    public static final int MATERIAL_SOLID = 0;
    public static final int MATERIAL_FABRIC = 1;
    public static final int MATERIAL_COTTON = 2;
    public static final int MATERIAL_LEATHER = 3;
    public static final int MATERIAL_SKIN = 4;
    public static final int MATERIAL_METAL = 5;

    public static final class Part {
        public final String id;
        public final Shape shape;
        public final Motion motion;
        public final float x, y, z, sx, sy, sz;
        public final float r, g, b, alpha, roll;
        public final int materialStyle;
        public final boolean followsBody;

        private Part(String id, Shape shape, Motion motion,
                     float x, float y, float z, float sx, float sy, float sz,
                     float r, float g, float b, float alpha, float roll, boolean followsBody) {
            this.id=id; this.shape=shape; this.motion=motion;
            this.x=x; this.y=y; this.z=z;
            this.sx=sx; this.sy=sy; this.sz=sz;
            this.r=r; this.g=g; this.b=b; this.alpha=alpha; this.roll=roll;
            this.materialStyle=materialStyleFor(id);
            this.followsBody=followsBody;
        }
    }

    private static int materialStyleFor(String id) {
        if (id.contains("zipper") || id.contains("buckle")) return MATERIAL_METAL;
        if (id.contains("shirt")) return MATERIAL_COTTON;
        if (id.contains("backpack") || id.contains("satchel") || id.contains("strap")) return MATERIAL_LEATHER;
        if (id.equals("head") || id.equals("neck") || id.contains("ear") || id.equals("nose") || id.contains("hand")) return MATERIAL_SKIN;
        if (id.contains("boot") || id.contains("leg") || id.contains("jacket") || id.contains("pocket") || id.contains("shoulder") || id.contains("arm") || id.contains("sleeve") || id.equals("torso") || id.equals("collar") || id.equals("cap")) return MATERIAL_FABRIC;
        return MATERIAL_SOLID;
    }

    private static Part part(String id, Shape shape, Motion motion,
                             float x, float y, float z, float sx, float sy, float sz,
                             float r, float g, float b, boolean followsBody) {
        return new Part(id,shape,motion,x,y,z,sx,sy,sz,r,g,b,1f,0f,followsBody);
    }

    private static Part partWithRoll(String id, Shape shape, Motion motion,
                                      float x, float y, float z, float sx, float sy, float sz,
                                      float r, float g, float b, float roll, boolean followsBody) {
        return new Part(id,shape,motion,x,y,z,sx,sy,sz,r,g,b,1f,roll,followsBody);
    }

    private static final Part[] PARTS = {
        part("left-boot",Shape.BOX,Motion.NONE,-.13f,.14f,.10f,.25f,.17f,.38f,.82f,.43f,.06f,false),
        part("right-boot",Shape.BOX,Motion.NONE,.13f,.14f,.10f,.25f,.17f,.38f,.82f,.43f,.06f,false),
        part("left-leg",Shape.LIMB,Motion.LEFT_LEG,-.13f,.61f,0f,.18f,.52f,.19f,.075f,.065f,.06f,false),
        part("right-leg",Shape.LIMB,Motion.RIGHT_LEG,.13f,.61f,0f,.18f,.52f,.19f,.075f,.065f,.06f,false),
        part("torso",Shape.BOX,Motion.NONE,0f,.99f,0f,.52f,.64f,.32f,.18f,.34f,.17f,true),
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
        part("head",Shape.SPHERE,Motion.NONE,0f,1.57f,.005f,.25f,.25f,.23f,.79f,.48f,.29f,true),
        part("left-ear",Shape.SPHERE,Motion.NONE,-.30f,1.57f,.005f,.065f,.095f,.075f,.72f,.51f,.36f,true),
        part("right-ear",Shape.SPHERE,Motion.NONE,.30f,1.57f,.005f,.065f,.095f,.075f,.72f,.51f,.36f,true),
        part("nose",Shape.SPHERE,Motion.NONE,0f,1.53f,.225f,.045f,.055f,.045f,.68f,.46f,.32f,true),
                part("hair",Shape.SPHERE,Motion.NONE,0f,1.72f,-.045f,.31f,.17f,.29f,.095f,.052f,.028f,true),
        part("cap",Shape.SPHERE,Motion.NONE,0f,1.82f,.005f,.34f,.10f,.31f,.105f,.095f,.085f,true),
        part("backpack",Shape.BOX,Motion.NONE,-.32f,.69f,-.22f,.34f,.42f,.16f,.58f,.39f,.19f,true),
        part("left-backpack-strap",Shape.BOX,Motion.NONE,-.19f,1.16f,-.305f,.055f,.38f,.055f,.24f,.15f,.075f,true),
        part("right-backpack-strap",Shape.BOX,Motion.NONE,.19f,1.16f,-.305f,.055f,.38f,.055f,.24f,.15f,.075f,true),
        part("backpack-flap",Shape.BOX,Motion.NONE,-.32f,.79f,-.31f,.27f,.15f,.025f,.68f,.48f,.25f,true),
        part("backpack-side-pocket",Shape.BOX,Motion.NONE,-.51f,.64f,-.285f,.085f,.18f,.07f,.48f,.31f,.15f,true),
        // Turquoise shirt and orange diagonal graphic visible through the open jacket.
        part("shirt-front",Shape.BOX,Motion.NONE,0f,1.00f,.174f,.27f,.50f,.035f,.04f,.58f,.52f,true),
        partWithRoll("shirt-orange-stripe",Shape.BOX,Motion.NONE,0f,1.00f,.205f,.12f,.43f,.025f,.94f,.29f,.08f,-.62f,true),
        // Open olive jacket fronts and folded collar.
        part("jacket-left-front",Shape.BOX,Motion.NONE,-.205f,1.00f,.19f,.13f,.56f,.055f,.25f,.32f,.12f,true),
        part("jacket-right-front",Shape.BOX,Motion.NONE,.205f,1.00f,.19f,.13f,.56f,.055f,.25f,.32f,.12f,true),
        part("jacket-left-collar",Shape.BOX,Motion.NONE,-.16f,1.30f,.19f,.15f,.14f,.07f,.31f,.37f,.15f,true),
        part("jacket-right-collar",Shape.BOX,Motion.NONE,.16f,1.30f,.19f,.15f,.14f,.07f,.31f,.37f,.15f,true),
        // Green eyes, dark pupils and simple brows.
        part("left-eye-white",Shape.SPHERE,Motion.NONE,-.08f,1.60f,.24f,.052f,.038f,.022f,.94f,.88f,.70f,true),
        part("right-eye-white",Shape.SPHERE,Motion.NONE,.08f,1.60f,.24f,.052f,.038f,.022f,.94f,.88f,.70f,true),
        part("left-eye-green",Shape.SPHERE,Motion.NONE,-.08f,1.60f,.265f,.024f,.029f,.012f,.08f,.48f,.20f,true),
        part("right-eye-green",Shape.SPHERE,Motion.NONE,.08f,1.60f,.265f,.024f,.029f,.012f,.08f,.48f,.20f,true),
        part("left-eyebrow",Shape.BOX,Motion.NONE,-.08f,1.65f,.19f,.085f,.018f,.018f,.12f,.065f,.035f,true),
        part("right-eyebrow",Shape.BOX,Motion.NONE,.08f,1.65f,.19f,.085f,.018f,.018f,.12f,.065f,.035f,true),
        part("left-pupil",Shape.SPHERE,Motion.NONE,-.08f,1.60f,.282f,.012f,.019f,.009f,.035f,.025f,.02f,true),
        part("right-pupil",Shape.SPHERE,Motion.NONE,.08f,1.60f,.282f,.012f,.019f,.009f,.035f,.025f,.02f,true),
        part("mouth",Shape.BOX,Motion.NONE,0f,1.47f,.16f,.085f,.014f,.012f,.25f,.09f,.065f,true),
        // Cross-body satchel strap, represented by short angled-looking segments.
        partWithRoll("satchel-strap-upper",Shape.BOX,Motion.NONE,.13f,1.20f,.205f,.095f,.20f,.035f,.30f,.19f,.10f,-.62f,true),
        partWithRoll("satchel-strap-middle",Shape.BOX,Motion.NONE,.015f,1.06f,.215f,.095f,.20f,.035f,.30f,.19f,.10f,-.62f,true),
        partWithRoll("satchel-strap-lower",Shape.BOX,Motion.NONE,-.12f,.91f,.215f,.095f,.20f,.035f,.30f,.19f,.10f,-.62f,true),
        part("satchel-flap",Shape.BOX,Motion.NONE,-.32f,.78f,.255f,.30f,.19f,.045f,.69f,.49f,.25f,true),
        part("satchel-buckle",Shape.BOX,Motion.NONE,-.32f,.75f,.283f,.055f,.07f,.018f,.72f,.55f,.18f,true),
        // Knit-cap cuff, fringe and a readable low-poly face.
        part("cap-band",Shape.BOX,Motion.NONE,0f,1.77f,.005f,.58f,.055f,.46f,.16f,.145f,.13f,true),
        part("hair-fringe",Shape.SPHERE,Motion.NONE,0f,1.66f,.17f,.22f,.06f,.09f,.095f,.052f,.028f,true),
        // Boot soles, pale laces and olive sleeve cuffs make the silhouette read at phone scale.
        part("left-boot-sole",Shape.BOX,Motion.NONE,-.13f,.055f,.11f,.27f,.04f,.40f,.76f,.65f,.46f,false),
        part("right-boot-sole",Shape.BOX,Motion.NONE,.13f,.055f,.11f,.27f,.04f,.40f,.76f,.65f,.46f,false),
        part("left-boot-lace-upper",Shape.BOX,Motion.NONE,-.13f,.18f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("left-boot-lace-middle",Shape.BOX,Motion.NONE,-.13f,.145f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("left-boot-lace-lower",Shape.BOX,Motion.NONE,-.13f,.11f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("right-boot-lace-upper",Shape.BOX,Motion.NONE,.13f,.18f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("right-boot-lace-middle",Shape.BOX,Motion.NONE,.13f,.145f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("right-boot-lace-lower",Shape.BOX,Motion.NONE,.13f,.11f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("left-sleeve-cuff",Shape.BOX,Motion.NONE,-.32f,.78f,.015f,.18f,.075f,.19f,.25f,.31f,.12f,true),
        part("right-sleeve-cuff",Shape.BOX,Motion.NONE,.32f,.78f,.015f,.18f,.075f,.19f,.25f,.31f,.12f,true)
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
