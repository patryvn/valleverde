package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerModelSpecTest {
    @Test public void modelContainsACompleteRecognizableHumanSilhouette() {
        assertTrue("model should contain a detailed low-poly silhouette and rear accessories",PlayerModelSpec.size()>=60);
        assertEquals(PlayerModelSpec.Shape.SPHERE,PlayerModelSpec.find("head").shape);
        assertEquals(PlayerModelSpec.Shape.BOX,PlayerModelSpec.find("torso").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("left-arm").shape);
        assertEquals(PlayerModelSpec.Shape.LIMB,PlayerModelSpec.find("right-arm").shape);
        assertEquals(PlayerModelSpec.Motion.LEFT_LEG,PlayerModelSpec.find("left-leg").motion);
        assertEquals(PlayerModelSpec.Motion.RIGHT_LEG,PlayerModelSpec.find("right-leg").motion);
        assertNotNull(PlayerModelSpec.find("backpack"));
        assertNotNull(PlayerModelSpec.find("cap"));
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
        assertTrue("straps must sit on the camera-facing rear surface",leftStrap.z < backpack.z);
        assertTrue(rightStrap.z < backpack.z);
        assertTrue(flap.z < backpack.z);
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

    @Test public void modelHasHumanScaleAndFeetAboveGroundPlane() {
        PlayerModelSpec.Part boot=PlayerModelSpec.find("left-boot");
        assertEquals(1.92f,PlayerModelSpec.nominalHeight(),0.0001f);
        assertTrue(boot.y-boot.sy*.5f>0f);
        assertTrue(PlayerModelSpec.find("head").sy>PlayerModelSpec.find("neck").sy);
        assertEquals(.25f,PlayerModelSpec.find("head").sy,0.0001f);
        assertEquals(.23f,PlayerModelSpec.find("head").sz,0.0001f);
        assertTrue("eyes must sit on the front surface",
            PlayerModelSpec.find("left-eye-white").z+PlayerModelSpec.find("left-eye-white").sz
                > PlayerModelSpec.find("head").z+PlayerModelSpec.find("head").sz);
    }
}
