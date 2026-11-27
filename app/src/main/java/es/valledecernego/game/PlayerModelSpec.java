package es.valledecernego.game;

/**
 * Declarative low-poly player model. Dimensions are in world units.
 * The renderer consumes this same list, so the tests validate real render data.
 */
public final class PlayerModelSpec {
    public enum Shape { BOX, SPHERE, LIMB }
    public enum Motion { NONE, LEFT_LEG, RIGHT_LEG, LEFT_ARM, RIGHT_ARM }

    /** Height of the walkable ground plane; boot soles are authored to meet it. */
    public static final float GROUND_LEVEL = 0.05f;

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
        if (id.contains("backpack") || id.contains("satchel") || id.contains("strap") || id.contains("boot")) return MATERIAL_LEATHER;
        if (id.equals("head") || id.equals("neck") || id.contains("ear") || id.equals("nose") || id.contains("hand")) return MATERIAL_SKIN;
        if (id.contains("boot") || id.contains("leg") || id.contains("jacket") || id.contains("pocket") || id.contains("shoulder") || id.contains("arm") || id.contains("sleeve") || id.equals("torso") || id.equals("collar") || id.contains("cap")) return MATERIAL_FABRIC;
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
        // Rounded ellipsoid keeps the jacket's original 0.52 x 0.64 x 0.32 m bounds.
        part("torso",Shape.SPHERE,Motion.NONE,0f,.99f,0f,.26f,.32f,.16f,.30f,.13f,.08f,true),
        part("jacket-hem",Shape.BOX,Motion.NONE,0f,.72f,.012f,.53f,.12f,.33f,.25f,.10f,.06f,true),
        part("collar",Shape.BOX,Motion.NONE,0f,1.31f,.015f,.19f,.14f,.20f,.72f,.69f,.55f,true),
        part("zipper",Shape.BOX,Motion.NONE,0f,1.00f,.166f,.025f,.45f,.014f,.76f,.70f,.52f,true),
        part("left-pocket",Shape.BOX,Motion.NONE,-.15f,.98f,.17f,.12f,.13f,.025f,.38f,.19f,.10f,true),
        part("right-pocket",Shape.BOX,Motion.NONE,.15f,.98f,.17f,.12f,.13f,.025f,.38f,.19f,.10f,true),
        part("left-shoulder",Shape.SPHERE,Motion.NONE,-.25f,1.20f,0f,.13f,.15f,.14f,.32f,.15f,.09f,true),
        part("right-shoulder",Shape.SPHERE,Motion.NONE,.25f,1.20f,0f,.13f,.15f,.14f,.32f,.15f,.09f,true),
        part("left-arm",Shape.LIMB,Motion.LEFT_ARM,-.36f,1.20f,0f,.12f,.44f,.13f,.32f,.15f,.09f,true),
        part("right-arm",Shape.LIMB,Motion.RIGHT_ARM,.36f,1.20f,0f,.12f,.44f,.13f,.32f,.15f,.09f,true),
        part("left-hand",Shape.SPHERE,Motion.NONE,-.37f,.82f,.025f,.075f,.09f,.075f,.75f,.54f,.38f,true),
        part("right-hand",Shape.SPHERE,Motion.NONE,.37f,.82f,.025f,.075f,.09f,.075f,.75f,.54f,.38f,true),
        part("neck",Shape.BOX,Motion.NONE,0f,1.35f,.015f,.14f,.14f,.14f,.72f,.51f,.36f,true),
        part("head",Shape.SPHERE,Motion.NONE,0f,1.57f,.005f,.19f,.22f,.18f,.79f,.48f,.29f,true),
        part("left-ear",Shape.SPHERE,Motion.NONE,-.22f,1.57f,.005f,.05f,.075f,.055f,.72f,.51f,.36f,true),
        part("right-ear",Shape.SPHERE,Motion.NONE,.22f,1.57f,.005f,.05f,.075f,.055f,.72f,.51f,.36f,true),
        part("nose",Shape.SPHERE,Motion.NONE,0f,1.53f,.225f,.045f,.055f,.045f,.68f,.46f,.32f,true),
        part("hair",Shape.SPHERE,Motion.NONE,0f,1.70f,-.045f,.18f,.11f,.17f,.095f,.052f,.028f,true),
        part("cap",Shape.SPHERE,Motion.NONE,0f,1.82f,.005f,.25f,.08f,.23f,.105f,.095f,.085f,true),
        part("backpack",Shape.BOX,Motion.NONE,0f,.84f,-.22f,.40f,.46f,.17f,.58f,.39f,.19f,true),
        part("left-backpack-strap",Shape.BOX,Motion.NONE,-.14f,1.16f,-.315f,.045f,.38f,.045f,.24f,.15f,.075f,true),
        part("right-backpack-strap",Shape.BOX,Motion.NONE,.14f,1.16f,-.315f,.045f,.38f,.045f,.24f,.15f,.075f,true),
        part("backpack-flap",Shape.BOX,Motion.NONE,0f,1.02f,-.315f,.32f,.14f,.025f,.68f,.48f,.25f,true),
        part("backpack-side-pocket",Shape.BOX,Motion.NONE,-.25f,.76f,-.29f,.10f,.18f,.07f,.48f,.31f,.15f,true),
        // Turquoise shirt and orange diagonal graphic visible through the open jacket.
        part("shirt-front",Shape.BOX,Motion.NONE,0f,1.00f,.174f,.27f,.50f,.035f,.04f,.58f,.52f,true),
        partWithRoll("shirt-orange-stripe",Shape.BOX,Motion.NONE,0f,1.00f,.205f,.12f,.43f,.025f,.94f,.29f,.08f,-.62f,true),
        // Open rust-red field jacket fronts and folded collar.
        part("jacket-left-front",Shape.BOX,Motion.NONE,-.205f,1.00f,.19f,.13f,.56f,.055f,.30f,.13f,.08f,true),
        part("jacket-right-front",Shape.BOX,Motion.NONE,.205f,1.00f,.19f,.13f,.56f,.055f,.30f,.13f,.08f,true),
        part("jacket-left-collar",Shape.BOX,Motion.NONE,-.16f,1.30f,.19f,.15f,.14f,.07f,.35f,.17f,.10f,true),
        part("jacket-right-collar",Shape.BOX,Motion.NONE,.16f,1.30f,.19f,.15f,.14f,.07f,.35f,.17f,.10f,true),
        // Green eyes, dark pupils and simple brows.
        part("left-eye-white",Shape.SPHERE,Motion.NONE,-.07f,1.60f,.19f,.045f,.032f,.020f,.94f,.88f,.70f,true),
        part("right-eye-white",Shape.SPHERE,Motion.NONE,.07f,1.60f,.19f,.045f,.032f,.020f,.94f,.88f,.70f,true),
        part("left-eye-green",Shape.SPHERE,Motion.NONE,-.07f,1.60f,.215f,.022f,.026f,.012f,.08f,.48f,.20f,true),
        part("right-eye-green",Shape.SPHERE,Motion.NONE,.07f,1.60f,.215f,.022f,.026f,.012f,.08f,.48f,.20f,true),
        part("left-eyebrow",Shape.BOX,Motion.NONE,-.07f,1.65f,.16f,.07f,.014f,.015f,.12f,.065f,.035f,true),
        part("right-eyebrow",Shape.BOX,Motion.NONE,.07f,1.65f,.16f,.07f,.014f,.015f,.12f,.065f,.035f,true),
        part("left-pupil",Shape.SPHERE,Motion.NONE,-.07f,1.60f,.242f,.011f,.017f,.008f,.035f,.025f,.02f,true),
        part("right-pupil",Shape.SPHERE,Motion.NONE,.07f,1.60f,.242f,.011f,.017f,.008f,.035f,.025f,.02f,true),
        part("mouth",Shape.BOX,Motion.NONE,0f,1.47f,.17f,.065f,.012f,.010f,.25f,.09f,.065f,true),
        // Cross-body satchel strap, represented by short angled-looking segments.
        partWithRoll("satchel-strap-upper",Shape.BOX,Motion.NONE,.13f,1.20f,.205f,.095f,.20f,.035f,.30f,.19f,.10f,-.62f,true),
        partWithRoll("satchel-strap-middle",Shape.BOX,Motion.NONE,.015f,1.06f,.215f,.095f,.20f,.035f,.30f,.19f,.10f,-.62f,true),
        partWithRoll("satchel-strap-lower",Shape.BOX,Motion.NONE,-.12f,.91f,.215f,.095f,.20f,.035f,.30f,.19f,.10f,-.62f,true),
        part("satchel-flap",Shape.BOX,Motion.NONE,-.32f,.78f,.255f,.30f,.19f,.045f,.69f,.49f,.25f,true),
        part("satchel-buckle",Shape.BOX,Motion.NONE,-.32f,.75f,.283f,.055f,.07f,.018f,.72f,.55f,.18f,true),
        // Knit-cap cuff, fringe and a readable low-poly face.
        part("cap-band",Shape.BOX,Motion.NONE,0f,1.77f,.005f,.48f,.05f,.44f,.16f,.145f,.13f,true),
        part("hair-fringe",Shape.SPHERE,Motion.NONE,0f,1.66f,.15f,.17f,.045f,.07f,.095f,.052f,.028f,true),
        // Boot soles, pale laces and olive sleeve cuffs make the silhouette read at phone scale.
        part("left-boot-sole",Shape.BOX,Motion.NONE,-.13f,.055f,.11f,.27f,.04f,.40f,.76f,.65f,.46f,false),
        part("right-boot-sole",Shape.BOX,Motion.NONE,.13f,.055f,.11f,.27f,.04f,.40f,.76f,.65f,.46f,false),
        part("left-boot-lace-upper",Shape.BOX,Motion.NONE,-.13f,.18f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("left-boot-lace-middle",Shape.BOX,Motion.NONE,-.13f,.145f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("left-boot-lace-lower",Shape.BOX,Motion.NONE,-.13f,.11f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("right-boot-lace-upper",Shape.BOX,Motion.NONE,.13f,.18f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("right-boot-lace-middle",Shape.BOX,Motion.NONE,.13f,.145f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("right-boot-lace-lower",Shape.BOX,Motion.NONE,.13f,.11f,.298f,.12f,.018f,.014f,.92f,.84f,.65f,false),
        part("left-sleeve-cuff",Shape.BOX,Motion.NONE,-.37f,.78f,.015f,.14f,.07f,.15f,.28f,.12f,.07f,true),
        part("right-sleeve-cuff",Shape.BOX,Motion.NONE,.37f,.78f,.015f,.14f,.07f,.15f,.28f,.12f,.07f,true),
    };

    private static final String[] FALLBACK_PART_IDS = {
        "left-boot","right-boot","left-leg","right-leg","torso",
        "left-arm","right-arm","neck","head","cap","backpack"
    };

    private PlayerModelSpec() { }

    public static int size() { return PARTS.length; }
    public static Part partAt(int index) { return PARTS[index]; }
    public static Part[] parts() { return PARTS.clone(); }

    /** Full character converted to the cube mesh only; preserves all part IDs, colours and motion channels. */
    public static Part[] boxCompatibilityParts() {
        Part[] boxes = new Part[PARTS.length];
        for (int i=0;i<PARTS.length;i++) boxes[i] = asCompatibilityBox(PARTS[i]);
        return boxes;
    }
    public static float nominalHeight() { return 1.92f; }

    /**
     * The compatibility silhouette deliberately uses only the cube mesh (vb).
     * Some mobile drivers can accept sphere/capsule draw calls yet fail to show
     * them consistently. The full BOX60 model converts every part; the final
     * 11-part fallback remains available if the full compatibility path fails.
     */
    private static Part asCompatibilityBox(Part source) {
        float sx = source.sx;
        float sy = source.sy;
        float sz = source.sz;
        if (source.shape == Shape.SPHERE) {
            // Sphere scales are radii; a cube's scales are full side lengths.
            sx *= 2f;
            sy *= 2f;
            sz *= 2f;
        } else if (source.shape == Shape.LIMB) {
            // Slightly widen the low-detail arms and legs for phone-sized screens.
            sx = Math.max(sx, 0.18f);
            sz = Math.max(sz, 0.18f);
        }
        return new Part(source.id, Shape.BOX, source.motion,
            source.x, source.y, source.z, sx, sy, sz,
            source.r, source.g, source.b, source.alpha, source.roll, source.followsBody);
    }

    /** Rounded core humanoid used before the final box-only driver fallback. */
    public static Part[] roundedFallbackParts() {
        Part[] rounded = new Part[FALLBACK_PART_IDS.length];
        for (int i=0;i<FALLBACK_PART_IDS.length;i++) {
            Part source=find(FALLBACK_PART_IDS[i]);
            if (source==null) throw new IllegalStateException("Missing rounded player part: "+FALLBACK_PART_IDS[i]);
            rounded[i]=source;
        }
        return rounded;
    }

    /** Core parts used by the independent low-feature GLES compatibility silhouette. */
    public static Part[] fallbackParts() {
        Part[] fallback = new Part[FALLBACK_PART_IDS.length];
        for (int i=0;i<FALLBACK_PART_IDS.length;i++) {
            Part source=find(FALLBACK_PART_IDS[i]);
            if (source==null) throw new IllegalStateException("Missing fallback player part: "+FALLBACK_PART_IDS[i]);
            fallback[i]=asCompatibilityBox(source);
        }
        return fallback;
    }

    public static Part find(String id) {
        for (Part part : PARTS) if (part.id.equals(id)) return part;
        return null;
    }
}
