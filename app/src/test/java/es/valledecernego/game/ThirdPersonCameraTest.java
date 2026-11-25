package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class ThirdPersonCameraTest {
    @Test public void cameraStaysBehindThePlayerAtZeroYaw() {
        ThirdPersonCamera.Pose pose = ThirdPersonCamera.follow(0f, 5f, 0f, 0f, 0f);
        assertEquals(0f, pose.x(), 0.0001f);
        assertTrue("camera must remain behind a player facing +Z", pose.z() < 5f);
    }

    @Test public void cameraOrbitsToTheCorrectSideWhenYawChanges() {
        ThirdPersonCamera.Pose pose = ThirdPersonCamera.follow(2f, 3f, 0f,
            (float) (Math.PI / 2.0), 0f);
        assertTrue(pose.x() > 2f);
        assertEquals(3f, pose.z(), 0.0001f);
    }

    @Test public void cameraKeepsAStableCloserThirdPersonDistance() {
        float[] yaws = {0f, .8f, 1.5707963f, -2.2f};
        for (float yaw : yaws) {
            ThirdPersonCamera.Pose pose = ThirdPersonCamera.follow(3f, -4f, .7f, yaw, .12f);
            double dx = pose.x() - 3f;
            double dy = pose.y() - pose.targetY();
            double dz = pose.z() + 4f;
            assertEquals(4.2, Math.sqrt(dx*dx + dy*dy + dz*dz), .001);
        }
    }

    @Test public void cameraFollowsPlayerTranslationAndJumpHeight() {
        ThirdPersonCamera.Pose first = ThirdPersonCamera.follow(1f, 2f, 0f, .3f, .1f);
        ThirdPersonCamera.Pose moved = ThirdPersonCamera.follow(4f, -3f, 1.2f, .3f, .1f);
        assertEquals(3f, moved.x() - first.x(), 0.0001f);
        assertEquals(-5f, moved.z() - first.z(), 0.0001f);
        assertEquals(1.2f, moved.y() - first.y(), 0.0001f);
        assertEquals(1.2f, moved.targetY() - first.targetY(), 0.0001f);
    }

    @Test public void completePlayerModelStaysInsideTheThirdPersonCameraFrustum() {
        float[] cameraYaws = {0f,.7f,1.5707963f,3.1415926f,-1.2f};
        float[] playerFacings = {0f,.8f,2.4f,-2.2f};
        float[] pitches = {-.2f,0f,.55f};
        float px=3f,pz=-4f;
        for(float cameraYaw:cameraYaws){
            for(float pitch:pitches){
                ThirdPersonCamera.Pose camera=ThirdPersonCamera.follow(
                    px,pz,0f,cameraYaw,pitch);
                for(float facing:playerFacings){
                    for(PlayerModelSpec.Part part:PlayerModelSpec.parts()){
                        PlayerModelTransform.Pose point=PlayerModelTransform.partPose(
                            px,0f,pz,facing,0f,part,0f);
                        assertInsideFrustum(camera,px,1.1f,pz,
                            point.x,point.y,point.z,
                            .5f*Math.max(part.sx,Math.max(part.sy,part.sz)));
                    }
                }
            }
        }
    }

    private static void assertInsideFrustum(ThirdPersonCamera.Pose camera,
                                            float targetX,float targetY,float targetZ,
                                            float pointX,float pointY,float pointZ,
                                            float radius){
        double fx=targetX-camera.x();
        double fy=targetY-camera.y();
        double fz=targetZ-camera.z();
        double length=Math.sqrt(fx*fx+fy*fy+fz*fz);
        fx/=length;fy/=length;fz/=length;
        double rx=-fz,ry=0d,rz=fx;
        double rightLength=Math.sqrt(rx*rx+rz*rz);
        rx/=rightLength;rz/=rightLength;
        double ux=fy*rz-fz*ry;
        double uy=fz*rx-fx*rz;
        double uz=fx*ry-fy*rx;
        double dx=pointX-camera.x();
        double dy=pointY-camera.y();
        double dz=pointZ-camera.z();
        double depth=dx*fx+dy*fy+dz*fz;
        double horizontal=dx*rx+dy*ry+dz*rz;
        double vertical=dx*ux+dy*uy+dz*uz;
        double halfHeight=depth*Math.tan(Math.toRadians(31d));
        double halfWidth=halfHeight*(16d/9d);
        assertTrue("every player part must remain between near and far clip planes",
            depth+radius>.1d && depth-radius<120d);
        assertTrue("player part must remain horizontally visible",Math.abs(horizontal)<=halfWidth+radius);
        assertTrue("player part must remain vertically visible",Math.abs(vertical)<=halfHeight+radius);
    }
}
