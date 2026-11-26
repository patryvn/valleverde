package es.valledecernego.game;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class DepthBufferPolicyTest {
    @Test public void prefersTwentyFourBitDepthAndFallsBackToSixteen() {
        assertArrayEquals(new int[]{24, 16}, DepthBufferPolicy.preferredDepthBits());
    }

    @Test public void callersCannotMutateTheSharedDepthPreference() {
        int[] first = DepthBufferPolicy.preferredDepthBits();
        first[0] = 8;
        assertArrayEquals(new int[]{24, 16}, DepthBufferPolicy.preferredDepthBits());
    }
}
