package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerModelSpecTest {
    @Test public void modelContainsACompleteRecognizableHumanSilhouette() {
        assertTrue("model should contain a detailed low-poly silhouette and rear accessories",PlayerModelSpec.size()>=60);
        assertEquals(PlayerModelSpec.Shape.SPHERE,PlayerModelSpec.find("head").shape);
        assertEquals(PlayerModelSpec.Shape.SPHERE,PlayerModelSpec.find("torso").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("left-arm").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("right-arm").shape);
        assertEquals(PlayerModelSpec.Motion.LEFT_LEG,PlayerModelSpec.find("left-leg").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_LEG,PlayerModelSpec.find("right-leg").motion);
        assertEquals(PlayerModelSpec.Motion.LEFT_FOOT,PlayerModelSpec.find("left-boot").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_FOOT,PlayerModelSpec.find("right-boot").motion);
        assertEquals(PlayerModelSpec.Motion.LEFT_HAND,PlayerModelSpec.find("left-hand").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_HAND,PlayerModelSpec.find("right-hand").motion);
        assertNotNull(PlayerModelSpec.find("backpack"));
        assertNotNull(PlayerModelSpec.find("cap"));
    }

    @Test public void jacketTorsoIsRoundedWithoutChangingItsOuterDimensions() {
        PlayerModelSpec.Part torso=PlayerModelSpec.find("torso");
        assertEquals(PlayerModelSpec.Shape.SPHERE,torso.shape);
        assertEquals(.26f,torso.sx,0.0001f);
        assertEquals(.32f,torso.sy,0.0001f);
        assertEquals(.16f,torso.sz,0.0001f);

        PlayerModelSpec.Part fallbackTorso=null;
        for(PlayerModelSpec.Part part:PlayerModelSpec.fallbackParts()) {
            if("torso".equals(part.id)) fallbackTorso=part;
        }
        assertNotNull(fallbackTorso);
        assertEquals(.52f,fallbackTorso.sx,0.0001f);
        assertEquals(.64f,fallbackTorso.sy,0.0001f);
        assertEquals(.32f,fallbackTorso.sz,0.0001f);
    }

    @Test public void explorerReferenceHasItsSignatureClothingAndAccessories() {
        assertNotNull(PlayerModelSpec.find("shirt-front"));
        assertNotNull(PlayerModelSpec.find("shirt-orange-stripe"));
        assertNotNull(PlayerModelSpec.find("left-pupil"));
        assertNotNull(PlayerModelSpec.find("cap-band"));
        assertNotNull(PlayerModelSpec.find("jacket-left-front"));
        assertNotNull(PlayerModelSpec.find("cap"));
        assertNotNull(PlayerModelSpec.find("satchel-flap"));
        assertNotNull(PlayerModelSpec.find("left-eye-green"));
        assertEquals(.82f,PlayerModelSpec.find("left-boot").r,0.001f);
        assertTrue(PlayerModelSpec.find("left-leg").r<.12f);
    }

    @Test public void diagonalClothingAndBagStrapHaveExplicitFrontPlaneRotation() {
        assertEquals(-.62f,PlayerModelSpec.find("shirt-orange-stripe").roll,0.0001f);
        assertEquals(-.62f,PlayerModelSpec.find("satchel-strap-middle").roll,0.0001f);
        assertTrue(PlayerModelSpec.find("left-eye-white").z
            > PlayerModelSpec.find("head").z);
        assertTrue(PlayerModelSpec.find("left-pupil").z
            > PlayerModelSpec.find("left-eye-green").z);
    }

    @Test public void backpackAndStrapsFaceTheThirdPersonRearCamera() {
        PlayerModelSpec.Part backpack=PlayerModelSpec.find("backpack");
        PlayerModelSpec.Part leftStrap=PlayerModelSpec.find("left-backpack-strap");
        PlayerModelSpec.Part rightStrap=PlayerModelSpec.find("right-backpack-strap");
        PlayerModelSpec.Part flap=PlayerModelSpec.find("backpack-flap");
        assertNotNull(backpack);
        assertNotNull(leftStrap);
        assertNotNull(rightStrap);
        assertNotNull(flap);
        assertTrue("backpack must protrude from the rear of the torso",backpack.z < -.16f);
        assertEquals("backpack must be centred behind the torso",0f,backpack.x,0.0001f);
        assertTrue("left strap must align left of the backpack centre",leftStrap.x<0f);
        assertTrue("right strap must align right of the backpack centre",rightStrap.x>0f);
        assertTrue("straps must sit on the camera-facing rear surface",leftStrap.z < backpack.z);
        assertTrue(rightStrap.z < backpack.z);
        assertTrue(flap.z < backpack.z);
    }

    @Test public void clothingUsesDistinctProceduralMaterialStyles() {
        assertEquals(PlayerModelSpec.MATERIAL_FABRIC,PlayerModelSpec.find("torso").materialStyle);
        assertEquals(PlayerModelSpec.MATERIAL_COTTON,PlayerModelSpec.find("shirt-front").materialStyle);
        assertEquals(PlayerModelSpec.MATERIAL_LEATHER,PlayerModelSpec.find("backpack").materialStyle);
        assertEquals(PlayerModelSpec.MATERIAL_SKIN,PlayerModelSpec.find("head").materialStyle);
        assertEquals(PlayerModelSpec.MATERIAL_METAL,PlayerModelSpec.find("satchel-buckle").materialStyle);
        assertEquals(PlayerModelSpec.MATERIAL_LEATHER,PlayerModelSpec.find("left-boot").materialStyle);
        assertEquals(PlayerModelSpec.MATERIAL_FABRIC,PlayerModelSpec.find("cap-band").materialStyle);
        for(PlayerModelSpec.Part part:PlayerModelSpec.parts()) {
            assertTrue("material style must be supported by the GLES shader",
                part.materialStyle>=PlayerModelSpec.MATERIAL_SOLID
                    && part.materialStyle<=PlayerModelSpec.MATERIAL_METAL);
        }
    }

    @Test public void allRenderedPartsHaveFinitePositiveDimensionsAndValidMaterials() {
        for(PlayerModelSpec.Part part:PlayerModelSpec.parts()) {
            assertNotNull(part.id);
            assertTrue(part.sx>0f && part.sy>0f && part.sz>0f);
            assertTrue(part.r>=0f && part.r<=1f);
            assertTrue(part.g>=0f && part.g<=1f);
            assertTrue(part.b>=0f && part.b<=1f);
            assertTrue(part.alpha>=0f && part.alpha<=1f);
            assertTrue(Float.isFinite(part.x) && Float.isFinite(part.y) && Float.isFinite(part.z));
            assertTrue(Float.isFinite(part.roll));
        }
    }

    @Test public void leftAndRightLimbsAreSymmetricAndUseOppositeGaitChannels() {
        PlayerModelSpec.Part leftArm=PlayerModelSpec.find("left-arm");
        PlayerModelSpec.Part rightArm=PlayerModelSpec.find("right-arm");
        PlayerModelSpec.Part leftLeg=PlayerModelSpec.find("left-leg");
        PlayerModelSpec.Part rightLeg=PlayerModelSpec.find("right-leg");
        assertEquals(-leftArm.x,rightArm.x,0.0001f);
        assertEquals(-leftLeg.x,rightLeg.x,0.0001f);
        assertEquals(leftArm.sy,rightArm.sy,0.0001f);
        assertEquals(leftLeg.sy,rightLeg.sy,0.0001f);
        assertEquals(PlayerModelSpec.Motion.LEFT_ARM,leftArm.motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_ARM,rightArm.motion);
    }

    @Test public void bootSolesMeetTheRenderedGroundPlane() {
        for(String id:new String[]{"left-boot","right-boot"}) {
            PlayerModelSpec.Part boot=PlayerModelSpec.find(id);
            assertNotNull(boot);
            assertEquals(PlayerModelSpec.GROUND_LEVEL,boot.y-boot.sy*.5f,0.006f);
        }
    }

    @Test public void modelHasHumanScaleAndFeetAboveGroundPlane() {
        PlayerModelSpec.Part boot=PlayerModelSpec.find("left-boot");
        assertEquals(1.92f,PlayerModelSpec.nominalHeight(),0.0001f);
        assertTrue(boot.y-boot.sy*.5f>0f);
        assertTrue(PlayerModelSpec.find("head").sy>PlayerModelSpec.find("neck").sy);
        assertEquals(.22f,PlayerModelSpec.find("head").sy,0.0001f);
        assertEquals(.18f,PlayerModelSpec.find("head").sz,0.0001f);
        assertTrue("head must be visibly narrower than the jacket torso",
            PlayerModelSpec.find("head").sx < PlayerModelSpec.find("torso").sx);
        assertTrue("cap should sit over, not engulf, the head",
            PlayerModelSpec.find("cap").sx < PlayerModelSpec.find("head").sx * 1.4f);
        assertTrue("eyes must sit on the front surface",
            PlayerModelSpec.find("left-eye-white").z+PlayerModelSpec.find("left-eye-white").sz
                > PlayerModelSpec.find("head").z+PlayerModelSpec.find("head").sz);
    }
    @Test public void roundedCompatibilityModelKeepsSphericalHeadTorsoAndCapsuleLimbs() {
        PlayerModelSpec.Part[] rounded=PlayerModelSpec.roundedFallbackParts();
        assertEquals(11,rounded.length);
        for(PlayerModelSpec.Part part:rounded) {
            PlayerModelSpec.Part source=PlayerModelSpec.find(part.id);
            assertNotNull(source);
            assertEquals(source.shape,part.shape);
            assertTrue(part.sx>0f && part.sy>0f && part.sz>0f);
        }
        assertEquals(PlayerModelSpec.Shape.SPHERE,findRounded(rounded,"head").shape);
        assertEquals(PlayerModelSpec.Shape.SPHERE,findRounded(rounded,"torso").shape);
        for(String id:new String[]{"left-arm","right-arm","left-leg","right-leg"}) {
            assertEquals(PlayerModelSpec.Shape.LIMB,findRounded(rounded,id).shape);
        }
        assertEquals(PlayerModelSpec.Shape.BOX,findRounded(rounded,"left-boot").shape);
        assertEquals(PlayerModelSpec.Shape.BOX,findRounded(rounded,"backpack").shape);
    }

    @Test public void fullBoxCompatibilityModelRetainsAllSixtyHumanPartsAndWalkChannels() {
        PlayerModelSpec.Part[] boxes=PlayerModelSpec.boxCompatibilityParts();
        assertEquals(PlayerModelSpec.size(),boxes.length);
        assertEquals(60,boxes.length);
        for(PlayerModelSpec.Part part:boxes) {
            assertEquals(PlayerModelSpec.Shape.BOX,part.shape);
            assertTrue(part.sx>0f && part.sy>0f && part.sz>0f);
            assertTrue(Float.isFinite(part.x) && Float.isFinite(part.y) && Float.isFinite(part.z));
        }
        assertEquals(PlayerModelSpec.Motion.LEFT_ARM,findBoxPart(boxes,"left-arm").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_ARM,findBoxPart(boxes,"right-arm").motion);
        assertEquals(PlayerModelSpec.Motion.LEFT_LEG,findBoxPart(boxes,"left-leg").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_LEG,findBoxPart(boxes,"right-leg").motion);
        assertEquals(PlayerModelSpec.Motion.LEFT_FOOT,findBoxPart(boxes,"left-boot").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_FOOT,findBoxPart(boxes,"right-boot").motion);
        assertEquals(PlayerModelSpec.Motion.LEFT_HAND,findBoxPart(boxes,"left-hand").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_HAND,findBoxPart(boxes,"right-hand").motion);
        assertEquals(PlayerModelSpec.Shape.SPHERE,PlayerModelSpec.find("head").shape);
        assertEquals(PlayerModelSpec.Shape.BOX,findBoxPart(boxes,"head").shape);
        assertEquals(PlayerModelSpec.find("head").sx*2f,findBoxPart(boxes,"head").sx,.0001f);
        assertEquals(PlayerModelSpec.find("torso").sy*2f,findBoxPart(boxes,"torso").sy,.0001f);
        assertNotNull(findBoxPart(boxes,"shirt-front"));
        assertNotNull(findBoxPart(boxes,"backpack"));
        assertNotNull(findBoxPart(boxes,"left-boot"));
    }

    private static PlayerModelSpec.Part findBoxPart(PlayerModelSpec.Part[] parts,String id) {
        for(PlayerModelSpec.Part part:parts) if(id.equals(part.id)) return part;
        return null;
    }

    private static PlayerModelSpec.Part findRounded(PlayerModelSpec.Part[] parts,String id) {
        for(PlayerModelSpec.Part part:parts) if(id.equals(part.id)) return part;
        return null;
    }

    @Test public void armsAreSeparatedFromTheTorsoForAReadableSilhouette() {
        PlayerModelSpec.Part torso=PlayerModelSpec.find("torso");
        PlayerModelSpec.Part left=PlayerModelSpec.find("left-arm");
        PlayerModelSpec.Part right=PlayerModelSpec.find("right-arm");
        assertTrue(Math.abs(left.x)-left.sx > torso.sx-.03f);
        assertTrue(Math.abs(right.x)-right.sx > torso.sx-.03f);
        assertEquals(-left.x,right.x,.0001f);
    }

    @Test public void compatibilitySilhouetteContainsAllEssentialHumanParts() {
        PlayerModelSpec.Part[] fallback=PlayerModelSpec.fallbackParts();
        assertEquals(11,fallback.length);

        // The full model keeps rounded geometry; only the emergency silhouette is box-only.
        assertEquals(PlayerModelSpec.Shape.SPHERE,PlayerModelSpec.find("torso").shape);
        assertEquals(PlayerModelSpec.Shape.SPHERE,PlayerModelSpec.find("head").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("left-arm").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("right-arm").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("left-leg").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("right-leg").shape);

        String[] essential={"left-boot","right-boot","left-leg","right-leg","torso",
            "left-arm","right-arm","neck","head","cap","backpack"};
        for(String id:essential) {
            PlayerModelSpec.Part fallbackPart=null;
            for(PlayerModelSpec.Part part:fallback) {
                if(id.equals(part.id)) fallbackPart=part;
            }
            assertNotNull("fallback must include "+id,fallbackPart);
            assertEquals("fallback mesh must be box-only for GLES compatibility",
                PlayerModelSpec.Shape.BOX,fallbackPart.shape);
            assertTrue(fallbackPart.sx>0f && fallbackPart.sy>0f && fallbackPart.sz>0f);
        }
    }

    @Test public void explorerJacketContrastsWithTheGreenWorldAndKeepsItsShirtAccent() {
        PlayerModelSpec.Part torso=PlayerModelSpec.find("torso");
        assertTrue("rust jacket must be distinct from green grass",torso.r>torso.g+.08f);
        assertTrue("rust jacket must keep a warm red-brown hue",torso.r>torso.b+.08f);
        assertNotNull(PlayerModelSpec.find("shirt-front"));
        assertNotNull(PlayerModelSpec.find("shirt-orange-stripe"));
    }


    @Test public void compatibilityModelUsesOnlyCubeMeshForAllElevenCoreParts() {
        PlayerModelSpec.Part[] parts = PlayerModelSpec.fallbackParts();
        assertEquals(11, parts.length);
        for (PlayerModelSpec.Part part : parts) {
            assertEquals("fallback must not depend on sphere/capsule buffers: " + part.id,
                PlayerModelSpec.Shape.BOX, part.shape);
        }
        for (String id : new String[] {
            "head", "cap", "torso", "left-arm", "right-arm",
            "left-leg", "right-leg", "left-boot", "right-boot", "neck", "backpack"
        }) {
            boolean found = false;
            for (PlayerModelSpec.Part part : parts) {
                if (id.equals(part.id)) found = true;
            }
            assertTrue("missing humanoid part " + id, found);
        }
    }

    @Test public void sphereFallbackPreservesTheOriginalOutsideDimensions() {
        PlayerModelSpec.Part original = PlayerModelSpec.find("head");
        PlayerModelSpec.Part box = null;
        for (PlayerModelSpec.Part part : PlayerModelSpec.fallbackParts()) {
            if ("head".equals(part.id)) box = part;
        }
        assertNotNull(box);
        assertEquals(original.sx * 2f, box.sx, 0.0001f);
        assertEquals(original.sy * 2f, box.sy, 0.0001f);
        assertEquals(original.sz * 2f, box.sz, 0.0001f);
    }

    @Test public void compatibilityArmsAndLegsStayArticulatedAndReadable() {
        for (String id : new String[] {"left-arm", "right-arm", "left-leg", "right-leg"}) {
            PlayerModelSpec.Part part = null;
            for (PlayerModelSpec.Part candidate : PlayerModelSpec.fallbackParts()) {
                if (id.equals(candidate.id)) part = candidate;
            }
            assertNotNull(part);
            assertEquals(PlayerModelSpec.Shape.BOX, part.shape);
            assertNotEquals(PlayerModelSpec.Motion.NONE, part.motion);
            assertTrue("limb should remain wide enough for phone screens", part.sx >= 0.18f);
            assertTrue(part.sy >= 0.44f);
            assertTrue(part.sz >= 0.18f);
        }
    }

}
