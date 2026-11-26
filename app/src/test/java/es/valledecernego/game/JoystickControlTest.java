package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class JoystickControlTest {
    @Test public void touchAtTheBaseProducesNoMovement() {
        JoystickControl.Vector value=JoystickControl.fromTouch(100f,200f,100f,200f,60f);
        assertEquals(0f,value.x,0.0001f);
        assertEquals(0f,value.y,0.0001f);
        assertFalse(JoystickControl.isMoving(value.x,value.y));
    }

    @Test public void horizontalDragMapsToFullRightInput() {
        JoystickControl.Vector value=JoystickControl.fromTouch(160f,200f,100f,200f,60f);
        assertEquals(1f,value.x,0.0001f);
        assertEquals(0f,value.y,0.0001f);
        assertTrue(JoystickControl.isMoving(value.x,value.y));
    }

    @Test public void dragOutsideRangeIsClamped() {
        JoystickControl.Vector value=JoystickControl.fromTouch(400f,-200f,100f,200f,60f);
        assertEquals(1f,value.x,0.0001f);
        assertEquals(-1f,value.y,0.0001f);
    }

    @Test public void nonPositiveOrNonFiniteRangeCannotCreateNanInput() {
        JoystickControl.Vector zero=JoystickControl.fromTouch(10f,10f,0f,0f,0f);
        assertEquals(0f,zero.x,0f);
        assertEquals(0f,zero.y,0f);
        JoystickControl.Vector invalid=JoystickControl.fromTouch(Float.NaN,10f,0f,0f,20f);
        assertEquals(0f,invalid.x,0f);
        assertEquals(0f,invalid.y,0f);
    }

    @Test public void deadZoneRejectsTinyInputAndAcceptsRealDrag() {
        assertFalse(JoystickControl.isMoving(.04f,.04f));
        assertTrue(JoystickControl.isMoving(.09f,0f));
    }
}
